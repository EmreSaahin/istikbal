package com.emresahin.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sap_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SapReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sales_year")
    private Integer year;

    @Column(name = "sales_month")
    private Integer month;

    @Column(name = "material_number")
    private Long materialNumber;

    @Column(name = "material_description")
    private String materialDescription;

    @Column(name = "period")
    private Integer period;

    @Column(name = "brand")
    private String brand;

    @Column(name = "size_info")
    private String size;

    @Column(name = "pack_info")
    private String pack;

    @Column(name = "client")
    private String client;

    @Column(name = "client_type")
    private String clientType;

    @Column(name = "volume")
    private Double volume;

    @Column(name = "gross_sales")
    private Double grossSales;

    @Column(name = "discounts")
    private Double discounts;

    @Column(name = "net_sales")
    private Double netSales;

    @Column(name = "cost_of_goods_sold")
    private Double costOfGoodsSold;

    @Column(name = "distribution")
    private Double distribution;

    @Column(name = "warehousing")
    private Double warehousing;
}
