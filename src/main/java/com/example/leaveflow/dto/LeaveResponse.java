package com.example.leaveflow.dto;

import com.example.leaveflow.enums.LeaveStatus;
import com.example.leaveflow.enums.LeaveType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for returning leave request details to the frontend.
 *
 * Key design decision:
 *   "days" is calculated dynamically in the service layer from startDate and endDate.
 *   It is NOT stored in the database. This avoids storing redundant derived data.
 *
 *   If we stored "days" in the database, it could become inconsistent
 *   (e.g., if startDate or endDate were ever changed). Calculating it
 *   on the fly always gives the correct answer.
 */
public class LeaveResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private long days;           // Calculated from startDate + endDate, not stored in DB
    private String reason;
    private LeaveStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public long getDays() {
        return days;
    }

    public void setDays(long days) {
        this.days = days;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
