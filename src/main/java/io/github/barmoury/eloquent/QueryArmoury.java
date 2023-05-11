package io.github.barmoury.eloquent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.barmoury.api.model.Model;
import io.github.barmoury.eloquent.impl.RequestParamFilterOperatorImpl;
import io.github.barmoury.util.FieldUtil;
import jakarta.persistence.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Setter;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.hibernate.query.sql.internal.NativeQueryImpl;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;

// TODO, convert true to 1 and false to 0 on query, if specified in query param
// TODO accept query class for custom query
public class QueryArmoury {

    SqlInterface sqlInterface;
    @Setter boolean isSnakeCase;
    @Setter EntityManager entityManager;
    ObjectMapper mapper = new ObjectMapper();
    @Setter AutowireCapableBeanFactory autowireCapableBeanFactory;

    final String INTERVAL_COLUMN_DATE_FORMAT = "yyyy-MM-dd HH:mm";
    final String PERCENTAGE_CHANGE_RELAY_KEY = "___percentage_change____";

    public QueryArmoury(SqlInterface sqlInterface) {
        this.sqlInterface = sqlInterface;
    }

    public String test() {
        return sqlInterface.database();
    }

    @SuppressWarnings("unchecked")
    public <T extends Model> T getEntityForUpdateById(Class<T> clazz, T field, Long entityId, Long id)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String tableName = clazz.getAnnotation(Entity.class).name();
        if (id != null && id > 0 && field != null && id != field.getId()) {
            Query query = entityManager.createNativeQuery(
                    String.format("SELECT entity.* FROM %s entity WHERE id = %d LIMIT 1",
                            tableName, id), clazz);
            return ((T) query.getSingleResult());
        }
        if (field != null && entityId != 0L) return field;
        T entity = clazz.getDeclaredConstructor().newInstance();
        entity.setId(id != null ? id : 0L);
        return entity;
    }

    public <T> Page<T> pageQuery(HttpServletRequest request, Pageable pageable, Class<T> clazz) {
        String tableName = FieldUtil.getTableName(clazz);
        Map<String, JoinColumn> joinTables = new HashMap<>();
        if (isSnakeCase) mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        MultiValuedMap<String, Object> requestFields = resolveQueryFields(clazz, request, joinTables,
                false);
        String queryString = String.format(" FROM %s entity %s", tableName, buildWhereFilter(requestFields, joinTables));
        Query countQuery = buildQueryObject(entityManager, String.format("SELECT COUNT(*) %s", queryString),
                clazz, requestFields, false);
        int totalElements = ((Number) countQuery.getSingleResult()).intValue();
        Query query = buildQueryObject(entityManager, String.format("SELECT entity.* %s %s", queryString,
                buildPageFilter(pageable)), clazz, requestFields, false);

        NativeQueryImpl<T> nativeQuery = (NativeQueryImpl<T>) query;
        nativeQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        List<T> content = resolveEntityList(query.getResultList(), clazz, true);

        return new PageImpl<>(content, pageable, totalElements);
    }

    public <T> JsonNode statWithQuery(HttpServletRequest request, Class<T> clazz) throws ParseException {
        String tableName = FieldUtil.getTableName(clazz);
        Map<String, JoinColumn> joinTables = new HashMap<>();
        StatQuery statQuery = FieldUtil.getAnnotation(clazz, StatQuery.class);
        MultiValuedMap<String, Object> requestFields = resolveQueryFields(clazz, request, joinTables, false);
        MultiValuedMap<String, Object> statRequestFields = resolveQueryFields(clazz, request, null, true);
        String whereFilterString = buildWhereFilter(requestFields, joinTables);
        return getResourceStat(statRequestFields, requestFields, null, entityManager, request,
                whereFilterString, statQuery, true, tableName, clazz);
    }

    <T> MultiValuedMap<String, Object> resolveQueryFields(Class<T> clazz, HttpServletRequest request,
                                                          Map<String, JoinColumn> joinTables,
                                                          boolean resolveStatQueryAnnotations) {
        MultiValuedMap<String, Object> requestFields = new ArrayListValuedHashMap<>();
        List<Field> fields = FieldUtil.getAllFields(clazz);
        for (Field field : fields) {
            String mainFieldName = field.getName();
            String columnName = FieldUtil.getFieldColumnName(field);

            RequestParamFilter[] requestParamFilters = field.getAnnotationsByType(RequestParamFilter.class);
            int requestParamFiltersCount = requestParamFilters.length;
            for (RequestParamFilter requestParamFilter : requestParamFilters) {
                if (!requestParamFilter.column().isEmpty()) columnName = requestParamFilter.column();
                if (requestParamFilter.columnIsSnakeCase()) columnName = FieldUtil.toSnakeCase(columnName);

                String fieldName = mainFieldName;
                if (requestParamFiltersCount > 1) {
                    String operator = requestParamFilter.operator().name();
                    fieldName = String.format("%s%s%c%s", fieldName,
                            requestParamFilter.multiFilterSeparator().equals("__") && isSnakeCase
                                    ? "_" : requestParamFilter.multiFilterSeparator(),
                            operator.charAt(0),
                            operator.substring(1).toLowerCase());
                }
                Set<String> extraFieldNames = new HashSet<>();
                extraFieldNames.add(fieldName);
                if (!resolveStatQueryAnnotations) {
                    Collections.addAll(extraFieldNames, requestParamFilter.aliases());
                    if (requestParamFilter.acceptSnakeCase()) {
                        for (String extraFieldName : new ArrayList<>(extraFieldNames))
                            extraFieldNames.add(FieldUtil.toSnakeCase(extraFieldName));
                    }
                    if (requestParamFilter.operator() == RequestParamFilter.Operator.RANGE) {
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
                        resolveQueryForSingleField(requestFields, fromRequestParamFilter2, false,
                                joinTables, request, fromExtraFieldNames, columnName, field);
                        resolveQueryForSingleField(requestFields, toRequestParamFilter2, false,
                                joinTables, request, toExtraFieldNames, columnName, field);
                        continue;
                    }
                }
                resolveQueryForSingleField(requestFields, requestParamFilter, resolveStatQueryAnnotations,
                        joinTables, request, extraFieldNames, columnName, field);
            }
            // for stat query
            if (resolveStatQueryAnnotations && requestParamFiltersCount == 0) {
                Set<String> extraFieldNames = new HashSet<>(); extraFieldNames.add(mainFieldName);
                resolveQueryForSingleField(requestFields, null, resolveStatQueryAnnotations,
                        joinTables, request, extraFieldNames, columnName, field);
            }

        }
        return requestFields;
    }

    public boolean hasStatQueryCapability(HttpServletRequest request, StatQuery statQuery, String capability) {
        if (statQuery == null) return false;
        if (!isSnakeCase) capability = FieldUtil.toCamelCase(capability);
        capability = "stat.query." + capability;
        return (!statQuery.enableClientQuery()
                || (!request.getParameterMap().containsKey(capability) ||
                (request.getParameterMap().containsKey(capability) &&
                        !request.getParameter(capability).equalsIgnoreCase("false"))));
    }

    public <T> ObjectNode getResourceStat(MultiValuedMap<String, Object> statRequestFields,
                                                 MultiValuedMap<String, Object> requestFields,
                                                 Map<String, Long> currentPercentageMap,
                                                 EntityManager entityManager,
                                                 HttpServletRequest request,
                                                 String whereFilterString,
                                                 StatQuery statQuery,
                                                 boolean isMainStat,
                                                 String tableName,
                                                 Class<T> clazz) throws ParseException {

        String newEndDateStr = null;
        String newStartDateStr = null;
        ObjectNode stat = mapper.createObjectNode();

        Map<String, StatQuery.MedianQuery[]> medianQueries = new HashMap<>();
        Map<String, StatQuery.ColumnQuery[]> columnQueries = new HashMap<>();
        Map<String, StatQuery.AverageQuery[]> averageQueries = new HashMap<>();
        Map<String, StatQuery.OccurrenceQuery[]> occurrenceQueries = new HashMap<>();
        Map<String, StatQuery.PercentageChangeQuery[]> percentageChangeQueries = new HashMap<>();

        for (String fieldName : statRequestFields.keySet()) {
            Object[] values = statRequestFields.get(fieldName).toArray();
            String columnName = (String) values[0];
            if (statQuery != null && statQuery.columnsAreSnakeCase()) columnName = FieldUtil.toSnakeCase(columnName);

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

        String to = null;
        String from = null;
        long different = 0;
        Date toDate = null;
        Date startDate = null;
        String toKey = null;
        String fromKey = null;
        boolean isCamelCase = false;
        String differentUnit = "days";
        boolean containsIntervalValues = false;
        if (statQuery != null) {
            String intervalColumn = statQuery.intervalColumn();
            if (hasStatQueryCapability(request, statQuery, "interval_column")) {
                String qValue = request.getParameter("stat.query.interval_column");
                if (qValue != null) intervalColumn = qValue;
            }
            fromKey = intervalColumn + "_from";
            if (!requestFields.containsKey(fromKey)) {
                isCamelCase = true;
                fromKey = FieldUtil.toCamelCase(fromKey);
            }
            toKey = isCamelCase ? intervalColumn + "To" : intervalColumn + "_to";
            containsIntervalValues = requestFields.containsKey(fromKey) && requestFields.containsKey(toKey);
        }
        if (containsIntervalValues) {
            from = (String) (requestFields.get(fromKey).toArray())[3];
            to = (String) (requestFields.get(toKey).toArray())[3];
            startDate = new SimpleDateFormat(INTERVAL_COLUMN_DATE_FORMAT, Locale.ENGLISH).parse(from);
            toDate = new SimpleDateFormat(INTERVAL_COLUMN_DATE_FORMAT, Locale.ENGLISH).parse(to);
        }
        boolean processPrevious = isMainStat && currentPercentageMap == null &&
                containsIntervalValues && statQuery.fetchPrevious() &&
                hasStatQueryCapability(request, statQuery, "fetch_previous");
        if (processPrevious) {
            Date newEndDate;
            Date newStartDate = newEndDate = startDate;
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(newStartDate);
            different = ChronoUnit.DAYS.between(toDate.toInstant(), startDate.toInstant());
            if (different < 0) {
                calendar.add(Calendar.DATE, (int) different);
            } else {
                different = ChronoUnit.HOURS.between(toDate.toInstant(), startDate.toInstant());
                if (different < 0) {
                    calendar.add(Calendar.HOUR, (int) different);
                    differentUnit = "hours";
                } else {
                    different = ChronoUnit.MINUTES.between(toDate.toInstant(), startDate.toInstant());
                    calendar.add(Calendar.MINUTE, (int) different);
                    differentUnit = "minutes";
                }
            }
            newStartDate = Date.from(calendar.toInstant());
            newEndDateStr = new SimpleDateFormat(INTERVAL_COLUMN_DATE_FORMAT).format(newEndDate); //lol - reuse
            newStartDateStr = new SimpleDateFormat(INTERVAL_COLUMN_DATE_FORMAT).format(newStartDate); //lol - reuse
        }
        long totalCount =
                resolveColumnQueries(request, clazz, stat, tableName, statQuery, whereFilterString,
                        entityManager, requestFields, columnQueries);

        if (!medianQueries.isEmpty() && hasStatQueryCapability(request, statQuery, "process_medians")) {
            resolveMedianQueries(clazz, stat, tableName, whereFilterString, entityManager, requestFields,
                    medianQueries);
        }
        if (!averageQueries.isEmpty() && hasStatQueryCapability(request, statQuery, "process_averages")) {
            resolveAverageQueries(clazz, stat, tableName, whereFilterString, entityManager, requestFields,
                    averageQueries);
        }
        if (!occurrenceQueries.isEmpty() && hasStatQueryCapability(request, statQuery, "process_occurrences")) {
            resolveOccurrenceQueries(clazz, stat, totalCount, tableName, whereFilterString, entityManager, requestFields,
                    occurrenceQueries);
        }

        Map<String, Long> percentageMap = null;
        if (!percentageChangeQueries.isEmpty()
                && hasStatQueryCapability(request, statQuery, "process_percentage_changes")) {
            percentageMap = resolvePercentageChangeQueries(clazz, tableName, whereFilterString, entityManager,
                    requestFields, percentageChangeQueries);
        }
        if (isMainStat && from != null) {
            stat.put("from", from);
            stat.put("to", to);
        }

        ObjectNode previous = null;
        if (processPrevious) {
            previous = getStatBetweenDate(statRequestFields, requestFields, percentageMap, entityManager, request,
                    whereFilterString, statQuery, tableName, clazz,
                    fromKey, toKey,
                    newStartDateStr, newEndDateStr,
                    differentUnit, -different);

        }
        if (percentageMap != null && currentPercentageMap != null) {
            stat.set(PERCENTAGE_CHANGE_RELAY_KEY, resolvePercentageChangeQueries(currentPercentageMap, percentageMap));
        }
        if (processPrevious) {
            if (previous.has(PERCENTAGE_CHANGE_RELAY_KEY)) {
                JsonNode percentageChange = previous.remove(PERCENTAGE_CHANGE_RELAY_KEY);
                Iterator<String> fieldNames = percentageChange.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    stat.put(fieldName, percentageChange.get(fieldName).doubleValue());
                }
            }
            stat.set("previous", previous);
        }
        if (isMainStat && startDate != null && statQuery.fetchHourly() &&
                hasStatQueryCapability(request, statQuery, "fetch_hourly")) {
            stat.set("hourly", fetchHourly(statRequestFields, requestFields, entityManager, request,
                    whereFilterString, statQuery, tableName, clazz,
                    fromKey, toKey, startDate, toDate));
        }
        if (isMainStat && startDate != null && statQuery.fetchMonthly() &&
                hasStatQueryCapability(request, statQuery, "fetch_monthly")) {
            stat.set("monthly", fetchMonthly(statRequestFields, requestFields, entityManager, request,
                    whereFilterString, statQuery, tableName, clazz,
                    fromKey, toKey, startDate));
        }
        if (isMainStat && startDate != null && statQuery.fetchWeekDays() &&
                hasStatQueryCapability(request, statQuery, "fetch_week_days")) {
            stat.set(isSnakeCase ? "week_days" : "weekDays", fetchWeekDays(statRequestFields, requestFields,
                    entityManager, request, whereFilterString, statQuery, tableName, clazz,
                    fromKey, toKey, startDate));
        }
        if (isMainStat && startDate != null && statQuery.fetchMonthDays() &&
                hasStatQueryCapability(request, statQuery, "fetch_month_days")) {
            stat.set(isSnakeCase ? "month_days" : "monthDays", fetchMonthDays(statRequestFields, requestFields,
                    entityManager, request, whereFilterString, statQuery, tableName, clazz,
                    fromKey, toKey, startDate));
        }
        if (isMainStat && startDate != null && statQuery.fetchYearly() &&
                hasStatQueryCapability(request, statQuery, "fetch_yearly")) {
            stat.set("yearly", fetchYearly(statRequestFields, requestFields, entityManager, request,
                    whereFilterString, statQuery, tableName, clazz,
                    fromKey, toKey, startDate, toDate));
        }

        return stat;
    }

    <T> ObjectNode getStatBetweenDate(MultiValuedMap<String, Object> statRequestFields,
                                             MultiValuedMap<String, Object> requestFields,
                                             Map<String, Long> currentPercentageMap,
                                             EntityManager entityManager,
                                             HttpServletRequest request,
                                             String whereFilterString,
                                             StatQuery statQuery,
                                             String tableName,
                                             Class<T> clazz,

                                             String fromKey,
                                             String toKey,
                                             String newStartStr,
                                             String newEndStr,

                                             String differentUnit,
                                             long different) throws ParseException  {
        Object[] modified = requestFields.remove(toKey).toArray(); modified[3] = newEndStr;
        requestFields.putAll(toKey, Arrays.asList(modified));
        modified = requestFields.remove(fromKey).toArray(); modified[3] = newStartStr;
        requestFields.putAll(fromKey, Arrays.asList(modified));
        ObjectNode result = getResourceStat(statRequestFields, requestFields, currentPercentageMap, entityManager, request,
                whereFilterString, statQuery, false, tableName, clazz);
        result.put("from", newStartStr);
        result.put("to", newEndStr);
        result.put(isSnakeCase ? "difference_unit" : "differenceUnit", differentUnit);
        result.put(isSnakeCase ? "difference_from_present" : "differenceFromPresent", different);
        return result;
    }

    <T> ArrayNode fetchHourly(MultiValuedMap<String, Object> statRequestFields,
                                     MultiValuedMap<String, Object> requestFields,
                                     EntityManager entityManager,
                                     HttpServletRequest request,
                                     String whereFilterString,
                                     StatQuery statQuery,
                                     String tableName,
                                     Class<T> clazz,

                                     String fromKey,
                                     String toKey,
                                     Date startDate,
                                     Date endDate) throws ParseException {
        ArrayNode arrayNode = mapper.createArrayNode();
        long different = ChronoUnit.HOURS.between(startDate.toInstant(), endDate.toInstant());

        while (different > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            calendar.add(Calendar.HOUR, 1);
            endDate = calendar.getTime();
            different--;
            arrayNode.add(getStatBetweenDate(statRequestFields, requestFields, null, entityManager, request,
                    whereFilterString, statQuery, tableName, clazz,
                    fromKey, toKey,
                    new SimpleDateFormat(INTERVAL_COLUMN_DATE_FORMAT).format(startDate),
                    new SimpleDateFormat(INTERVAL_COLUMN_DATE_FORMAT).format(endDate),
                    "hourly", different+1));
            startDate = endDate;
        }

        return arrayNode;
    }

    <T> ArrayNode fetchMonthly(MultiValuedMap<String, Object> statRequestFields,
                                      MultiValuedMap<String, Object> requestFields,
                                      EntityManager entityManager,
                                      HttpServletRequest request,
                                      String whereFilterString,
                                      StatQuery statQuery,
                                      String tableName,
                                      Class<T> clazz,

                                      String fromKey,
                                      String toKey,
                                      Date mainStartDate) throws ParseException {
        ArrayNode arrayNode = mapper.createArrayNode();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mainStartDate);
        int monthIndex = calendar.get(Calendar.MONTH);
        calendar.set(Calendar.DAY_OF_YEAR, 1);

        for (int index = 0; index < 12; index++) {
            calendar.set(Calendar.MONTH, index);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            Date startDate = calendar.getTime();
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            Date endDate = calendar.getTime();

            ObjectNode stat = getStatBetweenDate(statRequestFields, requestFields, null, entityManager, request,
                    whereFilterString, statQuery, tableName, clazz,
                    fromKey, toKey,
                    new SimpleDateFormat(INTERVAL_COLUMN_DATE_FORMAT).format(startDate),
                    new SimpleDateFormat(INTERVAL_COLUMN_DATE_FORMAT).format(endDate),
                    "monthly", (long)index - monthIndex);
            stat.put(isSnakeCase ? "month_name" : "monthName", Month.of(index+1)
                    .getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()));
            arrayNode.add(stat);
        }

        return arrayNode;
    }

    <T> ArrayNode fetchWeekDays(MultiValuedMap<String, Object> statRequestFields,
                                       MultiValuedMap<String, Object> requestFields,
                                       EntityManager entityManager,
                                       HttpServletRequest request,
                                       String whereFilterString,
                                       StatQuery statQuery,
                                       String tableName,
                                       Class<T> clazz,

                                       String fromKey,
                                       String toKey,
                                       Date mainStartDate) throws ParseException {
        ArrayNode arrayNode = mapper.createArrayNode();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mainStartDate);
        int monthIndex = calendar.get(Calendar.DAY_OF_WEEK);
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());

        for (int index = 0; index < 7; index++) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            Date startDate = calendar.getTime();
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            Date endDate = calendar.getTime();

            ObjectNode stat = getStatBetweenDate(statRequestFields, requestFields, null, entityManager, request,
                    whereFilterString, statQuery, tableName, clazz,
                    fromKey, toKey,
                    new SimpleDateFormat(INTERVAL_COLUMN_DATE_FORMAT).format(startDate),
                    new SimpleDateFormat(INTERVAL_COLUMN_DATE_FORMAT).format(endDate),
                    isSnakeCase ? "week_day" : "weekDay", (long)index - monthIndex+1);
            stat.put(isSnakeCase ? "week_day" : "weekDay", DayOfWeek.of(index == 0 ? 7 : index)
                    .getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()));
            arrayNode.add(stat);

            calendar.add(Calendar.DAY_OF_WEEK, 1);
        }

        return arrayNode;
    }

    <T> ArrayNode fetchMonthDays(MultiValuedMap<String, Object> statRequestFields,
                                        MultiValuedMap<String, Object> requestFields,
                                        EntityManager entityManager,
                                        HttpServletRequest request,
                                        String whereFilterString,
                                        StatQuery statQuery,
                                        String tableName,
                                        Class<T> clazz,

                                        String fromKey,
                                        String toKey,
                                        Date mainStartDate) throws ParseException {
        ArrayNode arrayNode = mapper.createArrayNode();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mainStartDate);
        int monthIndex = calendar.get(Calendar.DAY_OF_MONTH);
        int maxMonthDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int index = 1; index <= maxMonthDay; index++) {
            calendar.set(Calendar.DAY_OF_MONTH, index);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            Date startDate = calendar.getTime();
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            Date endDate = calendar.getTime();

            ObjectNode stat = getStatBetweenDate(statRequestFields, requestFields, null, entityManager, request,
                    whereFilterString, statQuery, tableName, clazz,
                    fromKey, toKey,
                    new SimpleDateFormat(INTERVAL_COLUMN_DATE_FORMAT).format(startDate),
                    new SimpleDateFormat(INTERVAL_COLUMN_DATE_FORMAT).format(endDate),
                    isSnakeCase ? "day_of_month" : "dayOfMonth", (long)index - monthIndex);
            stat.put(isSnakeCase ? "day_of_month" : "dayOfMonth", index);
            arrayNode.add(stat);

            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return arrayNode;
    }

    public int getDiffYears(Date startDate, Date endDate) {
        Calendar firstCalender = Calendar.getInstance();
        Calendar secondCalender = Calendar.getInstance();
        firstCalender.setTime(startDate);
        secondCalender.setTime(endDate);
        int diff = secondCalender.get(Calendar.YEAR) - firstCalender.get(Calendar.YEAR);
        if (firstCalender.get(Calendar.MONTH) > secondCalender.get(Calendar.MONTH) ||
                (firstCalender.get(Calendar.MONTH) == secondCalender.get(Calendar.MONTH) &&
                        firstCalender.get(Calendar.DATE) > secondCalender.get(Calendar.DATE))) {
            diff--;
        }
        return diff;
    }

    <T> ArrayNode fetchYearly(MultiValuedMap<String, Object> statRequestFields,
                                     MultiValuedMap<String, Object> requestFields,
                                     EntityManager entityManager,
                                     HttpServletRequest request,
                                     String whereFilterString,
                                     StatQuery statQuery,
                                     String tableName,
                                     Class<T> clazz,

                                     String fromKey,
                                     String toKey,
                                     Date startDate,
                                     Date endDate) throws ParseException {

        ArrayNode arrayNode = mapper.createArrayNode();
        long different = getDiffYears(startDate, endDate)+1;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);

        for (int index = 0; index < different; index++) {
            calendar.set(Calendar.MONTH, index);
            calendar.set(Calendar.DAY_OF_YEAR, 1);
            startDate = calendar.getTime();
            calendar.set(Calendar.MONTH, 11);
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            endDate = calendar.getTime();

            ObjectNode stat = getStatBetweenDate(statRequestFields, requestFields, null, entityManager, request,
                    whereFilterString, statQuery, tableName, clazz,
                    fromKey, toKey,
                    new SimpleDateFormat(INTERVAL_COLUMN_DATE_FORMAT).format(startDate),
                    new SimpleDateFormat(INTERVAL_COLUMN_DATE_FORMAT).format(endDate),
                    "yearly", different - index - 1L);
            stat.put("year", calendar.get(Calendar.YEAR));
            arrayNode.add(stat);

            calendar.add(Calendar.YEAR, 1);
        }

        return arrayNode;
    }

    @SuppressWarnings("unchecked")
    public <T>  List<Map<String, Object>> queryListResultAsMap(Class<T> clazz,
                                                                      String queryString,
                                                                      EntityManager entityManager,
                                                                      MultiValuedMap<String, Object> requestFields) {
        Query query = buildQueryObject(entityManager, queryString, clazz, requestFields, false);
        NativeQueryImpl<Map<String, Object>> nativeQuery = (NativeQueryImpl<Map<String, Object>>) query;
        nativeQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        return nativeQuery.getResultList();
    }

    @SuppressWarnings("unchecked")
    <T> long resolveColumnQueries(HttpServletRequest request,
                                         Class<T> clazz,
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

        if (hasStatQueryCapability(request, statQuery, "process_column_queries")) {
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
    <T> void resolveAverageQueries(Class<T> clazz,
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
    <T> void resolveMedianQueries(Class<T> clazz,
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
    <T> void resolveOccurrenceQueries(Class<T> clazz,
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
                    // TODO convert 0 and 1 to boolean if nooleanToInt is true
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
    @SuppressWarnings("unchecked")
    <T> Map<String, Long> resolvePercentageChangeQueries(Class<T> clazz,
                                                                String tableName,
                                                                String whereFilterString,
                                                                EntityManager entityManager,
                                                                MultiValuedMap<String, Object> requestFields,
                                                                Map<String, StatQuery.PercentageChangeQuery[]> percentageChangeQueries) {

        Map<String, Long> percentageMap = new HashMap<>();
        for (Map.Entry<String, StatQuery.PercentageChangeQuery[]> entry : percentageChangeQueries.entrySet()) {
            String columnName = entry.getKey();
            for (StatQuery.PercentageChangeQuery percentageQuery : entry.getValue()) {
                String name = !percentageQuery.name().isEmpty()
                        ? String.format(percentageQuery.name(), columnName)
                        : columnName;
                StringBuilder queryBuilder = new StringBuilder("SELECT ")
                        .append(percentageQuery.sqlFunction()).append("(entity.").append(columnName)
                        .append(") AS count").append(" FROM ").append(tableName).append(" entity ");
                if (!percentageQuery.whereClause().isEmpty()) {
                    queryBuilder.append((whereFilterString.trim().isEmpty() ? " WHERE " : whereFilterString + " AND "))
                            .append(String.format(percentageQuery.whereClause(), columnName));
                } else if (!whereFilterString.trim().isEmpty()) {
                    queryBuilder.append(whereFilterString);
                }
                Map<String, Object> row = singleQueryResultAsMap(clazz, queryBuilder.toString(), entityManager,
                        requestFields);
                Object value = row.get("count");
                if (row.get("count") instanceof BigDecimal) {
                    value = ((BigDecimal) value).longValue();
                }
                percentageMap.put(name, value == null ? 0 : (Long) value);
            }

        }
        return percentageMap;
    }

    // percentage, say last month is 10 this month = 20
    // ((20 - 10) / 10) * 100
    ObjectNode resolvePercentageChangeQueries(Map<String, Long> current, Map<String, Long> previous) {
        ObjectNode result = mapper.createObjectNode();
        for (Map.Entry<String, Long> entry : current.entrySet()) {
            String columnName = entry.getKey();
            double percentageChange = entry.getValue() - (double) previous.get(columnName);
            percentageChange = percentageChange / previous.get(columnName);
            percentageChange = percentageChange * 100.0;
            result.put(columnName, percentageChange);
        }
        return result;
    }

    void putStatField(ObjectNode stat, String name, Object value) {
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

    public <T> T getResourceById(JpaRepository<T, Long> repository, long id, String message) {
        Optional<T> resource = repository.findById(id);
        if (resource.isPresent()) return resource.get();
        throw new EntityNotFoundException(String.format("%s, %d", message, id));
    }
    
    void resolveQueryForSingleField(MultiValuedMap<String, Object> requestFields,
                                    RequestParamFilter requestParamFilter,
                                    boolean resolveStatQueryAnnotations,
                                    Map<String, JoinColumn> joinTables,
                                    HttpServletRequest request,
                                    Set<String> queryParams,
                                    String columnName,
                                    Field field) {
        for (String queryParam : queryParams) {
            String[] values = new String[]{null};
            boolean isPresent = false;
            boolean isEntity = !resolveStatQueryAnnotations && requestParamFilter.operator() == RequestParamFilter.Operator.ENTITY;
            boolean objectFilter = (!resolveStatQueryAnnotations
                    && (requestParamFilter.operator() == RequestParamFilter.Operator.OBJECT_EQ
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
                    if ((entry.getKey().equals(queryParam) || (objectFilter && entry.getKey().startsWith(queryParam))
                            || (isEntity && entry.getKey().startsWith(queryParam + ".")))) {
                        boolean anyValuePresent = false;
                        List<String> validValues = new ArrayList<>();
                        for (String value : entry.getValue()) {
                            if (value.isEmpty()) continue;
                            if (requestParamFilter.booleanToInt() &&
                                    FieldUtil.objectsHasAnyType(field.getType(), boolean.class, Boolean.class)) {
                                value = value.equals("true") ? "1" : "0";
                            }
                            validValues.add(value);
                            if (!anyValuePresent) anyValuePresent = true;
                        }
                        validValues.toArray(values);
                        if (!anyValuePresent) continue;
                        queryParam = (objectFilter && requestParamFilter.columnObjectFieldsIsSnakeCase()
                                ? FieldUtil.toSnakeCase(entry.getKey())
                                : entry.getKey());
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
            Entity entity = null;
            Class<?> fieldClass = null;
            JoinColumn joinColumn = null;
            if (isEntity) {
                fieldClass = field.getType();
                entity = fieldClass.getAnnotation(Entity.class);
                if (entity != null) {
                    columnName = String.format("%s_entity.%s", entity.name(),
                            queryParam.substring(queryParam.indexOf(".")+1));
                }
                Field actualSubField = null;
                String[] nameParts = queryParam.split("\\.");
                joinColumn = field.getAnnotation(JoinColumn.class);
                String actualFieldName = nameParts[nameParts.length-1];
                actualSubField = FieldUtil.getDeclaredField(fieldClass, isSnakeCase
                        ? FieldUtil.toCamelCase(actualFieldName)
                        : actualFieldName);
                if (actualSubField == null) {
                    actualSubField = FieldUtil.getDeclaredField(fieldClass, actualFieldName);
                }
                if (actualSubField != null) {
                    RequestParamFilter actualRequestParamFilter = actualSubField.getAnnotation(RequestParamFilter.class);
                    if (actualRequestParamFilter != null) requestParamFilter = actualRequestParamFilter;
                }
            }

            requestFields.put(queryParam, columnName);
            requestFields.put(queryParam, isPresent);
            requestFields.put(queryParam, requestParamFilter);
            requestFields.put(queryParam, values[0]);
            if (!resolveStatQueryAnnotations && entity != null) {
                requestFields.put(queryParam, fieldClass);
                joinTables.put(entity.name(), joinColumn);
            }
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

    String buildWhereFilter(MultiValuedMap<String, Object> requestFields,
                            Map<String, JoinColumn> joinTables) {
        boolean virginQuery = true;
        StringBuilder whereQuery = new StringBuilder();
        for (Map.Entry<String, JoinColumn> joinTable : joinTables.entrySet()) {
            whereQuery.append(" INNER JOIN ")
                    .append(joinTable.getKey()).append(" ").append(joinTable.getKey()).append("_entity ")
                    .append(" ON entity.").append(joinTable.getValue().name()).append(" = ")
                    .append(joinTable.getKey()).append("_entity.")
                    .append(joinTable.getValue().referencedColumnName()).append(" ");
        }
        for (String matchingFieldName : requestFields.keySet()) {
            Object[] values = requestFields.get(matchingFieldName).toArray();
            RequestParamFilter requestParamFilter = (RequestParamFilter) values[2];
            String columnName = (String) values[0];

            if (virginQuery) whereQuery.append(" WHERE ");
            else whereQuery.append("AND");
            whereQuery.append(getRelationQueryPart(columnName,
                    values.length > 4,
                    matchingFieldName,
                    requestParamFilter.operator()));
            virginQuery = false;
        }
        return whereQuery.toString();
    }

    String getRelationQueryPart(String column,
                                       boolean isEntityField,
                                       String matchingFieldName,
                                       RequestParamFilter.Operator operator) {
        StringBuilder relationPart = new StringBuilder(" (");
        if (!isEntityField) relationPart.append("entity.");
        relationPart.append(column).append(" ");
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
        } else if (operator == RequestParamFilter.Operator.IN) {
            relationPart.append(String.format(" IN :%s ", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.GT_EQ) {
            relationPart.append(String.format(" >= :%s ", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.LT_EQ) {
            relationPart.append(String.format(" <= :%s ", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.LIKE
                || operator == RequestParamFilter.Operator.CONTAINS) {
            relationPart.append(String.format(" LIKE CONCAT('%%', :%s, '%%')", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.ILIKE) {
            relationPart.append(String.format(" ILIKE CONCAT('%%', :%s, '%%')", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.NOT_LIKE
                || operator == RequestParamFilter.Operator.NOT_CONTAINS) {
            relationPart.append(String.format(" NOT LIKE CONCAT('%%', :%s, '%%')", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.NOT_ILIKE) {
            relationPart.append(String.format(" NOT ILIKE CONCAT('%%', :%s, '%%')", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.ENDS_WITH) {
            relationPart.append(String.format(" LIKE CONCAT('%%', :%s)", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.STARTS_WITH) {
            relationPart.append(String.format(" LIKE CONCAT(:%s, '%%')", matchingFieldName));
        } else if (operator == RequestParamFilter.Operator.NOT_IN) {
            relationPart.append(String.format(" NOT IN :%s ", matchingFieldName));
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
        } else if (operator == RequestParamFilter.Operator.ENTITY) {
            relationPart.append(String.format(" = :%s ", matchingFieldName));
        }
        relationPart.append(") ");
        return relationPart.toString();
    }

    <T> Query buildQueryObject(EntityManager entityManager,
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

    String buildPageFilter(Pageable pageable) {
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

    public <T> List<T> resolveEntityList(List<Map<String, Object>> rows,
                                                    Class<T> tClass,
                                                    boolean resolveSubEntities) {

        List<T> result = new ArrayList<>();
        Map<String, Object[]> joinColumnFields = resolveSubEntities ? FieldUtil.findJoinColumnFields(tClass) : null;
        for (Map<String, Object> row : rows) {
            // TODO this is not efficient, should fetch typed directly and not convert with mapper
            result.add(mapper.convertValue(processSingleRow(row, joinColumnFields, tClass, resolveSubEntities),
                    tClass));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    <T> Map<String, Object> processSingleRow(Map<String, Object> row,
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
                nrow.put(isSnakeCase ? FieldUtil.toSnakeCase(field.getName()) : FieldUtil.toCamelCase(field.getName()),
                        resolveSubEntity(field, joinColumn, tableClazz, row.get(key)));
            } else {
                Object value = row.get(key);
                Field field = FieldUtil.getDeclaredField(tClass, isSnakeCase ? FieldUtil.toCamelCase(key) : key);
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
                nrow.put(isSnakeCase ? FieldUtil.toSnakeCase(key) : FieldUtil.toCamelCase(key), value);
            }
        }
        return nrow;
    }

    Object resolveSubEntity(Field field,
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
        Map<String, Object> entry = singleQueryResultAsMap(null, queryString, entityManager, requestFields);
        Map<String, Object[]> joinColumnFields = FieldUtil.findJoinColumnFields(tClass);
        return processSingleRow(entry, joinColumnFields, tClass, true);
    }

    <T>  Map<String, Object> singleQueryResultAsMap(Class<T> clazz,
                                                    String queryString,
                                                    EntityManager entityManager,
                                                    MultiValuedMap<String, Object> requestFields) {
        Query query = buildQueryObject(entityManager, queryString, clazz, requestFields, false);
        NativeQueryImpl<Map<String, Object>> nativeQuery = (NativeQueryImpl<Map<String, Object>>) query;
        nativeQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        return nativeQuery.getSingleResult();
    }


}
