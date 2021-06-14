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
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class GeoJSONExporter implements WriterExporter {
    final static Logger logger = LoggerFactory.getLogger("GeoJSONExporter");

    FeatureCollection featureCollection;
    ArrayList<Feature> features;
    List<String> propertyColumns;
    String latitudeColumn = "";
    String longitudeColumn = "";
    boolean includeEmptyCoordinates = false;

    @Override
    public String getContentType() {
        return "application/geo+json";
    }

    @Override
    public void export(final Project project, Properties params, Engine engine, Writer writer) {
        featureCollection = new FeatureCollection();
        features = new ArrayList<>();
        propertyColumns = new ArrayList<>();

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
                includeEmptyCoordinates = JSONUtilities.getBoolean(options, "outputBlankRows", false);
            }

            @Override
            public void endFile() {
                try {
                    featureCollection.setFeatures(features);
                    writer.write(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(featureCollection));
                } catch (IOException e) {
                    logger.error("GeoJSONExporter::Export::Exception::{}", e);
                }
            }

            @Override
            public void addRow(List<CellData> cells, boolean isHeader) {
                double latitude = 0.0d;
                double longitude = 0.0d;
                Feature feature = new Feature();
                for (CellData cellData : cells) {
                    if (includeEmptyCoordinates || (cellData != null && cellData.text != null && cellData.columnName != null)) {
                        if (cellData != null && cellData.columnName.equals(latitudeColumn)) {
                            try {
                                latitude = Double.parseDouble(cellData.text);
                                latitude = Math.round(latitude * Constants.latLonFactor) / Constants.latLonFactor;
                            } catch (NumberFormatException nfe) {
                                logger.error("The '" +cellData.text+ "' value on the '" +cellData.columnName+ "' column could not be parsed to a latitude coordinate.");
                            }
                        } else if (cellData != null && cellData.columnName.equals(longitudeColumn)) {
                            try {
                                longitude = Double.parseDouble(cellData.text);
                                longitude = Math.round(longitude * Constants.latLonFactor) / Constants.latLonFactor;
                            } catch (NumberFormatException nfe) {
                                logger.error("The '" +cellData.text+ "' value on the '" +cellData.columnName+ "' column could not be parsed to a longitude coordinate.");
                            }
                        } else if (cellData != null && propertyColumns.contains(cellData.columnName)) {
                            feature.setProperty(cellData.columnName, cellData.text);
                        }
                    }
                }

                if (latitude == 0 && longitude == 0 && includeEmptyCoordinates) {
                    feature.setGeometry(new Point());
                    features.add(feature);
                } else if (latitude > 0 && longitude > 0) {
                    feature.setGeometry(new Point(longitude, latitude));
                    features.add(feature);
                }
            }
        };

        CustomizableTabularExporterUtilities.exportRows(project, engine, params, serializer);
    }
}
