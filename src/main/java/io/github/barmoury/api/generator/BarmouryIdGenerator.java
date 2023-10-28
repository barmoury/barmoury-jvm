package io.github.barmoury.api.generator;

import io.github.barmoury.util.FieldUtil;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.boot.model.naming.Identifier;
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
        Identifier schema = sharedSessionContractImplementor.getSessionFactory().getJdbcServices().getJdbcEnvironment()
                .getCurrentSchema();
        if (schema == null) {
            schema = sharedSessionContractImplementor.getSessionFactory().getJdbcServices().getJdbcEnvironment()
                    .getCurrentCatalog();
        }
        String query = String.format("SELECT `AUTO_INCREMENT` FROM INFORMATION_SCHEMA.TABLES " +
                " WHERE TABLE_SCHEMA = '%s' AND TABLE_NAME = '%s' ", schema.getCanonicalName(), tableName);
        long value = ((Long)sharedSessionContractImplementor
                .createNativeQuery(query)
                .getSingleResult()) + 1L;
        return value;
    }

}
