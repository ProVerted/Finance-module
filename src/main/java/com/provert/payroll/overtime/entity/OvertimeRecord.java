package com.provert.payroll.overtime.entity;

import com.provert.payroll.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "overtime_record", uniqueConstraints = {
        @UniqueConstraint(name = "uk_overtime_employee_month", columnNames = {"employee_id", "year", "month"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "total_ot_minutes", nullable = false)
    private Long totalOtMinutes = 0L;

    @Column(name = "hourly_rate", precision = 12, scale = 2, nullable = false)
    private BigDecimal hourlyRate;

    @Column(name = "ot_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal otAmount;

    @Column(name = "approved", nullable = false)
    private Boolean approved = false;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}