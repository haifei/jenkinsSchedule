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

package com.jenkins.jplugin.gleaning;


import com.jenkins.jplugin.gleaning.model.CronTask;
import com.jenkins.jplugin.gleaning.model.FailureTask;
import com.jenkins.jplugin.gleaning.model.Task;
import com.jenkins.jplugin.gleaning.model.TaskTypeEnum;
import hudson.model.AbstractModelObject;
import hudson.model.AbstractProject;
import hudson.model.Action;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Basic action for gleaning
 * 2020-04-21
 */
public abstract class AbstractGleaningAction implements Action {

    private final Logger LOGGER = Logger.getLogger(Logger.class.getName());

    public static List<CronTask> cronTasks;
    public static List<FailureTask> exceptionTasks;

    protected abstract Collection<? extends AbstractProject<?, ?>> getProjectsForGleaning();

    public abstract String getTitle();

    public abstract AbstractModelObject getParentObject();

    public boolean isGleaningEnabled() {
        return true;
    }

    public List<? extends Task> getTasks(String type) {
        if (type.equals(TaskTypeEnum.CRON_TASK.toString())) {
            return cronTasks;
        } else if (type.equals(TaskTypeEnum.FAILURE_TASK.toString())) {
            return exceptionTasks;
        }
        return null;
    }

    private int getTaskNum(String type) {
        List<? extends Task> tasks = getTasks(type);
        int count = 0;
        for (Task task : tasks) {
            if (!task.isDeleted()) {
                count++;
            }
        }
        return count;
    }

    public int getCronTaskNum() {
        return getTaskNum(TaskTypeEnum.CRON_TASK.toString());
    }

    public int getExceptionTaskNum() {
        return getTaskNum(TaskTypeEnum.FAILURE_TASK.toString());
    }

    @Override
    public String getIconFileName() {
        return "images/24x24/recovery.png";
    }

    @Override
    public String getDisplayName() {
        final String name = "任务检测";
        int count = getCronTaskNum() + getExceptionTaskNum();
        if (count > 0) {
            return name + String.format("【待处理:%d】", count);
        }
        return name;
    }

    @Override
    public String getUrlName() {
        return "gleaning";
    }

}
