package com.provert.payroll.config.dto;

import com.provert.payroll.enums.DayType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkingHourRuleRequest {

    @NotNull(message = "Day type is required")
    private DayType dayType;

    @NotNull(message = "Expected check-in time is required")
    private LocalTime expectedIn;

    @NotNull(message = "Expected check-out time is required")
    private LocalTime expectedOut;

    private Boolean otEligible;

    @DecimalMin(value = "0.0", message = "OT multiplier cannot be negative")
    private BigDecimal otMultiplier;
}
