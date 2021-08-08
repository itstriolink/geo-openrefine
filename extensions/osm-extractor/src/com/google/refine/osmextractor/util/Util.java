package com.google.refine.osmextractor.util;

public class Util {
    public static boolean isValidOverpassQuery(String query) {
        return !query.contains("[out:json]");
    }
}
