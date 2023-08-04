package io.github.barmoury.api.generator;

import io.github.barmoury.util.FieldUtil;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.beans.Statement;
import java.util.stream.Stream;

public class BarmouryIdGenerator implements IdentifierGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor sharedSessionContractImplementor, Object o)
            throws HibernateException {
        String tableName = FieldUtil.getTableName(o.getClass());
        String id = sharedSessionContractImplementor.getEntityPersister(o.getClass().getName(), o)
                .getIdentifierPropertyName();
        String query = String.format("SELECT %s FROM %s ORDER BY %s DESC LIMIT 1", id, tableName, id);

        Object[] ids = sharedSessionContractImplementor.createQuery(query, String.class).stream().toArray();
        if (ids.length > 0) return Long.parseLong(ids[0].toString()) + 1;
        return 1L;
    }

}
