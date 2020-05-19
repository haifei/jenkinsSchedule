package com.jenkins.jplugin.gleaning.model;

import hudson.util.CyclicGraphDetector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 2020-04-21  add by wanghf
 */

public class GleaningCyclicGraphDetector {
    private Graph graph = new Graph();

    private class Edge {
        String src, dst;

        private Edge(String src, String dst) {
            this.src = src;
            this.dst = dst;
        }
    }

    private class Graph extends ArrayList<Edge> {
        Graph e(String src, String dst) {
            add(new Edge(src, dst));
            return this;
        }

        Set<String> nodes() {
            Set<String> nodes = new LinkedHashSet<String>();
            for (Edge e : this) {
                nodes.add(e.src);
                nodes.add(e.dst);
            }
            return nodes;
        }

        Set<String> edges(String from) {
            Set<String> edges = new LinkedHashSet<String>();
            for (Edge e : this) {
                if (e.src.equals(from))
                    edges.add(e.dst);
            }
            return edges;
        }

        /**
         * Performs a cycle check.
         */
        void check() throws CyclicGraphDetector.CycleDetectedException {
            new CyclicGraphDetector<String>() {
                protected Set<String> getEdges(String s) {
                    return edges(s);
                }
            }.run(nodes());
        }


    }

    public void addRelation(String src, String dst) {
        graph.e(src, dst);
    }

    public void checkContainCycle() throws CyclicGraphDetector.CycleDetectedException {
        graph.check();
    }
}
