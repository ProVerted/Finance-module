package com.provert.payroll.config.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayCalendarRequest {

    @NotNull(message = "Holiday date is required")
    private LocalDate holidayDate;

    @Size(max = 100, message = "Description must not exceed 100 characters")
    private String description;
}
