package io.github.barmoury.api.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.barmoury.api.model.BarmouryModel;
import io.github.barmoury.util.FieldUtil;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class EloquentQuery {

    static EntityManager superEntityManager;
    static ObjectMapper mapper = new ObjectMapper();

    EloquentQuery() {}

    public static void setSuperEntityManager(EntityManager entityManager) {
        superEntityManager = entityManager;
    }

    public static <T> Page<T> buildQueryForPage(
            EntityManager entityManager,
            String tableName,
            Class<T> clazz,
            HttpServletRequest request,
            Pageable pageable) {

        if (superEntityManager == null) superEntityManager = entityManager;

        MultiValuedMap<String, Object> requestFields = resolveQueryFields(clazz, request);
        String queryString = String.format("FROM %s %s", tableName, buildWhereFilter(requestFields));
        Query countQuery = buildQueryObject(entityManager, String.format("SELECT count(*) %s", queryString),
                clazz, request, requestFields, false);
        int totalElements = ((Number) countQuery.getSingleResult()).intValue();
        Query query = buildQueryObject(entityManager, String.format("SELECT * %s %s", queryString,
                                            buildPageFilter(pageable)), clazz, request, requestFields, true);
        List<T> content = query.getResultList();

        return new PageImpl<>(content, pageable, totalElements);
    }

    static <T> MultiValuedMap<String, Object> resolveQueryFields(Class<T> clazz, HttpServletRequest request) {
        MultiValuedMap<String, Object> requestFields = new ArrayListValuedHashMap<>();
        List<Field> fields = FieldUtil.getAllFields(clazz);
        for (Field field : fields) {
            String fieldName = field.getName();
            String matchingFieldName = fieldName;
            RequestParamFilter requestParamFilter = field.getAnnotation(RequestParamFilter.class);
            if (requestParamFilter == null) continue;
            boolean isPresent = request.getParameterMap().containsKey(fieldName) &&
                    !request.getParameter(fieldName).trim().isEmpty();
            if (!isPresent && requestParamFilter.acceptSnakeCase()) {
                matchingFieldName = toSnakeCase(fieldName);
                isPresent = request.getParameterMap().containsKey(matchingFieldName)
                        && !request.getParameter(matchingFieldName).trim().isEmpty();
            }
            if (!isPresent && requestParamFilter.aliases() != null && requestParamFilter.aliases().length > 0) {
                for (String alias : requestParamFilter.aliases()) {
                    isPresent = request.getParameterMap().containsKey(alias) &&
                            !request.getParameter(alias).trim().isEmpty();
                    if (isPresent) {
                        matchingFieldName = alias;
                        break;
                    }
                }
            }
            requestFields.put(matchingFieldName, fieldName);
            requestFields.put(matchingFieldName, isPresent);
            requestFields.put(matchingFieldName, requestParamFilter);
        }
        return requestFields;
    }

    public static String buildWhereFilter(MultiValuedMap<String, Object> requestFields) {
        boolean virginQuery = true;
        StringBuilder whereQuery = new StringBuilder();
        for (String matchingFieldName : requestFields.keySet()) {
            Object[] values = requestFields.get(matchingFieldName).toArray();
            String fieldName = (String) values[0];
            boolean isPresent = (boolean) values[1];
            RequestParamFilter requestParamFilter = (RequestParamFilter) values[2];

            if (!isPresent && !requestParamFilter.alwaysQuery()) continue;
            if (virginQuery) whereQuery.append(" WHERE ");
            else whereQuery.append("AND");
            String columnName = requestParamFilter.column().isEmpty() ? fieldName : requestParamFilter.column();
            if (requestParamFilter.columnIsSnakeCase()) columnName = toSnakeCase(columnName);
            whereQuery.append(getRelationQueryPart(columnName, matchingFieldName, requestParamFilter.operator()));
            virginQuery = false;
        }
        return whereQuery.toString();
    }

    static String getRelationQueryPart(String column, String matchingFieldName, RequestParamFilter.Operator operator) {
        StringBuilder relationPart = new StringBuilder();
        relationPart.append(" (").append(column).append(" ");
        if (operator == RequestParamFilter.Operator.EQ) {
            relationPart.append(String.format(" = :%s ", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.GT) {
            relationPart.append(String.format(" > :%s ", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.LT) {
            relationPart.append(String.format(" < :%s ", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.NE) {
            relationPart.append(String.format(" != :%s ", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.GT_EQ) {
            relationPart.append(String.format(" >= :%s ", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.LT_EQ) {
            relationPart.append(String.format(" <= :%s ", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.LIKE || operator == RequestParamFilter.Operator.CONTAINS) {
            relationPart.append(String.format(" LIKE CONCAT('%%', :%s, '%%')", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.BETWEEN) { // TODO
            // get from to
            relationPart.append(String.format(" >= :%s ", matchingFieldName));
            relationPart.append(String.format(" <= :%s ", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.STARTS_WITH) {
            relationPart.append(String.format(" LIKE CONCAT(:%s, '%%')", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.ENDS_WITH) {
            relationPart.append(String.format(" LIKE CONCAT('%%', :%s)", matchingFieldName));
        }
        relationPart.append(") ");
        return relationPart.toString();
    }

    public static String buildPageFilter(Pageable pageable) {
        StringBuilder pagination = new StringBuilder();
        Sort sort = pageable.getSort();
        List<Sort.Order> orders = sort.toList();
        int sortSize = orders.size();
        if (sortSize > 0) { pagination.append("ORDER BY"); }
        for (int index = 0; index < sortSize; index++) {
            Sort.Order order = orders.get(index);
            pagination.append(String.format(" %s %s", order.getProperty(), order.getDirection()));
            if (index < sortSize-1) {
                pagination.append(",");
            }
        }
        pagination.append(String.format(" LIMIT %d,%d ", (pageable.getPageNumber() * pageable.getPageSize()),
                pageable.getPageSize()));
        return pagination.toString();
    }

    public static <T> Query buildQueryObject(EntityManager entityManager,
                                             String queryString,
                                             Class<T> clazz,
                                             HttpServletRequest request,
                                             MultiValuedMap<String, Object> requestFields,
                                             boolean typed) {
        Query query = (typed
                ? entityManager.createNativeQuery(queryString, clazz)
                : entityManager.createNativeQuery(queryString));

        for (String matchingFieldName : requestFields.keySet()) {
            Object[] values = requestFields.get(matchingFieldName).toArray();
            RequestParamFilter requestParamFilter = (RequestParamFilter) values[2];
            boolean isPresent = (boolean) values[1];
            if (isPresent) {
                query = query.setParameter(matchingFieldName, request.getParameter(matchingFieldName));
            } else if (requestParamFilter.alwaysQuery()) {
                query = query.setParameter(matchingFieldName, null);
            }
        }
        return query;
    }

    @SuppressWarnings("unchecked")
    public static <T extends BarmouryModel> T getEntityForUpdateById(Long entityId, T field, String tableName, Long id, Class<T> clazz)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (id != null && id > 0 && field != null && id != ((BarmouryModel)field).getId()) {
            if (superEntityManager == null) return null;
            Query query = superEntityManager.createNativeQuery(String.format("SELECT * FROM %s WHERE id = %d LIMIT 1",
                    tableName, id), clazz);
            return ((T) query.getSingleResult());
        }
        if (field != null && entityId != 0L) return field;
        T entity = clazz.getDeclaredConstructor().newInstance();
        entity.setId(id != null ? id : 0L);
        return entity;
    }

    // should accept object of object for json object of jsoon object
    // can use reflection, but seem overkill
    public static /*<T>*/ JsonNode getResourceStat(EntityManager entityManager,
                                                   String tableName,
                                                   Map<String, String> statMap,
                                                   HttpServletRequest request) {
        JsonNode stat = mapper.createObjectNode();

        /*StringBuilder sqlQueryString = new StringBuilder("SELECT ");
        String filterString = String.format("FROM %s %s", tableName, buildWhereFilter(columnMap,
                request, true, true));
        int statMapIndex = 0;
        int statMapSize = statMap.size();
        for (Map.Entry<String, String> statEntry : statMap.entrySet()) {
            statMapIndex++;
            String andOrWhere = (filterString.contains("WHERE") ? " AND " : " WHERE ");
            sqlQueryString.append(String.format("(SELECT COUNT(*) %s %s) as %s%s%n", filterString,
                    !statEntry.getValue().isEmpty() ?
                            (andOrWhere + statEntry.getValue())
                            : "",
                    toCamelCase(statEntry.getKey()), (statMapIndex < statMapSize ? "," : "")));
        }
        Query query = buildQueryObject(entityManager, sqlQueryString.toString(), null, columnMap, request,
                true, true);
        if (request == null) return stat;
        NativeQueryImpl<Map<String, BigInteger>> nativeQuery = (NativeQueryImpl<Map<String, BigInteger>>) query;
        nativeQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        Map<String, BigInteger> obj = nativeQuery.getSingleResult();

        // This code block can be better, and automated
        if (statMap.containsKey("total_count")) { stat.setTotalCount(obj.get("totalCount").longValue()); }
        if (statMap.containsKey("active_count")) { stat.setActiveCount(obj.get("activeCount").longValue()); }
        if (statMap.containsKey("inactive_count")) { stat.setInactiveCount(obj.get("inactiveCount").longValue()); }*/
        return stat;
    }

    public static <T> T getResourceById(JpaRepository<T, Long> repository, long id, String message) {
        Optional<T> resource = repository.findById(id);
        if (resource.isPresent()) return resource.get();
        throw new EntityNotFoundException(String.format("%s, %d", message, id));
    }

    public static String toCamelCase(String phrase) {
        while(phrase.contains("_")) {
            phrase = phrase.replaceFirst("_[a-zA-Z\\d]", String.valueOf(Character.toUpperCase(phrase.charAt(phrase.indexOf("_") + 1))));
        }
        return phrase;
    }

    public static String toSnakeCase(String str) {
        StringBuilder result = new StringBuilder();
        char c = str.charAt(0);
        result.append(Character.toLowerCase(c));
        for (int i = 1; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (Character.isUpperCase(ch)) {
                result.append('_');
                result.append(Character.toLowerCase(ch));
            }
            else {
                result.append(ch);
            }
        }
        return result.toString();
    }

}
