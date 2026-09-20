package com.emresahin.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dynamic_excel_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicExcelData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "row_index")
    private Integer rowIndex;

    @Column(name = "data_json", columnDefinition = "TEXT", nullable = false)
    private String dataJson;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
}
