package com.provert.payroll.payslip.repository;

import com.provert.payroll.payslip.entity.Payslip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    Optional<Payslip> findByEmployeeIdAndYearAndMonth(Long employeeId, Integer year, Integer month);

    @Query("SELECT p FROM Payslip p " +
           "WHERE (:employeeId IS NULL OR p.employee.id = :employeeId) " +
           "AND (:year IS NULL OR p.year = :year) " +
           "AND (:month IS NULL OR p.month = :month)")
    Page<Payslip> findAllFiltered(
            @Param("employeeId") Long employeeId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            Pageable pageable
    );
}
