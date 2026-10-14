package com.provert.payroll.employee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRequest {

    @NotBlank(message = "Employee code is required")
    @Size(max = 15, message = "Employee code must not exceed 15 characters")
    private String employeeCode;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @Size(max = 50, message = "Designation must not exceed 50 characters")
    private String designation;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotNull(message = "Basic salary is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Basic salary must be greater than zero")
    private BigDecimal basicSalary;

    @NotNull(message = "Joined date is required")
    private LocalDate joinedDate;
}
