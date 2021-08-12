package com.google.refine.osmextractor.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class OSMTags {
    public static final Set<String> MAIN_TAGS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("aerialway", "aeroway", "amenity", "barrier",
            "boundary", "building", "craft", "emergency", "ford", "geological", "highway", "historic", "landuse", "leisure", "man_made", "military", "natural",
            "office", "place", "power", "public_transport", "railway", "shop", "tourism", "waterway", "type", "entrance", "pipeline", "healthcare",
            "playground", "attraction", "traffic_sign", "traffic_sign:forward", "traffic_sign:backward", "golf", "indoor", "cemetry", "building:part",
            "landcover", "advertising", "traffic_calming", "club", "cemetery", "police", "telecom")));
}
