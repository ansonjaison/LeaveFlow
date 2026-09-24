package com.example.leaveflow.repository;

import com.example.leaveflow.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Employee database operations.
 *
 * By extending JpaRepository<Employee, Long>, Spring Data JPA automatically
 * provides implementations for common operations:
 *   - save(entity)
 *   - findById(id)
 *   - findAll()
 *   - delete(entity)
 *   - existsById(id)
 *   ... and many more
 *
 * The methods below are "derived query methods" — Spring Data JPA reads
 * the method name and automatically generates the correct SQL query.
 * No custom SQL is needed.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Generates: SELECT * FROM employees WHERE employee_code = ?
    Optional<Employee> findByEmployeeCode(String employeeCode);

    // Generates: SELECT COUNT(*) > 0 FROM employees WHERE employee_code = ?
    boolean existsByEmployeeCode(String employeeCode);

    // Generates: SELECT COUNT(*) > 0 FROM employees WHERE email = ?
    boolean existsByEmail(String email);

    // Generates: SELECT * FROM employees WHERE email = ?
    // Used after Supabase Auth login to find the employee record by email
    Optional<Employee> findByEmail(String email);

}
