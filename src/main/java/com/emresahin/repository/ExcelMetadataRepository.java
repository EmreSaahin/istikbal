package com.emresahin.repository;

import com.emresahin.model.ExcelMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExcelMetadataRepository extends JpaRepository<ExcelMetadata, Long> {
    Optional<ExcelMetadata> findTopByOrderByIdDesc();
}
