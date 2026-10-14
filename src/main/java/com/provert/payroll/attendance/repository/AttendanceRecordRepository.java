package com.provert.payroll.attendance.repository;

import com.provert.payroll.attendance.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    boolean existsByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    @Query("SELECT a FROM AttendanceRecord a " +
           "WHERE (:employeeId IS NULL OR a.employee.id = :employeeId) " +
           "AND (:dateFrom IS NULL OR a.attendanceDate >= :dateFrom) " +
           "AND (:dateTo IS NULL OR a.attendanceDate <= :dateTo) " +
           "ORDER BY a.attendanceDate ASC")
    List<AttendanceRecord> findFiltered(
            @Param("employeeId") Long employeeId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo
    );

    List<AttendanceRecord> findByEmployeeIdAndAttendanceDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);
}
