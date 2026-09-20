package com.emresahin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResponseDto {
    private boolean success;
    private String message;
    private String fileName;
    private long totalRows;
    private List<String> headers;
    private String analysisResult;
    private Map<String, Object> summaryStats;
    private String n8nWebhookUrl;
}
