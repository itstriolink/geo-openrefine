package com.google.refine.osmextractor.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Constants {
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
}

