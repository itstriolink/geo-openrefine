package com.google.refine.osmextractor.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.commands.Command;
import com.google.refine.osmextractor.util.Constants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class PreviousQueriesCommand extends Command {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        respondJSON(response, null);
    }
}