package com.provert.payroll.overtime.service;

import com.provert.payroll.attendance.entity.AttendanceRecord;
import com.provert.payroll.attendance.repository.AttendanceRecordRepository;
import com.provert.payroll.common.exception.ConflictException;
import com.provert.payroll.common.exception.ResourceNotFoundException;
import com.provert.payroll.config.entity.WorkingHourRule;
import com.provert.payroll.config.service.WorkingHourRuleService;
import com.provert.payroll.employee.entity.Employee;
import com.provert.payroll.employee.repository.EmployeeRepository;
import com.provert.payroll.overtime.dto.OvertimeRecordResponse;
import com.provert.payroll.overtime.entity.OvertimeRecord;
import com.provert.payroll.overtime.repository.OvertimeRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OvertimeService {

    private final OvertimeRecordRepository overtimeRecordRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkingHourRuleService workingHourRuleService;

    @Value("${payroll.overtime.monthly-standard-hours:208}")
    private int monthlyStandardHours;

    @Transactional
    public List<OvertimeRecordResponse> calculateOvertime(Integer year, Integer month, Long employeeId) {
        List<Employee> targets;
        if (employeeId != null) {
            Employee emp = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
            targets = Collections.singletonList(emp);
        } else {
            targets = employeeRepository.findAllByActive(true);
        }

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return targets.stream()
                .map(emp -> calculateForEmployee(emp, year, month, startDate, endDate))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private OvertimeRecord calculateForEmployee(Employee employee, Integer year, Integer month, LocalDate startDate, LocalDate endDate) {
        OvertimeRecord existing = overtimeRecordRepository.findByEmployeeIdAndYearAndMonth(employee.getId(), year, month)
                .orElse(null);

        if (existing != null && Boolean.TRUE.equals(existing.getApproved())) {
            throw new ConflictException(
                    "Overtime record for employee " + employee.getId() + " (" + year + "/" + month + ") is already approved and locked.",
                    "OT_ALREADY_APPROVED"
            );
        }

        List<AttendanceRecord> attendances = attendanceRecordRepository.findByEmployeeIdAndAttendanceDateBetween(employee.getId(), startDate, endDate);

        long totalOtMinutes = 0L;
        BigDecimal totalOtAmount = BigDecimal.ZERO;

        BigDecimal hourlyRate = employee.getBasicSalary()
                .divide(BigDecimal.valueOf(monthlyStandardHours), 4, RoundingMode.HALF_UP);
        BigDecimal minuteRate = hourlyRate.divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);

        for (AttendanceRecord att : attendances) {
            if (att.getCheckIn() == null || att.getCheckOut() == null) {
                continue;
            }

            WorkingHourRule rule = workingHourRuleService.getRuleByDayTypeEntity(att.getDayType());
            LocalTime expectedIn = rule != null ? rule.getExpectedIn() : LocalTime.of(9, 0);
            LocalTime expectedOut = rule != null ? rule.getExpectedOut() : LocalTime.of(17, 0);
            boolean otEligible = rule == null || Boolean.TRUE.equals(rule.getOtEligible());
            BigDecimal multiplier = (rule != null && rule.getOtMultiplier() != null) ? rule.getOtMultiplier() : BigDecimal.ONE;

            if (!otEligible) {
                continue;
            }

            long workedMinutes = Duration.between(att.getCheckIn(), att.getCheckOut()).toMinutes();
            long expectedMinutes = Duration.between(expectedIn, expectedOut).toMinutes();

            long otMinutes = Math.max(0, workedMinutes - expectedMinutes);
            if (otMinutes > 0) {
                totalOtMinutes += otMinutes;
                BigDecimal dayOtAmount = minuteRate
                        .multiply(BigDecimal.valueOf(otMinutes))
                        .multiply(multiplier);
                totalOtAmount = totalOtAmount.add(dayOtAmount);
            }
        }

        OvertimeRecord record = existing != null ? existing : new OvertimeRecord();
        record.setEmployee(employee);
        record.setYear(year);
        record.setMonth(month);
        record.setTotalOtMinutes(totalOtMinutes);
        record.setHourlyRate(hourlyRate.setScale(2, RoundingMode.HALF_UP));
        record.setOtAmount(totalOtAmount.setScale(2, RoundingMode.HALF_UP));
        record.setApproved(false);

        return overtimeRecordRepository.save(record);
    }

    @Transactional
    public OvertimeRecordResponse approveOvertime(Long id, String approvedBy) {
        OvertimeRecord record = overtimeRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Overtime record not found with id: " + id));

        if (Boolean.TRUE.equals(record.getApproved())) {
            throw new ConflictException("Overtime record is already approved", "OT_ALREADY_APPROVED");
        }

        record.setApproved(true);
        record.setApprovedBy(approvedBy);
        record.setApprovedAt(LocalDateTime.now());

        OvertimeRecord saved = overtimeRecordRepository.save(record);
        return mapToResponse(saved);
    }

    public Page<OvertimeRecordResponse> getOvertimeRecords(Integer year, Integer month, Boolean approved, Long departmentId, Pageable pageable) {
        return overtimeRecordRepository.findAllFiltered(year, month, approved, departmentId, pageable)
                .map(this::mapToResponse);
    }

    public OvertimeRecordResponse getOvertimeRecordById(Long id) {
        OvertimeRecord record = overtimeRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Overtime record not found with id: " + id));
        return mapToResponse(record);
    }

    public OvertimeRecordResponse mapToResponse(OvertimeRecord record) {
        return OvertimeRecordResponse.builder()
                .id(record.getId())
                .employeeId(record.getEmployee().getId())
                .employeeCode(record.getEmployee().getEmployeeCode())
                .employeeName(record.getEmployee().getFullName())
                .year(record.getYear())
                .month(record.getMonth())
                .totalOtMinutes(record.getTotalOtMinutes())
                .hourlyRate(record.getHourlyRate())
                .otAmount(record.getOtAmount())
                .approved(record.getApproved())
                .approvedBy(record.getApprovedBy())
                .approvedAt(record.getApprovedAt())
                .build();
    }
}
