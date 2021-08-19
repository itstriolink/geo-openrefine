package com.google.refine.osmextractor.extractor;

import de.topobyte.osm4j.core.model.iface.OsmMetadata;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

public class OSMElement {

    private Geometry geometry;
    private long id;
    private Map<String, String> tags;
    private OsmMetadata metadata;

    public OSMElement(Geometry geometry, long id, Map<String, String> tags, OsmMetadata metadata) {
        this.geometry = geometry;
        this.id = id;
        this.tags = tags;
        this.metadata = metadata;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public long getId() {
        return id;
    }

    public OsmMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(OsmMetadata metadata) {
        this.metadata = metadata;
    }

    public Geometry getGeometry() {
        return geometry;
    }
}
