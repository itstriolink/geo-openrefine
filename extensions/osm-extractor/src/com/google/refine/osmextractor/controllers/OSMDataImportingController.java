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
import com.google.refine.osmextractor.extractor.OSMExtractor;
import com.google.refine.osmextractor.util.Constants;
import com.google.refine.osmextractor.util.OSMTags;
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
import java.util.stream.Stream;

public class OSMDataImportingController implements ImportingController {
    private static final Logger logger = LoggerFactory.getLogger("OSMDataImportingController");
    protected RefineServlet servlet;
    private OSMExtractor osmExtractor;

    @Override
    public void init(RefineServlet servlet) {
        osmExtractor = new OSMExtractor();
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
            HttpUtilities.respond(response, "error", "No such sub command implepmented");
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
        if (!Util.isValidOverpassQuery(overpassQuery)) {
            HttpUtilities.respond(response, "error", " \"[out:json]\" command is not allowed in the Overpass QL query.");
            return;
        }

        osmExtractor.setOverpassQuery(overpassQuery);
        osmExtractor.setOverpassInstance(overpassInstance);

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
            job.prepareNewProject();
            job.setProgress(-1, "Parsing OpenStreetMap data...");
            ArrayNode tagsNode = ParsingUtilities.mapper.createArrayNode();
            ObjectNode statsNode = ParsingUtilities.mapper.createObjectNode();

            String encodedQuery = URLEncoder.encode(osmExtractor.getOverpassQuery(), "UTF-8");
            String query = osmExtractor.getOverpassInstance() + "?data=" + encodedQuery;

            InputStream input = new URL(query).openStream();
            OsmReader reader = new OsmXmlReader(input, false);
            InMemoryMapDataSet data = osmExtractor.loadData(reader);
            input.close();

            EntityFinder wayFinder = EntityFinders.create(data,
                    EntityNotFoundStrategy.IGNORE);

            List<OsmNode> wayNodes = new ArrayList<>();
            Set<OsmWay> relationWays = new HashSet<>();

            Map<String, Integer> tagsMap = new HashMap<>();

            for (OsmRelation relation : data.getRelations().valueCollection()) {
                MultiPolygon area = osmExtractor.getPolygon(relation);

                if (area != null && !area.isEmpty()) {
                    osmExtractor.addPolygon(area, OsmModelUtil.getTagsAsMap(relation));
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
                boolean found = false;
                if (relationWays.contains(way)) {
                    continue;
                }

                MultiPolygon area = osmExtractor.getPolygon(way);
                if (area != null && !area.isEmpty()) {
                    osmExtractor.addPolygon(area, OsmModelUtil.getTagsAsMap(way));
                    found = true;
                }

                Collection<LineString> paths = osmExtractor.getLine(way);

                if (!found) {
                    for (LineString path : paths) {
                        osmExtractor.addLineString(path, OsmModelUtil.getTagsAsMap(way));
                    }
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
                    osmExtractor.addPoint(nodeBuilder.build(node), OsmModelUtil.getTagsAsMap(node));

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

            Map<String, Integer> mainTagsMap = new HashMap<>();
            Map<String, Integer> otherTagsMap = new HashMap<>();

            for(Map.Entry<String, Integer> tag: tagsMap.entrySet()) {
                if(OSMTags.MAIN_TAGS.contains(tag.getKey())) {
                    mainTagsMap.put(tag.getKey(), tag.getValue());
                } else {
                    otherTagsMap.put(tag.getKey(), tag.getValue());
                }
            }

            List<String> mainTags = mainTagsMap.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getValue, Comparator.reverseOrder()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            List<String> otherTags = otherTagsMap.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getValue, Comparator.reverseOrder()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            List<String> finalTags = Stream.concat(mainTags.stream(), otherTags.stream())
                    .collect(Collectors.toList());

            for (String tagName : finalTags) {
                JSONUtilities.append(tagsNode, tagName);
            }

            statsNode.put("points", osmExtractor.getPointsSize());
            statsNode.put("lines", osmExtractor.getLineStringsSize());
            statsNode.put("polygons", osmExtractor.getPolygonsSize());

            ObjectNode result = ParsingUtilities.mapper.createObjectNode();

            JSONUtilities.safePut(result, "status", "ok");
            JSONUtilities.safePut(result, "tags", tagsNode);
            JSONUtilities.safePut(result, "stats", statsNode);

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
        final ObjectNode importOptions = ParsingUtilities.evaluateJsonStringToObjectNode(request.getParameter("importOptions"));

        boolean includePoints = importOptions.get("points").asBoolean(false);
        boolean pointsAsLatLon = importOptions.get("pointsAsLatLon").asBoolean(false);
        boolean pointsDelimited = importOptions.get("pointsDelimited").asBoolean(false);
        String pointsSeparator = importOptions.get("pointsSeparator").asText(", ");
        boolean pointsAsWKT = importOptions.get("pointsAsWKT").asBoolean(false);
        boolean includeLineStrings = importOptions.get("lines").asBoolean(false);
        boolean includePolygons = importOptions.get("polygons").asBoolean(false);
        boolean includeMultiPolygons = importOptions.get("multiPolygons").asBoolean(false);

        job.setState("creating-project");

        final Project project = new Project();
        new Thread(() -> {
            ProjectMetadata pm = new ProjectMetadata();
            pm.setName(JSONUtilities.getString(projectOptions, "projectName", "Untitled"));
            pm.setEncoding(JSONUtilities.getString(projectOptions, "encoding", "UTF-8"));
            pm.setDescription("OpenStreetMap data initially generated using Overpass API with the following query: " + osmExtractor.getOverpassQuery());

            if ((includePoints && pointsAsWKT) || includeLineStrings || includePolygons || includeMultiPolygons) {
                createColumn(project, Constants.Importing.wktColumnName, Constants.Importing.generatedColumnDescription);

                if (includePoints) {
                    if (pointsAsLatLon) {
                        createColumn(project, Constants.Importing.latitudeColumnName, Constants.Importing.generatedColumnDescription);
                        createColumn(project, Constants.Importing.longitudeColumnName, Constants.Importing.generatedColumnDescription);
                    }

                    if (pointsDelimited) {
                        createColumn(project, Constants.Importing.pointDelimitedColumnName, Constants.Importing.generatedColumnDescription);
                    }
                }
            }

            Map<String, String> tagMappings = new HashMap<>();

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

            Map<Point, Map<String, String>> points = osmExtractor.getPoints();
            Map<LineString, Map<String, String>> lineStrings = osmExtractor.getLineStrings();
            Map<MultiPolygon, Map<String, String>> polygons = osmExtractor.getMultiPolygons();

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

            if (includePoints && points != null && points.size() > 0) {
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

                        if (pointsAsLatLon && columnName.equals(Constants.Importing.latitudeColumnName)) {
                            value = String.valueOf(latitude);
                        } else if (pointsAsLatLon && columnName.equals(Constants.Importing.longitudeColumnName)) {
                            value = String.valueOf(longitude);
                        } else if (pointsDelimited && columnName.equals(Constants.Importing.pointDelimitedColumnName)) {
                            value = String.format("%f%s%f", latitude, pointsSeparator, (longitude));
                        } else if (pointsAsWKT && columnName.equals(Constants.Importing.wktColumnName)) {
                            value = osmExtractor.getWKTRepresentation(point);
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

            if (includeLineStrings && lineStrings != null && lineStrings.size() > 0) {
                int index = 0;
                for (Map.Entry<LineString, Map<String, String>> entry : lineStrings.entrySet()) {
                    LineString lineString = entry.getKey();
                    Row row = new Row(project.columnModel.getMaxCellIndex());
                    Map<String, String> currentTags = entry.getValue();

                    for (Column column : project.columnModel.columns) {
                        String columnName = column.getName();
                        String originalTagName = tagMappings.getOrDefault(columnName, null);

                        String value;

                        if (columnName.equals(Constants.Importing.wktColumnName)) {
                            value = osmExtractor.getWKTRepresentation(lineString);
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

            if (includePolygons && polygons != null && polygons.size() > 0) {
                int index = 0;
                for (Map.Entry<MultiPolygon, Map<String, String>> entry : polygons.entrySet()) {
                    MultiPolygon polygon = entry.getKey();
                    Row row = new Row(project.columnModel.getMaxCellIndex());
                    Map<String, String> currentTags = entry.getValue();

                    for (Column column : project.columnModel.columns) {
                        String columnName = column.getName();
                        String originalTagName = tagMappings.getOrDefault(columnName, null);

                        String value;

                        if (columnName.equals(Constants.Importing.wktColumnName)) {
                            value = osmExtractor.getWKTRepresentation(polygon);
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

    private static void createColumn(Project project, String columnName) {
        createColumn(project, columnName, null);
    }

    private static void createColumn(Project project, String columnName, String columnDescription) {
        if (columnName != null && !columnName.trim().isEmpty()) {
            try {
                Column column = new Column(project.columnModel.allocateNewCellIndex(), columnName);

                if(columnDescription != null && !columnDescription.trim().isEmpty()) {
                    column.setDescription(columnDescription);
                }

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
}

