package com.google.refine.osmextractor.extractor;

import de.topobyte.osm4j.core.access.OsmInputException;
import de.topobyte.osm4j.core.access.OsmReader;
import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.dataset.MapDataSetLoader;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;
import de.topobyte.osm4j.geometry.*;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTWriter;

import java.io.IOException;
import java.util.*;

public class OSMExtractor {
    private final Map<Point, Map<String, String>> points;
    private final Map<LineString, Map<String, String>> lineStrings;
    private final Map<MultiLineString, Map<String, String>> multiLineStrings;
    private final Map<MultiPolygon, Map<String, String>> multiPolygons;
    private final GeometryBuilder geometryBuilder;
    private final WayBuilder wayBuilder;
    private final RegionBuilder regionBuilder;
    private final WKTWriter wktWriter;
    private String overpassInstance;
    private String overpassQuery;
    private InMemoryMapDataSet data;


    public OSMExtractor() {
        this.points = new HashMap<>();
        this.lineStrings = new HashMap<>();
        this.multiLineStrings = new HashMap<>();
        this.multiPolygons = new HashMap<>();

        this.geometryBuilder = new GeometryBuilder();
        this.wayBuilder = new WayBuilder();
        this.regionBuilder = new RegionBuilder();
        this.wktWriter = new WKTWriter();
    }

    public String getOverpassInstance() {
        return overpassInstance;
    }

    public void setOverpassInstance(String overpassInstance) {
        this.overpassInstance = overpassInstance;
    }

    public String getOverpassQuery() {
        return overpassQuery;
    }

    public void setOverpassQuery(String overpassQuery) {
        this.overpassQuery = overpassQuery;
    }

    public Map<Point, Map<String, String>> getPoints() {
        return points;
    }


    public Map<LineString, Map<String, String>> getLineStrings() {
        return lineStrings;
    }

    public Map<MultiLineString, Map<String, String>> getMultiLineStrings() {
        return multiLineStrings;
    }


    public Map<MultiPolygon, Map<String, String>> getMultiPolygons() {
        return multiPolygons;
    }

    public void addPoint(Point point, Map<String, String> tags) {
        this.points.put(point, tags);
    }

    public void addLineString(LineString lineString, Map<String, String> tags) {
        this.lineStrings.put(lineString, tags);
    }

    public void addMultiLineString(MultiLineString multiLineString, Map<String, String> tags) {
        this.multiLineStrings.put(multiLineString, tags);
    }

    public void addMultiPolygon(MultiPolygon polygon, Map<String, String> tags) {
        this.multiPolygons.put(polygon, tags);
    }

    public InMemoryMapDataSet loadData(OsmReader reader) throws IOException, OsmInputException {
        return this.data = MapDataSetLoader.read(reader, true, true, true);
    }

    public int getPointsSize() {
        return this.points.size();
    }

    public int getLineStringsSize() {
        return this.lineStrings.size();
    }

    public int getMultiLineStringsSize() {
        return this.multiLineStrings.size();
    }

    public int getMultiPolygonsSize() {
        return this.multiPolygons.size();
    }

    public String getWKTRepresentation(Geometry g) {
        return wktWriter.write(g);
    }

    public Collection<LineString> getLine(OsmWay way) {
        List<LineString> results = new ArrayList<>();
        try {
            WayBuilderResult lines = wayBuilder.build(way, data);
            results.addAll(lines.getLineStrings());
        } catch (EntityNotFoundException e) {
            // ignore
        }
        return results;
    }

    public List<LineString> getLines(OsmRelation relation) {
        List<LineString> results = new ArrayList<>();

        try {
            RegionBuilderResult lines = regionBuilder.build(relation, data);
            results.addAll(lines.getLineStrings());
        } catch (EntityNotFoundException e) {
            // ignore
        }
        return results;
    }


    public MultiPolygon getPolygon(OsmWay way) {
        try {
            RegionBuilderResult region = regionBuilder.build(way, data);
            return region.getMultiPolygon();
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    public MultiPolygon getPolygon(OsmRelation relation) {
        try {
            RegionBuilderResult region = regionBuilder.build(relation, data);
            return region.getMultiPolygon();
        } catch (EntityNotFoundException e) {
            return null;
        }
    }
}
