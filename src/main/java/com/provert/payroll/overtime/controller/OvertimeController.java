package com.provert.payroll.overtime.controller;

import com.provert.payroll.overtime.dto.OvertimeApprovalRequest;
import com.provert.payroll.overtime.dto.OvertimeRecordResponse;
import com.provert.payroll.overtime.service.OvertimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Overtime Engine", description = "Monthly duration-based OT calculation and managerial approval workflows")
@RestController
@RequestMapping("/api/overtime")
@RequiredArgsConstructor
public class OvertimeController {

    private final OvertimeService overtimeService;

    @Operation(summary = "Calculate or recalculate overtime for a month (batch or per employee)")
    @PostMapping("/calculate")
    public ResponseEntity<List<OvertimeRecordResponse>> calculateOvertime(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(required = false) Long employeeId
    ) {
        return ResponseEntity.ok(overtimeService.calculateOvertime(year, month, employeeId));
    }

    @Operation(summary = "List overtime records with pagination and filters")
    @GetMapping
    public ResponseEntity<Page<OvertimeRecordResponse>> getOvertimeRecords(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Boolean approved,
            @RequestParam(required = false) Long departmentId,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(overtimeService.getOvertimeRecords(year, month, approved, departmentId, pageable));
    }

    @Operation(summary = "Get single overtime record by ID")
    @GetMapping("/{id}")
    public ResponseEntity<OvertimeRecordResponse> getOvertimeRecordById(@PathVariable Long id) {
        return ResponseEntity.ok(overtimeService.getOvertimeRecordById(id));
    }

    @Operation(summary = "Approve overtime record (locks record against future recalculation)")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<OvertimeRecordResponse> approveOvertime(
            @PathVariable Long id,
            @Valid @RequestBody OvertimeApprovalRequest request
    ) {
        return ResponseEntity.ok(overtimeService.approveOvertime(id, request.getApprovedBy()));
    }
}
