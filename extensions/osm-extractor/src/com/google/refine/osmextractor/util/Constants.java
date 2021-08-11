package com.google.refine.osmextractor.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Constants {
    public static final List<String> OVERPASS_INSTANCES = Collections.unmodifiableList(
            new ArrayList<String>() {{
                add("https://lz4.overpass-api.de/api/interpreter");
                add("https://z.overpass-api.de/api/interpreter");
                add("https://overpass.openstreetmap.ru/api/interpreter");
                add("https://overpass.openstreetmap.fr/api/interpreter");
                add("https://overpass.osm.ch/api/interpreter");
                add("https://overpass.kumi.systems/api/interpreter");
                add("https://overpass.nchc.org.tw/api/interpreter");
            }});

    public class Importing {
        public static final String latitudeColumnName = "latitude";
        public static final String longitudeColumnName = "longitude";
        public static final String pointDelimitedColumnName = "point_delimited";
        public static final String wktColumnName = "WKT";
        public static final String generatedColumnDescription = "Column generated automatically by the OSM importer";
    }
}



