package com.emresahin.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "excel_metadata")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "headers_json", columnDefinition = "TEXT", nullable = false)
    private String headersJson;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
}
