package com.example.leaveflow.service;

import com.example.leaveflow.dto.LeaveRequestDto;
import com.example.leaveflow.dto.LeaveResponse;
import com.example.leaveflow.entity.Employee;
import com.example.leaveflow.entity.LeaveRequest;
import com.example.leaveflow.enums.LeaveStatus;
import com.example.leaveflow.enums.LeaveType;
import com.example.leaveflow.enums.Role;
import com.example.leaveflow.exception.BusinessException;
import com.example.leaveflow.exception.ResourceNotFoundException;
import com.example.leaveflow.repository.EmployeeRepository;
import com.example.leaveflow.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LeaveService.
 *
 * These tests verify:
 *   - The working-day calculation algorithm (core business logic)
 *   - All leave-related business rules (apply, approve, reject)
 *   - Proper exception throwing for invalid scenarios
 *
 * No database is used. Repositories are mocked with Mockito.
 */
@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private LeaveService leaveService;

    private Employee activeEmployee;
    private LeaveRequestDto sampleDto;

    @BeforeEach
    void setUp() {
        activeEmployee = new Employee();
        activeEmployee.setId(1L);
        activeEmployee.setEmployeeCode("EMP001");
        activeEmployee.setName("Anson Jaison");
        activeEmployee.setRole(Role.EMPLOYEE);
        activeEmployee.setLeaveBalance(12);
        activeEmployee.setActive(true);

        // Default: next Monday → next Monday = 1 working day
        // Using dynamic future dates so the past-date validation never triggers.
        LocalDate nextMonday = LocalDate.now().with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY));
        sampleDto = new LeaveRequestDto();
        sampleDto.setEmployeeId(1L);
        sampleDto.setLeaveType(LeaveType.CASUAL);
        sampleDto.setStartDate(nextMonday);
        sampleDto.setEndDate(nextMonday);
        sampleDto.setReason("Personal work");
    }

    // =========================================================
    // Working-Day Calculation Tests
    // =========================================================

    @Test
    void calculateWorkingDays_MondayToMonday_Returns1() {
        LocalDate monday = LocalDate.of(2026, 9, 21);
        assertEquals(1, leaveService.calculateWorkingDays(monday, monday));
    }

    @Test
    void calculateWorkingDays_FridayToMonday_Returns2() {
        // Friday + Monday (Saturday and Sunday excluded)
        LocalDate friday = LocalDate.of(2026, 9, 18);
        LocalDate monday = LocalDate.of(2026, 9, 21);
        assertEquals(2, leaveService.calculateWorkingDays(friday, monday));
    }

    @Test
    void calculateWorkingDays_ThursdayToNextWednesday_Returns5() {
        // Thu Sep 17 → Wed Sep 23: Thu, Fri, Mon, Tue, Wed = 5 working days
        LocalDate thursday = LocalDate.of(2026, 9, 17);
        LocalDate wednesday = LocalDate.of(2026, 9, 23);
        assertEquals(5, leaveService.calculateWorkingDays(thursday, wednesday));
    }

    @Test
    void calculateWorkingDays_SaturdayToSunday_Returns0() {
        // Both days are weekend — no working days
        LocalDate saturday = LocalDate.of(2026, 9, 19);
        LocalDate sunday = LocalDate.of(2026, 9, 20);
        assertEquals(0, leaveService.calculateWorkingDays(saturday, sunday));
    }

    @Test
    void calculateWorkingDays_ExcludesSaturday() {
        // Monday to Saturday: Mon, Tue, Wed, Thu, Fri = 5 (Saturday excluded)
        LocalDate monday = LocalDate.of(2026, 9, 21);
        LocalDate saturday = LocalDate.of(2026, 9, 26);
        assertEquals(5, leaveService.calculateWorkingDays(monday, saturday));
    }

    @Test
    void calculateWorkingDays_ExcludesSunday() {
        // Sunday alone = 0
        LocalDate sunday = LocalDate.of(2026, 9, 20);
        assertEquals(0, leaveService.calculateWorkingDays(sunday, sunday));
    }

    // =========================================================
    // Apply Leave Tests
    // =========================================================

    @Test
    void applyLeave_Success() {
        LeaveRequest savedRequest = buildLeaveRequest(LeaveStatus.PENDING);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(activeEmployee));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(savedRequest);

        LeaveResponse response = leaveService.applyLeave(sampleDto);

        assertNotNull(response);
        assertEquals(LeaveStatus.PENDING, response.getStatus());
        assertEquals(1L, response.getDays());  // Mon → Mon = 1 working day
    }

    @Test
    void applyLeave_ZeroWorkingDays_ThrowsBusinessException() {
        // Next Saturday to Sunday → 0 working days
        LocalDate nextSaturday = LocalDate.now().with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.SATURDAY));
        LocalDate nextSunday = LocalDate.now().with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.SUNDAY));
        sampleDto.setStartDate(nextSaturday);
        sampleDto.setEndDate(nextSunday);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(activeEmployee));

        assertThrows(BusinessException.class, () -> leaveService.applyLeave(sampleDto));
    }

    @Test
    void applyLeave_InsufficientBalance_ThrowsBusinessException() {
        activeEmployee.setLeaveBalance(0);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(activeEmployee));

        assertThrows(BusinessException.class, () -> leaveService.applyLeave(sampleDto));
    }

    @Test
    void applyLeave_InactiveEmployee_ThrowsBusinessException() {
        activeEmployee.setActive(false);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(activeEmployee));

        assertThrows(BusinessException.class, () -> leaveService.applyLeave(sampleDto));
    }

    @Test
    void applyLeave_EndBeforeStart_ThrowsBusinessException() {
        sampleDto.setStartDate(LocalDate.of(2026, 9, 25));
        sampleDto.setEndDate(LocalDate.of(2026, 9, 21));  // end before start

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(activeEmployee));

        assertThrows(BusinessException.class, () -> leaveService.applyLeave(sampleDto));
    }

    // =========================================================
    // Approve Leave Tests
    // =========================================================

    @Test
    void approveLeave_Success_BalanceDecremented() {
        LeaveRequest pendingRequest = buildLeaveRequest(LeaveStatus.PENDING);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(leaveRequestRepository.save(any())).thenReturn(pendingRequest);
        when(employeeRepository.save(any())).thenReturn(activeEmployee);

        leaveService.approveLeave(1L);

        // Status should be APPROVED
        assertEquals(LeaveStatus.APPROVED, pendingRequest.getStatus());
        // Balance should decrease by 1 working day (Mon → Mon)
        assertEquals(11, activeEmployee.getLeaveBalance());
    }

    @Test
    void approveLeave_AlreadyApproved_ThrowsBusinessException() {
        LeaveRequest approvedRequest = buildLeaveRequest(LeaveStatus.APPROVED);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(approvedRequest));

        assertThrows(BusinessException.class, () -> leaveService.approveLeave(1L));
    }

    // =========================================================
    // Reject Leave Tests
    // =========================================================

    @Test
    void rejectLeave_Success_BalanceUnchanged() {
        LeaveRequest pendingRequest = buildLeaveRequest(LeaveStatus.PENDING);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(leaveRequestRepository.save(any())).thenReturn(pendingRequest);

        leaveService.rejectLeave(1L, "Project deadline");

        assertEquals(LeaveStatus.REJECTED, pendingRequest.getStatus());
        assertEquals("Project deadline", pendingRequest.getRejectionReason());
        // Balance must NOT change on rejection
        assertEquals(12, activeEmployee.getLeaveBalance());
    }

    @Test
    void rejectLeave_AlreadyRejected_ThrowsBusinessException() {
        LeaveRequest rejectedRequest = buildLeaveRequest(LeaveStatus.REJECTED);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(rejectedRequest));

        assertThrows(BusinessException.class, () -> leaveService.rejectLeave(1L, "reason"));
    }

    @Test
    void rejectLeave_EmptyReason_ThrowsBusinessException() {
        LeaveRequest pendingRequest = buildLeaveRequest(LeaveStatus.PENDING);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));

        assertThrows(BusinessException.class, () -> leaveService.rejectLeave(1L, ""));
    }

    @Test
    void rejectLeave_NullReason_ThrowsBusinessException() {
        LeaveRequest pendingRequest = buildLeaveRequest(LeaveStatus.PENDING);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));

        assertThrows(BusinessException.class, () -> leaveService.rejectLeave(1L, null));
    }

    // =========================================================
    // Helper
    // =========================================================

    /**
     * Helper method: builds a LeaveRequest with Mon Sep 21 → Mon Sep 21 (1 working day).
     */
    private LeaveRequest buildLeaveRequest(LeaveStatus status) {
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(1L);
        leaveRequest.setEmployee(activeEmployee);
        leaveRequest.setLeaveType(LeaveType.CASUAL);
        leaveRequest.setStartDate(LocalDate.of(2026, 9, 21));  // Monday
        leaveRequest.setEndDate(LocalDate.of(2026, 9, 21));    // Monday
        leaveRequest.setReason("Personal work");
        leaveRequest.setStatus(status);
        leaveRequest.setCreatedAt(LocalDateTime.now());
        return leaveRequest;
    }

}
