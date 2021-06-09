package com.google.refine.geojson.util;

public final class Constants {
    public static class RegEx {
        public static final String latitudeName = "^lat.*$";
        public static final String longitudeName = "^lon.*$";
        public static final String latitudeValue = "^(\\+|-)?(?:90(?:(?:\\.0{1,12})?)|(?:[0-9]|[1-8][0-9])(?:(?:\\.[0-9]{1,12})?))$";
        public static final String longitudeValue = "^(\\+|-)?(?:180(?:(?:\\.0{1,12})?)|(?:[0-9]|[1-9][0-9]|1[0-7][0-9])(?:(?:\\.[0-9]{1,12})?))$";
    }

    public static final double latLonFactor = 1e7;
}
