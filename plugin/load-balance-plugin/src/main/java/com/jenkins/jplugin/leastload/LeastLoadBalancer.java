/*
 * The MIT License
 * 
 * Copyright (c) 2013, Brendan Nolan
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
package com.jenkins.jplugin.leastload;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

import hudson.model.LoadBalancer;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Queue.Task;
import hudson.model.queue.MappingWorksheet;
import hudson.model.queue.MappingWorksheet.ExecutorChunk;
import hudson.model.queue.MappingWorksheet.Mapping;

import java.util.*;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

/**
 * A {@link LoadBalancer} implementation that the leastload plugin uses to replace the default
 * Jenkins {@link LoadBalancer}
 * <p>
 * <p>The {@link LeastLoadBalancer} chooses {@link Executor}s that have the least load. An {@link Executor} is defined
 * as having the least load if it is idle or has the most available {@link Executor}s
 * <p>
 * <p>If for any reason we are unsuccessful in creating a {@link Mapping} we fall back on the default Jenkins
 * {@link LoadBalancer#CONSISTENT_HASH} and try to use that.
 *
 * @author brendan.nolan@gmail.com
 */
public class LeastLoadBalancer extends LoadBalancer {

    private static final Logger LOGGER = Logger.getLogger(LeastLoadBalancer.class.getCanonicalName());

//    private static final Comparator<ExecutorChunk> EXECUTOR_CHUNK_COMPARATOR = Collections.reverseOrder(
//            new ExecutorChunkComparator(SystemLoadAverageConfig.get().getAverageThreshold()));

    private final LoadBalancer fallback;

    /**
     * Create the {@link LeastLoadBalancer} with a fallback that will be
     * used in case of any failures.
     */
    public LeastLoadBalancer(LoadBalancer fallback) {
        Preconditions.checkNotNull(fallback, "You must provide a fallback implementation of the LoadBalancer");
        this.fallback = fallback;
    }

    @Override
    public Mapping map(Task task, MappingWorksheet ws) {

        try {

            if (!isDisabled(task)) {

                List<ExecutorChunk> useableChunks = getApplicableSortedByLoad(ws);
                //可能机器的负载都比较高,返回null,表示不需要在slave上执行,依然在队列中
                if (useableChunks == null) {
                    return null;
                }
                Mapping m = ws.new Mapping();
                if (assignGreedily(m, useableChunks, 0)) {
                    assert m.isCompletelyValid();
                    return m;
                } else {
                    LOGGER.log(FINE, "Least load balancer was unable to define mapping. Falling back to double check");
                    return getFallBackLoadBalancer().map(task, ws);
                }

            } else {
                return getFallBackLoadBalancer().map(task, ws);
            }

        } catch (Exception e) {
            LOGGER.log(WARNING, "Least load balancer failed will use fallback", e);
            return getFallBackLoadBalancer().map(task, ws);
        }
    }

    /**
     * Extract a list of applicable {@link ExecutorChunk}s sorted in least loaded order
     *
     * @param ws - The mapping worksheet
     * @return -A list of ExecutorChunk in least loaded order
     */
    private List<ExecutorChunk> getApplicableSortedByLoad(MappingWorksheet ws) {

        List<ExecutorChunk> chunks = new ArrayList<ExecutorChunk>();
        for (int i = 0; i < ws.works.size(); i++) {
            for (ExecutorChunk ec : ws.works(i).applicableExecutorChunks()) {
                chunks.add(ec);
            }
        }

        //所有的node的负载都比较高,则直接返回null,表示不在slave节点上执行任务
        if (allMoreAverage(chunks)) {
            return null;
        }

        for (ExecutorChunk chunk : chunks) {
            LOGGER.info(String.format("Current Slave:%s System Load Average:%s",
                    chunk.computer.getName(), chunk.computer.currentSystemLoadAverage()));
        }
        Collections.shuffle(chunks); // See JENKINS-18323
        Collections.sort(chunks, Collections.reverseOrder(
                new ExecutorChunkComparator(SystemLoadAverageConfig.get().getAverageThreshold())));
        return chunks;

    }

    private boolean allMoreAverage(List<ExecutorChunk> chunks) {
        String threshold = SystemLoadAverageConfig.get().getAverageThreshold();

        Map<String, String> nodes = new HashMap<>();
        for (ExecutorChunk chunk : chunks) {
            if (!moreAverage(chunk.computer.currentSystemLoadAverage(), threshold))
                return false;
            nodes.put(chunk.computer.getDisplayName(), chunk.computer.currentSystemLoadAverage());
        }

        if (nodes.size() == 0) {
            return false;
        }

        LOGGER.warning(String.format("Current Slaves:%s Are More Than Thresthod:%s.", nodes, threshold));
        return true;
    }

    @SuppressWarnings("rawtypes")
    private boolean isDisabled(Task task) {

        if (task instanceof AbstractProject) {
            AbstractProject project = (AbstractProject) task;
            @SuppressWarnings("unchecked")
            LeastLoadDisabledProperty property = (LeastLoadDisabledProperty) project.getProperty(LeastLoadDisabledProperty.class);
            // If the job configuration hasn't been saved after installing the plugin, the property will be null. Assume
            // that the user wants to enable functionality by default.
            if (property != null) {
                return property.isLeastLoadDisabled();
            }
            return false;
        } else {
            return true;
        }

    }

    private boolean assignGreedily(Mapping m, List<ExecutorChunk> executors, int i) {

        if (m.size() == i) {
            return true;
        }

        for (ExecutorChunk ec : executors) {
            m.assign(i, ec);
            if (m.isPartiallyValid() && assignGreedily(m, executors, i + 1)) {
                return true;
            }
        }

        m.assign(i, null);
        return false;

    }

    /**
     * Retrieves the fallback {@link LoadBalancer}
     *
     * @return
     */
    public LoadBalancer getFallBackLoadBalancer() {
        return fallback;
    }

    protected static class ExecutorChunkComparator implements Comparator<ExecutorChunk> {

        String averageThreshold;

        ExecutorChunkComparator(String averageThreshold) {
            this.averageThreshold = averageThreshold;
        }

        public int compare(ExecutorChunk ec1, ExecutorChunk ec2) {

            if (ec1 == ec2) {
                return 0;
            }

            Computer com1 = ec1.computer;
            Computer com2 = ec2.computer;

            String average1 = com1.currentSystemLoadAverage();
            String average2 = com2.currentSystemLoadAverage();

            if (!moreAverage(average1, averageThreshold) && moreAverage(average2, averageThreshold)) {
                LOGGER.warning(String.format("Current Slave:%s System Load Average%s Is More Than Threshold:%s",
                        com2.getName(), average2, averageThreshold));
                return 1;
            } else if (moreAverage(average1, averageThreshold) && !moreAverage(average2, averageThreshold)) {
                LOGGER.warning(String.format("Current Slave:%s System Load Average%s Is More Than Threshold:%s",
                        com1.getName(), average1, averageThreshold));
                return -1;
            } else if (isIdle(com1) && !isIdle(com2)) {
                return 1;
            } else if (isIdle(com2) && !isIdle(com1)) {
                return -1;
            } else {
                return com1.countIdle() - com2.countIdle();
            }

        }

        // Can't use computer.isIdle() as it can return false when assigned
        // a multi-configuration job even though no executors are being used
        private boolean isIdle(Computer computer) {
            return computer.countExecutors() - computer.countIdle() == 0 ? true : false;
        }

    }

    private static boolean moreAverage(String average, String threshold) {
        if (Double.parseDouble(average) > Double.parseDouble(threshold)) {
            return true;
        } else {
            return false;
        }
    }


}
