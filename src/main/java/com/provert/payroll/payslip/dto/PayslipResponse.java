package com.provert.payroll.payslip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayslipResponse {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private Integer year;
    private Integer month;
    private BigDecimal basicSalary;
    private BigDecimal otAmount;
    private BigDecimal grossPay;
    private BigDecimal employeeEpf;
    private BigDecimal employerEpf;
    private BigDecimal employerEtf;
    private BigDecimal deductions;
    private BigDecimal totalDeductions;
    private BigDecimal netPay;
    private LocalDateTime generatedAt;
    private Boolean finalized;
}
