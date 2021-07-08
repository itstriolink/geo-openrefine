package com.google.refine.osmextractor.controllers;

import com.fasterxml.jackson.core.JsonGenerator;
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
import java.io.Writer;
import java.util.*;

public class OSMDataImportingController implements ImportingController {
    private static final Logger logger = LoggerFactory.getLogger("OSMDataImportingController");
    protected RefineServlet servlet;
    private String overpassInstance;
    private String overpassQuery;

    @Override
    public void init(RefineServlet servlet) {
        this.servlet = servlet;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpUtilities.respond(response, "error", "GET not implemented");
    }


    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
            HttpUtilities.respond(response, "error", "No such sub command");
        }
    }

    private void doInitializeParserUI(
            HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {
        ObjectNode result = ParsingUtilities.mapper.createObjectNode();
        ObjectNode options = ParsingUtilities.mapper.createObjectNode();

        JSONUtilities.safePut(result, "status", "ok");
        JSONUtilities.safePut(result, "options", options);
        JSONUtilities.safePut(options, "skipDataLines", 0); // number of initial data lines to skip
        JSONUtilities.safePut(options, "storeBlankRows", true);
        JSONUtilities.safePut(options, "storeBlankCellsAsNulls", true);


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
        job.prepareNewProject();
        String overpassQuery = parameters.getProperty("overpassQuery");
        String overpassInstance = parameters.getProperty("overpassInstance");

        setProgress(job, -1);
        try {
            ArrayNode tagsNode = ParsingUtilities.mapper.createArrayNode();
            if (overpassQuery != null && !overpassQuery.trim().isEmpty()
                    && overpassInstance != null && !overpassInstance.trim().isEmpty()) {
                if (!Util.validateOverpassQuery(overpassQuery)) {
                    HttpUtilities.respond(response, "error", "Invalid Overpass QL query.");
                    return;
                }
                this.overpassInstance = overpassInstance;
                this.overpassQuery = overpassQuery;

                try {
                    HttpClient httpClient = new HttpClient();
                    String _response = httpClient.postNameValue(overpassInstance, "data", overpassQuery);

                    ObjectNode object = ParsingUtilities.mapper.readValue(_response, ObjectNode.class);
                    ArrayNode elements = JSONUtilities.getArray(object, "elements");
                    ObjectNode firstElement = JSONUtilities.getObjectElement(elements, 0);
                    ObjectNode result = ParsingUtilities.mapper.createObjectNode();

                    JSONUtilities.safePut(result, "status", "ok");
                    JSONUtilities.safePut(result, "tags", tagsNode);
                    ObjectNode tags = JSONUtilities.getObject(firstElement, "tags");
                    Iterator<Map.Entry<String, JsonNode>> fields = tags.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        String name = entry.getKey();
                        JSONUtilities.append(tagsNode, name);
                    }
                    //Convert to for loop to get all tags from all elements

                    HttpUtilities.respond(response, result.toString());
                } catch (IOException e) {
                    throw new ServletException(e);
                }
            } else {
                HttpUtilities.respond(response, "error", "Missing Overpass query or Overpass API instance");
            }

            Writer w = response.getWriter();
            JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);
            try {
                writer.writeStartObject();
                job.project.update(); // update all internal models, indexes, caches, etc.
                writer.writeStringField("status", "ok");
                writer.writeStringField("tags", tagsNode.toString());

                writer.writeEndObject();
            } catch (IOException e) {
                HttpUtilities.respond(response, "error", ExceptionUtils.getStackTrace(e));
                throw new ServletException(e);
            } finally {
                writer.flush();
                writer.close();
                w.flush();
                w.close();
            }

        } catch (IOException e) {
            throw new ServletException(e);
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
        final ArrayNode mappings = ParsingUtilities.evaluateJsonStringToArrayNode(request.getParameter("mappings"));

        final List<Exception> exceptions = new LinkedList<Exception>();

        job.setState("creating-project");

        final Project project = new Project();
        new Thread(() -> {
            ProjectMetadata pm = new ProjectMetadata();
            pm.setName(JSONUtilities.getString(optionObj, "projectName", "Untitled"));
            pm.setEncoding(JSONUtilities.getString(optionObj, "encoding", "UTF-8"));

            try {
                HttpClient httpClient = new HttpClient();
                String _response = httpClient.postNameValue(overpassInstance, "data", overpassQuery);

                ObjectNode object = ParsingUtilities.mapper.readValue(_response, ObjectNode.class);
                ArrayNode elements = JSONUtilities.getArray(object, "elements");
                if (mappings != null && mappings.size() > 0) {
                    for (int i = 0; i < mappings.size(); i++) {
                        if (mappings.get(i) instanceof ObjectNode) {
                            ObjectNode obj = (ObjectNode) mappings.get(i);
                            String newColumnName = obj.get("newColumnName").asText();

                            if (newColumnName != null && !newColumnName.trim().isEmpty()) {
                                try {
                                    if (elements != null && elements.size() > 0) {
                                        Column column = new Column(project.columnModel.allocateNewCellIndex(), newColumnName);
                                        project.columnModel.addColumn(
                                                project.columnModel.columns.size(),
                                                column,
                                                false
                                        );
                                        project.update();

                                        for (int j = 0; j < elements.size(); j++) {
                                            if (mappings.get(i) instanceof ObjectNode) {
                                                ObjectNode node = (ObjectNode) elements.get(j);
                                                ObjectNode tags = node.get("tags").deepCopy();
                                                String osmTag = obj.get("osmTag").asText();
                                                String value = null;

                                                if (tags.has(osmTag)) {
                                                    value = tags.get(osmTag).asText();
                                                }

                                                Row row;
                                                try {
                                                    row = project.rows.get(j);
                                                } catch (IndexOutOfBoundsException e) {
                                                    Row _row = new Row(mappings.size());
                                                    row = _row;
                                                }

                                                row.setCell(column.getCellIndex(), new Cell(value, null));
                                                project.rows.add(row);
                                            }
                                        }
                                    }
                                } catch (ModelException e) {
                                    logger.error("Couldn't add column.", e);
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }

            setProgress(job, 100);

            if (!job.canceled) {
                if (exceptions.size() > 0) {
                    job.setError(exceptions);
                } else {
                    project.update(); // update all internal models, indexes, caches, etc.

                    ProjectManager.singleton.registerProject(project, pm);

                    job.setState("created-project");
                    job.setProjectID(project.id);
                }

                job.touch();
                job.updating = false;
            }
        }).start();

        HttpUtilities.respond(response, "ok", "done");
    }

    static private void setProgress(ImportingJob job, int percent) {
        job.setProgress(percent, "Parsing OpenStreetMap data...");
    }
}