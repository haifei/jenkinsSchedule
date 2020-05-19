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

package com.jenkins.jplugin.depgraph.job_depgraph.model.operations;

import hudson.tasks.BuildTrigger;
import jenkins.model.Jenkins;

import java.io.IOException;

public class PutEdgeOperation extends EdgeOperation {

    public PutEdgeOperation(String sourceJobName, String targetJobName) {
        super(sourceJobName, targetJobName);
    }

    public void perform() throws IOException {
        if (source != null && target != null) {
            final BuildTrigger buildTrigger = (BuildTrigger) source.getPublishersList().get(BuildTrigger.class);
            if (buildTrigger == null) {
                source.getPublishersList().add(new BuildTrigger(target.getName(), true));
            } else {
                final String childProjectsValue = normalizeChildProjectValue(buildTrigger.getChildProjectsValue() + ", " + target.getName());
                source.getPublishersList().remove(buildTrigger);
                source.getPublishersList().add(new BuildTrigger(childProjectsValue, true));
            }
            source.save();
            target.save();
            Jenkins.getInstance().rebuildDependencyGraph();
        }
    }
}
