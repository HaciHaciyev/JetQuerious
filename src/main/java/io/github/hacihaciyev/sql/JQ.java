package io.github.hacihaciyev.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import io.github.hacihaciyev.build_errors.SchemaVerificationException;
import io.github.hacihaciyev.config.Conf;
import io.github.hacihaciyev.jdbc.JetQuerious;
import io.github.hacihaciyev.sql.internal.schema.Column;
import io.github.hacihaciyev.sql.internal.schema.Table;
import io.github.hacihaciyev.types.SQLType;
import io.github.hacihaciyev.types.internal.TypeInfo;
import io.github.hacihaciyev.types.internal.TypeRegistry;
import io.github.hacihaciyev.util.Err;
import io.github.hacihaciyev.util.Ok;

import static io.github.hacihaciyev.sql.internal.schema.SchemaResolver.load;
import static java.util.Objects.requireNonNull;

public sealed interface JQ {
    String sql();
    JQ bind(JetQuerious instance);
    
    record Read(String sql, JetQuerious executor) implements JQ {
        public Read(String sql, TableRef[] tableRefs, ColumnRef[] columnRefs) throws SchemaVerificationException {
            Validator.validate(tableRefs, columnRefs);
            this(sql, JetQuerious.defaultInstance());
        }
        
        @Override
        public JQ bind(JetQuerious instance) {
            return new Read(sql, requireNonNull(instance, "JetQuerious instance cannot be null"));
        }
    }
    
    record Write(String sql, JetQuerious executor) implements JQ {
        public Write(String sql, TableRef[] tableRefs, ColumnRef[] columnRefs) throws SchemaVerificationException {
            Validator.validate(tableRefs, columnRefs);            
            this(sql, JetQuerious.defaultInstance());
        }
        
        @Override
        public JQ bind(JetQuerious instance) {
            return new Read(sql, requireNonNull(instance, "JetQuerious instance cannot be null"));
        }
    }
    
    static class Validator {
        private static void validate(TableRef[] tableRefs, ColumnRef[] columnRefs) throws SchemaVerificationException {
            if (tableRefs.length == 0) {
                assert columnRefs.length == 0 : "ColumnRefs without TableRefs";
                return;
            }
            
            var loadedTables = new HashMap<TableRef, Table>();
            
            for (var tref : tableRefs) {
                switch(load(tref, ds())) {
                    case Err(var err) -> throw new SchemaVerificationException("Table '" + tref + "' not found or failed to load", err);
                    case Ok(var table) -> loadedTables.put(tref, table);
                };
            }
            
            validateColumns(loadedTables, columnRefs);
        }
        
        private static void validateColumns(Map<TableRef, Table> tables, ColumnRef[] crefs) throws SchemaVerificationException {
            var errs = new ArrayList<String>();
            
            for (var cref : crefs) {
                boolean found = false;
                        
                for (var e : tables.entrySet()) {
                    var tref = e.getKey();
                    var table = e.getValue();
                    
                    var columnOpt = table.column(cref, tref);
                    if (columnOpt.isEmpty()) continue;
                    found = true;
                    
                    if (cref.type() instanceof ColumnRef.Type.Some(var type)) {
                        var foundedCol = columnOpt.get();
                        if (foundedCol instanceof Column.Known(_, var sqlType, _)) validateTypes(cref, type, sqlType, errs);
                    }
                    break;
                }
                
                if (!found) errs.add("Column '" + cref + "' not found in any of the referenced tables");
            }
            
            if (!errs.isEmpty()) throw new SchemaVerificationException("Schema validation failed:\n  - " + String.join("\n  - ", errs));
        }
        
        private static void validateTypes(ColumnRef cref, Class<?> type, SQLType sqlType, List<String> errs) {
            var typeInfo = TypeRegistry.info(type);
            
            switch (typeInfo) {
                case TypeInfo.Some(_, var compatibleSqlTypes) -> {
                    if (!compatibleSqlTypes.contains(sqlType)) errs.add(typeMismatchError(cref, type, sqlType, compatibleSqlTypes));
                }
                case TypeInfo.WithFactory(_, var compatibleSqlTypes, _, _) -> {
                    if (!compatibleSqlTypes.contains(sqlType)) errs.add(typeMismatchError(cref, type, sqlType, compatibleSqlTypes));
                }
                case TypeInfo.None _ -> errs.add(unsupportedType(cref, type));
            }
        }
        
        private static String typeMismatchError(ColumnRef cref, Class<?> type, SQLType sqlType, Set<SQLType> compatibleSqlTypes) {
            return String.format(
                "Type mismatch for column '%s': Java type '%s' is not compatible with SQL type '%s'. Expected one of: %s",
                cref, type.getSimpleName(), sqlType, compatibleSqlTypes
            );
        }
        
        private static String unsupportedType(ColumnRef cref, Class<?> type) {
            return String.format("Unsupported Java type '%s' for column '%s'", type.getName(), cref);
        }
        
        private static DataSource ds() throws SchemaVerificationException {
            return Conf.INSTANCE.dataSource().orElseThrow(() -> new SchemaVerificationException("Cannot obtain a datasource"));
        }
    }
}
