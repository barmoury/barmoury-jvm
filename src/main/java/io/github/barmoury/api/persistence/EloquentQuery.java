package io.github.barmoury.api.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.barmoury.api.model.BarmouryModel;
import io.github.barmoury.util.FieldUtil;
import jakarta.persistence.*;
import lombok.Setter;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.hibernate.query.sql.internal.NativeQueryImpl;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class EloquentQuery {

    @Setter static boolean isSnakeCase = false;
    @Setter static EntityManager superEntityManager;
    static ObjectMapper mapper = new ObjectMapper();
    @Setter static AutowireCapableBeanFactory autowireCapableBeanFactory;

    EloquentQuery() {}

    @SuppressWarnings("unchecked")
    public static <T> Page<T> buildQueryForPage(
            EntityManager entityManager,
            String tableName,
            Class<T> clazz,
            HttpServletRequest request,
            Pageable pageable) {

        if (superEntityManager == null) setSuperEntityManager(entityManager);

        MultiValuedMap<String, Object> requestFields = resolveQueryFields(clazz, request, false);
        String queryString = String.format(" FROM %s entity %s", tableName, buildWhereFilter(requestFields));
        Query countQuery = buildQueryObject(entityManager, String.format("SELECT COUNT(*) %s", queryString),
                clazz, requestFields, false);
        int totalElements = ((Number) countQuery.getSingleResult()).intValue();
        Query query = buildQueryObject(entityManager, String.format("SELECT * %s %s", queryString,
                                            buildPageFilter(pageable)), clazz, requestFields, false);

        NativeQueryImpl<Map<String, Object>> nativeQuery = (NativeQueryImpl<Map<String, Object>>) query;
        nativeQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        List<T> content = resolveEntityList(query.getResultList(), clazz, true);

        return new PageImpl<>(content, pageable, totalElements);
    }

    // if there is anyway to make it typed query rather than manual processing
    public static <T> List<Map<String, Object>> resolveEntityList(List<Map<String, Object>> rows,
                                                                  Class<T> tClass,
                                                                  boolean resolveSubEntities) {

        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object[]> joinColumnFields = resolveSubEntities ? FieldUtil.findJoinColumnFields(tClass) : null;
        for (Map<String, Object> row : rows) {
            result.add(processSingleRow(row, joinColumnFields, tClass, resolveSubEntities));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    static <T> Map<String, Object> processSingleRow(Map<String, Object> row,
                                                Map<String, Object[]> joinColumnFields,
                                                Class<T> tClass,
                                                boolean resolveSubEntities) {
        Map<String, Object> nrow = new HashMap<>();
        for (String key : row.keySet()) {
            if (resolveSubEntities && joinColumnFields.containsKey(key)) {
                Object[] values = joinColumnFields.get(key);
                Field field = (Field) values[0];
                Class<?> tableClazz = (Class<?>) values[1];
                JoinColumn joinColumn = (JoinColumn) values[2];
                nrow.put(isSnakeCase ? toSnakeCase(field.getName()) : toCamelCase(field.getName()),
                        resolveSubEntity(field, joinColumn, tableClazz, row.get(key)));
            } else {
                Object value = row.get(key);
                Field field = FieldUtil.getDeclaredField(tClass, isSnakeCase ? toCamelCase(key) : key);
                if (field != null) {
                    JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
                    if (jsonProperty != null && jsonProperty.access() == JsonProperty.Access.WRITE_ONLY) continue;
                    Convert convert = field.getAnnotation(Convert.class);
                    if (convert != null) {
                        try {
                            Constructor<?> converterConstructor = convert.converter().getDeclaredConstructor();
                            boolean accessibility = converterConstructor.canAccess(null);
                            if (!accessibility) converterConstructor.setAccessible(true);
                            AttributeConverter<?, String> attributeConverter =
                                    (AttributeConverter<?, String>) converterConstructor.newInstance();
                            if (autowireCapableBeanFactory != null) {
                                autowireCapableBeanFactory.autowireBean(attributeConverter);
                            }
                            if (!accessibility) converterConstructor.setAccessible(false);
                            value = attributeConverter.convertToEntityAttribute(String.valueOf(value));
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                                 NoSuchMethodException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                nrow.put(isSnakeCase ? toSnakeCase(key) : toCamelCase(key), value);
            }
        }
        return nrow;
    }

    public static Object resolveSubEntity(Field field,
                                          JoinColumn joinColumn,
                                          Class<?> tClass,
                                          Object value) {
        if (value == null) return null;
        if (field.getAnnotation(OneToMany.class) != null) {
            return new ArrayList<>();
        }
        String tableName = joinColumn.table();
        if (tableName.isEmpty()) {
            Entity entity = tClass.getAnnotation(Entity.class);
            if (entity != null) tableName = entity.name();
        }
        String queryString = String.format("SELECT sub_entity.* FROM %s sub_entity WHERE %s = :value ",
                tableName, joinColumn.referencedColumnName());
        MultiValuedMap<String, Object> requestFields = new ArrayListValuedHashMap<>();
        requestFields.put("value", null); requestFields.put("value", null);
        requestFields.put("value", null); requestFields.put("value", value);
        Map<String, Object> entry = singleQueryResultAsMap(null, queryString, superEntityManager, requestFields);
        Map<String, Object[]> joinColumnFields = FieldUtil.findJoinColumnFields(tClass);
        return processSingleRow(entry, joinColumnFields, tClass, true);
    }

    static void resolveQueryForSingleField(MultiValuedMap<String, Object> requestFields,
                                           RequestParamFilter requestParamFilter,
                                           boolean resolveStatQueryAnnotations,
                                           HttpServletRequest request,
                                           Set<String> queryParams,
                                           String columnName,
                                           Field field) {
        for (String queryParam : queryParams) {
            String[] values = new String[]{null};
            boolean isPresent = false;
            boolean objectFilter = (!resolveStatQueryAnnotations && (requestParamFilter.operator() == RequestParamFilter.Operator.OBJECT_EQ
                    || requestParamFilter.operator() == RequestParamFilter.Operator.OBJECT_NE
                    || requestParamFilter.operator() == RequestParamFilter.Operator.OBJECT_LIKE
                    || requestParamFilter.operator() == RequestParamFilter.Operator.OBJECT_STR_EQ
                    || requestParamFilter.operator() == RequestParamFilter.Operator.OBJECT_STR_NE
                    || requestParamFilter.operator() == RequestParamFilter.Operator.OBJECT_NOT_LIKE
                    || requestParamFilter.operator() == RequestParamFilter.Operator.OBJECT_CONTAINS
                    || requestParamFilter.operator() == RequestParamFilter.Operator.OBJECT_ENDS_WITH
                    || requestParamFilter.operator() == RequestParamFilter.Operator.OBJECT_STARTS_WITH
                    || requestParamFilter.operator() == RequestParamFilter.Operator.OBJECT_NOT_CONTAINS
                    || requestParamFilter.operator() == RequestParamFilter.Operator.OBJECT_STR_ENDS_WITH
                    || requestParamFilter.operator() == RequestParamFilter.Operator.OBJECT_STR_STARTS_WITH));

            if (!resolveStatQueryAnnotations) {
                for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                    if ((entry.getKey().equals(queryParam) || (objectFilter && entry.getKey().startsWith(queryParam)))
                            && !entry.getValue()[0].isEmpty()) {
                        queryParam = (objectFilter && requestParamFilter.columnObjectFieldsIsSnakeCase()
                                ? toSnakeCase(entry.getKey())
                                : entry.getKey());
                        values = entry.getValue();
                        isPresent = true;
                        break;
                    }
                }
            }
            if (!resolveStatQueryAnnotations && !isPresent && !requestParamFilter.alwaysQuery()) {
                continue;
            }
            if (resolveStatQueryAnnotations) queryParam = field.getName();
            if (requestFields.containsKey(queryParam)) {
                continue;
            }
            requestFields.put(queryParam, columnName);
            requestFields.put(queryParam, isPresent);
            requestFields.put(queryParam, requestParamFilter);
            requestFields.put(queryParam, values[0]);
            if (resolveStatQueryAnnotations) {
                StatQuery.MedianQuery[] medianQueries = field.getAnnotationsByType(StatQuery.MedianQuery.class);
                StatQuery.ColumnQuery[] columnQueries = field.getAnnotationsByType(StatQuery.ColumnQuery.class);
                StatQuery.AverageQuery[] averageQueries = field.getAnnotationsByType(StatQuery.AverageQuery.class);
                StatQuery.OccurrenceQuery[] occurrenceQueries = field.getAnnotationsByType(StatQuery.OccurrenceQuery.class);
                StatQuery.PercentageChangeQuery[] percentageChangeQueries = field.getAnnotationsByType(StatQuery.PercentageChangeQuery.class);
                requestFields.put(queryParam, medianQueries);
                requestFields.put(queryParam, columnQueries);
                requestFields.put(queryParam, averageQueries);
                requestFields.put(queryParam, occurrenceQueries);
                requestFields.put(queryParam, percentageChangeQueries);
            }
        }
    }

    // TODO seperate for stat and request query
    static <T> MultiValuedMap<String, Object> resolveQueryFields(Class<T> clazz, HttpServletRequest request,
                                                                 boolean resolveStatQueryAnnotations) {
        MultiValuedMap<String, Object> requestFields = new ArrayListValuedHashMap<>();
        List<Field> fields = FieldUtil.getAllFields(clazz);
        for (Field field : fields) {
            String fieldName = field.getName();

            RequestParamFilter requestParamFilter = field.getAnnotation(RequestParamFilter.class);

            if (!resolveStatQueryAnnotations && requestParamFilter == null) continue;
            String columnName = FieldUtil.getFieldColumnName(field);
            if (requestParamFilter != null) {
                if (!requestParamFilter.column().isEmpty()) columnName = requestParamFilter.column();
                if (requestParamFilter.columnIsSnakeCase()) columnName = toSnakeCase(columnName);
            }

            Set<String> extraFieldNames = new HashSet<>();
            extraFieldNames.add(fieldName);
            if (!resolveStatQueryAnnotations) Collections.addAll(extraFieldNames, requestParamFilter.aliases());
            if (!resolveStatQueryAnnotations && requestParamFilter.acceptSnakeCase()) {
                for (String extraFieldName : new ArrayList<>(extraFieldNames))
                    extraFieldNames.add(toSnakeCase(extraFieldName));
            }
            if (!resolveStatQueryAnnotations && requestParamFilter.operator() == RequestParamFilter.Operator.BETWEEN) {
                Set<String> fromExtraFieldNames = new HashSet<>();
                Set<String> toExtraFieldNames = new HashSet<>();
                for (String extraFieldName : extraFieldNames) {
                    fromExtraFieldNames.add(extraFieldName + (extraFieldName.contains("_") ? "_from" : "From"));
                    toExtraFieldNames.add(extraFieldName + (extraFieldName.contains("_") ? "_to" : "To"));
                }
                RequestParamFilter fromRequestParamFilter2 =
                        new RequestParamFilterOperatorImpl(requestParamFilter, RequestParamFilter.Operator.GT_EQ);
                RequestParamFilter toRequestParamFilter2 =
                        new RequestParamFilterOperatorImpl(requestParamFilter, RequestParamFilter.Operator.LT_EQ);
                resolveQueryForSingleField(requestFields, fromRequestParamFilter2, resolveStatQueryAnnotations,
                        request, fromExtraFieldNames, columnName, field);
                resolveQueryForSingleField(requestFields, toRequestParamFilter2, resolveStatQueryAnnotations,
                        request, toExtraFieldNames, columnName, field);
                continue;
            }
            resolveQueryForSingleField(requestFields, requestParamFilter, resolveStatQueryAnnotations,
                    request, extraFieldNames, columnName, field);

        }
        return requestFields;
    }

    public static String buildWhereFilter(MultiValuedMap<String, Object> requestFields) {
        boolean virginQuery = true;
        StringBuilder whereQuery = new StringBuilder();
        for (String matchingFieldName : requestFields.keySet()) {
            Object[] values = requestFields.get(matchingFieldName).toArray();
            RequestParamFilter requestParamFilter = (RequestParamFilter) values[2];
            String columnName = (String) values[0];

            if (virginQuery) whereQuery.append(" WHERE ");
            else whereQuery.append("AND");
            whereQuery.append(getRelationQueryPart(columnName, matchingFieldName, requestParamFilter.operator()));
            virginQuery = false;
        }
        return whereQuery.toString();
    }

    static String getRelationQueryPart(String column, String matchingFieldName, RequestParamFilter.Operator operator) {
        StringBuilder relationPart = new StringBuilder();
        relationPart.append(" (entity.").append(column).append(" ");
        String[] matchingFieldNameParts = matchingFieldName.split("\\.");
        String objectField =
                (matchingFieldNameParts.length > 1 ? matchingFieldNameParts[1] : matchingFieldNameParts[0]);
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
        } else if (operator == RequestParamFilter.Operator.LIKE
                || operator == RequestParamFilter.Operator.CONTAINS) {
            relationPart.append(String.format(" LIKE CONCAT('%%', :%s, '%%')", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.NOT_LIKE
                || operator == RequestParamFilter.Operator.NOT_CONTAINS) {
            relationPart.append(String.format(" NOT LIKE CONCAT('%%', :%s, '%%')", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.ENDS_WITH) {
            relationPart.append(String.format(" LIKE CONCAT('%%', :%s)", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.STARTS_WITH) {
            relationPart.append(String.format(" LIKE CONCAT(:%s, '%%')", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.OBJECT_EQ) {
            relationPart.append(String.format(" LIKE CONCAT('%%\"%s\":', :%s, ',%%') OR entity.%s LIKE CONCAT('%%\"%s\":', :%s, '}')",
                    objectField, matchingFieldName, column, objectField, matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.OBJECT_NE) {
            relationPart.append(String.format(" NOT LIKE CONCAT('%%\"%s\":', :%s, ',%%') AND entity.%s NOT LIKE CONCAT('%%\"%s\":', :%s, '}')",
                    objectField, matchingFieldName, column, objectField, matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.OBJECT_STR_EQ) {
            relationPart.append(String.format(" LIKE CONCAT('%%\"%s\":\"', :%s, '\",%%') OR entity.%s LIKE CONCAT('%%\"%s\":\"', :%s, '\"}')",
                    objectField, matchingFieldName, column, objectField, matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.OBJECT_STR_NE) {
            relationPart.append(String.format(" NOT LIKE CONCAT('%%\"%s\":\"', :%s, '\",%%') AND entity.%s NOT LIKE CONCAT('%%\"%s\":\"', :%s, '\"}')",
                    objectField, matchingFieldName, column, objectField, matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.OBJECT_LIKE
                || operator == RequestParamFilter.Operator.OBJECT_CONTAINS) {
            relationPart.append(String.format(" LIKE CONCAT('%%\"%s\":%%', :%s, '%%,%%') OR entity.%s LIKE CONCAT('%%\"%s\":%%', :%s, '%%}')",
                    objectField, matchingFieldName, column, objectField, matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.OBJECT_NOT_LIKE
                || operator == RequestParamFilter.Operator.OBJECT_NOT_CONTAINS) {
            relationPart.append(String.format(" NOT LIKE CONCAT('%%\"%s\":%%', :%s, '%%,%%') AND entity.%s NOT LIKE CONCAT('%%\"%s\":%%', :%s, '%%}')",
                    objectField, matchingFieldName, column, objectField, matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.OBJECT_ENDS_WITH) {
            relationPart.append(String.format(" LIKE CONCAT('%%\"%s\":%%', :%s, ',%%') OR entity.%s LIKE CONCAT('%%\"%s\":%%', :%s, '}')",
                    objectField, matchingFieldName, column, objectField, matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.OBJECT_STARTS_WITH) {
            relationPart.append(String.format(" LIKE CONCAT('%%\"%s\":', :%s, '%%,%%') OR entity.%s LIKE CONCAT('%%\"%s\":', :%s, '%%}')",
                    objectField, matchingFieldName, column, objectField, matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.OBJECT_STR_ENDS_WITH) {
            relationPart.append(String.format(" LIKE CONCAT('%%\"%s\":\"%%', :%s, '\",%%') OR entity.%s LIKE CONCAT('%%\"%s\":\"%%', :%s, '\"}')",
                    objectField, matchingFieldName, column, objectField, matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.OBJECT_STR_STARTS_WITH) {
            relationPart.append(String.format(" LIKE CONCAT('%%\"%s\":\"', :%s, '%%\",%%') OR entity.%s LIKE CONCAT('%%\"%s\":\"', :%s, '%%\"}')",
                    objectField, matchingFieldName, column, objectField, matchingFieldName));
        }
        relationPart.append(") ");
        return relationPart.toString();
    }

    public static <T> Query buildQueryObject(EntityManager entityManager,
                                             String queryString,
                                             Class<T> clazz,
                                             MultiValuedMap<String, Object> requestFields,
                                             boolean typed) {
        Query query = (typed
                ? entityManager.createNativeQuery(queryString, clazz)
                : entityManager.createNativeQuery(queryString));

        for (String matchingFieldName : requestFields.keySet()) {
            Object[] values = requestFields.get(matchingFieldName).toArray();
            Object value = values[3];
            query = query.setParameter(matchingFieldName, value);
        }
        return query;
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

    @SuppressWarnings("unchecked")
    public static <T> JsonNode getResourceStat(EntityManager entityManager,
                                               HttpServletRequest request,
                                               String tableName,
                                               Class<T> clazz) {
        return getResourceStat(entityManager, request, tableName, clazz, false);
    }

    public static <T> ObjectNode getResourceStat(EntityManager entityManager,
                                               HttpServletRequest request,
                                               String tableName,
                                               Class<T> clazz,
                                               boolean isPrevious) {

        ObjectNode stat = mapper.createObjectNode();
        StatQuery statQuery = FieldUtil.getAnnotation(clazz, StatQuery.class);
        MultiValuedMap<String, Object> requestFields = resolveQueryFields(clazz, request, false);
        MultiValuedMap<String, Object> statRequestFields = resolveQueryFields(clazz, request, true);
        String whereFilterString = buildWhereFilter(requestFields);

        Map<String, StatQuery.MedianQuery[]> medianQueries = new HashMap<>();
        Map<String, StatQuery.ColumnQuery[]> columnQueries = new HashMap<>();
        Map<String, StatQuery.AverageQuery[]> averageQueries = new HashMap<>();
        Map<String, StatQuery.OccurrenceQuery[]> occurrenceQueries = new HashMap<>();
        Map<String, StatQuery.PercentageChangeQuery[]> percentageChangeQueries = new HashMap<>();

        for (String fieldName : statRequestFields.keySet()) {
            Object[] values = statRequestFields.get(fieldName).toArray();
            String columnName = (String) values[0];
            if (statQuery != null && statQuery.columnsAreSnakeCase()) columnName = toSnakeCase(columnName);

            if (((StatQuery.MedianQuery[]) values[4]).length > 0)
                medianQueries.put(columnName, (StatQuery.MedianQuery[]) values[4]);
            if (((StatQuery.ColumnQuery[]) values[5]).length > 0)
                columnQueries.put(columnName, (StatQuery.ColumnQuery[]) values[5]);
            if (((StatQuery.AverageQuery[]) values[6]).length > 0)
                averageQueries.put(columnName, (StatQuery.AverageQuery[]) values[6]);
            if (((StatQuery.OccurrenceQuery[]) values[7]).length > 0)
                occurrenceQueries.put(columnName, (StatQuery.OccurrenceQuery[]) values[7]);
            if (((StatQuery.PercentageChangeQuery[]) values[8]).length > 0)
                percentageChangeQueries.put(columnName, (StatQuery.PercentageChangeQuery[]) values[8]);
        }

        boolean processPrevious = !isPrevious && statQuery != null
                && statQuery.fetchPrevious() && !statQuery.intervalColumn().isEmpty()
                && requestFields.containsKey(statQuery.intervalColumn());
        if (processPrevious) {
            String from = (String) ((Object[])requestFields.get(statQuery.intervalColumn()+"_from").toArray())[3];
            String to = "";
            //long daysDifferent = ChronoUnit.DAYS.between(date1, date2);
        }
        long totalCount =
                resolveColumnQueries(clazz, stat, tableName, statQuery, whereFilterString, entityManager, requestFields,
                        columnQueries);;

        if (!medianQueries.isEmpty()) {
            resolveMedianQueries(clazz, stat, tableName, whereFilterString, entityManager, requestFields,
                    medianQueries);
        }
        if (!averageQueries.isEmpty()) {
            resolveAverageQueries(clazz, stat, tableName, whereFilterString, entityManager, requestFields,
                    averageQueries);
        }
        if (!occurrenceQueries.isEmpty()) {
            resolveOccurrenceQueries(clazz, stat, totalCount, tableName, whereFilterString, entityManager, requestFields,
                    occurrenceQueries);
        }

        if (processPrevious) {
            stat.set("previous", getResourceStat(entityManager, request, tableName, clazz, true));
        }

        return stat;
    }

    @SuppressWarnings("unchecked")
    public static <T>  Map<String, Object> singleQueryResultAsMap(Class<T> clazz,
                                                                  String queryString,
                                                                  EntityManager entityManager,
                                                                  MultiValuedMap<String, Object> requestFields) {
        Query query = buildQueryObject(entityManager, queryString, clazz, requestFields, false);
        NativeQueryImpl<Map<String, Object>> nativeQuery = (NativeQueryImpl<Map<String, Object>>) query;
        nativeQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        return nativeQuery.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public static <T>  List<Map<String, Object>> queryListResultAsMap(Class<T> clazz,
                                                           String queryString,
                                                           EntityManager entityManager,
                                                           MultiValuedMap<String, Object> requestFields) {
        Query query = buildQueryObject(entityManager, queryString, clazz, requestFields, false);
        NativeQueryImpl<Map<String, Object>> nativeQuery = (NativeQueryImpl<Map<String, Object>>) query;
        nativeQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        return nativeQuery.getResultList();
    }

    @SuppressWarnings("unchecked")
    static <T> long resolveColumnQueries(Class<T> clazz,
                                         ObjectNode stat,
                                         String tableName,
                                         StatQuery statQuery,
                                         String whereFilterString,
                                         EntityManager entityManager,
                                         MultiValuedMap<String, Object> requestFields,
                                         Map<String, StatQuery.ColumnQuery[]> columnQueries) {
        List<String> columnQueryList = new ArrayList<>();
        String countName = statQuery != null && statQuery.columnsAreSnakeCase()
                ? tableName + "_count"
                : tableName + "Count";
        columnQueryList.add(String.format("(SELECT COUNT(*) FROM %s entity %s) AS %s",
                tableName, whereFilterString, countName));

        for (Map.Entry<String, StatQuery.ColumnQuery[]> entry : columnQueries.entrySet()) {
            String columnName = entry.getKey();

            for (StatQuery.ColumnQuery columnQuery : entry.getValue()) {
                StringBuilder queryBuilder = new StringBuilder("(SELECT ");
                boolean hasFunction = !columnQuery.sqlFunction().isEmpty();
                String name = !columnQuery.name().isEmpty()
                        ? String.format(columnQuery.name(), columnName)
                        : columnName;
                if (hasFunction) queryBuilder.append(columnQuery.sqlFunction()).append("(entity.");
                queryBuilder.append(columnName);
                if (hasFunction) queryBuilder.append(')');
                queryBuilder.append(" FROM ").append(tableName).append(" entity ");
                if (!columnQuery.whereClause().isEmpty()) {
                    queryBuilder.append((whereFilterString.trim().isEmpty() ? " WHERE " : whereFilterString + " AND "))
                            .append(String.format(columnQuery.whereClause(), columnName));
                } else if (!whereFilterString.trim().isEmpty()) {
                    queryBuilder.append(whereFilterString);
                }
                queryBuilder.append(")").append(" AS ").append(name);
                columnQueryList.add(queryBuilder.toString());
            }
        }
        StringBuilder columnQueryBuilder = new StringBuilder("SELECT ");
        for (int index = 0; index < columnQueryList.size(); index++) {
            String columnQuery = columnQueryList.get(index);
            columnQueryBuilder.append(columnQuery);
            if (index != columnQueryList.size()-1) {
                columnQueryBuilder.append(", ");
            }
        }
        long totalCount = 0;
        Map<String, Object> columns = singleQueryResultAsMap(clazz, columnQueryBuilder.toString(), entityManager,
                requestFields);
        for (Map.Entry<String, Object> column : columns.entrySet()) {
            if (column.getKey().equals(countName)) totalCount = Long.parseLong(column.getValue().toString());
            putStatField(stat, column.getKey(), column.getValue());
        }
        return totalCount;
    }

    @SuppressWarnings("unchecked")
    static <T> void resolveAverageQueries(Class<T> clazz,
                                          ObjectNode stat,
                                          String tableName,
                                          String whereFilterString,
                                          EntityManager entityManager,
                                          MultiValuedMap<String, Object> requestFields,
                                          Map<String, StatQuery.AverageQuery[]> averageQueries) {

        List<String> columnQueryList = new ArrayList<>();
        for (Map.Entry<String, StatQuery.AverageQuery[]> entry : averageQueries.entrySet()) {
            String columnName = entry.getKey();
            for (StatQuery.AverageQuery averageQuery : entry.getValue()) {
                String name = !averageQuery.name().isEmpty()
                        ? String.format(averageQuery.name(), columnName)
                        : columnName;
                StringBuilder queryBuilder = new StringBuilder("(SELECT AVG(entity.");
                queryBuilder.append(columnName).append(')');
                queryBuilder.append(" FROM ").append(tableName).append(" entity ");
                if (!averageQuery.whereClause().isEmpty()) {
                    queryBuilder.append((whereFilterString.trim().isEmpty() ? " WHERE " : whereFilterString + " AND "))
                            .append(String.format(averageQuery.whereClause(), columnName));
                } else if (!whereFilterString.trim().isEmpty()) {
                    queryBuilder.append(whereFilterString);
                }
                queryBuilder.append(")").append(" AS ").append(name);
                columnQueryList.add(queryBuilder.toString());
            }
        }
        StringBuilder columnQueryBuilder = new StringBuilder("SELECT ");
        for (int index = 0; index < columnQueryList.size(); index++) {
            String columnQuery = columnQueryList.get(index);
            columnQueryBuilder.append(columnQuery);
            if (index != columnQueryList.size()-1) {
                columnQueryBuilder.append(", ");
            }
        }
        Map<String, Object> columns = singleQueryResultAsMap(clazz, columnQueryBuilder.toString(), entityManager,
                requestFields);
        for (Map.Entry<String, Object> column : columns.entrySet()) {
            putStatField(stat, column.getKey(), column.getValue());
        }

    }

    @SuppressWarnings("unchecked")
    static <T> void resolveMedianQueries(Class<T> clazz,
                                         ObjectNode stat,
                                         String tableName,
                                         String whereFilterString,
                                         EntityManager entityManager,
                                         MultiValuedMap<String, Object> requestFields,
                                         Map<String, StatQuery.MedianQuery[]> averageQueries) {

        List<String> columnQueryList = new ArrayList<>();
        for (Map.Entry<String, StatQuery.MedianQuery[]> entry : averageQueries.entrySet()) {
            String columnName = entry.getKey();
            for (StatQuery.MedianQuery averageQuery : entry.getValue()) {
                String name = !averageQuery.name().isEmpty()
                        ? String.format(averageQuery.name(), columnName)
                        : columnName;
                StringBuilder queryBuilder = new StringBuilder("(SELECT entity.");
                queryBuilder.append(columnName).append(" FROM ").append(tableName).append(" entity, ")
                        .append(tableName).append(" entity2 ");
                if (!averageQuery.whereClause().isEmpty()) {
                    queryBuilder.append((whereFilterString.trim().isEmpty() ? " WHERE " : whereFilterString + " AND "))
                            .append(String.format(averageQuery.whereClause(), columnName));
                } else if (!whereFilterString.trim().isEmpty()) {
                    queryBuilder.append(whereFilterString);
                }
                queryBuilder.append(" GROUP BY entity.").append(columnName).append(" HAVING SUM(SIGN(1-SIGN(entity2.")
                        .append(columnName).append("-entity.").append(columnName).append(")))/COUNT(*) > .5 LIMIT 1)")
                        .append(" AS ").append(name);
                columnQueryList.add(queryBuilder.toString());
            }
        }
        StringBuilder columnQueryBuilder = new StringBuilder("SELECT ");
        for (int index = 0; index < columnQueryList.size(); index++) {
            String columnQuery = columnQueryList.get(index);
            columnQueryBuilder.append(columnQuery);
            if (index != columnQueryList.size()-1) {
                columnQueryBuilder.append(", ");
            }
        }
        Map<String, Object> columns = singleQueryResultAsMap(clazz, columnQueryBuilder.toString(), entityManager,
                requestFields);
        for (Map.Entry<String, Object> column : columns.entrySet()) {
            putStatField(stat, column.getKey(), column.getValue());
        }

    }

    @SuppressWarnings("unchecked")
    static <T> void resolveOccurrenceQueries(Class<T> clazz,
                                             ObjectNode stat,
                                             long totalCount,
                                             String tableName,
                                             String whereFilterString,
                                             EntityManager entityManager,
                                             MultiValuedMap<String, Object> requestFields,
                                             Map<String, StatQuery.OccurrenceQuery[]> occurrenceQueries) {

        for (Map.Entry<String, StatQuery.OccurrenceQuery[]> entry : occurrenceQueries.entrySet()) {
            String columnName = entry.getKey();
            for (StatQuery.OccurrenceQuery occurrenceQuery : entry.getValue()) {
                StringBuilder queryBuilder = new StringBuilder("SELECT entity.").append(columnName).append(", ")
                        .append("COUNT(entity.").append(columnName).append(") AS count")
                        .append(" FROM ").append(tableName).append(" entity ");
                String name = (!occurrenceQuery.name().isEmpty()
                        ? String.format(occurrenceQuery.name(), columnName, occurrenceQuery.type().name().toLowerCase())
                        : String.format(columnName, occurrenceQuery.type().name().toLowerCase()));
                if (!occurrenceQuery.whereClause().isEmpty()) {
                    queryBuilder.append((whereFilterString.trim().isEmpty() ? " WHERE " : whereFilterString + " AND "))
                            .append(String.format(occurrenceQuery.whereClause(), columnName));
                } else if (!whereFilterString.trim().isEmpty()) {
                    queryBuilder.append(whereFilterString);
                }
                queryBuilder.append(" GROUP BY entity.").append(columnName).append(" ORDER BY count DESC ")
                        .append(" LIMIT ").append(occurrenceQuery.fetchCount());
                List<Map<String, Object>> rows = queryListResultAsMap(clazz, queryBuilder.toString(), entityManager,
                        requestFields);
                ObjectNode occurrence = mapper.createObjectNode();
                for (Map<String, Object> row : rows) {
                    Long count = (Long) row.get("count");
                    Object key = row.get(columnName);
                    if (key == null) continue;
                    if (occurrenceQuery.type() == StatQuery.OccurrenceQuery.Type.PERCENTAGE) {
                        putStatField(occurrence, key.toString(), ((count * 100) / (double) totalCount));
                    } else {
                        putStatField(occurrence, key.toString(), count);
                    }
                }
                stat.set(name, occurrence);
            }

        }

    }

    // percentage, say last month is 10 this month = 20
    // ((20 - 10) / 10) * 100

    static void putStatField(ObjectNode stat, String name, Object value) {
        if (value == null) {
            stat.putNull(name);
        } else if (value.getClass() == Integer.class) {
            stat.put(name, (Integer) value);
        } else if (value.getClass() == Long.class) {
            stat.put(name, (Long) value);
        } else if (value.getClass() == Float.class) {
            stat.put(name, (Float) value);
        } else if (value.getClass() == Double.class) {
            stat.put(name, (Double) value);
        } else if (value.getClass() == Boolean.class) {
            stat.put(name, (Boolean) value);
        } else {
            stat.put(name, String.valueOf(value));
        }
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
