package com.emresahin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SapReportDto {
    private Long id;
    private Integer year;
    private Integer month;
    private Long materialNumber;
    private String materialDescription;
    private Integer period;
    private String brand;
    private String size;
    private String pack;
    private String client;
    private String clientType;
    private Double volume;
    private Double grossSales;
    private Double discounts;
    private Double netSales;
    private Double costOfGoodsSold;
    private Double distribution;
    private Double warehousing;
}
