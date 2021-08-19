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
        public static final String LATITUDE_COLUMN_NAME = "latitude";
        public static final String LONGITUDE_COLUMN_NAME = "longitude";
        public static final String POINT_DELIMITED_COLUMN_NAME = "point_delimited";
        public static final String WKT_COLUMN_NAME = "WKT";
        public static final String GENERATED_COLUMN_DESCRIPTION = "Column generated automatically by the OSM importer";

        public static final String COMMENTED_METADATA_REGEX = "(\\/\\/)(.?)(out meta)";
        public static final String COMMENTED_CENTER_REGEX = "(\\/\\/)(.?)(out)(.*)(center)";
        public static final String CONTAINS_CENTER_REGEX = "(out)(.*)(center)";
    }

    public static final List<String> METADATA_TAGS = Collections.unmodifiableList(
            new ArrayList<String>() {{
                add("@uid");
                add("@version");
                add("@timestamp");
                add("@changeset");
                add("@user");
                add("@visible");
            }}
    );
}



