package com.emresahin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.emresahin.dto.SendMailRequestDto;
import com.emresahin.model.EmailRecipient;
import com.emresahin.repository.EmailRecipientRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class EmailRecipientService {

    private final EmailRecipientRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${n8n.webhook.url:http://localhost:5678/webhook-test/api/mail/send}")
    private String n8nWebhookUrl;

    @PostConstruct
    public void initDefaultRecipients() {
        if (repository.count() == 0) {
            List<EmailRecipient> defaults = List.of(
                    EmailRecipient.builder().fullName("Emre Şahin").email("emre.sahin@istikbal.com").department("SAP Operations").status("ACTIVE").build(),
                    EmailRecipient.builder().fullName("Ramazan Mert Ural").email("mert.ural@istikbal.com").department("Logistics Manager").status("ACTIVE").build(),
                    EmailRecipient.builder().fullName("Güleser Kaba").email("guleser.kaba@istikbal.com").department("Finance Director").status("ACTIVE").build(),
                    EmailRecipient.builder().fullName("Harun Dervişoğlu").email("harun.dervis@istikbal.com").department("Warehouse Supervisor").status("ACTIVE").build(),
                    EmailRecipient.builder().fullName("Burak Millidere").email("burak.millidere@istikbal.com").department("Supply Chain Analyst").status("ACTIVE").build()
            );
            repository.saveAll(defaults);
        }
    }

    public List<EmailRecipient> getAllRecipients() {
        return repository.findAll();
    }

    @Transactional
    public EmailRecipient addRecipient(EmailRecipient recipient) {
        if (recipient.getStatus() == null || recipient.getStatus().isEmpty()) {
            recipient.setStatus("ACTIVE");
        }
        return repository.save(recipient);
    }

    @Transactional
    public void deleteRecipient(Long id) {
        repository.deleteById(id);
    }

    public Map<String, Object> sendMailViaN8n(SendMailRequestDto dto) {
        // Extract strictly only email, title, and message for n8n payload
        String email = dto.getEmail() != null ? dto.getEmail() : dto.getRecipientEmail();
        String title = dto.getTitle() != null ? dto.getTitle() : dto.getSubject();
        String message = dto.getMessage() != null ? dto.getMessage() : "";

        // Minimal JSON payload with ONLY 3 keys: email, title, message
        Map<String, String> n8nPayload = new LinkedHashMap<>();
        n8nPayload.put("email", email);
        n8nPayload.put("title", title);
        n8nPayload.put("message", message);

        System.out.println("\n======================================================================");
        System.out.println("🚀 [N8N MAIL DISPATCH INITIATED - HTTP POST]");
        System.out.println("📌 Target Webhook URL : " + n8nWebhookUrl);
        System.out.println("📧 email              : " + email);
        System.out.println("📌 title              : " + title);
        System.out.println("💬 message            : " + message);
        System.out.println("----------------------------------------------------------------------");

        try {
            String jsonPayload = objectMapper.writeValueAsString(n8nPayload);
            return dispatchPost(n8nWebhookUrl, jsonPayload, email);
        } catch (Exception e) {
            System.err.println("❌ [N8N DISPATCH ERROR]: " + e.getMessage());
            Map<String, Object> errorRes = new HashMap<>();
            errorRes.put("success", false);
            errorRes.put("message", "Mail gönderimi başarısız: " + e.getMessage());
            errorRes.put("email", email);
            return errorRes;
        }
    }

    private Map<String, Object> dispatchPost(String primaryUrl, String jsonPayload, String email) {
        Map<String, Object> response = new HashMap<>();
        Set<String> targetUrls = new LinkedHashSet<>();
        targetUrls.add(primaryUrl);

        // Include fallback URL variations (test vs production, localhost vs 127.0.0.1)
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

        String lastErrorMsg = "n8n sunucusuna ulaşılamadı veya webhook dinlenmiyor.";

        for (String targetUrl : targetUrls) {
            try {
                System.out.println("📡 Sending HTTP POST to n8n: " + targetUrl);
                URL url = new URL(targetUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("User-Agent", "Spring-Boot-Istikbal-Portal/1.0");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(12000); // 12 seconds read timeout for n8n workflow execution
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
                    System.out.println("✅ [N8N DISPATCH SUCCESS] Target: " + targetUrl + " | HTTP Code: " + responseCode);
                    if (!responseBody.isBlank()) {
                        System.out.println("📩 n8n Response: " + responseBody);
                    }
                    System.out.println("======================================================================\n");

                    response.put("success", true);
                    response.put("message", "Mail başarıyla gönderildi (" + email + ")");
                    response.put("email", email);
                    response.put("webhookUrl", targetUrl);
                    response.put("n8nResponse", responseBody);
                    return response;
                } else if (responseCode == 404) {
                    lastErrorMsg = "Webhook dinlenmiyor (HTTP 404). n8n üzerinde 'Listen for test event' butonuna basın.";
                    System.out.println("⚠️ [n8n Webhook Notice] URL: " + targetUrl + " returned HTTP 404 (Webhook not listening)");
                } else {
                    lastErrorMsg = "n8n hata yanıtı döndürdü (HTTP " + responseCode + ").";
                    System.out.println("⚠️ [n8n Webhook Response] URL: " + targetUrl + " | HTTP Code: " + responseCode);
                }
            } catch (java.net.SocketTimeoutException e) {
                lastErrorMsg = "n8n yanıt zaman aşımına uğradı (Request timed out).";
                System.out.println("⚠️ [n8n Connection Attempt Failed] URL: " + targetUrl + " (request timed out)");
            } catch (Exception e) {
                lastErrorMsg = "Bağlantı hatası: " + e.getMessage();
                System.out.println("⚠️ [n8n Connection Attempt Failed] URL: " + targetUrl + " (" + e.getMessage() + ")");
            }
        }

        System.out.println("❌ [N8N DISPATCH FAILED ON ALL URLS]");
        System.out.println("💡 CHECKLIST TO RESOLVE:");
        System.out.println("   1. Open n8n workflow (http://localhost:5678)");
        System.out.println("   2. In Webhook Node, click 'Listen for test event'");
        System.out.println("   3. In Webhook Node, set Respond option to 'Immediately' or 'When Last Node Finishes'");
        System.out.println("======================================================================\n");

        response.put("success", false);
        response.put("message", "Mail gönderimi başarısız: " + lastErrorMsg);
        response.put("email", email);
        return response;
    }
}
