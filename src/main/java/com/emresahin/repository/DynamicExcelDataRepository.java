package com.emresahin.repository;

import com.emresahin.model.DynamicExcelData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DynamicExcelDataRepository extends JpaRepository<DynamicExcelData, Long> {
    Page<DynamicExcelData> findAllByOrderByIdAsc(Pageable pageable);
}
