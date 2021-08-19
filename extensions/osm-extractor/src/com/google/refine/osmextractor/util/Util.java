package com.google.refine.osmextractor.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    public static boolean isValidOverpassQuery(String query) {
        return !query.contains("[out:json]");
    }

    public static boolean overpassQueryContainsMetadata(String query) {
        Pattern pattern = Pattern.compile(Constants.Importing.COMMENTED_METADATA_REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(query);
        boolean matchFound = matcher.find();

        return !matchFound && query.contains("out meta");

    }

    public static boolean overpassQueryContainsOutCenter(String query) {
        Pattern pattern = Pattern.compile(Constants.Importing.COMMENTED_CENTER_REGEX, Pattern.CASE_INSENSITIVE);
        Pattern pattern2 = Pattern.compile(Constants.Importing.CONTAINS_CENTER_REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(query);
        Matcher matcher2 = pattern2.matcher(query);
        boolean matchFound = matcher.find();
        boolean matchFound2 = matcher2.find();

        return !matchFound && matchFound2;
    }
}
