package com.emresahin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.emresahin.dto.AiAnalysisRequestDto;
import com.emresahin.dto.AiAnalysisResponseDto;
import com.emresahin.model.DynamicExcelData;
import com.emresahin.model.ExcelMetadata;
import com.emresahin.repository.DynamicExcelDataRepository;
import com.emresahin.repository.ExcelMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final ExcelMetadataRepository excelMetadataRepository;
    private final DynamicExcelDataRepository dynamicExcelDataRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${n8n.ai.webhook.url:http://localhost:5678/webhook-test/api/ai/analyze}")
    private String n8nAiWebhookUrl;

    public AiAnalysisResponseDto analyzeExcelData(AiAnalysisRequestDto request) {
        Optional<ExcelMetadata> metaOpt = excelMetadataRepository.findTopByOrderByIdDesc();
        if (metaOpt.isEmpty()) {
            return AiAnalysisResponseDto.builder()
                    .success(false)
                    .message("Sistemde aktif bir Excel dosyası bulunamadı. Lütfen önce bir Excel dosyası yükleyin.")
                    .build();
        }

        ExcelMetadata metadata = metaOpt.get();
        List<String> headers = Collections.emptyList();
        try {
            headers = objectMapper.readValue(metadata.getHeadersJson(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            headers = Collections.emptyList();
        }

        int limit = (request != null && request.getSampleRowsLimit() != null && request.getSampleRowsLimit() > 0)
                ? request.getSampleRowsLimit() : 50;

        List<DynamicExcelData> dbRecords = dynamicExcelDataRepository
                .findAllByOrderByIdAsc(PageRequest.of(0, limit))
                .getContent();

        List<Map<String, Object>> sampleRecords = new ArrayList<>();
        for (DynamicExcelData record : dbRecords) {
            try {
                Map<String, Object> rowMap = objectMapper.readValue(record.getDataJson(), new TypeReference<Map<String, Object>>() {});
                sampleRecords.add(rowMap);
            } catch (Exception ignored) {}
        }

        String userPrompt = (request != null && request.getPrompt() != null && !request.getPrompt().isBlank())
                ? request.getPrompt().trim()
                : "Excel verisini analiz et, sayısal istatistikleri, önemli eğilimleri ve yönetici özetini çıkar.";

        Map<String, Object> summaryStats = calculateQuickStats(sampleRecords, headers);

        Map<String, Object> n8nPayload = new LinkedHashMap<>();
        n8nPayload.put("fileName", metadata.getFileName());
        n8nPayload.put("totalRows", metadata.getTotalRows());
        n8nPayload.put("headers", headers);
        n8nPayload.put("sampleRecordsCount", sampleRecords.size());
        n8nPayload.put("sampleRecords", sampleRecords);
        n8nPayload.put("summaryStats", summaryStats);
        n8nPayload.put("userPrompt", userPrompt);

        System.out.println("\n======================================================================");
        System.out.println("🤖 [N8N AI EXCEL ANALYSIS DISPATCH INITIATED]");
        System.out.println("📌 Target AI Webhook URL : " + n8nAiWebhookUrl);
        System.out.println("📄 File Name             : " + metadata.getFileName());
        System.out.println("📊 Total Rows            : " + metadata.getTotalRows());
        System.out.println("💬 User Prompt           : " + userPrompt);
        System.out.println("----------------------------------------------------------------------");

        try {
            String jsonPayload = objectMapper.writeValueAsString(n8nPayload);
            return dispatchToN8nAi(n8nAiWebhookUrl, jsonPayload, metadata.getFileName(), metadata.getTotalRows(), headers, summaryStats);
        } catch (Exception e) {
            System.err.println("❌ [N8N AI DISPATCH ERROR]: " + e.getMessage());
            return AiAnalysisResponseDto.builder()
                    .success(false)
                    .message("n8n AI Webhook gönderim hatası: " + e.getMessage())
                    .fileName(metadata.getFileName())
                    .totalRows(metadata.getTotalRows())
                    .headers(headers)
                    .summaryStats(summaryStats)
                    .build();
        }
    }

    private AiAnalysisResponseDto dispatchToN8nAi(String primaryUrl, String jsonPayload, String fileName, long totalRows, List<String> headers, Map<String, Object> summaryStats) {
        Set<String> targetUrls = new LinkedHashSet<>();
        targetUrls.add(primaryUrl);

        if (primaryUrl.contains("webhook-test")) {
            targetUrls.add(primaryUrl.replace("webhook-test", "webhook"));
        } else if (primaryUrl.contains("/webhook/")) {
            targetUrls.add(primaryUrl.replace("/webhook/", "/webhook-test/"));
        }
        if (primaryUrl.contains("localhost")) {
            targetUrls.add(primaryUrl.replace("localhost", "127.0.0.1"));
            if (primaryUrl.contains("webhook-test")) {
                targetUrls.add(primaryUrl.replace("localhost", "127.0.0.1").replace("webhook-test", "webhook"));
            }
        }

        String lastErrorMsg = "n8n AI sunucusuna ulaşılamadı.";

        for (String targetUrl : targetUrls) {
            try {
                System.out.println("📡 Sending HTTP POST to n8n AI: " + targetUrl);
                URL url = new URL(targetUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("User-Agent", "Spring-Boot-Istikbal-Portal/1.0");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(30000); // 30 seconds timeout for AI Model inference
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    String responseBody = "";
                    if (conn.getInputStream() != null) {
                        responseBody = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    }
                    System.out.println("✅ [N8N AI DISPATCH SUCCESS] Target: " + targetUrl + " | HTTP Code: " + responseCode);
                    System.out.println("======================================================================\n");

                    String extractedAnalysisText = parseAiAnalysisText(responseBody);

                    return AiAnalysisResponseDto.builder()
                            .success(true)
                            .message("AI analizi n8n üzerinden başarıyla tamamlandı.")
                            .fileName(fileName)
                            .totalRows(totalRows)
                            .headers(headers)
                            .analysisResult(extractedAnalysisText)
                            .summaryStats(summaryStats)
                            .n8nWebhookUrl(targetUrl)
                            .build();
                } else if (responseCode == 404) {
                    lastErrorMsg = "n8n AI Webhook dinlenmiyor (HTTP 404). n8n üzerinde AI workflow 'Listen for test event' butonuna basın.";
                    System.out.println("⚠️ [n8n AI Webhook Notice] URL: " + targetUrl + " returned HTTP 404");
                } else {
                    lastErrorMsg = "n8n AI workflow hata yanıtı döndürdü (HTTP " + responseCode + ").";
                    System.out.println("⚠️ [n8n AI Webhook Response] URL: " + targetUrl + " | HTTP Code: " + responseCode);
                }
            } catch (java.net.SocketTimeoutException e) {
                lastErrorMsg = "n8n AI model yanıt süresi zaman aşımına uğradı (30s timeout).";
                System.out.println("⚠️ [n8n AI Attempt Failed] URL: " + targetUrl + " (request timed out)");
            } catch (Exception e) {
                lastErrorMsg = "n8n AI Bağlantı hatası: " + e.getMessage();
                System.out.println("⚠️ [n8n AI Attempt Failed] URL: " + targetUrl + " (" + e.getMessage() + ")");
            }
        }

        System.out.println("❌ [N8N AI DISPATCH FAILED ON ALL URLS]");
        System.out.println("======================================================================\n");

        return AiAnalysisResponseDto.builder()
                .success(false)
                .message("AI Analizi Başarısız: " + lastErrorMsg)
                .fileName(fileName)
                .totalRows(totalRows)
                .headers(headers)
                .summaryStats(summaryStats)
                .n8nWebhookUrl(primaryUrl)
                .build();
    }

    private String parseAiAnalysisText(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "n8n AI tarafından boş yanıt döndürüldü.";
        }
        try {
            Map<String, Object> resMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            if (resMap.containsKey("analysis")) {
                return String.valueOf(resMap.get("analysis"));
            } else if (resMap.containsKey("text")) {
                return String.valueOf(resMap.get("text"));
            } else if (resMap.containsKey("output")) {
                return String.valueOf(resMap.get("output"));
            } else if (resMap.containsKey("message")) {
                return String.valueOf(resMap.get("message"));
            }
        } catch (Exception ignored) {}
        return responseBody;
    }

    private Map<String, Object> calculateQuickStats(List<Map<String, Object>> records, List<String> headers) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRecordsCount", records.size());
        stats.put("columnCount", headers.size());

        if (records.isEmpty()) return stats;

        Map<String, Double> numericSums = new LinkedHashMap<>();
        for (String col : headers) {
            double sum = 0.0;
            boolean isNumeric = true;
            int validCount = 0;

            for (Map<String, Object> row : records) {
                Object val = row.get(col);
                if (val != null) {
                    try {
                        double dVal = Double.parseDouble(val.toString().replaceAll("[^0-9.-]", ""));
                        sum += dVal;
                        validCount++;
                    } catch (Exception e) {
                        isNumeric = false;
                        break;
                    }
                }
            }

            if (isNumeric && validCount > 0) {
                numericSums.put(col, sum);
            }
        }

        stats.put("numericColumnSums", numericSums);
        return stats;
    }
}
