package com.provert.payroll.payslip.entity;

import com.provert.payroll.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payslip", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payslip_employee_month", columnNames = {"employee_id", "year", "month"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payslip {

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

    @Column(name = "basic_salary", precision = 12, scale = 2, nullable = false)
    private BigDecimal basicSalary;

    @Column(name = "ot_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal otAmount = BigDecimal.ZERO;

    @Column(name = "gross_pay", precision = 12, scale = 2, nullable = false)
    private BigDecimal grossPay;

    @Column(name = "employee_epf", precision = 12, scale = 2, nullable = false)
    private BigDecimal employeeEpf = BigDecimal.ZERO;

    @Column(name = "employer_epf", precision = 12, scale = 2, nullable = false)
    private BigDecimal employerEpf = BigDecimal.ZERO;

    @Column(name = "employer_etf", precision = 12, scale = 2, nullable = false)
    private BigDecimal employerEtf = BigDecimal.ZERO;

    @Column(name = "deductions", precision = 12, scale = 2, nullable = false)
    private BigDecimal deductions = BigDecimal.ZERO;

    @Column(name = "total_deductions", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "net_pay", precision = 12, scale = 2, nullable = false)
    private BigDecimal netPay;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "finalized", nullable = false)
    private Boolean finalized = false;
}
