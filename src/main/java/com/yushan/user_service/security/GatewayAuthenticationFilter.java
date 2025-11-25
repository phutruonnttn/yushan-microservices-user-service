package com.yushan.user_service.security;

import com.yushan.user_service.entity.User;
import com.yushan.user_service.repository.UserRepository;
import com.yushan.user_service.util.HmacUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Gateway Authentication Filter
 * 
 * This filter trusts requests that have been validated by the API Gateway.
 * 
 * Flow:
 * 1. Check if request has X-Gateway-Validated header (gateway already validated)
 * 2. If yes, extract user info from gateway headers (X-User-Id, X-User-Email, X-User-Role)
 * 3. Load user from database to ensure user still exists and is active
 * 4. Set authentication in SecurityContext
 * 5. If no gateway header, fallback to JWT validation (backward compatibility)
 * 
 * This filter should run BEFORE JwtAuthenticationFilter
 */
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private UserRepository userRepository;

    /**
     * Shared secret for HMAC signature verification
     * Must match the secret in API Gateway
     */
    @Value("${gateway.hmac.secret:${GATEWAY_HMAC_SECRET:yushan-gateway-hmac-secret-key-for-request-signature-2024}}")
    private String hmacSecret;

    /**
     * Filter method that processes each request
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain Filter chain
     * @throws ServletException if servlet error occurs
     * @throws IOException if I/O error occurs
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, 
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Check if request is gateway-validated
            String gatewayValidated = request.getHeader("X-Gateway-Validated");
            
            if ("true".equals(gatewayValidated)) {
                // Extract user info from gateway headers
                String userIdStr = request.getHeader("X-User-Id");
                String email = request.getHeader("X-User-Email");
                String role = request.getHeader("X-User-Role");
                String timestampStr = request.getHeader("X-Gateway-Timestamp");
                String signature = request.getHeader("X-Gateway-Signature");
                
                // Security: Verify HMAC signature to prevent header forgery
                if (userIdStr == null || email == null || timestampStr == null || signature == null) {
                    logger.warn("Gateway-validated request but missing required headers (userId, email, timestamp, or signature)");
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Invalid gateway headers\"}");
                    return;
                }
                
                try {
                    long timestamp = Long.parseLong(timestampStr);
                    
                    // Verify HMAC signature
                    if (!HmacUtil.verifySignature(userIdStr, email, role, timestamp, signature, hmacSecret)) {
                        logger.warn("Gateway-validated request with invalid HMAC signature from IP: " + 
                                   request.getRemoteAddr() + " for path: " + request.getRequestURI());
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Invalid gateway signature\"}");
                        return;
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Invalid timestamp format in gateway headers: " + timestampStr);
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Invalid timestamp format\"}");
                    return;
                }
                
                // Signature verified - trust gateway headers and load user from DB
                try {
                    UUID userId = UUID.fromString(userIdStr);
                    
                    // Load user from database to ensure user still exists and is active
                    User user = userRepository.findById(userId);
                    
                    if (user != null) {
                        // Create CustomUserDetails from User
                        CustomUserDetailsService.CustomUserDetails userDetails = 
                            new CustomUserDetailsService.CustomUserDetails(user);
                        
                        // Check if user is enabled (not suspended/banned) - same as JwtAuthenticationFilter
                        if (!userDetails.isEnabled()) {
                            // User is disabled, reject request with 403 Forbidden
                            logger.warn("Gateway-validated request but user is disabled (status: " + user.getStatus() + ") for user: " + userIdStr + " from IP: " + request.getRemoteAddr() + " for path: " + request.getRequestURI());
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"User account is disabled or suspended\",\"status\":403}");
                            return;
                        }
                        
                        // Create authentication object
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                userDetails, 
                                null, 
                                userDetails.getAuthorities()
                            );
                        
                        // Set additional details
                        authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        
                        // Set authentication in SecurityContext
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        logger.debug("Gateway-validated request authenticated for user: " + email + " (" + userId + ")");
                    } else {
                        // User not found, reject request with 403 Forbidden
                        logger.warn("Gateway-validated request but user not found: " + userIdStr + " from IP: " + request.getRemoteAddr() + " for path: " + request.getRequestURI());
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"User account not found\",\"status\":403}");
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid user ID format from gateway: " + userIdStr, e);
                }
            }
            // If not gateway-validated, let JwtAuthenticationFilter handle it (backward compatibility)
            
        } catch (Exception e) {
            // Log error but don't stop the request
            logger.error("Cannot set gateway authentication: " + e.getMessage(), e);
        }
        
        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Check if the request should be filtered
     * Skip filtering for certain paths (like login, register)
     * 
     * @param request HTTP request
     * @return true if should skip filtering, false otherwise
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Skip filtering for these paths (same as JwtAuthenticationFilter)
        return path.startsWith("/api/v1/auth/login") ||
               path.startsWith("/api/v1/auth/register") ||
               path.startsWith("/api/v1/auth/refresh") ||
               path.startsWith("/api/v1/public/") ||
               path.startsWith("/actuator/") ||
               path.equals("/error") ||
               // Skip OPTIONS requests (CORS preflight)
               "OPTIONS".equals(method);
    }
}

