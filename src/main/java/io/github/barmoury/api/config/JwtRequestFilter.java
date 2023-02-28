package io.github.barmoury.api.config;

import io.github.barmoury.api.exception.SubModelResolveException;
import io.github.barmoury.api.model.BarmouryUserDetails;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class JwtRequestFilter extends OncePerRequestFilter {

    Map<String, List<String>> openUrlMatchers = new HashMap<>();

    public abstract String getContextPath();
    public abstract JwtTokenUtil getJwtTokenUtil();
    public abstract boolean validate(HttpServletRequest httpServletRequest, BarmouryUserDetails<?> userDetails);
    public abstract void processResponse(HttpServletResponse httpServletResponse, String message) throws IOException;

    public String getAuthoritiesPrefix() {
        return "ROLE_";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String header = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            processResponse(httpServletResponse, "Authorization token is missing");
            return;
        }

        final String token = header.split(" ")[1].trim();
        BarmouryUserDetails<?> barmouryUserDetails;
        try {
            barmouryUserDetails = getJwtTokenUtil().validate(token);
        } catch (ExpiredJwtException ex) {
            processResponse(httpServletResponse, "The authorization token has expired");
            return;
        }
        if (barmouryUserDetails == null) {
            processResponse(httpServletResponse, "Invalid Authorization token");
            filterChain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }

        barmouryUserDetails.setAuthorityPrefix(getAuthoritiesPrefix());
        if (!validate(httpServletRequest, barmouryUserDetails)) {
            httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            processResponse(httpServletResponse, "user details validation failed");
            return;
        }

        UsernamePasswordAuthenticationToken
                authentication = new UsernamePasswordAuthenticationToken(
                barmouryUserDetails, null,
                barmouryUserDetails.getAuthorities());

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    Map<String, List<String>> getOpenUrlMatchers() {
        return openUrlMatchers;
    }

    public void addOpenUrl(String path) {
        String method = "ANY";
        if (!openUrlMatchers.containsKey(method)) openUrlMatchers.put(method, new ArrayList<>());
        openUrlMatchers.get(method).add(path);
    }

    public void addOpenUrl(String method, String path) {
        if (!openUrlMatchers.containsKey(method)) openUrlMatchers.put(method, new ArrayList<>());
        openUrlMatchers.get(method).add(path);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        AntPathMatcher matcher = new AntPathMatcher();
        String route = request.getRequestURI().replaceAll(getContextPath(), "/");
        List<String> paths = openUrlMatchers.getOrDefault("ANY", new ArrayList<>());
        paths.addAll(openUrlMatchers.getOrDefault(method, new ArrayList<>()));
        for (String path : paths) {
            if (matcher.match(path, route)) return true;
        }
        return false;
    }

}
