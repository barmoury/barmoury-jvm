package io.github.barmoury.api.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.barmoury.audit.Audit;
import io.github.barmoury.audit.Auditor;
import io.github.barmoury.trace.Device;
import io.github.barmoury.trace.IpData;
import io.github.barmoury.trace.Isp;
import io.github.barmoury.trace.Location;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;
import java.util.*;

@ControllerAdvice
public abstract class RequestAuditorAdapter extends RequestBodyAdviceAdapter implements HandlerInterceptor {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    HttpServletRequest request;

    @Autowired
    HttpServletRequest httpServletRequest;

    Map<String, List<String>> excludeUrlPatterns = new HashMap<>();

    public abstract Auditor<Object> getAuditor();
    public abstract IpData getIpData(String ipAddress);

    public <T> Audit<T> resolve(HttpServletRequest request, Audit<T> audit) {
        return audit;
    }

    public <T> T beforeAuditable(T object) {
        return object;
    }

    public Object headerSanitizer(String headerName, Object value) {
        if (headerName.toLowerCase().contains("authorization")
                || headerName.toLowerCase().contains("key")) {
            return "**********";
        }
        return value;
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type type,
                            Class<? extends HttpMessageConverter<?>> aClass) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {

        if (shouldNotAudit(httpServletRequest)) return true;
        String action = httpServletRequest.getMethod();
        String source = httpServletRequest.getRequestURI();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.set("headers", objectMapper.convertValue(resolveHeaders(httpServletRequest), JsonNode.class));
        objectNode.set("parameters", objectMapper.convertValue(httpServletRequest.getParameterMap(), JsonNode.class));
        IpData ipData = this.getIpData(httpServletRequest.getRemoteAddr());
        Object auditableNode = objectMapper.convertValue(body, Object.class);
        getAuditor().audit(resolve(request, Audit.builder()
                .type("HTTP.REQUEST")
                .isp(ipData.getIsp())
                .extraData(objectNode)
                .location(ipData.getLocation())
                .auditable(beforeAuditable(auditableNode))
                .source(source != null ? source : "UNKNOWN")
                .action(action != null ? action : "UNKNOWN")
                .ipAddress(httpServletRequest.getRemoteAddr())
                .device(Device.build(httpServletRequest.getHeader("User-Agent"))).build()));
        return body;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (shouldNotAudit(request)) return true;
        if (DispatcherType.REQUEST.name().equals(request.getDispatcherType().name())
                && request.getMethod().equals(HttpMethod.GET.name())) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.set("headers", objectMapper.convertValue(resolveHeaders(httpServletRequest), JsonNode.class));
            objectNode.set("parameters", objectMapper.convertValue(httpServletRequest.getParameterMap(), JsonNode.class));
            IpData ipData = this.getIpData(httpServletRequest.getRemoteAddr());
            getAuditor().audit(resolve(request, Audit.builder()
                    .type("HTTP.REQUEST")
                    .isp(ipData.getIsp())
                    .extraData(objectNode)
                    .location(ipData.getLocation())
                    .action(request.getMethod())
                    .source(request.getRequestURI())
                    .ipAddress(request.getRemoteAddr())
                    .device(Device.build(request.getHeader("User-Agent"))).build()));
        }
        return true;
    }

    public void excludeUrlPattern(RequestMethod requestMethod, String path) {
        String method = requestMethod.name();
        excludeUrlPatterns.computeIfAbsent(method, k -> new ArrayList<>());
        excludeUrlPatterns.get(method).add(path);
    }

    public void excludeUrlPattern(String path) {
        excludeUrlPatterns.computeIfAbsent("ANY", k -> new ArrayList<>());
        excludeUrlPatterns.get("ANY").add(path);
    }

    protected synchronized boolean shouldNotAudit(HttpServletRequest request) {
        String method = request.getMethod();
        AntPathMatcher matcher = new AntPathMatcher();
        String route = request.getRequestURI().replaceAll(request.getContextPath(), "/");
        List<String> paths = excludeUrlPatterns.getOrDefault("ANY", new ArrayList<>());
        paths.addAll(excludeUrlPatterns.getOrDefault(method, new ArrayList<>()));
        for (String path : paths) {
            if (matcher.match(path, route)) return true;
        }
        return false;
    }

    Map<String, String> resolveHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, String.valueOf(headerSanitizer(headerName, request.getHeader(headerName))));
        }
        return headers;
    }

}
