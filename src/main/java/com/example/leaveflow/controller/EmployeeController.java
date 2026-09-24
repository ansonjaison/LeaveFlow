package com.example.leaveflow.controller;

import com.example.leaveflow.dto.EmployeeRequest;
import com.example.leaveflow.dto.EmployeeResponse;
import com.example.leaveflow.dto.LeaveResponse;
import com.example.leaveflow.security.AuthUtils;
import com.example.leaveflow.service.EmployeeService;
import com.example.leaveflow.service.LeaveService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for employee management.
 *
 * Authorization is enforced via AuthUtils (backed by JwtFilter).
 * All business logic is delegated to EmployeeService / LeaveService.
 */
@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "*")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final LeaveService leaveService;

    public EmployeeController(EmployeeService employeeService, LeaveService leaveService) {
        this.employeeService = employeeService;
        this.leaveService = leaveService;
    }

    /** POST /api/employees — Create a new employee. Admin only. */
    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody EmployeeRequest request,
            HttpServletRequest httpRequest) {
        AuthUtils.requireAdmin(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.createEmployee(request));
    }

    /** GET /api/employees — List all employees. Admin only. */
    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees(HttpServletRequest httpRequest) {
        AuthUtils.requireAdmin(httpRequest);
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    /** GET /api/employees/{id} — Get employee by ID. Admin or self. */
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        AuthUtils.requireAdminOrSelf(httpRequest, id);
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    /** GET /api/employees/code/{employeeCode} — Search by employee code. Admin only. */
    @GetMapping("/code/{employeeCode}")
    public ResponseEntity<EmployeeResponse> getEmployeeByCode(
            @PathVariable String employeeCode,
            HttpServletRequest httpRequest) {
        AuthUtils.requireAdmin(httpRequest);
        return ResponseEntity.ok(employeeService.getEmployeeByCode(employeeCode));
    }

    /** PUT /api/employees/{id} — Update employee details. Admin only. */
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request,
            HttpServletRequest httpRequest) {
        AuthUtils.requireAdmin(httpRequest);
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    /** PATCH /api/employees/{id}/deactivate — Soft-delete an employee. Admin only. */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<EmployeeResponse> deactivateEmployee(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        AuthUtils.requireAdmin(httpRequest);
        return ResponseEntity.ok(employeeService.deactivateEmployee(id));
    }

    /** PATCH /api/employees/{id}/reactivate — Reactivate an employee. Admin only. */
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<EmployeeResponse> reactivateEmployee(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        AuthUtils.requireAdmin(httpRequest);
        return ResponseEntity.ok(employeeService.reactivateEmployee(id));
    }

    /**
     * PATCH /api/employees/{id}/balance — Update leave balance. Admin only.
     * Body: { "leaveBalance": 15 }
     */
    @PatchMapping("/{id}/balance")
    public ResponseEntity<EmployeeResponse> updateLeaveBalance(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body,
            HttpServletRequest httpRequest) {
        AuthUtils.requireAdmin(httpRequest);
        Integer newBalance = body.get("leaveBalance");
        if (newBalance == null) {
            throw new com.example.leaveflow.exception.BusinessException(
                    "Request body must contain 'leaveBalance' field.");
        }
        return ResponseEntity.ok(employeeService.updateLeaveBalance(id, newBalance));
    }

    /**
     * GET /api/employees/email/{email} — Fetch employee by email. Any authenticated user.
     *
     * Called by the frontend after Supabase Auth login to load the employee's
     * role and ID. Non-admins may only look up their own email.
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<EmployeeResponse> getEmployeeByEmail(
            @PathVariable String email,
            HttpServletRequest httpRequest) {
        var auth = AuthUtils.requireAuthenticated(httpRequest);
        if (!auth.isAdmin() && !auth.email().equalsIgnoreCase(email)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "You may only look up your own employee record.");
        }
        return ResponseEntity.ok(employeeService.getEmployeeByEmail(email));
    }

    /** GET /api/employees/{employeeId}/leaves — Get leaves for an employee. Admin or self. */
    @GetMapping("/{employeeId}/leaves")
    public ResponseEntity<List<LeaveResponse>> getLeavesByEmployee(
            @PathVariable Long employeeId,
            HttpServletRequest httpRequest) {
        AuthUtils.requireAdminOrSelf(httpRequest, employeeId);
        return ResponseEntity.ok(leaveService.getLeavesByEmployee(employeeId));
    }
}
