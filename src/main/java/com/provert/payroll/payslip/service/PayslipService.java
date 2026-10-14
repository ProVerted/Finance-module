package com.provert.payroll.payslip.service;

import com.provert.payroll.common.exception.BadRequestException;
import com.provert.payroll.common.exception.ConflictException;
import com.provert.payroll.common.exception.ResourceNotFoundException;
import com.provert.payroll.employee.entity.Employee;
import com.provert.payroll.employee.repository.EmployeeRepository;
import com.provert.payroll.overtime.entity.OvertimeRecord;
import com.provert.payroll.overtime.repository.OvertimeRecordRepository;
import com.provert.payroll.payslip.dto.PayslipResponse;
import com.provert.payroll.payslip.entity.Payslip;
import com.provert.payroll.payslip.repository.PayslipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayslipService {

    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;
    private final OvertimeRecordRepository overtimeRecordRepository;

    private static final BigDecimal EMPLOYEE_EPF_RATE = new BigDecimal("0.08"); // 8%
    private static final BigDecimal EMPLOYER_EPF_RATE = new BigDecimal("0.12"); // 12%
    private static final BigDecimal EMPLOYER_ETF_RATE = new BigDecimal("0.03"); // 3%

    @Transactional
    public List<PayslipResponse> generatePayslips(Integer year, Integer month, Long employeeId, BigDecimal deductions) {
        List<Employee> targets;
        if (employeeId != null) {
            Employee emp = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
            targets = Collections.singletonList(emp);
        } else {
            targets = employeeRepository.findAllByActive(true);
        }

        BigDecimal safeOtherDeductions = deductions != null ? deductions : BigDecimal.ZERO;

        return targets.stream()
                .map(emp -> generateForEmployee(emp, year, month, safeOtherDeductions))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Payslip generateForEmployee(Employee employee, Integer year, Integer month, BigDecimal otherDeductions) {
        Payslip existing = payslipRepository.findByEmployeeIdAndYearAndMonth(employee.getId(), year, month)
                .orElse(null);

        if (existing != null && Boolean.TRUE.equals(existing.getFinalized())) {
            throw new ConflictException("Payslip is already finalized for employee " + employee.getId() + " (" + year + "/" + month + ")", "PAYSLIP_FINALIZED");
        }

        OvertimeRecord otRecord = overtimeRecordRepository.findByEmployeeIdAndYearAndMonth(employee.getId(), year, month)
                .orElseThrow(() -> new BadRequestException(
                        "No overtime record found for employee " + employee.getId() + " for period " + year + "/" + month,
                        "OT_NOT_APPROVED"
                ));

        if (!Boolean.TRUE.equals(otRecord.getApproved())) {
            throw new BadRequestException(
                    "Overtime record for employee " + employee.getId() + " has not been approved yet.",
                    "OT_NOT_APPROVED"
            );
        }

        BigDecimal basicSalary = employee.getBasicSalary();
        BigDecimal otAmount = otRecord.getOtAmount() != null ? otRecord.getOtAmount() : BigDecimal.ZERO;
        BigDecimal grossPay = basicSalary.add(otAmount);

        // Sri Lanka Statutory Calculations
        BigDecimal employeeEpf = basicSalary.multiply(EMPLOYEE_EPF_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal employerEpf = basicSalary.multiply(EMPLOYER_EPF_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal employerEtf = basicSalary.multiply(EMPLOYER_ETF_RATE).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalDeductions = employeeEpf.add(otherDeductions);
        BigDecimal netPay = grossPay.subtract(totalDeductions);

        Payslip payslip = existing != null ? existing : new Payslip();
        payslip.setEmployee(employee);
        payslip.setYear(year);
        payslip.setMonth(month);
        payslip.setBasicSalary(basicSalary);
        payslip.setOtAmount(otAmount);
        payslip.setGrossPay(grossPay);
        payslip.setEmployeeEpf(employeeEpf);
        payslip.setEmployerEpf(employerEpf);
        payslip.setEmployerEtf(employerEtf);
        payslip.setDeductions(otherDeductions);
        payslip.setTotalDeductions(totalDeductions);
        payslip.setNetPay(netPay);
        payslip.setGeneratedAt(LocalDateTime.now());
        payslip.setFinalized(false);

        return payslipRepository.save(payslip);
    }

    @Transactional
    public PayslipResponse finalizePayslip(Long id) {
        Payslip payslip = payslipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found with id: " + id));

        if (Boolean.TRUE.equals(payslip.getFinalized())) {
            throw new ConflictException("Payslip is already finalized", "PAYSLIP_FINALIZED");
        }

        payslip.setFinalized(true);
        Payslip saved = payslipRepository.save(payslip);
        return mapToResponse(saved);
    }

    public Payslip getPayslipEntity(Long id) {
        return payslipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found with id: " + id));
    }

    public PayslipResponse getPayslipById(Long id) {
        Payslip payslip = getPayslipEntity(id);
        return mapToResponse(payslip);
    }

    public Page<PayslipResponse> getPayslips(Long employeeId, Integer year, Integer month, Pageable pageable) {
        return payslipRepository.findAllFiltered(employeeId, year, month, pageable)
                .map(this::mapToResponse);
    }

    public PayslipResponse mapToResponse(Payslip payslip) {
        return PayslipResponse.builder()
                .id(payslip.getId())
                .employeeId(payslip.getEmployee().getId())
                .employeeCode(payslip.getEmployee().getEmployeeCode())
                .employeeName(payslip.getEmployee().getFullName())
                .year(payslip.getYear())
                .month(payslip.getMonth())
                .basicSalary(payslip.getBasicSalary())
                .otAmount(payslip.getOtAmount())
                .grossPay(payslip.getGrossPay())
                .employeeEpf(payslip.getEmployeeEpf())
                .employerEpf(payslip.getEmployerEpf())
                .employerEtf(payslip.getEmployerEtf())
                .deductions(payslip.getDeductions())
                .totalDeductions(payslip.getTotalDeductions())
                .netPay(payslip.getNetPay())
                .generatedAt(payslip.getGeneratedAt())
                .finalized(payslip.getFinalized())
                .build();
    }
}
