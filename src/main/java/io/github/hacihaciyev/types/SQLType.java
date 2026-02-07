package io.github.hacihaciyev.types;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum SQLType {
    NULL,

    TINYINT,
    SMALLINT,
    INT,
    INTEGER,
    BIGINT,

    DECIMAL,
    NUMERIC,

    FLOAT,
    REAL,
    DOUBLE,
    DOUBLE_PRECISION,

    MONEY,
    SMALLMONEY,

    BOOLEAN,
    BIT,

    CHAR,
    CHARACTER,
    NCHAR,

    VARCHAR,
    CHARACTER_VARYING,
    NATIONAL_CHAR,
    NVARCHAR,
    NATIONAL_CHAR_VARYING,

    TEXT,
    CLOB,

    BINARY,
    VARBINARY,
    BINARY_VARYING,
    BLOB,

    DATE,
    TIME,
    TIMESTAMP,
    TIMESTAMP_WITH_TIME_ZONE,
    TIMESTAMP_WITHOUT_TIME_ZONE,

    DATETIME,
    DATETIME2,
    DATETIMEOFFSET,
    SMALLDATETIME,

    INTERVAL,
    YEAR,

    UUID,
    UNIQUEIDENTIFIER,

    XML,
    JSON,
    JSONB,
    JSON_ELEMENT,
    GEOMETRY,
    GEOGRAPHY,
    HIERARCHYID,
    ROWVERSION,
    SQL_VARIANT,
    CURSOR,
    TABLE_TYPE,

    ARRAY,
    LIST,
    MULTISET,
    SET,

    VARIANT,
    OBJECT,
    ANY,

    ENUM,
    SET_TYPE;

    private static final Map<String, SQLType> LOOKUP = Arrays.stream(SQLType.values())
            .collect(Collectors.toMap(
                    type -> type.name(),
                    type -> type
            ));

    public static Optional<SQLType> parse(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(LOOKUP.get(normalize(name)));
    }

    public static boolean contains(String name) {
        return name != null && LOOKUP.containsKey(normalize(name));
    }

    private static String normalize(String name) {
        return name.trim()
                .toUpperCase()
                .replace(" ", "_");
    }
}