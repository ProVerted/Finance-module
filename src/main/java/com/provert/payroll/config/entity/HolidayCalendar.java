package com.provert.payroll.config.entity;

import com.provert.payroll.common.AuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "holiday_calendar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HolidayCalendar extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate holidayDate;

    @Column(name = "description", length = 100)
    private String description;
}