package io.github.hacihaciyev.sql;

import javax.sql.DataSource;

import io.github.hacihaciyev.build_errors.SchemaVerificationException;
import io.github.hacihaciyev.config.Conf;
import io.github.hacihaciyev.sql.internal.schema.SchemaResolver;
import io.github.hacihaciyev.util.Err;

public sealed interface JQ {
    String sql();
    
    record Read(String sql) implements JQ {
        public Read(String sql, TableRef[] tableRefs, ColumnRef[] columnRefs) throws SchemaVerificationException {
            
            validate(tableRefs, columnRefs);
            this(sql);
        }
    }
    
    record Write(String sql) implements JQ {
        public Write(String sql, TableRef[] tableRefs, ColumnRef[] columnRefs) throws SchemaVerificationException {
            validate(tableRefs, columnRefs);            
            this(sql);
        }
    }
    
    private static void validate(TableRef[] tableRefs, ColumnRef[] columnRefs) throws SchemaVerificationException {
        for (var tref : tableRefs) {
            var tres = SchemaResolver.load(tref, ds());
            if (tres instanceof Err(var err)) throw err;
            
            // TODO Validate for columns
        }
    }
    
    private static DataSource ds() throws SchemaVerificationException {
        return Conf.INSTANCE.dataSource().orElseThrow(() -> new SchemaVerificationException("Cannot obtain a datasource"));
    }
}
