package com.emresahin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.emresahin.dto.DynamicExcelResponseDto;
import com.emresahin.dto.OccupancyResponseDto;
import com.emresahin.dto.SapReportDto;
import com.emresahin.model.DynamicExcelData;
import com.emresahin.model.ExcelMetadata;
import com.emresahin.model.SapReport;
import com.emresahin.repository.DynamicExcelDataRepository;
import com.emresahin.repository.ExcelMetadataRepository;
import com.emresahin.repository.SapReportRepository;
import com.emresahin.utils.ExcelHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SapReportService {

    private final SapReportRepository sapReportRepository;
    private final ExcelMetadataRepository excelMetadataRepository;
    private final DynamicExcelDataRepository dynamicExcelDataRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.excel.file-path}")
    private String excelFilePath;

    @Transactional
    public int importExcelData() {

        List<SapReport> reports = ExcelHelper.excelToSapReports(excelFilePath);
        sapReportRepository.deleteAll();

        int batchSize = 1000;
        for (int i = 0; i < reports.size(); i += batchSize) {
            List<SapReport> batch = reports.subList(i, Math.min(i + batchSize, reports.size()));
            sapReportRepository.saveAll(batch);
        }

        return reports.size();
    }

    @Transactional
    public DynamicExcelResponseDto importUploadedExcel(org.springframework.web.multipart.MultipartFile file) {
        try {
            ExcelHelper.DynamicExcelResult result = ExcelHelper.parseInputStream(file.getInputStream());
            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "Uploaded_Excel.xlsx";

            excelMetadataRepository.deleteAll();
            dynamicExcelDataRepository.deleteAll();
            sapReportRepository.deleteAll();

            ExcelMetadata metadata = ExcelMetadata.builder()
                    .fileName(fileName)
                    .headersJson(objectMapper.writeValueAsString(result.getHeaders()))
                    .totalRows(result.getRows().size())
                    .uploadedAt(LocalDateTime.now())
                    .build();
            excelMetadataRepository.save(metadata);

            List<Map<String, Object>> allRows = result.getRows();
            List<DynamicExcelData> dynamicRecords = new ArrayList<>();
            for (int i = 0; i < allRows.size(); i++) {
                Map<String, Object> rowMap = allRows.get(i);
                DynamicExcelData record = DynamicExcelData.builder()
                        .fileName(fileName)
                        .rowIndex(i + 1)
                        .dataJson(objectMapper.writeValueAsString(rowMap))
                        .uploadedAt(LocalDateTime.now())
                        .build();
                dynamicRecords.add(record);
            }

            int batchSize = 1000;
            for (int i = 0; i < dynamicRecords.size(); i += batchSize) {
                List<DynamicExcelData> batch = dynamicRecords.subList(i, Math.min(i + batchSize, dynamicRecords.size()));
                dynamicExcelDataRepository.saveAll(batch);
            }

            List<SapReport> reports = result.getSapReports();
            if (reports != null && !reports.isEmpty()) {
                for (int i = 0; i < reports.size(); i += batchSize) {
                    List<SapReport> batch = reports.subList(i, Math.min(i + batchSize, reports.size()));
                    sapReportRepository.saveAll(batch);
                }
            }

            int pageSize = 10;
            int total = allRows.size();

            return DynamicExcelResponseDto.builder()
                    .success(true)
                    .message("Excel file '" + fileName + "' successfully parsed & saved into PostgreSQL (" + total + " rows).")
                    .headers(result.getHeaders())
                    .rows(allRows)
                    .totalElements(total)
                    .totalPages((int) Math.ceil((double) total / pageSize))
                    .currentPage(0)
                    .pageSize(pageSize)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload and parse Excel file: " + e.getMessage(), e);
        }
    }

    public DynamicExcelResponseDto getDynamicExcelPage(Pageable pageable) {
        Optional<ExcelMetadata> metaOpt = excelMetadataRepository.findTopByOrderByIdDesc();
        if (metaOpt.isEmpty()) {
            return DynamicExcelResponseDto.builder()
                    .success(true)
                    .message("No active Excel file found in PostgreSQL.")
                    .headers(Collections.emptyList())
                    .rows(Collections.emptyList())
                    .totalElements(0)
                    .totalPages(0)
                    .currentPage(pageable.getPageNumber())
                    .pageSize(pageable.getPageSize())
                    .build();
        }

        ExcelMetadata meta = metaOpt.get();
        List<String> headers = Collections.emptyList();
        try {
            headers = objectMapper.readValue(meta.getHeadersJson(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            headers = Collections.emptyList();
        }

        Page<DynamicExcelData> pageRecords = dynamicExcelDataRepository.findAllByOrderByIdAsc(pageable);
        List<Map<String, Object>> rows = new ArrayList<>();

        for (DynamicExcelData record : pageRecords.getContent()) {
            try {
                Map<String, Object> map = objectMapper.readValue(record.getDataJson(), new TypeReference<Map<String, Object>>() {});
                rows.add(map);
            } catch (Exception ignored) {}
        }

        return DynamicExcelResponseDto.builder()
                .success(true)
                .message("Fetched page " + pageable.getPageNumber() + " from PostgreSQL.")
                .headers(headers)
                .rows(rows)
                .totalElements(pageRecords.getTotalElements())
                .totalPages(pageRecords.getTotalPages())
                .currentPage(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .build();
    }

    public Page<SapReportDto> getAllReports(Pageable pageable) {
        return sapReportRepository.findAll(pageable)
                .map(this::convertToDto);
    }

    public SapReportDto getReportById(Long id) {
        SapReport report = sapReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SAP Report record not found with ID: " + id));
        return convertToDto(report);
    }

    @Transactional
    public SapReportDto updateReport(Long id, SapReportDto dto) {
        SapReport report = sapReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SAP Report record not found with ID: " + id));

        // Update fields
        report.setYear(dto.getYear());
        report.setMonth(dto.getMonth());
        report.setMaterialNumber(dto.getMaterialNumber());
        report.setMaterialDescription(dto.getMaterialDescription());
        report.setPeriod(dto.getPeriod());
        report.setBrand(dto.getBrand());
        report.setSize(dto.getSize());
        report.setPack(dto.getPack());
        report.setClient(dto.getClient());
        report.setClientType(dto.getClientType());
        report.setVolume(dto.getVolume());
        report.setGrossSales(dto.getGrossSales());
        report.setDiscounts(dto.getDiscounts());
        report.setNetSales(dto.getNetSales());
        report.setCostOfGoodsSold(dto.getCostOfGoodsSold());
        report.setDistribution(dto.getDistribution());
        report.setWarehousing(dto.getWarehousing());

        SapReport updated = sapReportRepository.save(report);
        return convertToDto(updated);
    }

    public OccupancyResponseDto getOccupancyAnalysis(Long id) {
        SapReport report = sapReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SAP Report record not found with ID: " + id));

        Double totalVolume = sapReportRepository.sumTotalVolume();
        if (totalVolume == null || totalVolume == 0) {
            totalVolume = 1.0;
        }

        Double reportVolume = report.getVolume() != null ? report.getVolume() : 0.0;
        double volumeOccupancyPercentage = (reportVolume / totalVolume) * 100.0;

        Double grossSales = report.getGrossSales() != null ? report.getGrossSales() : 0.0;
        Double netSales = report.getNetSales() != null ? report.getNetSales() : 0.0;
        double financialYieldPercentage = (grossSales > 0) ? (netSales / grossSales) * 100.0 : 0.0;

        double unitVolumeLiters = 0.0;
        try {
            double parsedSize = parseNumericValue(report.getSize());
            double parsedPack = parseNumericValue(report.getPack());
            if (parsedSize > 0 && parsedPack > 0) {
                unitVolumeLiters = parsedSize * parsedPack;
            } else if (parsedSize > 0) {
                unitVolumeLiters = parsedSize;
            }
        } catch (Exception ignored) {
        }

        return OccupancyResponseDto.builder()
                .recordId(report.getId())
                .materialDescription(report.getMaterialDescription())
                .volume(reportVolume)
                .totalVolume(totalVolume)
                .volumeOccupancyPercentage(volumeOccupancyPercentage)
                .grossSales(grossSales)
                .netSales(netSales)
                .financialYieldPercentage(financialYieldPercentage)
                .unitVolumeLiters(unitVolumeLiters)
                .build();
    }

    private double parseNumericValue(String text) {
        if (text == null || text.trim().isEmpty()) return 0.0;
        String digits = text.replaceAll("[^0-9.]", "");
        if (digits.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(digits);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private SapReportDto convertToDto(SapReport report) {
        return SapReportDto.builder()
                .id(report.getId())
                .year(report.getYear())
                .month(report.getMonth())
                .materialNumber(report.getMaterialNumber())
                .materialDescription(report.getMaterialDescription())
                .period(report.getPeriod())
                .brand(report.getBrand())
                .size(report.getSize())
                .pack(report.getPack())
                .client(report.getClient())
                .clientType(report.getClientType())
                .volume(report.getVolume())
                .grossSales(report.getGrossSales())
                .discounts(report.getDiscounts())
                .netSales(report.getNetSales())
                .costOfGoodsSold(report.getCostOfGoodsSold())
                .distribution(report.getDistribution())
                .warehousing(report.getWarehousing())
                .build();
    }
}
