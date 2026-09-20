package com.emresahin.controller;

import com.emresahin.dto.AiAnalysisRequestDto;
import com.emresahin.dto.AiAnalysisResponseDto;
import com.emresahin.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<AiAnalysisResponseDto> analyzeExcel(@RequestBody(required = false) AiAnalysisRequestDto request) {
        if (request == null) {
            request = new AiAnalysisRequestDto();
        }
        return ResponseEntity.ok(aiAnalysisService.analyzeExcelData(request));
    }
}
