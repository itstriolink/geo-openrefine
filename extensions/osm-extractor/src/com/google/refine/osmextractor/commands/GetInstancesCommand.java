package com.google.refine.osmextractor.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.commands.Command;
import com.google.refine.osmextractor.util.Constants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class GetInstancesCommand extends Command {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        respondJSON(response, new InstancesList());
    }

    public static class InstancesList {
        @JsonProperty("instances")
        List<String> instances;

        protected InstancesList() {
            this.instances = Constants.OVERPASS_INSTANCES;
        }
    }
}
