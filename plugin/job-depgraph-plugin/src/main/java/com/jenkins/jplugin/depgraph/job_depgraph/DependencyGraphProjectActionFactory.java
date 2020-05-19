/*
 * Copyright (c) 2010 Stefan Wolf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.jenkins.jplugin.depgraph.job_depgraph;

import com.jenkins.jplugin.depgraph.job_depgraph.model.context.BuildInfo;
import com.jenkins.jplugin.depgraph.job_depgraph.util.ProjectUtil;
import hudson.Extension;
import hudson.model.*;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

/**
 * Factory to add a dependency graph view action to each project
 */
@Extension
public class DependencyGraphProjectActionFactory extends TransientProjectActionFactory {
    /**
     * Shows the connected component of the project
     */
    public static class DependencyGraphProjectAction extends AbstractDependencyGraphAction {
        final private AbstractProject<?, ?> project;

        private List<BuildInfo> buildInfos = new ArrayList<>();

        public String getTotal() {
            return "Total: " + buildInfos.size();
        }

        private List<AbstractProject> transStream(int stream, AbstractProject project) {
            switch (stream) {
                case 1:
                    return project.getUpstreamProjects();
                case 2:
                default:
                    return project.getDownstreamProjects();
            }
        }

        private BuildInfo setStreamFlag(BuildInfo info, int stream) {
            switch (stream) {
                case 1:
                    info.setStreamFlag("↑");
                    break;
                case 2:
                    info.setStreamFlag("↓");
                    break;
                default:
                    info.setStreamFlag("-");
            }
            return info;
        }

        private void loadLastBuildsInfo(List<BuildInfo> buildInfos, List<AbstractProject> projects, String context, int stream) {
            if (projects != null && projects.size() > 0) {
                for (AbstractProject project : projects) {
                    BuildInfo info = ProjectUtil.getLastBuildInfo(project, context);
                    info = setStreamFlag(info, stream);
                    addBuildInfo(buildInfos, info);
                    loadLastBuildsInfo(buildInfos, transStream(stream, project), context, stream);
                }
            }
        }

        private void addBuildInfo(List<BuildInfo> buildInfos, BuildInfo info) {
            boolean isExist = false;
            for (BuildInfo buildInfo : buildInfos) {
                if (buildInfo.getJobName().equals(info.getJobName())) {
                    isExist = true;
                }
            }
            if (!isExist) {
                buildInfos.add(info);
            }
        }

        // TODO: 2017/5/2 小心循环依赖问题
        private List<BuildInfo> getLastBuildsInDAG(String context) {
            List<BuildInfo> buildInfos = this.buildInfos;
            //上游
            if (upstream) {
                loadLastBuildsInfo(buildInfos, project.getUpstreamProjects(), context, 1);
            }

            if (upstream && downstream) {
                BuildInfo curBuildInfo = ProjectUtil.getLastBuildInfo(project, context);
                curBuildInfo = setStreamFlag(curBuildInfo, 0);
                addBuildInfo(buildInfos, curBuildInfo);
            }
            //下游
            if (downstream) {
                loadLastBuildsInfo(buildInfos, project.getDownstreamProjects(), context, 2);
            }
            return buildInfos;
        }

        //获取当前Project的历史context
        public List<BuildInfo> getBuildInfos() {
            buildInfos.clear();
            return getLastBuildsInDAG(context);
        }

        public HttpResponse doContext(StaplerRequest req) throws Descriptor.FormException, ServletException, IOException {

            JSONObject param = req.getSubmittedForm();

            if (param.containsKey("context")) {
                context = param.getString("context");
            }

            return HttpResponses.forwardToView(this, "table");
        }

        public DependencyGraphProjectAction(AbstractProject<?, ?> project) {
            this.project = project;
        }

        @Override
        protected Collection<AbstractProject<?, ?>> getProjectsForDepgraph() {
            return Collections.<AbstractProject<?, ?>>singleton(project);
        }

        @Override
        public String getTitle() {
            return com.jenkins.jplugin.depgraph.job_depgraph.Messages.AbstractDependencyGraphAction_DependencyGraphOf(project.getDisplayName());
        }

        @Override
        public AbstractModelObject getParentObject() {
            return project;
        }
    }

    @Override
    public Collection<? extends Action> createFor(AbstractProject target) {
        return Collections.singleton(new DependencyGraphProjectAction(target));
    }

}
