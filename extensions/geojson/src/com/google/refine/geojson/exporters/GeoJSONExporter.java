package com.google.refine.geojson.exporters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.refine.browsing.Engine;
import com.google.refine.exporters.CustomizableTabularExporterUtilities;
import com.google.refine.exporters.TabularSerializer;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.geojson.util.Constants;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Point;
import org.wololo.jts2geojson.GeoJSONWriter;

import java.io.IOException;
import java.io.Writer;
import java.util.*;


public class GeoJSONExporter implements WriterExporter {
    final static Logger logger = LoggerFactory.getLogger("GeoJSONExporter");
    private final WKTReader wktReader;
    private final double[] emptyLatLon = new double[]{0.0, 0.0};
    GeoJSONWriter geoJSONWriter;
    ArrayList<Feature> features;
    FeatureCollection featureCollection;
    List<String> propertyColumns;
    String latitudeColumn = "";
    String longitudeColumn = "";
    String wktColumn = "";

    public GeoJSONExporter() {
        wktReader = new WKTReader();
        geoJSONWriter = new GeoJSONWriter();
    }

    @Override
    public String getContentType() {
        return "application/geo+json";
    }

    @Override
    public void export(final Project project, Properties params, Engine engine, Writer writer) {
        propertyColumns = new ArrayList<>();
        features = new ArrayList<>();

        TabularSerializer serializer = new TabularSerializer() {
            @Override
            public void startFile(JsonNode options) {
                List<JsonNode> array = JSONUtilities.getArray(options, "propertyColumns");
                for (JsonNode columnOptions : array) {
                    if (columnOptions != null) {
                        String name = JSONUtilities.getString(columnOptions, "name", null);
                        if (name != null) {
                            propertyColumns.add(name);
                        }
                    }
                }

                latitudeColumn = JSONUtilities.getString(options, "latitudeColumn", null);
                longitudeColumn = JSONUtilities.getString(options, "longitudeColumn", null);
                wktColumn = JSONUtilities.getString(options, "wktColumn", null);
            }

            @Override
            public void endFile() {
                FeatureCollection featureCollection = geoJSONWriter.write(features);
                try {
                    writer.write(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(featureCollection));
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error("GeoJSONExporter::Export::Exception::{}", e);
                }
            }

            @Override
            public void addRow(List<CellData> cells, boolean isHeader) {
                double latitude = 0.0d;
                double longitude = 0.0d;
                Geometry jtsGeometry = null;
                Map<String, Object> properties = new HashMap<String, Object>();
                for (CellData cellData : cells) {
                    if (cellData != null && cellData.text != null && cellData.columnName != null) {
                        if (cellData.columnName.equals(latitudeColumn)) {
                            try {
                                latitude = Double.parseDouble(cellData.text);
                                latitude = Math.round(latitude * Constants.latLonFactor) / Constants.latLonFactor;
                            } catch (NumberFormatException nfe) {
                                logger.error("The '" + cellData.text + "' value on the '" + cellData.columnName + "' column could not be parsed to a latitude coordinate.");
                            }
                        } else if (cellData.columnName.equals(longitudeColumn)) {
                            try {
                                longitude = Double.parseDouble(cellData.text);
                                longitude = Math.round(longitude * Constants.latLonFactor) / Constants.latLonFactor;
                            } catch (NumberFormatException nfe) {
                                logger.error("The '" + cellData.text + "' value on the '" + cellData.columnName + "' column could not be parsed to a longitude coordinate.");
                            }
                        } else if (cellData != null && cellData.columnName.equals(wktColumn)) {
                            try {
                                jtsGeometry = wktReader.read(cellData.text);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        } else if (cellData != null && propertyColumns.contains(cellData.columnName)) {
                            properties.put(cellData.columnName, cellData.text);
                        } else {

                        }
                    }
                }

                if (latitude > 0 && longitude > 0) {
                    Feature feature = new Feature(new Point(new double[]{latitude, longitude}), properties);
                    features.add(feature);
                } else if (jtsGeometry != null) {
                    org.wololo.geojson.Geometry geometry = geoJSONWriter.write(jtsGeometry);
                    features.add(new Feature(geometry, properties));
                }
            }
        };

        CustomizableTabularExporterUtilities.exportRows(project, engine, params, serializer);
    }
}
