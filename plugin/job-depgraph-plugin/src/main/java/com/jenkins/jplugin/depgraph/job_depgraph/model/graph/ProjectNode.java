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
import com.jenkins.jplugin.depgraph.job_depgraph.model.context.JOB_STATE;
import com.jenkins.jplugin.depgraph.job_depgraph.util.ProjectUtil;
import hudson.model.*;

/**
 * A Node in the DependencyGraph, which corresponds to a Project
 */
public class ProjectNode {
    private final AbstractProject<?,?> project;
    private final ProjectStateNode projectStateNode;
    private boolean isSelected;
    public static ProjectNode node(AbstractProject<?, ?> project) {
        ProjectNode node = new ProjectNode(project);
        return node;
    }

    public ProjectNode(AbstractProject<?, ?> project) {
        Preconditions.checkNotNull(project);
        this.isSelected = false;
        this.project = project;
        projectStateNode = new ProjectStateNode(this);
    }

    public String getFatName() {
       return projectStateNode.getFatName();
    }

    public String getStateColor() {
        JOB_STATE state = ProjectUtil.getJobState(this.project);
        switch (state) {
            case BUILDING:
                return "green";
            case FAILURE:
                return "red";
            case PEDDING:
                return "yellow";
            case SUCCESS:
                return "blue";
            case ABORTED:
                return "gray";
            case NOT_BUILT:
            case UNKNOWN:
            default:
                return "black";
        }
    }
    public String getName() {
        String owner = project.getOwnerName();
        if(owner!=null && !owner.trim().equals("")){
            owner = "(" + owner + ")";
        }
        else{
            owner = "";
        }
        return project.getFullDisplayName()+ owner;
    }

    public String getFillColor(){
        if (isSelected) {
            return "orange";
        }
        return "white";
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public AbstractProject<?, ?> getProject() {
        return project;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProjectNode that = (ProjectNode) o;

        if (!project.equals(that.project)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return project.hashCode();
    }
}
