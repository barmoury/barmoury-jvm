package io.github.barmoury.api.config;

import io.github.barmoury.api.model.UserDetails;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public abstract class JwtRequestFilter extends OncePerRequestFilter {

    Map<String, List<String>> openUrlMatchers = new HashMap<>();

    public abstract JwtTokenUtil getJwtTokenUtil();
    public abstract void processResponse(HttpServletResponse httpServletResponse, String message, int status) throws IOException;

    public boolean validate(HttpServletRequest httpServletRequest, UserDetails<?> userDetails) {
        return true;
    }

    public String getAuthoritiesPrefix() {
        return "ROLE_";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String header = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            processResponse(httpServletResponse, "Authorization token is missing", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final String token = header.split(" ")[1].trim();
        UserDetails<?> userDetails;
        try {
            userDetails = getJwtTokenUtil().validate(token);
        } catch (ExpiredJwtException ex) {
            processResponse(httpServletResponse, "The authorization token has expired", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if (userDetails == null) {
            processResponse(httpServletResponse, "Invalid Authorization token", HttpServletResponse.SC_UNAUTHORIZED);
            filterChain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }

        userDetails.setAuthorityPrefix(getAuthoritiesPrefix());
        if (!validate(httpServletRequest, userDetails)) {
            processResponse(httpServletResponse, "User details validation failed", HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        UsernamePasswordAuthenticationToken
                authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null,
                userDetails.getAuthorities());

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    Map<String, List<String>> getOpenUrlMatchers() {
        return openUrlMatchers;
    }

    void addOpenUrlPattern(String method, String path) {
        openUrlMatchers.computeIfAbsent(method, k -> new ArrayList<>());
        openUrlMatchers.get(method).add(path);
    }

    public void addOpenUrlPattern(String path) {
        addOpenUrlPattern("ANY", path);
    }

    public void addOpenUrlPattern(RequestMethod requestMethod, String path) {
        String method = requestMethod.name();
        addOpenUrlPattern(method, path);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        AntPathMatcher matcher = new AntPathMatcher();
        String route = request.getRequestURI().replaceAll(request.getContextPath(), "");
        List<String> paths = openUrlMatchers.getOrDefault("ANY", new ArrayList<>());
        paths.addAll(openUrlMatchers.getOrDefault(method, new ArrayList<>()));
        for (String path : paths) {
            if (matcher.match(path, route)) return true;
        }
        return false;
    }

}
