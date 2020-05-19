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
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Factory to a dependency graph view action to all views
 */
@Extension
public class DependencyGraphViewActionFactory extends TransientViewActionFactory {
    /**
     * Shows the connected components containing the projects of the view
     */
    public static class DependencyGraphViewAction extends AbstractDependencyGraphAction
            implements Action {

        private View view;

        List<BuildInfo> buildInfos = new ArrayList<>();

        public String getTotal() {
            return "Total: " + buildInfos.size();
        }

        public DependencyGraphViewAction(View view) {
            this.view = view;
        }

        public HttpResponse doContext(StaplerRequest req) throws Descriptor.FormException, ServletException, IOException {
            JSONObject param = req.getSubmittedForm();
            if (param.containsKey("context")) {
                context = param.getString("context");
            }
            return HttpResponses.forwardToView(this, "table");
        }

        public FormValidation doCheckContext(@QueryParameter String context) {
            return FormValidation.ok();
        }

        public List<BuildInfo> getBuildInfos() {
            buildInfos.clear();
            Collection<TopLevelItem> items = view.getItems();
            for (TopLevelItem item : items) {
                if (item instanceof AbstractProject<?, ?>) {
                    BuildInfo buildInfo = ProjectUtil.getLastBuildInfo((AbstractProject<?, ?>) item, context);
                    buildInfos.add(buildInfo);
                }
            }
            if (buildInfos.size() == 0) {
                return null;
            }
            return buildInfos;
        }

        @Override
        protected Collection<? extends AbstractProject<?, ?>> getProjectsForDepgraph() {
            Collection<TopLevelItem> items = view.getItems();
            Collection<AbstractProject<?, ?>> projects = new ArrayList<AbstractProject<?, ?>>();
            for (TopLevelItem item : items) {
                if (item instanceof AbstractProject<?, ?>) {
                    projects.add((AbstractProject<?, ?>) item);
                }
            }
            return projects;
        }

        @Override
        public String getTitle() {
            return Messages.AbstractDependencyGraphAction_DependencyGraphOf(view.getDisplayName());
        }

        @Override
        public AbstractModelObject getParentObject() {
            return view;
        }
    }

    @Override
    public List<Action> createFor(View v) {
        return Collections.<Action>singletonList(new DependencyGraphViewAction(v));
    }

}
