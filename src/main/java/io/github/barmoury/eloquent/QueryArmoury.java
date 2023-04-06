package io.github.barmoury.eloquent;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.barmoury.api.model.Model;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.Setter;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import java.lang.reflect.InvocationTargetException;

// TODO, convert true to 1 and false to 0 on query, if specified in query param
// TODO accept query class for custom query
public class QueryArmoury {

    SqlInterface sqlInterface;
    @Setter boolean isSnakeCase;
    @Setter EntityManager entityManager;
    ObjectMapper mapper = new ObjectMapper();
    @Setter AutowireCapableBeanFactory autowireCapableBeanFactory;

    static final String INTERVAL_COLUMN_DATE_FORMAT = "yyyy-MM-dd HH:mm";
    static final String PERCENTAGE_CHANGE_RELAY_KEY = "___percentage_change____";

    public QueryArmoury(SqlInterface sqlInterface) {
        this.sqlInterface = sqlInterface;
    }

    public String test() {
        return sqlInterface.database();
    }

    @SuppressWarnings("unchecked")
    public <T extends Model> T getEntityForUpdateById(T field, Long entityId, Long id)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<T> clazz = (Class<T>) Model.class;
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

}
