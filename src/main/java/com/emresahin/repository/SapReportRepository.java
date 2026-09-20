package com.emresahin.repository;

import com.emresahin.model.SapReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SapReportRepository extends JpaRepository<SapReport, Long> {

    @Query("SELECT SUM(s.volume) FROM SapReport s WHERE s.volume IS NOT NULL")
    Double sumTotalVolume();

    @Query("SELECT SUM(s.grossSales) FROM SapReport s WHERE s.grossSales IS NOT NULL")
    Double sumTotalGrossSales();

    @Query("SELECT SUM(s.netSales) FROM SapReport s WHERE s.netSales IS NOT NULL")
    Double sumTotalNetSales();
}
