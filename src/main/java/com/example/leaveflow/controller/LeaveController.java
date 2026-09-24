package com.example.leaveflow.controller;

import com.example.leaveflow.dto.LeaveRequestDto;
import com.example.leaveflow.dto.LeaveResponse;
import com.example.leaveflow.security.AuthUtils;
import com.example.leaveflow.security.AuthenticatedEmployee;
import com.example.leaveflow.service.LeaveService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for leave request management.
 *
 * Authorization:
 *   POST   /api/leaves              → any authenticated user (self only)
 *   GET    /api/leaves              → Admin only
 *   GET    /api/leaves/{id}         → Admin only
 *   PATCH  /api/leaves/{id}/approve → Admin only
 *   PATCH  /api/leaves/{id}/reject  → Admin only
 */
@RestController
@RequestMapping("/api/leaves")
@CrossOrigin(origins = "*")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    /**
     * POST /api/leaves — Submit a leave request.
     * Employees may only submit for themselves; admins cannot apply for leave.
     * Business validations (balance, dates, overlaps) are enforced in LeaveService.
     */
    @PostMapping
    public ResponseEntity<LeaveResponse> applyLeave(
            @Valid @RequestBody LeaveRequestDto dto,
            HttpServletRequest httpRequest) {
        AuthenticatedEmployee auth = AuthUtils.requireAuthenticated(httpRequest);
        if (!auth.id().equals(dto.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You may only submit leave requests for yourself.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.applyLeave(dto));
    }

    /** GET /api/leaves — List all leave requests. Admin only. */
    @GetMapping
    public ResponseEntity<List<LeaveResponse>> getAllLeaves(HttpServletRequest httpRequest) {
        AuthUtils.requireAdmin(httpRequest);
        return ResponseEntity.ok(leaveService.getAllLeaves());
    }

    /** GET /api/leaves/{id} — Get a leave request by ID. Admin only. */
    @GetMapping("/{id}")
    public ResponseEntity<LeaveResponse> getLeaveById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        AuthUtils.requireAdmin(httpRequest);
        return ResponseEntity.ok(leaveService.getLeaveById(id));
    }

    /** PATCH /api/leaves/{id}/approve — Approve a pending leave request. Admin only. */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<LeaveResponse> approveLeave(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        AuthUtils.requireAdmin(httpRequest);
        return ResponseEntity.ok(leaveService.approveLeave(id));
    }

    /** PATCH /api/leaves/{id}/reject — Reject a pending leave request. Admin only. */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<LeaveResponse> rejectLeave(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {
        AuthUtils.requireAdmin(httpRequest);
        return ResponseEntity.ok(leaveService.rejectLeave(id, body.get("rejectionReason")));
    }
}
