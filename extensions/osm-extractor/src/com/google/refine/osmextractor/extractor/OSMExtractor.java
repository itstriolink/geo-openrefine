package com.google.refine.osmextractor.extractor;

import de.topobyte.osm4j.core.access.OsmInputException;
import de.topobyte.osm4j.core.access.OsmReader;
import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.dataset.MapDataSetLoader;
import de.topobyte.osm4j.core.model.iface.OsmMetadata;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;
import de.topobyte.osm4j.geometry.RegionBuilder;
import de.topobyte.osm4j.geometry.RegionBuilderResult;
import de.topobyte.osm4j.geometry.WayBuilder;
import de.topobyte.osm4j.geometry.WayBuilderResult;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OSMExtractor {
    private final List<OSMElement> points;
    private final List<OSMElement> lineStrings;
    private final List<OSMElement> multiLineStrings;
    private final List<OSMElement> multiPolygons;
    private final GeometryFactory geometryFactory;
    private final WayBuilder wayBuilder;
    private final RegionBuilder regionBuilder;
    private final WKTWriter wktWriter;
    private String overpassInstance;
    private String overpassQuery;
    private boolean includeMetadata;
    private boolean isCenter;
    private InMemoryMapDataSet data;


    public OSMExtractor() {
        this.points = new ArrayList<>();
        this.lineStrings = new ArrayList<>();
        this.multiLineStrings = new ArrayList<>();
        this.multiPolygons = new ArrayList<>();

        isCenter = false;
        includeMetadata = false;

        this.geometryFactory = new GeometryFactory();
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

    public boolean getIncludeMetadata() {
        return includeMetadata;
    }

    public void setIncludeMetadata(boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }

    public boolean getIsCenter() {
        return isCenter;
    }

    public void setIsCenter(boolean isCenter) {
        this.isCenter = isCenter;
    }

    public String getOverpassQuery() {
        return overpassQuery;
    }

    public void setOverpassQuery(String overpassQuery) {
        this.overpassQuery = overpassQuery;
    }

    public List<OSMElement> getPoints() {
        return points;
    }


    public List<OSMElement> getLineStrings() {
        return lineStrings;
    }

    public List<OSMElement> getMultiLineStrings() {
        return lineStrings;
    }

    public List<OSMElement> getMultiPolygons() {
        return multiPolygons;
    }

    public void addPoint(Point point, long id, Map<String, String> tags, OsmMetadata metadata) {
        this.points.add(new OSMElement(point, id, tags, metadata));
    }

    public void addLineString(LineString lineString, long id, Map<String, String> tags, OsmMetadata metadata) {
        this.lineStrings.add(new OSMElement(lineString, id, tags, metadata));
    }

    public void addMultiLineString(MultiLineString multiLineString, long id, Map<String, String> tags, OsmMetadata metadata) {
        this.lineStrings.add(new OSMElement(multiLineString, id, tags, metadata));
    }

    public void addMultiPolygon(MultiPolygon multiPolygon, long id, Map<String, String> tags, OsmMetadata metadata) {
        this.multiPolygons.add(new OSMElement(multiPolygon, id, tags, metadata));
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
        return this.lineStrings.size();
    }

    public int getMultiPolygonsSize() {
        return this.multiPolygons.size();
    }

    public Point buildPoint(double lat, double lon) {
        if (lat > 0.0d && lon > 0.0d) {
            Coordinate coordinate = new Coordinate(lat, lon);
            return geometryFactory.createPoint(coordinate);
        } else {
            return null;
        }
    }

    public MultiLineString buildMultiLineString(LineString[] lineStrings) {
        return geometryFactory.createMultiLineString(lineStrings);
    }

    public String getWKTRepresentation(Geometry g) {
        return wktWriter.write(g);
    }

    public Collection<LineString> getLine(OsmWay way) {
        List<LineString> results = new ArrayList<>();
        try {
            WayBuilderResult lines = wayBuilder.build(way, data);
            results.addAll(lines.getLineStrings());
            if (lines.getLinearRing() != null && !lines.getLinearRing().isEmpty()) {
                results.add(lines.getLinearRing());
            }
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
