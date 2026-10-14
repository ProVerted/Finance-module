package com.provert.payroll;

import com.provert.payroll.attendance.entity.AttendanceRecord;
import com.provert.payroll.attendance.repository.AttendanceRecordRepository;
import com.provert.payroll.attendance.service.AttendanceService;
import com.provert.payroll.common.exception.BadRequestException;
import com.provert.payroll.common.exception.ConflictException;
import com.provert.payroll.config.entity.WorkingHourRule;
import com.provert.payroll.config.service.HolidayCalendarService;
import com.provert.payroll.config.service.WorkingHourRuleService;
import com.provert.payroll.employee.entity.Department;
import com.provert.payroll.employee.entity.Employee;
import com.provert.payroll.employee.repository.EmployeeRepository;
import com.provert.payroll.enums.DayType;
import com.provert.payroll.overtime.dto.OvertimeRecordResponse;
import com.provert.payroll.overtime.entity.OvertimeRecord;
import com.provert.payroll.overtime.repository.OvertimeRecordRepository;
import com.provert.payroll.overtime.service.OvertimeService;
import com.provert.payroll.payslip.dto.PayslipResponse;
import com.provert.payroll.payslip.entity.Payslip;
import com.provert.payroll.payslip.repository.PayslipRepository;
import com.provert.payroll.payslip.service.PayslipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTests {

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private HolidayCalendarService holidayCalendarService;
    @Mock
    private WorkingHourRuleService workingHourRuleService;
    @Mock
    private OvertimeRecordRepository overtimeRecordRepository;
    @Mock
    private PayslipRepository payslipRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    @InjectMocks
    private OvertimeService overtimeService;

    @InjectMocks
    private PayslipService payslipService;

    private Employee sampleEmployee;

    @BeforeEach
    void setUp() {
        Department dept = new Department();
        dept.setId(1L);
        dept.setName("Finance");
        dept.setCode("FIN");

        sampleEmployee = new Employee();
        sampleEmployee.setId(10L);
        sampleEmployee.setEmployeeCode("EMP-001");
        sampleEmployee.setFullName("Janarththan");
        sampleEmployee.setDepartment(dept);
        sampleEmployee.setBasicSalary(new BigDecimal("104000.00")); // 104,000 / 208 = 500 / hr
        sampleEmployee.setJoinedDate(LocalDate.of(2025, 1, 1));
        sampleEmployee.setActive(true);

        // Inject configuration property into overtimeService
        ReflectionTestUtils.setField(overtimeService, "monthlyStandardHours", 208);
    }

    @Test
    void testDayTypeResolution() {
        LocalDate holidayDate = LocalDate.of(2026, 12, 25);
        LocalDate saturdayDate = LocalDate.of(2026, 10, 17); // Saturday
        LocalDate sundayDate = LocalDate.of(2026, 10, 18);   // Sunday
        LocalDate weekdayDate = LocalDate.of(2026, 10, 19);  // Monday

        when(holidayCalendarService.isHoliday(holidayDate)).thenReturn(true);
        when(holidayCalendarService.isHoliday(saturdayDate)).thenReturn(false);
        when(holidayCalendarService.isHoliday(sundayDate)).thenReturn(false);
        when(holidayCalendarService.isHoliday(weekdayDate)).thenReturn(false);

        assertEquals(DayType.Holiday, attendanceService.resolveDayType(holidayDate));
        assertEquals(DayType.Saturday, attendanceService.resolveDayType(saturdayDate));
        assertEquals(DayType.Sunday, attendanceService.resolveDayType(sundayDate));
        assertEquals(DayType.Weekday, attendanceService.resolveDayType(weekdayDate));
    }

    @Test
    void testOvertimeCalculation_Success() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(sampleEmployee));
        when(overtimeRecordRepository.findByEmployeeIdAndYearAndMonth(10L, 2026, 10)).thenReturn(Optional.empty());

        AttendanceRecord att = new AttendanceRecord();
        att.setId(100L);
        att.setEmployee(sampleEmployee);
        att.setAttendanceDate(LocalDate.of(2026, 10, 1));
        att.setCheckIn(LocalDateTime.of(2026, 10, 1, 9, 0));
        att.setCheckOut(LocalDateTime.of(2026, 10, 1, 19, 0)); // 10 hours = 600 mins (expected 480 mins -> 120 mins OT)
        att.setDayType(DayType.Weekday);

        when(attendanceRecordRepository.findByEmployeeIdAndAttendanceDateBetween(eq(10L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(att));

        WorkingHourRule rule = new WorkingHourRule();
        rule.setDayType(DayType.Weekday);
        rule.setExpectedIn(LocalTime.of(9, 0));
        rule.setExpectedOut(LocalTime.of(17, 0));
        rule.setOtEligible(true);
        rule.setOtMultiplier(new BigDecimal("1.00"));

        when(workingHourRuleService.getRuleByDayTypeEntity(DayType.Weekday)).thenReturn(rule);
        when(overtimeRecordRepository.save(any(OvertimeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<OvertimeRecordResponse> results = overtimeService.calculateOvertime(2026, 10, 10L);

        assertNotNull(results);
        assertEquals(1, results.size());
        OvertimeRecordResponse ot = results.get(0);
        assertEquals(120L, ot.getTotalOtMinutes());
        assertEquals(new BigDecimal("500.00"), ot.getHourlyRate()); // 104,000 / 208 = 500.00
        assertEquals(new BigDecimal("1000.00"), ot.getOtAmount());  // 2 hours * 500.00 = 1000.00
        assertFalse(ot.getApproved());
    }

    @Test
    void testOvertimeCalculation_AlreadyApproved_ThrowsConflict() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(sampleEmployee));

        OvertimeRecord existingApproved = new OvertimeRecord();
        existingApproved.setId(1L);
        existingApproved.setApproved(true);
        when(overtimeRecordRepository.findByEmployeeIdAndYearAndMonth(10L, 2026, 10)).thenReturn(Optional.of(existingApproved));

        assertThrows(ConflictException.class, () -> overtimeService.calculateOvertime(2026, 10, 10L));
    }

    @Test
    void testPayslipGeneration_RequiresApprovedOvertime() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(sampleEmployee));
        when(payslipRepository.findByEmployeeIdAndYearAndMonth(10L, 2026, 10)).thenReturn(Optional.empty());

        OvertimeRecord unapprovedOt = new OvertimeRecord();
        unapprovedOt.setApproved(false);
        when(overtimeRecordRepository.findByEmployeeIdAndYearAndMonth(10L, 2026, 10)).thenReturn(Optional.of(unapprovedOt));

        assertThrows(BadRequestException.class, () ->
                payslipService.generatePayslips(2026, 10, 10L, BigDecimal.ZERO)
        );
    }

    @Test
    void testPayslipGeneration_WithStatutoryDeductions_Success() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(sampleEmployee));
        when(payslipRepository.findByEmployeeIdAndYearAndMonth(10L, 2026, 10)).thenReturn(Optional.empty());

        OvertimeRecord approvedOt = new OvertimeRecord();
        approvedOt.setApproved(true);
        approvedOt.setOtAmount(new BigDecimal("1000.00"));
        when(overtimeRecordRepository.findByEmployeeIdAndYearAndMonth(10L, 2026, 10)).thenReturn(Optional.of(approvedOt));
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<PayslipResponse> responses = payslipService.generatePayslips(2026, 10, 10L, new BigDecimal("5000.00"));

        assertEquals(1, responses.size());
        PayslipResponse payslip = responses.get(0);
        assertEquals(new BigDecimal("104000.00"), payslip.getBasicSalary());
        assertEquals(new BigDecimal("1000.00"), payslip.getOtAmount());
        assertEquals(new BigDecimal("105000.00"), payslip.getGrossPay());

        // Statutory Breakdown: 8% employee EPF = 8,320.00, 12% employer EPF = 12,480.00, 3% employer ETF = 3,120.00
        assertEquals(new BigDecimal("8320.00"), payslip.getEmployeeEpf());
        assertEquals(new BigDecimal("12480.00"), payslip.getEmployerEpf());
        assertEquals(new BigDecimal("3120.00"), payslip.getEmployerEtf());

        assertEquals(new BigDecimal("5000.00"), payslip.getDeductions());
        assertEquals(new BigDecimal("13320.00"), payslip.getTotalDeductions()); // 8,320 + 5,000

        // Net Pay: 105,000 - 13,320 = 91,680.00
        assertEquals(new BigDecimal("91680.00"), payslip.getNetPay());
        assertFalse(payslip.getFinalized());
    }

    @Test
    void testPayslipFinalization_Lock() {
        Payslip payslip = new Payslip();
        payslip.setId(55L);
        payslip.setFinalized(true);

        when(payslipRepository.findById(55L)).thenReturn(Optional.of(payslip));

        assertThrows(ConflictException.class, () -> payslipService.finalizePayslip(55L));
    }
}
