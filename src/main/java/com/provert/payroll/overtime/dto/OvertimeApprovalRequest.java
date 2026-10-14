package com.provert.payroll.overtime.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeApprovalRequest {

    @NotBlank(message = "Approved by username/name is required")
    private String approvedBy;
}
