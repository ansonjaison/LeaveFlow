package com.example.leaveflow.security;

import com.example.leaveflow.entity.Employee;
import com.example.leaveflow.repository.EmployeeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Base64;
import java.util.Optional;

/**
 * Validates Supabase JWTs on every incoming request (ES256 / ECDSA).
 *
 * At startup, fetches the EC public key from Supabase's JWKS endpoint
 * ({@code /auth/v1/.well-known/jwks.json}) and uses it to verify token signatures.
 * On success, stores an {@link AuthenticatedEmployee} as a request attribute for
 * controllers to read via {@link AuthUtils}.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    public static final String ATTR_AUTH_EMPLOYEE = "authenticatedEmployee";

    private final PublicKey publicKey;
    private final EmployeeRepository employeeRepository;

    public JwtFilter(
            @Value("${supabase.url}") String supabaseUrl,
            EmployeeRepository employeeRepository) {
        this.publicKey = fetchJwksPublicKey(supabaseUrl);
        this.employeeRepository = employeeRepository;
    }

    private PublicKey fetchJwksPublicKey(String supabaseUrl) {
        try {
            String jwksUrl = supabaseUrl + "/auth/v1/.well-known/jwks.json";

            String json = new String(
                    URI.create(jwksUrl).toURL().openStream().readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8
            );

            JsonNode root = new ObjectMapper().readTree(json);
            JsonNode keys = root.get("keys");

            if (keys == null || keys.isEmpty()) {
                throw new RuntimeException("No keys found in JWKS response");
            }

            JsonNode key = keys.get(0);
            if (!"EC".equals(key.get("kty").asText())) {
                throw new RuntimeException("Expected EC key type in JWKS");
            }

            byte[] xBytes = Base64.getUrlDecoder().decode(key.get("x").asText());
            byte[] yBytes = Base64.getUrlDecoder().decode(key.get("y").asText());

            ECPoint point = new ECPoint(new BigInteger(1, xBytes), new BigInteger(1, yBytes));
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec ecSpec = parameters.getParameterSpec(ECParameterSpec.class);

            return KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(point, ecSpec));

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch JWKS public key from Supabase: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Claims claims = Jwts.parser()
                        .verifyWith(publicKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String email = claims.get("email", String.class);
                if (email != null) {
                    Optional<Employee> employeeOpt = employeeRepository.findByEmail(email);
                    if (employeeOpt.isPresent()) {
                        Employee employee = employeeOpt.get();
                        request.setAttribute(ATTR_AUTH_EMPLOYEE,
                                new AuthenticatedEmployee(employee.getId(), employee.getEmail(), employee.getRole()));
                    }
                }

            } catch (JwtException ex) {
                writeUnauthorized(response, "Invalid or expired token.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":401,\"message\":\"" + message + "\"}");
    }
}
