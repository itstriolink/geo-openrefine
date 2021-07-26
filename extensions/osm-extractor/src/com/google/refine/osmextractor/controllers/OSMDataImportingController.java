package com.google.refine.osmextractor.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineServlet;
import com.google.refine.commands.HttpUtilities;
import com.google.refine.importing.ImportingController;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.model.*;
import com.google.refine.osmextractor.extractor.OSMData;
import com.google.refine.osmextractor.util.Util;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;
import de.topobyte.osm4j.core.access.OsmInputException;
import de.topobyte.osm4j.core.access.OsmReader;
import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.core.resolve.EntityFinder;
import de.topobyte.osm4j.core.resolve.EntityFinders;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;
import de.topobyte.osm4j.core.resolve.EntityNotFoundStrategy;
import de.topobyte.osm4j.geometry.NodeBuilder;
import de.topobyte.osm4j.xml.dynsax.OsmXmlReader;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

public class OSMDataImportingController implements ImportingController {
    private static final Logger logger = LoggerFactory.getLogger("OSMDataImportingController");
    protected RefineServlet servlet;
    protected String overpassInstance;
    protected String overpassQuery;
    private OSMData osmData;

    private static void createColumn(Project project, String newColumnName) {
        if (newColumnName != null && !newColumnName.trim().isEmpty()) {
            try {
                Column column = new Column(project.columnModel.allocateNewCellIndex(), newColumnName);
                project.columnModel.addColumn(
                        project.columnModel.columns.size(),
                        column,
                        false
                );
            } catch (ModelException e) {
                logger.error("Couldn't add column.", e);
            }
        }
    }

    @Override
    public void init(RefineServlet servlet) {
        this.servlet = servlet;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpUtilities.respond(response, "error", "GET not implemented");
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Properties parameters = ParsingUtilities.parseUrlParameters(request);
        response.setCharacterEncoding("UTF-8");
        String subCommand = parameters.getProperty("subCommand");

        if ("initialize-parser-ui".equals(subCommand)) {
            doInitializeParserUI(request, response, parameters);
        } else if ("parse-preview".equals(subCommand)) {
            doParsePreview(request, response, parameters);
        } else if ("create-project".equals(subCommand)) {
            doCreateProject(request, response, parameters);
        } else {
            HttpUtilities.respond(response, "error", "No such sub command implemented");
        }
    }

    private void doInitializeParserUI(
            HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {
        ObjectNode result = ParsingUtilities.mapper.createObjectNode();
        String overpassQuery = parameters.getProperty("overpassQuery");
        String overpassInstance = parameters.getProperty("overpassInstance");
        if (overpassInstance == null || overpassInstance.trim().isEmpty()
                || overpassQuery == null || overpassQuery.trim().isEmpty()) {
            HttpUtilities.respond(response, "error", "Missing Overpass query or Overpass API instance");
            return;
        }
        if (!Util.validateOverpassQuery(overpassQuery)) {
            HttpUtilities.respond(response, "error", "Invalid Overpass QL query");
            return;
        }

        this.overpassQuery = overpassQuery;
        this.overpassInstance = overpassInstance;

        JSONUtilities.safePut(result, "status", "ok");

        HttpUtilities.respond(response, result.toString());
    }

    private void doParsePreview(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {
        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "Import job doesn't exist.");
            return;
        }

        job.updating = true;

        try {
            osmData = new OSMData();

            job.prepareNewProject();
            job.setProgress(-1, "Parsing OpenStreetMap data...");
            ArrayNode tagsNode = ParsingUtilities.mapper.createArrayNode();
            ArrayNode geometryNode = ParsingUtilities.mapper.createArrayNode();
            ObjectNode statsNode = ParsingUtilities.mapper.createObjectNode();

            String encodedQuery = URLEncoder.encode(overpassQuery, "UTF-8");
            String query = overpassInstance + "?data=" + encodedQuery;

            InputStream input = new URL(query).openStream();
            OsmReader reader = new OsmXmlReader(input, false);
            InMemoryMapDataSet data = osmData.loadData(reader);
            input.close();

            EntityFinder wayFinder = EntityFinders.create(data,
                    EntityNotFoundStrategy.IGNORE);

            List<OsmNode> wayNodes = new ArrayList<>();
            Set<OsmWay> relationWays = new HashSet<>();

            Map<String, Integer> tagsMap = new HashMap<>();

            for (OsmRelation relation : data.getRelations().valueCollection()) {
                MultiPolygon area = osmData.getPolygon(relation);

                if (area != null && !area.isEmpty()) {
                    osmData.addPolygon(area, OsmModelUtil.getTagsAsMap(relation));
                }

                try {
                    wayFinder.findMemberWays(relation, relationWays);
                } catch (EntityNotFoundException e) {

                }

                if (relation.getNumberOfTags() > 0) {
                    for (int i = 0; i < relation.getNumberOfTags(); i++) {
                        OsmTag tag = relation.getTag(i);
                        if (!tagsMap.containsKey(tag.getKey())) {
                            tagsMap.put(tag.getKey(), 1);
                        } else {
                            tagsMap.put(tag.getKey(), tagsMap.get(tag.getKey()) + 1);
                        }
                    }
                }
            }

            wayFinder.findWayNodes(data.getWays().valueCollection(), wayNodes);
            for (OsmWay way : data.getWays().valueCollection()) {
                if (relationWays.contains(way)) {
                    continue;
                }

                MultiPolygon area = osmData.getPolygon(way);
                if (area != null && !area.isEmpty()) {
                    osmData.addPolygon(area, OsmModelUtil.getTagsAsMap(way));
                    continue;
                }

                Collection<LineString> paths = osmData.getLine(way);
                for (LineString path : paths) {
                    osmData.addLineString(path, OsmModelUtil.getTagsAsMap(way));
                }

                if (way.getNumberOfTags() > 0) {
                    for (int i = 0; i < way.getNumberOfTags(); i++) {
                        OsmTag tag = way.getTag(i);
                        if (!tagsMap.containsKey(tag.getKey())) {
                            tagsMap.put(tag.getKey(), 1);
                        } else {
                            tagsMap.put(tag.getKey(), tagsMap.get(tag.getKey()) + 1);
                        }
                    }
                }
            }

            for (OsmNode node : data.getNodes().valueCollection()) {
                if (!wayNodes.contains(node)) {
                    NodeBuilder nodeBuilder = new NodeBuilder();
                    osmData.addPoint(nodeBuilder.build(node), OsmModelUtil.getTagsAsMap(node));

                    for (int i = 0; i < node.getNumberOfTags(); i++) {
                        OsmTag tag = node.getTag(i);
                        if (!tagsMap.containsKey(tag.getKey())) {
                            tagsMap.put(tag.getKey(), 1);
                        } else {
                            tagsMap.put(tag.getKey(), tagsMap.get(tag.getKey()) + 1);
                        }
                    }
                }
            }

            List<String> tags = tagsMap.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getValue, Comparator.reverseOrder()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            for (String tagName : tags) {
                JSONUtilities.append(tagsNode, tagName);
            }

            statsNode.put("points", osmData.getPointsSize());
            statsNode.put("lines", osmData.getLineStringsSize());
            statsNode.put("polygons", osmData.getPolygonsSize());

            ObjectNode result = ParsingUtilities.mapper.createObjectNode();

            JSONUtilities.safePut(result, "status", "ok");
            JSONUtilities.safePut(result, "tags", tagsNode);
            JSONUtilities.safePut(result, "geometry", geometryNode);
            JSONUtilities.safePut(result, "stats", statsNode);

            JSONUtilities.append(geometryNode, "lat");
            JSONUtilities.append(geometryNode, "lon");
            JSONUtilities.append(geometryNode, "WKT");
            HttpUtilities.respondJSON(response, result);
        } catch (IOException | OsmInputException | EntityNotFoundException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            HttpUtilities.respondException(response, e);
        } finally {
            job.touch();
            job.updating = false;
        }
    }

    private void doCreateProject(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws IOException {

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        final ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "Import job doesn't exist.");
            return;
        }

        job.updating = true;
        final ObjectNode projectOptions = ParsingUtilities.evaluateJsonStringToObjectNode(
                request.getParameter("projectOptions"));
        final ArrayNode tags = ParsingUtilities.evaluateJsonStringToArrayNode(request.getParameter("tags"));
        final ArrayNode geometry = ParsingUtilities.evaluateJsonStringToArrayNode(request.getParameter("geometry"));
        final ObjectNode osmOptions = ParsingUtilities.evaluateJsonStringToObjectNode(request.getParameter("osmOptions"));

        boolean includePoints = osmOptions.get("points").asBoolean(false);
        String pointsOption = osmOptions.get("pointsOption").asText(null);
        String pointsSeparator = osmOptions.get("pointsSeparator").asText(", ");
        boolean includeLineStrings = osmOptions.get("lines").asBoolean(false);
        boolean includePolygons = osmOptions.get("polygons").asBoolean(false);
        boolean includeMultiPolygons = osmOptions.get("multiPolygons").asBoolean(false);

        job.setState("creating-project");

        final Project project = new Project();
        new Thread(() -> {
            ProjectMetadata pm = new ProjectMetadata();
            pm.setName(JSONUtilities.getString(projectOptions, "projectName", "Untitled"));
            pm.setEncoding(JSONUtilities.getString(projectOptions, "encoding", "UTF-8"));
            String latitudeColumnName = "";
            String longitudeColumnName = "";
            String WKTColumnName = "";

            Map<String, String> tagMappings = new HashMap<>();

            if (geometry != null && geometry.size() > 0) {
                for (JsonNode node : geometry) {
                    if (node instanceof ObjectNode) {
                        ObjectNode obj = (ObjectNode) node;
                        String newColumnName = obj.get("newColumnName").asText();
                        String coordinate = obj.get("coordinate").asText();
                        if (coordinate.equals("lat")) {
                            latitudeColumnName = newColumnName;
                        } else if (coordinate.equals("lon")) {
                            longitudeColumnName = newColumnName;
                        } else if (coordinate.equals("WKT")) {
                            WKTColumnName = newColumnName;
                        }

                        createColumn(project, newColumnName);
                    }
                }
            }
            if (tags != null && tags.size() > 0) {
                for (JsonNode node : tags) {
                    if (node instanceof ObjectNode) {
                        ObjectNode obj = (ObjectNode) node;
                        String osmTag = obj.get("osmTag").asText();
                        String newColumnName = obj.get("newColumnName").asText();
                        tagMappings.put(newColumnName, osmTag);

                        createColumn(project, newColumnName);
                    }
                }
            }

            Map<Point, Map<String, String>> points = osmData.getPoints();
            Map<LineString, Map<String, String>> lineStrings = osmData.getLineStrings();
            Map<MultiPolygon, Map<String, String>> polygons = osmData.getMultiPolygons();

            int includeItemsCount = 0;
            if (includePoints) {
                includeItemsCount++;
            }

            if (includeLineStrings) {
                includeItemsCount++;
            }

            if (includePolygons) {
                includeItemsCount++;
            }

            if (points != null && points.size() > 0 && includePoints) {
                int index = 0;
                for (Map.Entry<Point, Map<String, String>> entry : points.entrySet()) {
                    Point point = entry.getKey();
                    Row row = new Row(project.columnModel.getMaxCellIndex());
                    Map<String, String> currentTags = entry.getValue();

                    double latitude = point.getX();
                    double longitude = point.getY();

                    for (Column column : project.columnModel.columns) {
                        String columnName = column.getName();
                        String originalTagName = tagMappings.getOrDefault(columnName, null);

                        String value;

                        if (columnName.equals(latitudeColumnName)) {
                            value = String.valueOf(latitude);
                        } else if (columnName.equals(longitudeColumnName)) {
                            value = String.valueOf(longitude);
                        } else if (columnName.equals(WKTColumnName)) {
                            value = osmData.getWKTRepresentation(point);
                        } else if (originalTagName != null && currentTags.get(originalTagName) != null) {
                            value = currentTags.get(originalTagName);
                        } else {
                            value = null;
                        }

                        row.setCell(column.getCellIndex(), new Cell(value, null));
                    }

                    project.rows.add(row);
                    job.setProgress(index * 100 / points.size(),
                            "Parsed " + index + "/" + points.size() + " OpenStreetMap points.");
                    index++;
                }
            }

            if (lineStrings != null && lineStrings.size() > 0 && includeLineStrings) {
                int index = 0;
                for (Map.Entry<LineString, Map<String, String>> entry : lineStrings.entrySet()) {
                    LineString lineString = entry.getKey();
                    Row row = new Row(project.columnModel.getMaxCellIndex());
                    Map<String, String> currentTags = entry.getValue();

                    for (Column column : project.columnModel.columns) {
                        String columnName = column.getName();
                        String originalTagName = tagMappings.getOrDefault(columnName, null);

                        String value;

                        if (columnName.equals(WKTColumnName)) {
                            value = osmData.getWKTRepresentation(lineString);
                        } else if (originalTagName != null && currentTags.get(originalTagName) != null) {
                            value = currentTags.get(originalTagName);
                        } else {
                            value = null;
                        }

                        row.setCell(column.getCellIndex(), new Cell(value, null));
                    }

                    project.rows.add(row);

                    job.setProgress(index * 100 / lineStrings.size() / includeItemsCount,
                            "Parsed " + index + "/" + lineStrings.size() + " OpenStreetMap lines.");
                    index++;
                }
            }

            if (polygons != null && polygons.size() > 0 && includePolygons) {
                int index = 0;
                for (Map.Entry<MultiPolygon, Map<String, String>> entry : polygons.entrySet()) {
                    MultiPolygon polygon = entry.getKey();
                    Row row = new Row(project.columnModel.getMaxCellIndex());
                    Map<String, String> currentTags = entry.getValue();

                    for (Column column : project.columnModel.columns) {
                        String columnName = column.getName();
                        String originalTagName = tagMappings.getOrDefault(columnName, null);

                        String value;

                        if (columnName.equals(WKTColumnName)) {
                            value = osmData.getWKTRepresentation(polygon);
                        } else if (originalTagName != null && currentTags.get(originalTagName) != null) {
                            value = currentTags.get(originalTagName);
                        } else {
                            value = null;
                        }

                        row.setCell(column.getCellIndex(), new Cell(value, null));
                    }

                    project.rows.add(row);

                    job.setProgress(index * 100 / polygons.size(),
                            "Parsed " + index + "/" + polygons.size() + " OpenStreetMap polygons.");
                    index++;
                }
            }

            job.setProgress(100, "Finished parsing OSM elements.");

            if (!job.canceled) {
                project.update();

                ProjectManager.singleton.registerProject(project, pm);

                job.setState("created-project");
                job.setProjectID(project.id);

                job.touch();
                job.updating = false;
            }
        }).start();

        HttpUtilities.respond(response, "ok", "done");
    }
}

