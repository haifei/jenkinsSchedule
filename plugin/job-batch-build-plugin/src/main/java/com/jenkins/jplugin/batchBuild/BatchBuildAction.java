/*
 *  The MIT License
 *
 *  Copyright 2012 Rino Kadijk.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package com.jenkins.jplugin.batchBuild;

import com.jenkins.jplugin.batchBuild.utils.DateUtils;
import hudson.model.*;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reschedules last completed build for the project if available.
 * Otherwise it behaves as if the user clicked on the build now button.
 */
public class BatchBuildAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(BatchBuildAction.class);
    private transient Run<?, ?> build;
    private transient String parameters = "batchParam";
    private static final String PARAMETERIZED_URL = "parameterized";
    private static final String CONTEXT = "time_hour";


    @Override
    public String getUrlName() {
        if (isAvailable()) {
            return "batchBuild";
        } else {
            return null;
        }
    }

    private boolean isAvailable() {
        StaplerRequest request = Stapler.getCurrentRequest();
        if (request != null) {
            Job project = request.findAncestorObject(Job.class);
            return project != null
                    && project.hasPermission(Item.BUILD)
                    && project.isBuildable()
                    && project instanceof Queue.Task;
        } else {
            return false;
        }
    }

    @Override
    public String getIconFileName() {
        if (isAvailable()) {
            return "images/24x24/batchBuild.png";
        } else {
            return null;
        }

    }

    @Override
    public String getDisplayName() {
        if (isAvailable()) {
            return "批量构建";
        } else {
            return null;
        }

    }

    /**
     * Getter method for build.
     *
     * @return build.
     */
    public Run<?, ?> getBuild() {
        return build;
    }

    /**
     * Getter method for parameters.
     *
     * @return parameters.
     */
    public String getParameters() {
        return parameters;
    }


    public void doIndex(StaplerRequest request, StaplerResponse response) throws IOException, ServletException, InterruptedException {

        Run currentBuild = request.findAncestorObject(Run.class);

        if (currentBuild != null) {
            ParametersAction paramAction = currentBuild.getAction(ParametersAction.class);
            if (paramAction != null) {
                response.sendRedirect(PARAMETERIZED_URL);
            }

        }
        response.sendRedirect(PARAMETERIZED_URL);
    }

    /**
     * 执行批量构建任务
     */
    public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException, InterruptedException {
        Job project = getProject();
        if (project == null) {
            return;
        }
        project.checkPermission(Item.BUILD);

        if (!req.getMethod().equals("POST")) {
            // show the parameter entry form.
            req.getView(this, "index.jelly").forward(req, rsp);
            return;
        }
        List<ParameterValue> values = new ArrayList<ParameterValue>();
        JSONObject formData = req.getSubmittedForm();

        if (!formData.isEmpty()) {
            String startTime = ((JSONObject) formData.get("start_time")).getString("start_time");
            String endTime = ((JSONObject) formData.get("end_time")).getString("end_time");
            batchSchedule(project, startTime, endTime);
            rsp.sendRedirect("../");
        } else {
            return;
        }
    }

    private void batchSchedule(Job project, String startTime, String endTime) {
        List<String> dateList = DateUtils.getDateRangebyDay(startTime, endTime);
        for (String timeHour : dateList) {

       /*     Hudson.getInstance().getQueue().schedule((Queue.Task) project, 0, new ParametersAction(
                    new StringParameterValue(CONTEXT, timeHour)));*/
            // 单次构建， 取消触发下游构建
            Jenkins.getInstance().getQueue().schedule(
                    (Queue.Task) project,
                    0,
                    new ParametersAction(new StringParameterValue(CONTEXT, timeHour)),
                    new CauseAction(new Cause.UserIdCause()),
                    new ParameterizedJobMixIn.SingleBuildInvisibleAction());
        }
    }


    public Job getProject() {
        if (build != null) {
            return build.getParent();
        }

        Job currentProject = null;
        StaplerRequest request = Stapler.getCurrentRequest();
        if (request != null) {
            currentProject = request.findAncestorObject(Job.class);
        }

        return currentProject;
    }

}
