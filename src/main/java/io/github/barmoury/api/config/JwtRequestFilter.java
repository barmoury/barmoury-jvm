package io.github.barmoury.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.barmoury.api.model.ApiResponse;
import io.github.barmoury.api.model.UserDetails;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    @Autowired ObjectMapper objectMapper;
    Map<String, List<String>> openUrlMatchers = new HashMap<>();

    @Deprecated
    public JwtTokenUtil getJwtTokenUtil() {
        return null;
    }

    public JwtTokenUtil getJwtTokenUtil(String key) {
        return getJwtTokenUtil();
    }

    public String[] getTokenKeys() {
        return new String[]{null};
    }

    public void processResponse(HttpServletResponse httpServletResponse, String message, int status) throws IOException {
        httpServletResponse.setStatus(status);
        httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        List<Object> errors = new ArrayList<>(); errors.add(message);
        objectMapper.writeValue(httpServletResponse.getWriter(), new ApiResponse<>(errors));
    }

    public boolean validate(HttpServletRequest httpServletRequest, String key, UserDetails<?> userDetails) {
        return validate(httpServletRequest, userDetails);
    }

    public boolean validate(HttpServletRequest httpServletRequest, UserDetails<?> userDetails) {
        return true;
    }

    public String getAuthoritiesPrefix(String key) {
        return getAuthoritiesPrefix();
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
        String[] tokenKeys = getTokenKeys();
        for (int index = 0; index < tokenKeys.length; index++) {
            String key = tokenKeys[index];
            UserDetails<?> userDetails = null;
            try {
                userDetails = getJwtTokenUtil(key).validate(key, token);
            } catch (ExpiredJwtException ex) {
                processResponse(httpServletResponse, "The authorization token has expired", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            } catch (MalformedJwtException ex) {
                processResponse(httpServletResponse, "The authorization token is malformed", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            } catch (SignatureException ex) {
                if (index == tokenKeys.length - 1) {
                    processResponse(httpServletResponse, "Access denied. Suspicious request detected", HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            } catch (Exception ex) {
                processResponse(httpServletResponse, "The JWT authorization failed", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            if (userDetails == null) {
                if (index == tokenKeys.length - 1) {
                    processResponse(httpServletResponse, "Invalid Authorization token", HttpServletResponse.SC_UNAUTHORIZED);
                    filterChain.doFilter(httpServletRequest, httpServletResponse);
                    return;
                }
                continue;
            }

            userDetails.setAuthorityPrefix(getAuthoritiesPrefix(key));
            if (!validate(httpServletRequest, key, userDetails)) {
                processResponse(httpServletResponse, "User details validation failed", HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            UsernamePasswordAuthenticationToken
                    authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null,
                    userDetails.getAuthorities());

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            break;
        }
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
        List<String> paths = new ArrayList<>();
        paths.addAll(openUrlMatchers.getOrDefault(method, new ArrayList<>()));
        paths.addAll(openUrlMatchers.getOrDefault("ANY", new ArrayList<>()));
        for (String path : paths) {
            if (matcher.match(path, route)) return true;
        }
        return false;
    }

}
