/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package io.github.barmoury.api.generator;

import io.github.barmoury.util.FieldUtil;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class BarmouryIdGenerator implements IdentifierGenerator {

    String column;
    String sqlQuery;
    BigInteger currentValue;
    List<QualifiedTableName> tables;

    @Override
    public synchronized Object generate(SharedSessionContractImplementor sharedSessionContractImplementor, Object o)
            throws HibernateException {
        if (sqlQuery != null) {
            currentValue = sharedSessionContractImplementor
                    .createNativeQuery(sqlQuery, BigInteger.class)
                    .getSingleResult();
            if (currentValue == null) currentValue = BigInteger.ZERO;
            sqlQuery = null;
        }
        currentValue = currentValue.add(BigInteger.ONE);
        return currentValue;
    }

    @Override
    public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
        JdbcEnvironment jdbcEnvironment = serviceRegistry.getService(JdbcEnvironment.class);
        IdentifierHelper identifierHelper = jdbcEnvironment.getIdentifierHelper();
        ObjectNameNormalizer normalizer = (ObjectNameNormalizer) params
                .get(PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER);
        column = params.getProperty("column");
        if (column == null) column = params.getProperty(PersistentIdentifierGenerator.PK);
        column = normalizer.normalizeIdentifierQuoting(column).render( jdbcEnvironment.getDialect() );
        final String schema = normalizer.toDatabaseIdentifierText(params
                .getProperty(PersistentIdentifierGenerator.SCHEMA));
        final String catalog = normalizer.toDatabaseIdentifierText(params
                .getProperty(PersistentIdentifierGenerator.CATALOG));
        String tables = params.getProperty("tables");
        if (tables == null) tables = params.getProperty(PersistentIdentifierGenerator.TABLES);
        this.tables = new ArrayList<>();
        for (String table : tables.split(", ")) {
            this.tables.add(new QualifiedTableName(identifierHelper.toIdentifier(catalog),
                    identifierHelper.toIdentifier(schema), identifierHelper.toIdentifier(table)));
        }
    }

    @Override
    public void initialize(SqlStringGenerationContext context) {
        String maxColumn;
        StringBuilder union = new StringBuilder();
        for (int i = 0; i < tables.size(); i++) {
            final String tableName = context.format(tables.get(i));
            if (tables.size() > 1 ) union.append("SELECT MAX(").append(column).append(") AS mx FROM ");
            union.append(tableName);
            if (i < tables.size() - 1) union.append(" UNION ");
        }
        if (tables.size() > 1 ) {
            union.insert( 0, "( " ).append( " ) ids_" );
            maxColumn = "ids_.mx";
        } else {
            maxColumn = column;
        }
        sqlQuery = "SELECT MAX(" + maxColumn + ") FROM " + union;
    }
}
