package com.provert.payroll.config.entity;

import com.provert.payroll.common.AuditableEntity;
import com.provert.payroll.enums.DayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "working_hour_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkingHourRule extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false, unique = true, length = 20)
    private DayType dayType;

    @Column(name = "expected_in", nullable = false)
    private LocalTime expectedIn;

    @Column(name = "expected_out", nullable = false)
    private LocalTime expectedOut;

    @Column(name = "ot_eligible", nullable = false)
    private Boolean otEligible = true;

    @Column(name = "ot_multiplier", precision = 3, scale = 2, nullable = false)
    private BigDecimal otMultiplier = new BigDecimal("1.00");
}
