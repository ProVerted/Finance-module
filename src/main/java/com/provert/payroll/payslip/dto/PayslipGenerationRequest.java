package com.provert.payroll.payslip.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayslipGenerationRequest {

    @DecimalMin(value = "0.0", message = "Deductions cannot be negative")
    private BigDecimal deductions;
}
