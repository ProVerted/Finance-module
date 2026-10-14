package com.provert.payroll.config.dto;

import com.provert.payroll.enums.DayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkingHourRuleResponse {
    private Long id;
    private DayType dayType;
    private LocalTime expectedIn;
    private LocalTime expectedOut;
    private Boolean otEligible;
    private BigDecimal otMultiplier;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
