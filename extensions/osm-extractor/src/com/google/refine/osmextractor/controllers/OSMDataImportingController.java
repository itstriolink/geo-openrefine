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
import com.google.refine.osmextractor.util.Util;
import com.google.refine.util.HttpClient;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class OSMDataImportingController implements ImportingController {
    private static final Logger logger = LoggerFactory.getLogger("OSMDataImportingController");
    protected RefineServlet servlet;
    protected ArrayNode elements;

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
        String overpassQuery = parameters.getProperty("overpassQuery");
        String overpassInstance = parameters.getProperty("overpassInstance");
        if (overpassInstance == null || overpassInstance.trim().isEmpty()
                || overpassQuery == null || overpassQuery.trim().isEmpty()) {
            HttpUtilities.respond(response, "error", "Missing Overpass query or Overpass API instance");
            return;
        }
        if (!Util.validateOverpassQuery(overpassQuery)) {
            HttpUtilities.respond(response, "error", "Invalid Overpass QL query.");
            return;
        }

        job.updating = true;

        try {
            job.prepareNewProject();
            job.setProgress(-1, "Parsing OpenStreetMap data...");
            ArrayNode tagsNode = ParsingUtilities.mapper.createArrayNode();
            ArrayNode geometryNode = ParsingUtilities.mapper.createArrayNode();
            ObjectNode statsNode = ParsingUtilities.mapper.createObjectNode();
            int nodes = 0;
            int ways = 0;
            int relations = 0;

            HttpClient httpClient = new HttpClient();
            String overpassResponse = httpClient.postNameValue(overpassInstance, "data", overpassQuery);

            ObjectNode object = ParsingUtilities.mapper.readValue(overpassResponse, ObjectNode.class);
            ArrayNode elements = JSONUtilities.getArray(object, "elements");
            List<String> tags = new ArrayList<>();
            if(elements != null && elements.size() > 0) {
                for(JsonNode node : elements) {
                    if(node instanceof ObjectNode) {
                        ObjectNode obj = (ObjectNode) node;
                        String type = obj.get("type").asText();

                        if(type != null) {
                            switch(type) {
                                case "node":
                                    nodes++;
                                    break;
                                case "way":
                                    ways++;
                                    break;
                                case "relation":
                                    relations++;
                                    break;
                                default:
                                    break;
                            }
                        }

                        ObjectNode elementTags = JSONUtilities.getObject(obj, "tags");

                        if (elementTags != null) {
                            Iterator<Map.Entry<String, JsonNode>> fields = elementTags.fields();
                            while (fields.hasNext()) {
                                Map.Entry<String, JsonNode> entry = fields.next();
                                String name = entry.getKey();
                                if(!tags.contains(name)) {
                                    tags.add(name);
                                }
                            }
                        }
                    }
                }

                for(String tagName : tags) {
                    JSONUtilities.append(tagsNode, tagName);
                }
            }

            statsNode.put("nodes", nodes);
            statsNode.put("ways", ways);
            statsNode.put("relations", relations);

            this.elements = elements;
            ObjectNode result = ParsingUtilities.mapper.createObjectNode();

            JSONUtilities.safePut(result, "status", "ok");
            JSONUtilities.safePut(result, "tags", tagsNode);
            JSONUtilities.safePut(result, "geometry", geometryNode);
            JSONUtilities.safePut(result, "stats", statsNode);

            JSONUtilities.append(geometryNode, "lat");
            JSONUtilities.append(geometryNode, "lon");
            //Convert to for loop to get all tags from all elements
            HttpUtilities.respondJSON(response, result);
        } catch (IOException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            HttpUtilities.respondException(response, e);
        } finally {
            job.touch();
            job.updating = false;
        }
    }

    private void doCreateProject(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        final ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "Import job doesn't exist.");
            return;
        }

        job.updating = true;
        final ObjectNode optionObj = ParsingUtilities.evaluateJsonStringToObjectNode(
                request.getParameter("options"));
        final ArrayNode tags = ParsingUtilities.evaluateJsonStringToArrayNode(request.getParameter("tags"));
        final ArrayNode geometry = ParsingUtilities.evaluateJsonStringToArrayNode(request.getParameter("geometry"));

        job.setState("creating-project");

        final Project project = new Project();
        new Thread(() -> {
            ProjectMetadata pm = new ProjectMetadata();
            pm.setName(JSONUtilities.getString(optionObj, "projectName", "Untitled"));
            pm.setEncoding(JSONUtilities.getString(optionObj, "encoding", "UTF-8"));
            String latitudeColumnName = "";
            String longitudeColumnName = "";

            Map<String, String> tagMappings = new HashMap<>();

            if (geometry != null && geometry.size() > 0) {
                for (JsonNode node : geometry) {
                    if (node instanceof ObjectNode) {
                        ObjectNode obj = (ObjectNode) node;
                        String newColumnName = obj.get("newColumnName").asText();
                        String coordinate = obj.get("coordinate").asText();
                        if (coordinate.equals("lat")) {
                            latitudeColumnName = newColumnName;
                        } else {
                            longitudeColumnName = newColumnName;
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

            if (elements != null && elements.size() > 0) {
                for (int i = 0; i < elements.size(); i++) {
                    if (elements.get(i) instanceof ObjectNode) {
                        Row row = new Row(project.columnModel.getMaxCellIndex());
                        ObjectNode obj = (ObjectNode) elements.get(i);
                        ObjectNode nodeTags = obj.get("tags").deepCopy();

                        double latitude = obj.get("lat").asDouble();
                        double longitude = obj.get("lon").asDouble();

                        for (Column column : project.columnModel.columns) {
                            String columnName = column.getName();
                            String originalTagName = tagMappings.getOrDefault(columnName, null);

                            String value;

                            if (originalTagName != null && nodeTags.has(originalTagName)) {
                                value = nodeTags.get(originalTagName).asText();
                            } else if (columnName.equals(latitudeColumnName)) {
                                value = String.valueOf(latitude);
                            } else if (columnName.equals(longitudeColumnName)) {
                                value = String.valueOf(longitude);
                            } else {
                                value = null;
                            }

                            row.setCell(column.getCellIndex(), new Cell(value, null));
                        }

                        project.rows.add(row);
                    }

                    job.setProgress(i * 100 / elements.size(),
                            "Parsed " + i + "/" + elements.size() + " OpenStreetMap elements.");
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
}