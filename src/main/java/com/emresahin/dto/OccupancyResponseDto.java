package com.emresahin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OccupancyResponseDto {
    private Long recordId;
    private String materialDescription;

    private Double volume;
    private Double totalVolume;
    private Double volumeOccupancyPercentage;
    
    private Double grossSales;
    private Double netSales;
    private Double financialYieldPercentage; 
    
    private Double unitVolumeLiters;  
}
