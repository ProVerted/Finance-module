package com.provert.payroll.config.repository;

import com.provert.payroll.config.entity.WorkingHourRule;
import com.provert.payroll.enums.DayType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkingHourRuleRepository extends JpaRepository<WorkingHourRule, Long> {
    Optional<WorkingHourRule> findByDayType(DayType dayType);
    boolean existsByDayType(DayType dayType);
}
