package com.hadzhy.jdbclight.sql;

public class Util {

    private Util() {}

    static void deleteSurplusComa(StringBuilder query) {
        if (query.charAt(query.length() - 1) == ',') query.deleteCharAt(query.length() - 1);
        if (query.charAt(query.length() - 2) == ',') query.deleteCharAt(query.length() - 2);
    }
}
