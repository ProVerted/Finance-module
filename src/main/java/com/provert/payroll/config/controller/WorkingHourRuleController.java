package com.provert.payroll.config.controller;

import com.provert.payroll.config.dto.WorkingHourRuleRequest;
import com.provert.payroll.config.dto.WorkingHourRuleResponse;
import com.provert.payroll.config.service.WorkingHourRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Working Hour Rules", description = "Work schedule configurations and overtime multipliers per day type")
@RestController
@RequestMapping("/api/working-hour-rules")
@RequiredArgsConstructor
public class WorkingHourRuleController {

    private final WorkingHourRuleService workingHourRuleService;

    @Operation(summary = "Create working hour rule for a day type")
    @PostMapping
    public ResponseEntity<WorkingHourRuleResponse> createRule(@Valid @RequestBody WorkingHourRuleRequest request) {
        WorkingHourRuleResponse response = workingHourRuleService.createRule(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get working hour rule by ID")
    @GetMapping("/{id}")
    public ResponseEntity<WorkingHourRuleResponse> getRuleById(@PathVariable Long id) {
        return ResponseEntity.ok(workingHourRuleService.getRuleById(id));
    }

    @Operation(summary = "List all working hour rules")
    @GetMapping
    public ResponseEntity<List<WorkingHourRuleResponse>> getAllRules() {
        return ResponseEntity.ok(workingHourRuleService.getAllRules());
    }

    @Operation(summary = "Update working hour rule")
    @PutMapping("/{id}")
    public ResponseEntity<WorkingHourRuleResponse> updateRule(@PathVariable Long id, @Valid @RequestBody WorkingHourRuleRequest request) {
        return ResponseEntity.ok(workingHourRuleService.updateRule(id, request));
    }

    @Operation(summary = "Delete working hour rule")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        workingHourRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
