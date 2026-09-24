package com.example.leaveflow.controller;

import com.example.leaveflow.dto.EmployeeRequest;
import com.example.leaveflow.dto.EmployeeResponse;
import com.example.leaveflow.enums.Role;
import com.example.leaveflow.exception.ResourceNotFoundException;
import com.example.leaveflow.repository.EmployeeRepository;
import com.example.leaveflow.security.JwtFilter;
import com.example.leaveflow.service.EmployeeService;
import com.example.leaveflow.service.LeaveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller layer tests using MockMvc.
 *
 * @WebMvcTest(EmployeeController.class) loads only the web layer:
 *   - EmployeeController
 *   - GlobalExceptionHandler (@RestControllerAdvice is included automatically)
 *
 * Services and the JwtFilter are mocked with @MockBean — no real database,
 * business logic, or JWT validation runs.
 *
 * These tests verify that:
 *   - Correct HTTP status codes are returned
 *   - The response JSON contains expected fields
 *   - Validation errors produce 400 responses
 *   - Not-found errors produce 404 responses
 */
@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private LeaveService leaveService;

    @MockBean
    private JwtFilter jwtFilter;

    @MockBean
    private EmployeeRepository employeeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private EmployeeResponse sampleResponse;
    private EmployeeRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = new EmployeeResponse();
        sampleResponse.setId(1L);
        sampleResponse.setEmployeeCode("EMP001");
        sampleResponse.setName("Anson Jaison");
        sampleResponse.setEmail("anson@example.com");
        sampleResponse.setDepartment("Development");
        sampleResponse.setRole(Role.EMPLOYEE);
        sampleResponse.setLeaveBalance(12);
        sampleResponse.setActive(true);

        sampleRequest = new EmployeeRequest();
        sampleRequest.setEmployeeCode("EMP001");
        sampleRequest.setName("Anson Jaison");
        sampleRequest.setEmail("anson@example.com");
        sampleRequest.setDepartment("Development");
        sampleRequest.setRole(Role.EMPLOYEE);
    }

    @Test
    void createEmployee_Returns201() throws Exception {
        when(employeeService.createEmployee(any(EmployeeRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeCode").value("EMP001"))
                .andExpect(jsonPath("$.name").value("Anson Jaison"))
                .andExpect(jsonPath("$.leaveBalance").value(12));
    }

    @Test
    void getEmployeeById_Returns200() throws Exception {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.employeeCode").value("EMP001"));
    }

    @Test
    void getEmployeeById_Returns404_WhenNotFound() throws Exception {
        when(employeeService.getEmployeeById(99L))
                .thenThrow(new ResourceNotFoundException("Employee not found with id: 99"));

        mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Employee not found with id: 99"));
    }

    @Test
    void getEmployeeByCode_Returns200() throws Exception {
        when(employeeService.getEmployeeByCode("EMP001")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/employees/code/EMP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCode").value("EMP001"))
                .andExpect(jsonPath("$.name").value("Anson Jaison"));
    }

    @Test
    void createEmployee_Returns400_WhenNameIsBlank() throws Exception {
        sampleRequest.setName("");  // Violates @NotBlank on EmployeeRequest.name

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

}
