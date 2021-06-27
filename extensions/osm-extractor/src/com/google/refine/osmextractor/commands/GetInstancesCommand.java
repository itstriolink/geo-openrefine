package com.google.refine.osmextractor.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.commands.Command;
import com.google.refine.commands.expr.GetExpressionHistoryCommand;
import com.google.refine.commands.lang.GetLanguagesCommand;
import com.google.refine.osmextractor.util.Constants;
import edu.mit.simile.butterfly.ButterflyModule;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class GetInstancesCommand extends Command {
    public static class InstancesList  {
        @JsonProperty("instances")
        List<String> instances;

        protected InstancesList() {
            this.instances = Constants.OVERPASS_INSTANCES;
        }
    }
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        respondJSON(response, new InstancesList());
    }
}
