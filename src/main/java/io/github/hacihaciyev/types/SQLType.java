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

    public static Optional<SQLType> fromJDBCType(int jdbcType) {
        return parseJDBCType(jdbcType);
    }
    
    public static boolean contains(String name) {
        return name != null && LOOKUP.containsKey(normalize(name));
    }

    private static String normalize(String name) {
        return name.trim()
                .toUpperCase()
                .replace(" ", "_");
    }
    
    private static Optional<SQLType> parseJDBCType(int jdbcType) {
        return Optional.ofNullable(switch (jdbcType) {
            case java.sql.Types.NULL                    -> NULL;
            case java.sql.Types.TINYINT                 -> TINYINT;
            case java.sql.Types.SMALLINT                -> SMALLINT;
            case java.sql.Types.INTEGER                 -> INTEGER;
            case java.sql.Types.BIGINT                  -> BIGINT;
            case java.sql.Types.DECIMAL                 -> DECIMAL;
            case java.sql.Types.NUMERIC                 -> NUMERIC;
            case java.sql.Types.FLOAT                   -> FLOAT;
            case java.sql.Types.REAL                    -> REAL;
            case java.sql.Types.DOUBLE                  -> DOUBLE;
            case java.sql.Types.BOOLEAN                 -> BOOLEAN;
            case java.sql.Types.BIT                     -> BIT;
            case java.sql.Types.CHAR                    -> CHAR;
            case java.sql.Types.NCHAR                   -> NCHAR;
            case java.sql.Types.VARCHAR                 -> VARCHAR;
            case java.sql.Types.NVARCHAR                -> NVARCHAR;
            case java.sql.Types.LONGVARCHAR             -> TEXT;
            case java.sql.Types.LONGNVARCHAR            -> TEXT;
            case java.sql.Types.CLOB                    -> CLOB;
            case java.sql.Types.NCLOB                   -> CLOB;
            case java.sql.Types.BINARY                  -> BINARY;
            case java.sql.Types.VARBINARY               -> VARBINARY;
            case java.sql.Types.LONGVARBINARY           -> BLOB;
            case java.sql.Types.BLOB                    -> BLOB;
            case java.sql.Types.DATE                    -> DATE;
            case java.sql.Types.TIME                    -> TIME;
            case java.sql.Types.TIME_WITH_TIMEZONE      -> TIME;
            case java.sql.Types.TIMESTAMP               -> TIMESTAMP;
            case java.sql.Types.TIMESTAMP_WITH_TIMEZONE -> TIMESTAMP_WITH_TIME_ZONE;
            case java.sql.Types.SQLXML                  -> XML;
            case java.sql.Types.ARRAY                   -> ARRAY;
            case java.sql.Types.JAVA_OBJECT             -> OBJECT;
            case java.sql.Types.REF_CURSOR              -> CURSOR;
            case java.sql.Types.OTHER                   -> ANY;
            default                                     -> null;
        });
    }
}