package com.example.leaveflow.service;

import com.example.leaveflow.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages Supabase Auth users via the Supabase Admin REST API.
 * Uses the service_role key to programmatically create users without email verification.
 */
@Service
public class SupabaseAuthService {

    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final RestTemplate restTemplate;

    public SupabaseAuthService(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-role-key}") String serviceRoleKey) {
        this.supabaseUrl = supabaseUrl;
        this.serviceRoleKey = serviceRoleKey;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Creates a new user in Supabase Auth.
     * Sets email_confirm = true so the user can log in immediately.
     */
    public void createAuthUser(String email, String password) {
        String url = supabaseUrl + "/auth/v1/admin/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("apikey", serviceRoleKey);

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("email_confirm", true);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(url, request, Map.class);
        } catch (HttpClientErrorException e) {
            throw new BusinessException(
                    "Could not create login account in Supabase Auth: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new BusinessException(
                    "Could not reach Supabase Auth service: " + e.getMessage());
        }
    }

    /**
     * Generates a default password: reversedName@reversedDigitsOfEmployeeCode
     * Example: "Anson Jaison", "EMP001" → "nosiajnosna@100"
     */
    public String generateDefaultPassword(String name, String employeeCode) {
        String cleanName = name.toLowerCase().replaceAll("\\s+", "");
        String revName = new StringBuilder(cleanName).reverse().toString();

        String digits = employeeCode.replaceAll("[^0-9]", "");
        String revDigits = new StringBuilder(digits).reverse().toString();

        return revName + "@" + revDigits;
    }
}
