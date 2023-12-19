package io.github.barmoury.api.controller;

import io.github.barmoury.api.exception.InvalidBactuatorQueryException;
import io.github.barmoury.api.model.Model;
import io.github.barmoury.eloquent.RequestParamFilter;
import io.github.barmoury.eloquent.StatQuery;
import io.github.barmoury.util.FieldUtil;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO add field to fetch class with their query and stat props and fields, at startup once
@Transactional
public abstract class BactuatorController {
    
    static String SQL_QUERY_SUCCESSFUL = "query successfully";
    static String SQL_QUERY_ERROR_MESSAGE = "You do not have the '%s' permission to perform this operation";

    Map<String, Object> introspect;
    Map<String, Object> resourcesMap;
    Map<String, Object> controllersMap;
    @Value("${server.servlet.context-path:}") String contextPath;
    @Value("${spring.jackson.property-naming-strategy:CAMEL_CASE}") String namingStrategy;

    public abstract String location();
    public abstract String apiBaseUrl();
    public abstract String serviceName();
    public abstract boolean isServiceOk();
    public abstract long downloadsCount();
    public abstract String iconLocation();
    public abstract String healthEndpoint();
    public abstract String serviceApiName();
    public abstract String serviceDescription();
    public abstract String databaseQueryRoute();
    public abstract EntityManager getEntityManager();
    public abstract List<Map<String, String>> logUrls();
    public abstract String databaseMultipleQueryRoute();
    public abstract Map<String, Integer> userStatistics();
    public abstract Map<String, Integer> earningStatistics();
    public abstract List<Class<? extends Model>> resources();
    public abstract String buildTableColumnQuery(String tableName);
    public abstract boolean principalCan(HttpServletRequest httpServletRequest, String dbMethod);
    public abstract <T> ResponseEntity<?> processResponse(HttpStatus httpStatus, T data, String message);
    public abstract List<Class<? extends Controller<? extends Model, ? extends Model.Request>>> controllers();

    @RequestMapping(value = "/health", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        boolean serviceIsOk = isServiceOk();
        response.put("status", serviceIsOk ? "ok" : "not ok");
        return processResponse(serviceIsOk ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR,
                response, "health check successful");
    }

    @RequestMapping(value = "/introspect", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> introspect() {
        if (introspect == null) {
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            resolveControllers(baseUrl);
            resolveResources();
            resolveIntrospect();
        }
        introspect.put("users", userStatistics());
        introspect.put("earnings", earningStatistics());
        introspect.put(resolveCasing("downloadCounts"), downloadsCount());
        return processResponse(HttpStatus.OK, introspect, "introspect data fetched successfully");
    }

    Object executeQueryForResult(HttpServletRequest httpServletRequest, String queryString,
                                 boolean includeColumnNames) {
        try {
            Query query = getEntityManager().createNativeQuery(queryString);
            if (queryString.toLowerCase().contains("update")) {
                if (!principalCan(httpServletRequest, "UPDATE")) {
                    throw new RuntimeException(String.format(SQL_QUERY_ERROR_MESSAGE, "UPDATE"));
                }
                return query.executeUpdate();

            } else if (queryString.toLowerCase().contains("delete")) {
                if (!principalCan(httpServletRequest, "DELETE")) {
                    throw new RuntimeException(String.format(SQL_QUERY_ERROR_MESSAGE, "DELETE"));
                }
                return query.executeUpdate();
            } else if (queryString.toLowerCase().contains("insert")) {
                if (!principalCan(httpServletRequest, "INSERT")) {
                    throw new RuntimeException(String.format(SQL_QUERY_ERROR_MESSAGE, "INSERT"));
                }
                return query.executeUpdate();
            } else if (queryString.toLowerCase().contains("truncate")) {
                if (!principalCan(httpServletRequest, "TRUNCATE")) {
                    throw new RuntimeException(String.format(SQL_QUERY_ERROR_MESSAGE, "TRUNCATE"));
                }
                return query.executeUpdate();
            }
            if (!principalCan(httpServletRequest, "SELECT")) {
                throw new RuntimeException(String.format(SQL_QUERY_ERROR_MESSAGE, "SELECT"));
            }
            if (!includeColumnNames) {
                return query.getResultList();
            }
            String tableName = queryString.substring(queryString.indexOf("FROM")+4).trim();
            tableName = tableName.substring(0, (tableName+" ").indexOf(" "));
            List<String> columnNames = getEntityManager()
                    .createNativeQuery(buildTableColumnQuery(tableName)).getResultList();
            List<Object> result = new ArrayList<>();
            result.add(columnNames);
            result.addAll(query.getResultList());
            return result;
        } catch (Exception ex) {
            StringBuilder stringBuilder = new StringBuilder(ex.getMessage());
            Throwable throwable = ex.getCause();
            while (throwable != null) {
                stringBuilder.append("\n").append(throwable.getMessage());
                throwable = throwable.getCause();
            }
            throw new InvalidBactuatorQueryException(stringBuilder.toString());
        }
    }

    @RequestMapping(value = "/database/query/single", method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> executeSingleQueries(HttpServletRequest httpServletRequest,
                                                  @RequestBody Map<String, String> body) {
        Object result = executeQueryForResult(httpServletRequest,
                body.get("query"),
                body.getOrDefault(resolveCasing("includeColumnNames"), "false")
                        .equalsIgnoreCase("true"));
        return processResponse(HttpStatus.OK, result, SQL_QUERY_SUCCESSFUL);
    }

    @RequestMapping(value = "/database/query/multiple", method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> executeMultipleQueries(HttpServletRequest httpServletRequest,
                                                  @RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        boolean includeColumnNames = (boolean) body
                .getOrDefault(resolveCasing("includeColumnNames"), false);
        List<String> queryStrings = (List<String>) body.getOrDefault("queries", new ArrayList<>());
        for (String queryString : queryStrings) {
            try {
                Object result = executeQueryForResult(httpServletRequest, queryString, includeColumnNames);
                response.put(queryString, result);
            } catch (Exception ex) {
                response.put(queryString, ex.getMessage());
            }
        }
        return processResponse(HttpStatus.OK, response, SQL_QUERY_SUCCESSFUL);
    }

    void resolveIntrospect() {
        introspect = new HashMap<>();
        introspect.put("name", serviceName());
        introspect.put("location", location());
        introspect.put("resources", resourcesMap);
        introspect.put("api_base_url", apiBaseUrl());
        introspect.put("controllers", controllersMap);
        introspect.put("health_endpoint", healthEndpoint());
        introspect.put("description", serviceDescription());
        introspect.put(resolveCasing("logUrls"), logUrls());
        introspect.put(resolveCasing("iconLocation"), iconLocation());
        introspect.put(resolveCasing("serviceApiName"), serviceApiName());
        introspect.put(resolveCasing("databaseQueryRoute"), databaseQueryRoute());
        introspect.put(resolveCasing("databaseMultipleQueryRoute"), databaseMultipleQueryRoute());
    }

    void resolveControllers(String baseUrl) {
        controllersMap = new HashMap<>();
        List<Class<? extends Controller<? extends Model, ? extends Model.Request>>> controllers = controllers();
        if (controllers == null) return;
        for (Class<? extends Controller<? extends Model, ? extends Model.Request>> controller : controllers) {
            Map<String, Object> methodsMap = new HashMap<>();
            Method[] methods = controller.getMethods();
            RequestMapping requestMapping = controller.getAnnotation(RequestMapping.class);
            for (Method method : methods) {
                Map<String, Object> requestMap = methodRequestMap(baseUrl,
                        (requestMapping != null && requestMapping.value().length > 0
                                ? requestMapping.value()[0] : ""),
                        method);
                if (requestMap.isEmpty()) continue;
                List<String> params = new ArrayList<>();
                Parameter[] parameters = method.getParameters();
                for (Parameter parameter : parameters) {
                    params.add(parameter.getType().getSimpleName());
                }
                requestMap.put("parameters", params);
                methodsMap.put(method.getName(), requestMap);
            }
            controllersMap.put(controller.getSimpleName(), methodsMap);
        }
    }

    Map<String, Object> methodRequestMap(String baseUrl, String controllerRoute, Method method) {
        List<String> meths = new ArrayList<>();
        List<String> routes = new ArrayList<>();
        Map<String, Object> requestMap = new HashMap<>();
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        if (requestMapping != null) {
            String[] values = requestMapping.value();
            if (values.length == 0) values = new String[] {""};
            for (String value : values) {
                routes.add(String.format("%s/%s%s%s", contextPath, controllerRoute, value.isEmpty() ? "" : "/", value)
                        .replaceAll("(?<!(http:|https:))/+", "/"));
            }
            for (RequestMethod meth : requestMapping.method()) {
                meths.add(meth.name());
            }
            requestMap.put("routes", routes);
            requestMap.put("methods", meths);
            requestMap.put("consumes", requestMapping.consumes());
            requestMap.put("produces", requestMapping.produces());
            requestMap.put("method", meths.size() == 0 ? "GET" : meths.get(0));
            String firstRoute = routes.get(0).replace(contextPath, "");
            requestMap.put(resolveCasing("absoluteRoute"), String.format("%s%s", baseUrl,
                    firstRoute.length() > 0 ? "/" + firstRoute : ""));
            return requestMap;
        }
        return requestMap;
    }

    void resolveResources() {
        resourcesMap = new HashMap<>();
        List<Class<? extends Model>> models = resources();
        if (models == null) return;
        for (Class<? extends Model> model : models) {
            Map<String, Object> modelMap = new HashMap<>();
            List<Field> fields = FieldUtil.getAllFields(model);
            Map<String, Object> fieldsAttrs = new HashMap<>();
            for (Field field : fields) {
                Map<String, Object> fieldAttrs = new HashMap<>();
                fieldAttrs.put("type", field.getType().getName());

                Map<String, Boolean> statProps = new HashMap<>();
                statProps.put(resolveCasing("medianQuery"),
                        field.getAnnotationsByType(StatQuery.MedianQuery.class).length > 0);
                statProps.put(resolveCasing("columnQuery"),
                        field.getAnnotationsByType(StatQuery.ColumnQuery.class).length > 0);
                statProps.put(resolveCasing("averageQuery"),
                        field.getAnnotationsByType(StatQuery.AverageQuery.class).length > 0);
                statProps.put(resolveCasing("occurrenceQuery"),
                        field.getAnnotationsByType(StatQuery.OccurrenceQuery.class).length > 0);
                statProps.put(resolveCasing("percentageChangeQuery"),
                        field.getAnnotationsByType(StatQuery.PercentageChangeQuery.class).length > 0);
                fieldAttrs.put("stat", statProps);

                // query params
                List<Object> queryProps = new ArrayList<>();
                RequestParamFilter[] requestParamFilters = field.getAnnotationsByType(RequestParamFilter.class);
                for (RequestParamFilter requestParamFilter : requestParamFilters) {
                    if (requestParamFilter.operator() == RequestParamFilter.Operator.RANGE) {
                        queryProps.add(getOperatorQueryObj(requestParamFilters.length, field.getName(),
                                requestParamFilter.operator().name() + "_FROM", requestParamFilter));
                        queryProps.add(getOperatorQueryObj(requestParamFilters.length, field.getName(),
                                requestParamFilter.operator().name() + "_TO", requestParamFilter));
                        continue;
                    }
                    queryProps.add(getOperatorQueryObj(requestParamFilters.length, field.getName(),
                            requestParamFilter.operator().name(), requestParamFilter));
                }
                fieldAttrs.put("query", queryProps);

                fieldsAttrs.put(field.getName(), fieldAttrs);
            }
            StatQuery statQuery = FieldUtil.getAnnotation(model, StatQuery.class);
            if (statQuery != null) {
                Map<String, Object> statAttrs = new HashMap<>();
                statAttrs.put(resolveCasing("fetchHourly"), statQuery.fetchHourly());
                statAttrs.put(resolveCasing("fetchYearly"), statQuery.fetchYearly());
                statAttrs.put(resolveCasing("fetchMonthly"), statQuery.fetchMonthly());
                statAttrs.put(resolveCasing("fetchPrevious"), statQuery.fetchPrevious());
                statAttrs.put(resolveCasing("fetchWeekDays"), statQuery.fetchWeekDays());
                statAttrs.put(resolveCasing("fetchMonthDays"), statQuery.fetchMonthDays());
                statAttrs.put(resolveCasing("intervalColumn"), statQuery.intervalColumn());
                statAttrs.put(resolveCasing("enableClientQuery"), statQuery.enableClientQuery());
                statAttrs.put(resolveCasing("columnsAreSnakeCase"), statQuery.columnsAreSnakeCase());
                modelMap.put("stat", statAttrs);
            }
            modelMap.put(resolveCasing("fields"), fieldsAttrs);
            resourcesMap.put(model.getSimpleName(), modelMap);
        }
    }

    String resolveCasing(String value) {
        return (namingStrategy.equalsIgnoreCase("SNAKE_CASE")
                ? FieldUtil.toSnakeCase(value) : value);
    }

    Map<String, String> getOperatorQueryObj(int length, String name, String operator,
                                            RequestParamFilter requestParamFilter) {
        String fieldName = length > 1
                ? String.format("%s%s%c%s", name,
                requestParamFilter.multiFilterSeparator().equals("__") &&
                        namingStrategy.equalsIgnoreCase("SNAKE_CASE")
                        ? "_" : requestParamFilter.multiFilterSeparator(),
                operator.charAt(0),
                operator.substring(1).toLowerCase())
                : name;
        Map<String, String> requestParamFilterProps = new HashMap<>();
        requestParamFilterProps.put(operator, resolveCasing(fieldName));
        return requestParamFilterProps;
    }

}
