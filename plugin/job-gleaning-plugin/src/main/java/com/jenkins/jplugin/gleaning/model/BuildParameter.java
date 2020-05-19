package com.jenkins.jplugin.gleaning.model;

import hudson.model.Project;

/**
 * 2020-04-21  add by wanghf
 */
public class BuildParameter {
    private Project project;
    private String context;
    private String type;

    public BuildParameter(Project project, String context, String type) {
        this.project = project;
        this.context = context;
        this.type = type;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
