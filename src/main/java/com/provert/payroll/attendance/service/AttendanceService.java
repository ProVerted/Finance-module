package com.provert.payroll.attendance.service;

import com.provert.payroll.attendance.dto.AttendanceRequest;
import com.provert.payroll.attendance.dto.AttendanceResponse;
import com.provert.payroll.attendance.entity.AttendanceRecord;
import com.provert.payroll.attendance.repository.AttendanceRecordRepository;
import com.provert.payroll.common.exception.ConflictException;
import com.provert.payroll.common.exception.ResourceNotFoundException;
import com.provert.payroll.config.service.HolidayCalendarService;
import com.provert.payroll.employee.entity.Employee;
import com.provert.payroll.employee.repository.EmployeeRepository;
import com.provert.payroll.enums.DayType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final HolidayCalendarService holidayCalendarService;

    @Transactional
    public AttendanceResponse recordAttendance(AttendanceRequest request) {
        if (attendanceRecordRepository.existsByEmployeeIdAndAttendanceDate(request.getEmployeeId(), request.getAttendanceDate())) {
            throw new ConflictException(
                    "Attendance already recorded for employee " + request.getEmployeeId() + " on " + request.getAttendanceDate(),
                    "ATTENDANCE_DUPLICATE"
            );
        }

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getEmployeeId()));

        DayType dayType = resolveDayType(request.getAttendanceDate());

        AttendanceRecord record = new AttendanceRecord();
        record.setEmployee(employee);
        record.setAttendanceDate(request.getAttendanceDate());
        record.setCheckIn(request.getCheckIn());
        record.setCheckOut(request.getCheckOut());
        record.setDayType(dayType);

        AttendanceRecord saved = attendanceRecordRepository.save(record);
        return mapToResponse(saved);
    }

    @Transactional
    public AttendanceResponse updateAttendance(Long id, AttendanceRequest request) {
        AttendanceRecord record = attendanceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with id: " + id));

        record.setCheckIn(request.getCheckIn());
        record.setCheckOut(request.getCheckOut());

        if (request.getAttendanceDate() != null && !record.getAttendanceDate().equals(request.getAttendanceDate())) {
            if (attendanceRecordRepository.existsByEmployeeIdAndAttendanceDate(record.getEmployee().getId(), request.getAttendanceDate())) {
                throw new ConflictException("Attendance already exists for date " + request.getAttendanceDate(), "ATTENDANCE_DUPLICATE");
            }
            record.setAttendanceDate(request.getAttendanceDate());
            record.setDayType(resolveDayType(request.getAttendanceDate()));
        }

        AttendanceRecord updated = attendanceRecordRepository.save(record);
        return mapToResponse(updated);
    }

    public List<AttendanceResponse> getAttendanceRecords(Long employeeId, LocalDate dateFrom, LocalDate dateTo) {
        return attendanceRecordRepository.findFiltered(employeeId, dateFrom, dateTo).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AttendanceResponse getAttendanceById(Long id) {
        AttendanceRecord record = attendanceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with id: " + id));
        return mapToResponse(record);
    }

    public DayType resolveDayType(LocalDate date) {
        if (holidayCalendarService.isHoliday(date)) {
            return DayType.Holiday;
        }
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY) {
            return DayType.Saturday;
        }
        if (dow == DayOfWeek.SUNDAY) {
            return DayType.Sunday;
        }
        return DayType.Weekday;
    }

    public AttendanceResponse mapToResponse(AttendanceRecord record) {
        return AttendanceResponse.builder()
                .id(record.getId())
                .employeeId(record.getEmployee().getId())
                .employeeCode(record.getEmployee().getEmployeeCode())
                .employeeName(record.getEmployee().getFullName())
                .attendanceDate(record.getAttendanceDate())
                .checkIn(record.getCheckIn())
                .checkOut(record.getCheckOut())
                .dayType(record.getDayType())
                .build();
    }
}
