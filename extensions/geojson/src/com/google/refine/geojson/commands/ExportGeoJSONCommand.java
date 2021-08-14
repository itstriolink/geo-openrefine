package com.google.refine.geojson.commands;

import com.google.common.net.PercentEscaper;
import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.commands.Command;
import com.google.refine.commands.project.ExportRowsCommand;
import com.google.refine.exporters.*;
import com.google.refine.exporters.sql.SqlExporterException;
import com.google.refine.geojson.exporters.GeoJSONExporter;
import com.google.refine.model.Project;
import org.apache.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Properties;

public class ExportGeoJSONCommand extends Command{
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ProjectManager.singleton.setBusy(true);

        try {
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            Properties params = ExportRowsCommand.getRequestParameters(request);

            String format = params.getProperty("format");
            Exporter exporter = ExporterRegistry.getExporter(format);
            if (exporter == null) {
                exporter = new GeoJSONExporter();
            }

            String contentType = params.getProperty("contentType");
            if (contentType == null) {
                contentType = exporter.getContentType();
            }
            response.setHeader("Content-Type", contentType);

            String preview = params.getProperty("preview");
            if (!"true".equals(preview)) {
                String path = request.getPathInfo();
                String fileName = path.substring(path.lastIndexOf('/') + 1);
                PercentEscaper escaper = new PercentEscaper("", false);
                fileName = escaper.escape(fileName);
                response.setHeader("Content-Disposition", "attachment; filename=" +fileName+"; filename*=utf-8' '" + fileName);
            }

            if (exporter instanceof WriterExporter) {
                String encoding = params.getProperty("encoding");

                response.setCharacterEncoding(encoding != null ? encoding : "UTF-8");
                Writer writer = encoding == null ?
                        response.getWriter() :
                        new OutputStreamWriter(response.getOutputStream(), encoding);

                ((WriterExporter) exporter).export(project, params, engine, writer);
                writer.close();
            }
            else if (exporter instanceof StreamExporter) {
                response.setCharacterEncoding("UTF-8");

                OutputStream stream = response.getOutputStream();
                ((StreamExporter) exporter).export(project, params, engine, stream);
                stream.close();
//          } else if (exporter instanceof UrlExporter) {
//              ((UrlExporter) exporter).export(project, options, engine);

            } else {
                // TODO: Should this use ServletException instead of respondException?
                respondException(response, new RuntimeException("Unknown exporter type"));
            }
        } catch (Exception e) {
            // Use generic error handling rather than our JSON handling
            logger.info("error:{}", e.getMessage());
            if (e instanceof SqlExporterException) {
                response.sendError(HttpStatus.SC_BAD_REQUEST, e.getMessage());
            }
            throw new ServletException(e);
        } finally {
            ProjectManager.singleton.setBusy(false);
        }
    }
}
