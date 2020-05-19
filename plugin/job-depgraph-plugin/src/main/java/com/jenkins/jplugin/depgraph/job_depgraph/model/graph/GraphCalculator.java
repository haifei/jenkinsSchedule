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


import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import hudson.model.AbstractProject;
import hudson.model.Item;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

/**
 * Generates the {@link DependencyGraph} given a set of {@link EdgeProvider}s.
 */
public class GraphCalculator {

    private Set<EdgeProvider> edgeProviders;

    @Inject
    public GraphCalculator(Set<EdgeProvider> edgeProviders) {
        this.edgeProviders = Sets.newHashSet(edgeProviders);
    }

    public DependencyGraph generateGraph(Iterable<ProjectNode> initialProjects) {
        DependencyGraph graph = new DependencyGraph();
        graph.addNodes(initialProjects);
        extendGraphSingal(graph, initialProjects, true);
        extendGraphSingal(graph, initialProjects, false);
        return graph;
    }

    private void extendGraph(DependencyGraph graph, Iterable<ProjectNode> fromProjects) {
        List<Edge> newEdges = Lists.newArrayList();
        for (ProjectNode projectNode : fromProjects) {
            AbstractProject<?,?> project = projectNode.getProject();
            if (project.hasPermission(Item.READ)) {
                for (EdgeProvider edgeProvider : edgeProviders) {
                    Iterables.addAll(newEdges, edgeProvider.getEdgesIncidentWith(project));
                }
            }
        }
        Set<ProjectNode> newProj = graph.addEdgesWithNodes(newEdges);
        if (!newProj.isEmpty()) {
            extendGraph(graph, newProj);
        }
    }

    private void extendGraphSingal(DependencyGraph graph, Iterable<ProjectNode> fromProjects, boolean upOrDown) {
        List<Edge> newEdges = Lists.newArrayList();
        for (ProjectNode projectNode : fromProjects) {
            AbstractProject<?,?> project = projectNode.getProject();
            if (project.hasPermission(Item.READ)) {
                for (EdgeProvider edgeProvider : edgeProviders) {
                    Iterables.addAll(newEdges, edgeProvider.getEdgesIncidentWith(project, upOrDown));
                }
            }
        }
        Set<ProjectNode> newProj = graph.addEdgesWithNodes(newEdges);
        if (!newProj.isEmpty()) {
            extendGraphSingal(graph, newProj, upOrDown);
        }
    }

    public static Iterable<ProjectNode> abstractProjectSetToProjectNodeSet(Iterable<? extends AbstractProject<?,?>> projects) {
        return Iterables.transform(projects, new Function<AbstractProject<?, ?>,ProjectNode>() {
            @Override
            public ProjectNode apply(AbstractProject<?, ?> input) {
                return ProjectNode.node(input);
            }
        });
    }

    public static Iterable<ProjectNode> setFillColor(Iterable<ProjectNode> projectNodes, Iterable<? extends AbstractProject<?,?>> projects) {
        for (AbstractProject initProject: projects) {
            for (ProjectNode node : projectNodes) {
                if (initProject.getName().equals(node.getProject().getName())) {
                    node.setSelected(true);
                }
            }
        }
        return projectNodes;
    }
}
