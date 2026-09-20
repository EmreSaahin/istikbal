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
public class DynamicExcelResponseDto {
    private boolean success;
    private String message;
    private List<String> headers;
    private List<Map<String, Object>> rows;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
}
