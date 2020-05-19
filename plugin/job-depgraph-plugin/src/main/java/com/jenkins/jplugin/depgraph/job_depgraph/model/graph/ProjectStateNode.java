/*
 * Copyright (c) 2012 Stefan Wolf
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

package com.jenkins.jplugin.depgraph.job_depgraph.model.graph;

import com.google.common.base.Preconditions;
import com.jenkins.jplugin.depgraph.job_depgraph.util.ProjectUtil;
import hudson.model.*;

/**
 * A Node in the DependencyGraph, which corresponds to a Project
 */
public class ProjectStateNode {
    private final AbstractProject<?, ?> project;

    public ProjectStateNode(ProjectNode projectNode) {
        Preconditions.checkNotNull(projectNode);
        this.project = projectNode.getProject();
    }

    public String getFatName() {
        String context = ProjectUtil.getLastBuildContext(project);
        context = context == null ? "未设置" : context;
        return this.getName() +  "\n" + context;
    }

    public String getName() {
        String owner = project.getOwnerName();
        if (owner != null && !owner.trim().equals("")) {
            owner = "(" + owner + ")";
        } else {
            owner = "";
        }
        return project.getFullDisplayName() + owner;
    }
}

