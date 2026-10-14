package com.provert.payroll.payslip.controller;

import com.provert.payroll.payslip.dto.PayslipResponse;
import com.provert.payroll.payslip.entity.Payslip;
import com.provert.payroll.payslip.service.PayslipPdfService;
import com.provert.payroll.payslip.service.PayslipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Payslips", description = "Monthly compensation generation, statutory deductions, locking, and PDF exports")
@RestController
@RequestMapping("/api/payslips")
@RequiredArgsConstructor
public class PayslipController {

    private final PayslipService payslipService;
    private final PayslipPdfService payslipPdfService;

    @Operation(summary = "Generate monthly payslips (requires approved overtime)")
    @PostMapping("/generate")
    public ResponseEntity<List<PayslipResponse>> generatePayslips(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) BigDecimal deductions
    ) {
        return ResponseEntity.ok(payslipService.generatePayslips(year, month, employeeId, deductions));
    }

    @Operation(summary = "Get single payslip by ID")
    @GetMapping("/{id}")
    public ResponseEntity<PayslipResponse> getPayslipById(@PathVariable Long id) {
        return ResponseEntity.ok(payslipService.getPayslipById(id));
    }

    @Operation(summary = "List payslips with pagination and filters")
    @GetMapping
    public ResponseEntity<Page<PayslipResponse>> getPayslips(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(payslipService.getPayslips(employeeId, year, month, pageable));
    }

    @Operation(summary = "Finalize and permanently lock a payslip")
    @PatchMapping("/{id}/finalize")
    public ResponseEntity<PayslipResponse> finalizePayslip(@PathVariable Long id) {
        return ResponseEntity.ok(payslipService.finalizePayslip(id));
    }

    @Operation(summary = "Download formatted PDF payslip")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPayslipPdf(@PathVariable Long id) {
        Payslip payslip = payslipService.getPayslipEntity(id);
        byte[] pdfBytes = payslipPdfService.generatePayslipPdf(payslip);

        String filename = String.format("payslip-%s-%d-%02d.pdf",
                payslip.getEmployee().getEmployeeCode(),
                payslip.getYear(),
                payslip.getMonth());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }
}
