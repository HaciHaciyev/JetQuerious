package io.github.hacihaciyev.sql;

import java.util.ArrayList;
import java.util.HashMap;

import javax.sql.DataSource;

import io.github.hacihaciyev.build_errors.SchemaVerificationException;
import io.github.hacihaciyev.config.Conf;
import io.github.hacihaciyev.sql.internal.schema.Table;
import io.github.hacihaciyev.util.Err;
import io.github.hacihaciyev.util.Ok;

import static io.github.hacihaciyev.sql.internal.schema.SchemaResolver.load;

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
                   
        var errors = new ArrayList<String>();
        
        for (var cref : columnRefs) {
            boolean found = false;
                        
            for (var e : loadedTables.entrySet()) {
                if (e.getValue().hasColumn(cref, e.getKey())) {
                    found = true;
                    break;
                }
            }

            if (!found) errors.add("Column '" + cref + "' not found in any of the referenced tables");
        }
        
        if (!errors.isEmpty()) throw new SchemaVerificationException("Schema validation failed:\n  - " + String.join("\n  - ", errors));
    }
         
    private static DataSource ds() throws SchemaVerificationException {
        return Conf.INSTANCE.dataSource().orElseThrow(() -> new SchemaVerificationException("Cannot obtain a datasource"));
    }
}
