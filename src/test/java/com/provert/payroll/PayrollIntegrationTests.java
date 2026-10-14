package com.provert.payroll;

import tools.jackson.databind.ObjectMapper;
import com.provert.payroll.attendance.dto.AttendanceRequest;
import com.provert.payroll.config.dto.WorkingHourRuleRequest;
import com.provert.payroll.employee.dto.DepartmentRequest;
import com.provert.payroll.employee.dto.DepartmentResponse;
import com.provert.payroll.employee.dto.EmployeeDto;
import com.provert.payroll.employee.dto.EmployeeRequest;
import com.provert.payroll.enums.DayType;
import com.provert.payroll.overtime.dto.OvertimeApprovalRequest;
import com.provert.payroll.overtime.dto.OvertimeRecordResponse;
import com.provert.payroll.payslip.dto.PayslipResponse;
import com.provert.payroll.security.dto.AuthRequest;
import com.provert.payroll.security.dto.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PayrollIntegrationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static String financeToken;
    private static String hrToken;
    private static Long departmentId;
    private static Long employeeId;
    private static Long overtimeRecordId;
    private static Long payslipId;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(this.webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @Order(1)
    void testAuthentication_Login() throws Exception {
        // 1. Login as Finance Manager
        AuthRequest financeLogin = AuthRequest.builder()
                .username("finance_mgr")
                .password("password123")
                .build();

        MvcResult finResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(financeLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ROLE_FINANCE_MANAGER"))
                .andReturn();

        AuthResponse finResponse = objectMapper.readValue(finResult.getResponse().getContentAsString(), AuthResponse.class);
        financeToken = "Bearer " + finResponse.getToken();

        // 2. Login as HR Admin
        AuthRequest hrLogin = AuthRequest.builder()
                .username("hr_admin")
                .password("password123")
                .build();

        MvcResult hrResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hrLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ROLE_HR_ADMIN"))
                .andReturn();

        AuthResponse hrResponse = objectMapper.readValue(hrResult.getResponse().getContentAsString(), AuthResponse.class);
        hrToken = "Bearer " + hrResponse.getToken();
    }

    @Test
    @Order(2)
    void testUnauthenticatedAccess_Returns401() throws Exception {
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    void testCreateDepartment() throws Exception {
        DepartmentRequest request = DepartmentRequest.builder()
                .name("Engineering")
                .code("ENG")
                .build();

        MvcResult result = mockMvc.perform(post("/api/departments")
                        .header(HttpHeaders.AUTHORIZATION, hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.code").value("ENG"))
                .andReturn();

        DepartmentResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), DepartmentResponse.class);
        departmentId = response.getId();
    }

    @Test
    @Order(4)
    void testCreateEmployee() throws Exception {
        EmployeeRequest request = EmployeeRequest.builder()
                .employeeCode("EMP-1001")
                .fullName("Alex Morgan")
                .designation("Senior Engineer")
                .departmentId(departmentId)
                .basicSalary(new BigDecimal("104000.00"))
                .joinedDate(LocalDate.of(2025, 3, 1))
                .build();

        MvcResult result = mockMvc.perform(post("/api/employees")
                        .header(HttpHeaders.AUTHORIZATION, hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.employeeCode").value("EMP-1001"))
                .andReturn();

        EmployeeDto response = objectMapper.readValue(result.getResponse().getContentAsString(), EmployeeDto.class);
        employeeId = response.getId();
    }

    @Test
    @Order(5)
    void testConfigureWorkingHourRule() throws Exception {
        WorkingHourRuleRequest ruleRequest = WorkingHourRuleRequest.builder()
                .dayType(DayType.Weekday)
                .expectedIn(LocalTime.of(9, 0))
                .expectedOut(LocalTime.of(17, 0))
                .otEligible(true)
                .otMultiplier(new BigDecimal("1.00"))
                .build();

        mockMvc.perform(post("/api/working-hour-rules")
                        .header(HttpHeaders.AUTHORIZATION, hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ruleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dayType").value("Weekday"));
    }

    @Test
    @Order(6)
    void testRecordAttendance() throws Exception {
        LocalDate attDate = LocalDate.of(2026, 10, 5); // Monday
        AttendanceRequest request = AttendanceRequest.builder()
                .employeeId(employeeId)
                .attendanceDate(attDate)
                .checkIn(LocalDateTime.of(2026, 10, 5, 9, 0))
                .checkOut(LocalDateTime.of(2026, 10, 5, 19, 0)) // 2 hours OT
                .build();

        mockMvc.perform(post("/api/attendance")
                        .header(HttpHeaders.AUTHORIZATION, hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dayType").value("Weekday"))
                .andExpect(jsonPath("$.employeeId").value(employeeId));
    }

    @Test
    @Order(7)
    void testCalculateOvertime() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/overtime/calculate")
                        .header(HttpHeaders.AUTHORIZATION, financeToken)
                        .param("year", "2026")
                        .param("month", "10")
                        .param("employeeId", String.valueOf(employeeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalOtMinutes").value(120))
                .andExpect(jsonPath("$[0].otAmount").value(1000.00))
                .andExpect(jsonPath("$[0].approved").value(false))
                .andReturn();

        OvertimeRecordResponse[] responses = objectMapper.readValue(result.getResponse().getContentAsString(), OvertimeRecordResponse[].class);
        overtimeRecordId = responses[0].getId();
    }

    @Test
    @Order(8)
    void testPayslipGenerationBeforeApproval_FailsWith400() throws Exception {
        mockMvc.perform(post("/api/payslips/generate")
                        .header(HttpHeaders.AUTHORIZATION, financeToken)
                        .param("year", "2026")
                        .param("month", "10")
                        .param("employeeId", String.valueOf(employeeId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OT_NOT_APPROVED"));
    }

    @Test
    @Order(9)
    void testApproveOvertime() throws Exception {
        OvertimeApprovalRequest request = OvertimeApprovalRequest.builder()
                .approvedBy("FinanceManager")
                .build();

        mockMvc.perform(patch("/api/overtime/" + overtimeRecordId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(true))
                .andExpect(jsonPath("$.approvedBy").value("FinanceManager"));
    }

    @Test
    @Order(10)
    void testGeneratePayslipWithStatutoryDeductions() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/payslips/generate")
                        .header(HttpHeaders.AUTHORIZATION, financeToken)
                        .param("year", "2026")
                        .param("month", "10")
                        .param("employeeId", String.valueOf(employeeId))
                        .param("deductions", "2000.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].basicSalary").value(104000.00))
                .andExpect(jsonPath("$[0].otAmount").value(1000.00))
                .andExpect(jsonPath("$[0].grossPay").value(105000.00))
                .andExpect(jsonPath("$[0].employeeEpf").value(8320.00))
                .andExpect(jsonPath("$[0].employerEpf").value(12480.00))
                .andExpect(jsonPath("$[0].employerEtf").value(3120.00))
                .andExpect(jsonPath("$[0].deductions").value(2000.00))
                .andExpect(jsonPath("$[0].totalDeductions").value(10320.00))
                .andExpect(jsonPath("$[0].netPay").value(94680.00))
                .andExpect(jsonPath("$[0].finalized").value(false))
                .andReturn();

        PayslipResponse[] responses = objectMapper.readValue(result.getResponse().getContentAsString(), PayslipResponse[].class);
        payslipId = responses[0].getId();
    }

    @Test
    @Order(11)
    void testDownloadPayslipPdf() throws Exception {
        mockMvc.perform(get("/api/payslips/" + payslipId + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, financeToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @Order(12)
    void testFinalizePayslip() throws Exception {
        mockMvc.perform(patch("/api/payslips/" + payslipId + "/finalize")
                        .header(HttpHeaders.AUTHORIZATION, financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalized").value(true));

        // Attempting to finalize again should return 409 Conflict
        mockMvc.perform(patch("/api/payslips/" + payslipId + "/finalize")
                        .header(HttpHeaders.AUTHORIZATION, financeToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYSLIP_FINALIZED"));
    }
}
