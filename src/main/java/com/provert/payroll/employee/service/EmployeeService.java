package com.provert.payroll.employee.service;

import com.provert.payroll.common.exception.ConflictException;
import com.provert.payroll.common.exception.ResourceNotFoundException;
import com.provert.payroll.employee.dto.EmployeeDto;
import com.provert.payroll.employee.dto.EmployeeRequest;
import com.provert.payroll.employee.entity.Department;
import com.provert.payroll.employee.entity.Employee;
import com.provert.payroll.employee.repository.DepartmentRepository;
import com.provert.payroll.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public EmployeeDto createEmployee(EmployeeRequest request) {
        if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new ConflictException("Employee with code " + request.getEmployeeCode() + " already exists", "EMPLOYEE_CODE_EXISTS");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        Employee employee = new Employee();
        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setFullName(request.getFullName());
        employee.setDesignation(request.getDesignation());
        employee.setDepartment(department);
        employee.setBasicSalary(request.getBasicSalary());
        employee.setJoinedDate(request.getJoinedDate());
        employee.setActive(true);

        Employee saved = employeeRepository.save(employee);
        return mapToDto(saved);
    }

    public EmployeeDto getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return mapToDto(employee);
    }

    public Page<EmployeeDto> getEmployees(Long departmentId, Boolean active, Pageable pageable) {
        return employeeRepository.findAllFiltered(departmentId, active, pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public EmployeeDto updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        if (!employee.getEmployeeCode().equalsIgnoreCase(request.getEmployeeCode()) &&
                employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new ConflictException("Employee code already in use: " + request.getEmployeeCode(), "EMPLOYEE_CODE_EXISTS");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setFullName(request.getFullName());
        employee.setDesignation(request.getDesignation());
        employee.setDepartment(department);
        employee.setBasicSalary(request.getBasicSalary());
        employee.setJoinedDate(request.getJoinedDate());

        Employee updated = employeeRepository.save(employee);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        employee.setActive(false);
        employeeRepository.save(employee);
    }

    public EmployeeDto mapToDto(Employee employee) {
        return EmployeeDto.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .fullName(employee.getFullName())
                .designation(employee.getDesignation())
                .departmentId(employee.getDepartment().getId())
                .departmentName(employee.getDepartment().getName())
                .basicSalary(employee.getBasicSalary())
                .joinedDate(employee.getJoinedDate())
                .active(employee.getActive())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }
}
