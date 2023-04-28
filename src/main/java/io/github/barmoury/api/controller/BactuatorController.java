package io.github.barmoury.api.controller;

import io.github.barmoury.api.exception.InvalidBactuatorQueryException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO add field to fetch class with their query and stat props and fields, at startup once
@Transactional
public abstract class BactuatorController {
    
    static String SQL_QUERY_SUCCESSFUL = "query successfully";
    static String SQL_QUERY_ERROR_MESSAGE = "You do not have the '%s' permission to perform this operation";

    // list of query possiblt for staticstics
    public abstract String serviceName();

    public abstract long downloadsCount();

    public abstract String iconLocation();

    public abstract String serviceApiName();

    public abstract String serviceDescription();

    public abstract String databaseQueryRoute();

    public abstract String databaseMultipleQueryRoute();

    public abstract EntityManager getEntityManager();

    public abstract List<Map<String, String>> logUrls();

    public abstract Map<String, Object> userStatistics();

    public abstract Map<String, Object> earningStatistics();

    public abstract Logger getLogger();

    public abstract String buildTableColumnQuery(String tableName);

    public abstract boolean isServiceOk();

    public abstract <T> ResponseEntity<?> processResponse(HttpStatus httpStatus, T data, String message);

    public abstract boolean principalCan(HttpServletRequest httpServletRequest, String dbMethod);

    @RequestMapping(value = "/health", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        boolean serviceIsOk = isServiceOk();
        response.put("status", serviceIsOk ? "ok" : "not ok");
        return processResponse(serviceIsOk ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR,
                response, "health check successfully");
    }

    @RequestMapping(value = "/introspect", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> introspect() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", serviceName());
        response.put("description", serviceDescription());
        response.put("icon_location", iconLocation());
        response.put("service_api_name", serviceApiName());
        response.put("users", userStatistics());
        response.put("earnings", earningStatistics());
        response.put("download_counts", downloadsCount());
        response.put("log_urls", logUrls());
        response.put("database_query_route", databaseQueryRoute());
        response.put("database_multiple_query_route", databaseMultipleQueryRoute());
        return processResponse(HttpStatus.OK, response, "introspect data fetched successfully");
    }

    Object executeQueryForResult(HttpServletRequest httpServletRequest, String queryString, boolean includeColumnNames) {
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
            List<String> columnNames = getEntityManager().createNativeQuery(buildTableColumnQuery(tableName)).getResultList();
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
                body.getOrDefault("include_column_names", "false").equalsIgnoreCase("true"));
        return processResponse(HttpStatus.OK, result, SQL_QUERY_SUCCESSFUL);
    }

    @RequestMapping(value = "/database/query/multiple", method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> executeMultipleQueries(HttpServletRequest httpServletRequest,
                                                  @RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        boolean includeColumnNames = (boolean) body.getOrDefault("include_column_names", false);
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

}
