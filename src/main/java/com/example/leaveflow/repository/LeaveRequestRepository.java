package com.example.leaveflow.repository;

import com.example.leaveflow.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for LeaveRequest database operations.
 *
 * Extends JpaRepository to inherit standard CRUD operations.
 *
 * findByEmployeeId: Spring Data JPA traverses the @ManyToOne relationship
 * and generates: SELECT * FROM leave_requests WHERE employee_id = ?
 */
@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    // Returns all leave requests belonging to a specific employee.
    // Used for the employee's leave history view.
    List<LeaveRequest> findByEmployeeId(Long employeeId);

    /**
     * Counts how many PENDING or APPROVED leave requests for a given employee
     * overlap with the given date range.
     *
     * Overlap condition (standard interval overlap logic):
     *   existingStart <= newEnd   AND   existingEnd >= newStart
     *
     * We exclude REJECTED leaves because a rejected leave
     * means those dates are free to apply again.
     *
     * Used in LeaveService.applyLeave() to enforce the "no duplicate leave" rule.
     *
     * JPQL (not SQL): we reference the Java entity and enum class names.
     */
    @Query("SELECT COUNT(l) FROM LeaveRequest l " +
           "WHERE l.employee.id = :empId " +
           "AND l.status <> com.example.leaveflow.enums.LeaveStatus.REJECTED " +
           "AND l.startDate <= :endDate " +
           "AND l.endDate >= :startDate")
    long countOverlappingLeaves(
            @Param("empId")      Long      employeeId,
            @Param("startDate")  LocalDate startDate,
            @Param("endDate")    LocalDate endDate
    );

}
