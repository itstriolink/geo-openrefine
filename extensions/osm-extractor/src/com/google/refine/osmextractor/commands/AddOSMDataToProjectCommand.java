package com.google.refine.osmextractor.commands;

import com.google.refine.browsing.EngineConfig;
import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.osmextractor.operations.AddOSMDataToProjectOperation;

import javax.servlet.http.HttpServletRequest;

public class AddOSMDataToProjectCommand extends EngineDependentCommand {
    @Override
    protected AbstractOperation createOperation(Project project,
                                                HttpServletRequest request, EngineConfig engineConfig) throws Exception {
        String mappings = request.getParameter("mappings");
        String data = request.getParameter("data");

        return new AddOSMDataToProjectOperation(
                engineConfig,
                mappings,
                data
        );
    }
}
