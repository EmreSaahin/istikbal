package com.emresahin.controller;

import com.emresahin.dto.DynamicExcelResponseDto;
import com.emresahin.dto.OccupancyResponseDto;
import com.emresahin.dto.SapReportDto;
import com.emresahin.service.SapReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
public class SapReportController {

    private final SapReportService sapReportService;

    @PostMapping("/upload")
    public ResponseEntity<DynamicExcelResponseDto> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            DynamicExcelResponseDto dto = sapReportService.importUploadedExcel(file);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    DynamicExcelResponseDto.builder()
                            .success(false)
                            .message("Excel upload failed: " + e.getMessage())
                            .build()
            );
        }
    }

    @GetMapping("/dynamic")
    public ResponseEntity<DynamicExcelResponseDto> getDynamicExcelPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(sapReportService.getDynamicExcelPage(pageable));
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importExcel() {
        Map<String, Object> response = new HashMap<>();
        try {
            int importedCount = sapReportService.importExcelData();
            response.put("success", true);
            response.put("message", "Excel data successfully imported.");
            response.put("recordsImported", importedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Excel import failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping
    public ResponseEntity<Page<SapReportDto>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(sapReportService.getAllReports(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SapReportDto> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(sapReportService.getReportById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<SapReportDto> update(@PathVariable Long id, @RequestBody SapReportDto dto) {
        try {
            return ResponseEntity.ok(sapReportService.updateReport(id, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/occupancy")
    public ResponseEntity<OccupancyResponseDto> getOccupancy(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(sapReportService.getOccupancyAnalysis(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
