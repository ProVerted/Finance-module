package com.provert.payroll.overtime.dto;

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
public class OvertimeRecordResponse {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private Integer year;
    private Integer month;
    private Long totalOtMinutes;
    private BigDecimal hourlyRate;
    private BigDecimal otAmount;
    private Boolean approved;
    private String approvedBy;
    private LocalDateTime approvedAt;
}
