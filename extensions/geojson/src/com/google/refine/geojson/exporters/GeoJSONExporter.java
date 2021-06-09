package com.google.refine.geojson.exporters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.refine.browsing.Engine;
import com.google.refine.exporters.CustomizableTabularExporterUtilities;
import com.google.refine.exporters.TabularSerializer;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.geojson.util.Constants;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;
import org.apache.jena.atlas.json.JSON;
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

    @Override
    public String getContentType() {
        return "application/geo+json";
    }

    @Override
    public void export(final Project project, Properties params, Engine engine, Writer writer) throws IOException {
        FeatureCollection featureCollection = new FeatureCollection();
        ArrayList<Feature> features = new ArrayList<>();
        List<String> propertyColumns = new ArrayList<>();
        TabularSerializer serializer = new TabularSerializer() {
            @Override
            public void startFile(JsonNode options) {
                List<JsonNode> array = JSONUtilities.getArray(options, "propertyColumns");
                for (int i = 0; i < array.size(); i++) {
                    JsonNode columnOptions = array.get(i);
                    if (columnOptions != null) {
                        String name = JSONUtilities.getString(columnOptions, "name", null);
                        if (name != null) {
                            propertyColumns.add(name);
                        }
                    }
                }
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
                    if (cellData != null && cellData.text != null && cellData.columnName != null) {
                        if (cellData.text.matches(Constants.RegEx.latitudeValue)
                                && cellData.columnName.matches(Constants.RegEx.latitudeName)
                                && latitude == 0) {
                            latitude = Double.parseDouble(cellData.text);
                            latitude = Math.round(latitude * Constants.latLonFactor) / Constants.latLonFactor;
                        } else if (cellData.text.matches(Constants.RegEx.longitudeValue)
                                && cellData.columnName.matches(Constants.RegEx.longitudeName)
                                && longitude == 0) {
                            longitude = Double.parseDouble(cellData.text);
                            longitude = Math.round(longitude * Constants.latLonFactor) / Constants.latLonFactor;
                        } else if (propertyColumns.contains(cellData.columnName)){
                            feature.setProperty(cellData.columnName, cellData.text);
                        }
                    }
                }

                if (latitude > 0 && longitude > 0) {
                    feature.setGeometry(new Point(longitude, latitude));
                    features.add(feature);
                }
            }
        };

        CustomizableTabularExporterUtilities.exportRows(project, engine, params, serializer);
    }
}
