package com.example.leaveflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the LeaveFlow application.
 *
 * @SpringBootApplication is a convenience annotation that combines:
 *   - @Configuration       : marks this as a Spring configuration class
 *   - @EnableAutoConfiguration : tells Spring Boot to auto-configure beans
 *   - @ComponentScan       : scans this package and sub-packages for Spring components
 */
@SpringBootApplication
public class LeaveFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaveFlowApplication.class, args);
    }

}
