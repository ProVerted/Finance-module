package com.provert.payroll.overtime.repository;

import com.provert.payroll.overtime.entity.OvertimeRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OvertimeRecordRepository extends JpaRepository<OvertimeRecord, Long> {

    Optional<OvertimeRecord> findByEmployeeIdAndYearAndMonth(Long employeeId, Integer year, Integer month);

    @Query("SELECT o FROM OvertimeRecord o " +
           "WHERE (:year IS NULL OR o.year = :year) " +
           "AND (:month IS NULL OR o.month = :month) " +
           "AND (:approved IS NULL OR o.approved = :approved) " +
           "AND (:departmentId IS NULL OR o.employee.department.id = :departmentId)")
    Page<OvertimeRecord> findAllFiltered(
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("approved") Boolean approved,
            @Param("departmentId") Long departmentId,
            Pageable pageable
    );
}
