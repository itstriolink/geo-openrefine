package com.google.refine.geojson.commands;

import java.io.IOException;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.ProjectManager;
import com.google.refine.commands.workspace.GetAllProjectMetadataCommand;
import com.google.refine.geojson.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.refine.commands.Command;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;

public class GenerateRandomNumber extends Command {
    private static final Logger logger = LoggerFactory.getLogger("RandomNumberCommand");


    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if(!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        if(logger.isDebugEnabled()) {
            logger.debug("GenerateRandomNumber::Post::{}");
        }

        try {
            String numberA = request.getParameter("numberA");
            String numberB = request.getParameter("numberB");

            if(numberA == null || numberB == null) {
                //respondError(response, "Both numbers need to provided correctly.");
                return;
            }

            respondJSON(response, Util.randomNumber(Integer.parseInt(numberA), Integer.parseInt(numberB)));
        } catch (Exception e) {
            logger.error("GenerateRandomNumber::Post::Exception::{}", e);
            throw new ServletException(e);
        }
//        finally {
//           // ProjectManager.singleton.setBusy(false);
//        }


    }
}
