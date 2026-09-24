package com.example.leaveflow.service;

import com.example.leaveflow.dto.EmployeeRequest;
import com.example.leaveflow.dto.EmployeeResponse;
import com.example.leaveflow.entity.Employee;
import com.example.leaveflow.enums.Role;
import com.example.leaveflow.exception.BusinessException;
import com.example.leaveflow.exception.ResourceNotFoundException;
import com.example.leaveflow.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final SupabaseAuthService supabaseAuthService;

    public EmployeeService(EmployeeRepository employeeRepository,
                           SupabaseAuthService supabaseAuthService) {
        this.employeeRepository = employeeRepository;
        this.supabaseAuthService = supabaseAuthService;
    }

    /**
     * Creates a new employee.
     * Validates uniqueness of code and email, creates a matching Supabase Auth user,
     * then saves to the DB. The generated password is returned once in the response.
     */
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new BusinessException("Employee code '" + request.getEmployeeCode() + "' already exists.");
        }
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email '" + request.getEmail() + "' already exists.");
        }

        String defaultPassword = supabaseAuthService.generateDefaultPassword(
                request.getName(), request.getEmployeeCode());

        // Create Supabase Auth user before saving to DB.
        // If this fails, no orphaned employee record is left behind.
        supabaseAuthService.createAuthUser(request.getEmail(), defaultPassword);

        Employee employee = new Employee();
        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setDepartment(request.getDepartment());
        employee.setRole(request.getRole());
        employee.setLeaveBalance(request.getRole() == Role.ADMIN ? 0 : 12);
        employee.setActive(true);
        employee.setCreatedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());

        Employee saved = employeeRepository.save(employee);
        EmployeeResponse response = toResponse(saved);
        response.setGeneratedPassword(defaultPassword);
        return response;
    }

    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return toResponse(employee);
    }

    public EmployeeResponse getEmployeeByCode(String employeeCode) {
        Employee employee = employeeRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with code: " + employeeCode));
        return toResponse(employee);
    }

    /**
     * Finds an employee by email — called after Supabase Auth login
     * to load the employee's role and ID.
     */
    public EmployeeResponse getEmployeeByEmail(String email) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No employee account found for email: " + email));
        return toResponse(employee);
    }

    /** Updates employee fields, checking uniqueness of code/email only if changed. */
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        if (!employee.getEmployeeCode().equals(request.getEmployeeCode()) &&
                employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new BusinessException("Employee code '" + request.getEmployeeCode() + "' already exists.");
        }
        if (!employee.getEmail().equals(request.getEmail()) &&
                employeeRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email '" + request.getEmail() + "' already exists.");
        }

        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setDepartment(request.getDepartment());
        employee.setRole(request.getRole());
        employee.setUpdatedAt(LocalDateTime.now());

        return toResponse(employeeRepository.save(employee));
    }

    /** Soft-deletes an employee — record is kept for historical leave data. */
    public EmployeeResponse deactivateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        if (!employee.isActive()) {
            throw new BusinessException("Employee is already deactivated.");
        }
        employee.setActive(false);
        employee.setUpdatedAt(LocalDateTime.now());
        return toResponse(employeeRepository.save(employee));
    }

    public EmployeeResponse reactivateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        if (employee.isActive()) {
            throw new BusinessException("Employee is already active.");
        }
        employee.setActive(true);
        employee.setUpdatedAt(LocalDateTime.now());
        return toResponse(employeeRepository.save(employee));
    }

    public EmployeeResponse updateLeaveBalance(Long id, int newBalance) {
        if (newBalance < 0) {
            throw new BusinessException("Leave balance cannot be negative.");
        }
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        employee.setLeaveBalance(newBalance);
        employee.setUpdatedAt(LocalDateTime.now());
        return toResponse(employeeRepository.save(employee));
    }

    private EmployeeResponse toResponse(Employee employee) {
        EmployeeResponse response = new EmployeeResponse();
        response.setId(employee.getId());
        response.setEmployeeCode(employee.getEmployeeCode());
        response.setName(employee.getName());
        response.setEmail(employee.getEmail());
        response.setDepartment(employee.getDepartment());
        response.setRole(employee.getRole());
        response.setLeaveBalance(employee.getLeaveBalance());
        response.setActive(employee.isActive());
        return response;
    }
}
