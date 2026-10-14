package com.provert.payroll.employee.repository;

import com.provert.payroll.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    boolean existsByEmployeeCode(String employeeCode);

    @Query("SELECT e FROM Employee e " +
           "WHERE (:departmentId IS NULL OR e.department.id = :departmentId) " +
           "AND (:active IS NULL OR e.active = :active)")
    Page<Employee> findAllFiltered(
            @Param("departmentId") Long departmentId,
            @Param("active") Boolean active,
            Pageable pageable
    );

    List<Employee> findAllByActive(Boolean active);
}
