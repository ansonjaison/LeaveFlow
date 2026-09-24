package com.example.leaveflow.service;

import com.example.leaveflow.dto.LeaveRequestDto;
import com.example.leaveflow.dto.LeaveResponse;
import com.example.leaveflow.entity.Employee;
import com.example.leaveflow.entity.LeaveRequest;
import com.example.leaveflow.enums.LeaveStatus;
import com.example.leaveflow.enums.Role;
import com.example.leaveflow.exception.BusinessException;
import com.example.leaveflow.exception.ResourceNotFoundException;
import com.example.leaveflow.repository.EmployeeRepository;
import com.example.leaveflow.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveService(LeaveRequestRepository leaveRequestRepository,
                        EmployeeRepository employeeRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
    }

    /**
     * Applies for leave after running all business validations:
     *   1. Employee must exist and be active
     *   2. Admins cannot apply for leave
     *   3. Start date must be today or future
     *   4. End date must be >= start date
     *   5. Range must contain at least one working day
     *   6. Employee must have sufficient balance
     *   7. No overlapping PENDING/APPROVED leave
     */
    @Transactional
    public LeaveResponse applyLeave(LeaveRequestDto dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + dto.getEmployeeId()));

        if (!employee.isActive()) {
            throw new BusinessException("Inactive employees cannot apply for leave.");
        }

        if (employee.getRole() == Role.ADMIN) {
            throw new BusinessException(
                    "Admin accounts cannot apply for leave through this system. " +
                    "Admin leave is managed externally.");
        }

        if (dto.getStartDate().isBefore(LocalDate.now())) {
            throw new BusinessException(
                    "Leave cannot be applied for a past date. Start date must be today or a future date.");
        }

        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessException("End date cannot be before start date.");
        }

        long workingDays = calculateWorkingDays(dto.getStartDate(), dto.getEndDate());

        if (workingDays == 0) {
            throw new BusinessException(
                    "The selected date range contains no working days (Saturday and Sunday are excluded).");
        }

        if (employee.getLeaveBalance() < workingDays) {
            throw new BusinessException("Insufficient leave balance. Available: "
                    + employee.getLeaveBalance() + " day(s), Requested: " + workingDays + " day(s).");
        }

        long overlapping = leaveRequestRepository.countOverlappingLeaves(
                employee.getId(), dto.getStartDate(), dto.getEndDate());
        if (overlapping > 0) {
            throw new BusinessException(
                    "You already have a PENDING or APPROVED leave request that overlaps with the selected dates. " +
                    "Please choose different dates.");
        }

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(dto.getLeaveType());
        leaveRequest.setStartDate(dto.getStartDate());
        leaveRequest.setEndDate(dto.getEndDate());
        leaveRequest.setReason(dto.getReason());
        leaveRequest.setStatus(LeaveStatus.PENDING);
        leaveRequest.setCreatedAt(LocalDateTime.now());
        leaveRequest.setUpdatedAt(LocalDateTime.now());

        return toResponse(leaveRequestRepository.save(leaveRequest));
    }

    public List<LeaveResponse> getAllLeaves() {
        return leaveRequestRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public LeaveResponse getLeaveById(Long id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Leave request not found with id: " + id));
        return toResponse(leaveRequest);
    }

    public List<LeaveResponse> getLeavesByEmployee(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id: " + employeeId);
        }
        return leaveRequestRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Approves a PENDING leave request and deducts working days from the employee's balance.
     * @Transactional ensures both saves (leave status + employee balance) are atomic.
     * Balance is re-checked at approval time — it may have changed since the request was filed.
     */
    @Transactional
    public LeaveResponse approveLeave(Long leaveId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Leave request not found with id: " + leaveId));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException("Only PENDING requests can be approved. Current status: "
                    + leaveRequest.getStatus());
        }

        Employee employee = leaveRequest.getEmployee();
        long workingDays = calculateWorkingDays(leaveRequest.getStartDate(), leaveRequest.getEndDate());

        if (employee.getLeaveBalance() < workingDays) {
            throw new BusinessException("Insufficient leave balance to approve. Available: "
                    + employee.getLeaveBalance() + " day(s), Required: " + workingDays + " day(s).");
        }

        employee.setLeaveBalance(employee.getLeaveBalance() - (int) workingDays);
        employee.setUpdatedAt(LocalDateTime.now());
        employeeRepository.save(employee);

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setUpdatedAt(LocalDateTime.now());
        return toResponse(leaveRequestRepository.save(leaveRequest));
    }

    /** Rejects a PENDING leave request. A reason is required. Balance is not affected. */
    @Transactional
    public LeaveResponse rejectLeave(Long leaveId, String rejectionReason) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Leave request not found with id: " + leaveId));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException("Only PENDING requests can be rejected. Current status: "
                    + leaveRequest.getStatus());
        }

        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new BusinessException("A rejection reason is required.");
        }

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setRejectionReason(rejectionReason);
        leaveRequest.setUpdatedAt(LocalDateTime.now());
        return toResponse(leaveRequestRepository.save(leaveRequest));
    }

    /**
     * Counts working days (Mon–Fri) between startDate and endDate, inclusive.
     * Weekends are excluded; public holidays are not considered.
     */
    public long calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        long count = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            DayOfWeek day = current.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }

    /** Maps a LeaveRequest entity to a response DTO. 'days' is computed, not stored. */
    private LeaveResponse toResponse(LeaveRequest leaveRequest) {
        LeaveResponse response = new LeaveResponse();
        response.setId(leaveRequest.getId());
        response.setEmployeeId(leaveRequest.getEmployee().getId());
        response.setEmployeeName(leaveRequest.getEmployee().getName());
        response.setLeaveType(leaveRequest.getLeaveType());
        response.setStartDate(leaveRequest.getStartDate());
        response.setEndDate(leaveRequest.getEndDate());
        response.setDays(calculateWorkingDays(leaveRequest.getStartDate(), leaveRequest.getEndDate()));
        response.setReason(leaveRequest.getReason());
        response.setStatus(leaveRequest.getStatus());
        response.setRejectionReason(leaveRequest.getRejectionReason());
        response.setCreatedAt(leaveRequest.getCreatedAt());
        return response;
    }
}
