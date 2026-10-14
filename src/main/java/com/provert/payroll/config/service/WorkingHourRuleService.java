package com.provert.payroll.config.service;

import com.provert.payroll.common.exception.ConflictException;
import com.provert.payroll.common.exception.ResourceNotFoundException;
import com.provert.payroll.config.dto.WorkingHourRuleRequest;
import com.provert.payroll.config.dto.WorkingHourRuleResponse;
import com.provert.payroll.config.entity.WorkingHourRule;
import com.provert.payroll.config.repository.WorkingHourRuleRepository;
import com.provert.payroll.enums.DayType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkingHourRuleService {

    private final WorkingHourRuleRepository workingHourRuleRepository;

    @Transactional
    public WorkingHourRuleResponse createRule(WorkingHourRuleRequest request) {
        if (workingHourRuleRepository.existsByDayType(request.getDayType())) {
            throw new ConflictException("Rule for day type " + request.getDayType() + " already exists", "RULE_ALREADY_EXISTS");
        }

        WorkingHourRule rule = new WorkingHourRule();
        rule.setDayType(request.getDayType());
        rule.setExpectedIn(request.getExpectedIn());
        rule.setExpectedOut(request.getExpectedOut());
        rule.setOtEligible(request.getOtEligible() != null ? request.getOtEligible() : true);
        rule.setOtMultiplier(request.getOtMultiplier() != null ? request.getOtMultiplier() : BigDecimal.ONE);
        rule.setActive(true);

        WorkingHourRule saved = workingHourRuleRepository.save(rule);
        return mapToResponse(saved);
    }

    public WorkingHourRuleResponse getRuleById(Long id) {
        WorkingHourRule rule = workingHourRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkingHourRule not found with id: " + id));
        return mapToResponse(rule);
    }

    public WorkingHourRule getRuleByDayTypeEntity(DayType dayType) {
        return workingHourRuleRepository.findByDayType(dayType)
                .orElse(null);
    }

    public List<WorkingHourRuleResponse> getAllRules() {
        return workingHourRuleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkingHourRuleResponse updateRule(Long id, WorkingHourRuleRequest request) {
        WorkingHourRule rule = workingHourRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkingHourRule not found with id: " + id));

        if (rule.getDayType() != request.getDayType() && workingHourRuleRepository.existsByDayType(request.getDayType())) {
            throw new ConflictException("Rule for day type " + request.getDayType() + " already exists", "RULE_ALREADY_EXISTS");
        }

        rule.setDayType(request.getDayType());
        rule.setExpectedIn(request.getExpectedIn());
        rule.setExpectedOut(request.getExpectedOut());
        if (request.getOtEligible() != null) rule.setOtEligible(request.getOtEligible());
        if (request.getOtMultiplier() != null) rule.setOtMultiplier(request.getOtMultiplier());

        WorkingHourRule updated = workingHourRuleRepository.save(rule);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteRule(Long id) {
        WorkingHourRule rule = workingHourRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkingHourRule not found with id: " + id));
        workingHourRuleRepository.delete(rule);
    }

    private WorkingHourRuleResponse mapToResponse(WorkingHourRule rule) {
        return WorkingHourRuleResponse.builder()
                .id(rule.getId())
                .dayType(rule.getDayType())
                .expectedIn(rule.getExpectedIn())
                .expectedOut(rule.getExpectedOut())
                .otEligible(rule.getOtEligible())
                .otMultiplier(rule.getOtMultiplier())
                .active(rule.getActive())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
