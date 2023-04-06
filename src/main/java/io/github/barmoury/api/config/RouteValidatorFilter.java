package io.github.barmoury.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public abstract class RouteValidatorFilter extends OncePerRequestFilter {

    Map<String, ValidationExecutor> routeValidationExecutors = new HashMap<>();

    public abstract void processResponse(HttpServletResponse httpServletResponse, String message) throws IOException;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        String route = request.getRequestURI().replaceAll(request.getContextPath(), "");
        ValidationExecutor validationExecutor = routeValidationExecutors.get(String.format("%s<=#=>%s", method, route));
        if (validationExecutor != null && !validationExecutor.valid(request)) {
            processResponse(response, "Validation failed for the request");
            return;
        }
        filterChain.doFilter(request, response);
    }

    void registerRouteValidation(String method, String path, ValidationExecutor validationExecutor) {
        routeValidationExecutors.put(String.format("%s<=#=>%s", method, path), validationExecutor);
    }

    public void registerRouteValidation(String path, ValidationExecutor validationExecutor) {
        registerRouteValidation("ANY", path, validationExecutor);
    }

    public void registerRouteValidation(RequestMethod requestMethod, String path, ValidationExecutor validationExecutor) {
        String method = requestMethod.name();
        registerRouteValidation(method, path, validationExecutor);
    }

    public interface ValidationExecutor {
        boolean valid(HttpServletRequest request);
    }

}
