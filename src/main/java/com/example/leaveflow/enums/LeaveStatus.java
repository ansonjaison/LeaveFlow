package com.example.leaveflow.enums;

/**
 * Represents the lifecycle status of a leave request.
 *
 * Every new leave request starts as PENDING.
 * An admin can then move it to APPROVED or REJECTED.
 * Once approved or rejected, the status cannot be changed.
 */
public enum LeaveStatus {
    PENDING,
    APPROVED,
    REJECTED
}
