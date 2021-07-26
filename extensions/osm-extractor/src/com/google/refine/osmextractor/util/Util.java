package com.google.refine.osmextractor.util;

public class Util {
    public static boolean validateOverpassQuery(String query) {
        return query != null && !query.trim().isEmpty()
                && !query.contains("[out:json]");
    }
}
