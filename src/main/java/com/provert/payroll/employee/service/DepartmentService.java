package com.provert.payroll.employee.service;

import com.provert.payroll.common.exception.ConflictException;
import com.provert.payroll.common.exception.ResourceNotFoundException;
import com.provert.payroll.employee.dto.DepartmentRequest;
import com.provert.payroll.employee.dto.DepartmentResponse;
import com.provert.payroll.employee.entity.Department;
import com.provert.payroll.employee.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByCode(request.getCode())) {
            throw new ConflictException("Department with code " + request.getCode() + " already exists", "DEPARTMENT_CODE_EXISTS");
        }
        if (departmentRepository.existsByName(request.getName())) {
            throw new ConflictException("Department with name " + request.getName() + " already exists", "DEPARTMENT_NAME_EXISTS");
        }

        Department department = new Department();
        department.setName(request.getName());
        department.setCode(request.getCode());
        department.setActive(true);

        Department saved = departmentRepository.save(department);
        return mapToResponse(saved);
    }

    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        return mapToResponse(department);
    }

    public List<DepartmentResponse> getAllDepartments(Boolean activeOnly) {
        List<Department> list = (activeOnly != null && activeOnly)
                ? departmentRepository.findAllByActive(true)
                : departmentRepository.findAll();
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        if (!department.getCode().equalsIgnoreCase(request.getCode()) && departmentRepository.existsByCode(request.getCode())) {
            throw new ConflictException("Department code already taken: " + request.getCode(), "DEPARTMENT_CODE_EXISTS");
        }
        if (!department.getName().equalsIgnoreCase(request.getName()) && departmentRepository.existsByName(request.getName())) {
            throw new ConflictException("Department name already taken: " + request.getName(), "DEPARTMENT_NAME_EXISTS");
        }

        department.setName(request.getName());
        department.setCode(request.getCode());

        Department updated = departmentRepository.save(department);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        department.setActive(false);
        departmentRepository.save(department);
    }

    private DepartmentResponse mapToResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .code(department.getCode())
                .active(department.getActive())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }
}
