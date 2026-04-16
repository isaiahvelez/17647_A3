package edu.cmu.bookstore.web_app_bff;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestValidationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestValidationFilter.class);
    private final JwtValidator jwtValidator;

    public RequestValidationFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String clientType = request.getHeader("X-Client-Type");
        log.info("Incoming Web-BFF Request: {} {}?{} | X-Client-Type: {}", 
            request.getMethod(), request.getRequestURI(), request.getQueryString(), clientType);

        // Checking the token here keeps the backend services focused on bookstore behavior.
        if (!jwtValidator.isValidBearerToken(request.getHeader("Authorization"))) {
            log.warn("Unauthorized: JWT validation failed for {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // This BFF only exists for browser traffic, so it should stop anything else right away.
        if (clientType == null || !"Web".equalsIgnoreCase(clientType)) {
            log.warn("Blocking request due to invalid X-Client-Type: {}", clientType);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
