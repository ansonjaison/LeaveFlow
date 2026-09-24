package com.example.leaveflow.service;

import com.example.leaveflow.dto.EmployeeRequest;
import com.example.leaveflow.dto.EmployeeResponse;
import com.example.leaveflow.entity.Employee;
import com.example.leaveflow.enums.Role;
import com.example.leaveflow.exception.BusinessException;
import com.example.leaveflow.exception.ResourceNotFoundException;
import com.example.leaveflow.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmployeeService.
 *
 * @ExtendWith(MockitoExtension.class) enables Mockito annotations.
 * @Mock creates a mock EmployeeRepository — no real database involved.
 * @InjectMocks creates EmployeeService and injects the mock repository into it.
 *
 * Each test verifies a specific behaviour of the service in isolation.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SupabaseAuthService supabaseAuthService;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee sampleEmployee;
    private EmployeeRequest sampleRequest;

    @BeforeEach
    void setUp() {
        // A reusable employee object for tests
        sampleEmployee = new Employee();
        sampleEmployee.setId(1L);
        sampleEmployee.setEmployeeCode("EMP001");
        sampleEmployee.setName("Anson Jaison");
        sampleEmployee.setEmail("anson@example.com");
        sampleEmployee.setDepartment("Development");
        sampleEmployee.setRole(Role.EMPLOYEE);
        sampleEmployee.setLeaveBalance(12);
        sampleEmployee.setActive(true);

        // A reusable request DTO for tests
        sampleRequest = new EmployeeRequest();
        sampleRequest.setEmployeeCode("EMP001");
        sampleRequest.setName("Anson Jaison");
        sampleRequest.setEmail("anson@example.com");
        sampleRequest.setDepartment("Development");
        sampleRequest.setRole(Role.EMPLOYEE);
    }

    // --- Test: Create Employee ---

    @Test
    void createEmployee_Success() {
        when(employeeRepository.existsByEmployeeCode("EMP001")).thenReturn(false);
        when(employeeRepository.existsByEmail("anson@example.com")).thenReturn(false);
        when(supabaseAuthService.generateDefaultPassword("Anson Jaison", "EMP001")).thenReturn("nosiaJ nosna@100");
        doNothing().when(supabaseAuthService).createAuthUser("anson@example.com", "nosiaJ nosna@100");
        when(employeeRepository.save(any(Employee.class))).thenReturn(sampleEmployee);

        EmployeeResponse response = employeeService.createEmployee(sampleRequest);

        assertNotNull(response);
        assertEquals("EMP001", response.getEmployeeCode());
        assertEquals("Anson Jaison", response.getName());
        assertEquals(12, response.getLeaveBalance());
        assertTrue(response.isActive());
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    void createEmployee_DuplicateEmployeeCode_ThrowsBusinessException() {
        when(employeeRepository.existsByEmployeeCode("EMP001")).thenReturn(true);

        assertThrows(BusinessException.class, () -> employeeService.createEmployee(sampleRequest));

        // save() should never be called if code already exists
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createEmployee_DuplicateEmail_ThrowsBusinessException() {
        when(employeeRepository.existsByEmployeeCode("EMP001")).thenReturn(false);
        when(employeeRepository.existsByEmail("anson@example.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> employeeService.createEmployee(sampleRequest));
        verify(employeeRepository, never()).save(any());
    }

    // --- Test: Get Employee ---

    @Test
    void getEmployeeById_Success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));

        EmployeeResponse response = employeeService.getEmployeeById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("EMP001", response.getEmployeeCode());
    }

    @Test
    void getEmployeeById_NotFound_ThrowsResourceNotFoundException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.getEmployeeById(99L));
    }

    // --- Test: Search Employee by Code ---

    @Test
    void getEmployeeByCode_Success() {
        when(employeeRepository.findByEmployeeCode("EMP001")).thenReturn(Optional.of(sampleEmployee));

        EmployeeResponse response = employeeService.getEmployeeByCode("EMP001");

        assertNotNull(response);
        assertEquals("EMP001", response.getEmployeeCode());
    }

    @Test
    void getEmployeeByCode_NotFound_ThrowsResourceNotFoundException() {
        when(employeeRepository.findByEmployeeCode("INVALID")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.getEmployeeByCode("INVALID"));
    }

    // --- Test: Deactivate Employee ---

    @Test
    void deactivateEmployee_SetsActiveToFalse() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(sampleEmployee);

        employeeService.deactivateEmployee(1L);

        // Verify that the employee saved to the repository has active = false
        verify(employeeRepository).save(argThat(emp -> !emp.isActive()));
    }

    // --- Test: Update Employee ---

    @Test
    void updateEmployee_Success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(sampleEmployee);

        EmployeeResponse response = employeeService.updateEmployee(1L, sampleRequest);

        assertNotNull(response);
        verify(employeeRepository).save(any(Employee.class));
    }

}
