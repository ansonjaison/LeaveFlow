package com.example.leaveflow.dto;

import com.example.leaveflow.enums.Role;

/**
 * DTO for sending employee data back to the frontend.
 *
 * This is the shape of the JSON the backend returns.
 * It deliberately excludes sensitive or internal fields like createdAt/updatedAt.
 *
 * Why not return the Employee entity directly?
 * - Entities can have JPA proxies and lazy-loaded relationships
 *   that cause serialization issues.
 * - DTOs give us full control over what gets exposed in the API.
 * - This is a core principle of clean API design.
 *
 * generatedPassword: Only populated once — on the create-employee response.
 * Null on all other responses. The frontend shows it in a popup so the admin
 * can share it with the new employee.
 */
public class EmployeeResponse {

    private Long id;
    private String employeeCode;
    private String name;
    private String email;
    private String department;
    private Role role;
    private int leaveBalance;
    private boolean active;

    /**
     * The auto-generated default password. Only returned once (on employee creation).
     * Not stored in the database. Null on all other API responses.
     */
    private String generatedPassword;

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public int getLeaveBalance() {
        return leaveBalance;
    }

    public void setLeaveBalance(int leaveBalance) {
        this.leaveBalance = leaveBalance;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getGeneratedPassword() {
        return generatedPassword;
    }

    public void setGeneratedPassword(String generatedPassword) {
        this.generatedPassword = generatedPassword;
    }

}
