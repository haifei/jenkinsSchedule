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


import com.jenkins.jplugin.gleaning.model.*;
import com.jenkins.jplugin.gleaning.util.GleaningCheckUtil;
import hudson.Extension;
import hudson.init.Initializer;
import hudson.model.*;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.*;

import static hudson.init.InitMilestone.JOB_LOADED;

/**
 * Factory to a dependency graph view action to all views
 */
@Extension
public class GleaningViewActionFactory extends TransientViewActionFactory {
    /**
     * Shows the connected components containing the projects of the view
     */
    @ExportedBean
    public static class GleaningViewAction extends AbstractGleaningAction
            implements Action {

        private View view;

        public GleaningViewAction(View view) {
            this.view = view;
        }

        @Initializer(after = JOB_LOADED)
        public static void check() {
            cronTasks = GleaningCheckUtil.getAllCronTasks();
            exceptionTasks = GleaningCheckUtil.getAllExceptionTasks();
        }

        @Override
        protected Collection<? extends AbstractProject<?, ?>> getProjectsForGleaning() {
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
            return view.getDisplayName();
        }

        @Override
        public AbstractModelObject getParentObject() {
            return view;
        }

        public List<CronTask> getCronTasks() {
            List<CronTask> unDeleteList = new ArrayList<>();
            for (CronTask task : cronTasks) {
                if (!GleaningCheckUtil.filterCronTask(task)) {
                    task.setDeleted(true);
                }
                if (!task.isDeleted()) {
                    unDeleteList.add(task);
                }
            }
            return unDeleteList;
        }

        public List<FailureTask> getExceptionTasks() {
            List<FailureTask> unDeleteList = new ArrayList<>();
            for (FailureTask task : exceptionTasks) {
                if (!GleaningCheckUtil.filterFailureTask(task)) {
                    task.setDeleted(true);
                }
                if (!task.isDeleted()) {
                    unDeleteList.add(task);
                }
            }
            return unDeleteList;
        }

        public List<DagTask> getDagTasks() {
            return GleaningCheckUtil.checkDAGCyclic();
        }

        public Api getApi() {
            return new Api(this);
        }

        @Exported
        public boolean checkDagCyclic() {
            return GleaningCheckUtil.containDAGCyclic();
        }

        private void removeTask(int id, String type) {
            List<? extends Task> tasks = getTasks(type);
            for (Task task : tasks) {
                if (task.getId() == id) {
                    task.setDeleted(true);
                }
            }
        }

        @JavaScriptMethod
        public void insert(int id, String name, String calContext, String type) {
            Project project = GleaningCheckUtil.getProjectByName(name);
            if (project != null) {
                GleaningCheckUtil.execTasks(new BuildParameter(project, calContext, type));
            }
            removeTask(id, type);
        }

        @JavaScriptMethod
        public void ignore(int id, String type) {
            //todo: add lock.
            removeTask(id, type);
        }

        @JavaScriptMethod
        public String getChecked(int id, String type) {
            //todo: add lock.
            List<? extends Task> tasks = getTasks(type);
            for (Task task : tasks) {
                if (task.getId() == id) {
                    return task.getIsChecked();
                }
            }
            return "false";
        }

        @JavaScriptMethod
        public void onCheck(int id, String type) {
            List<? extends Task> tasks = getTasks(type);
            for (Task task : tasks) {
                if (task.getId() == id) {
                    boolean isChecked = Boolean.parseBoolean(task.getIsChecked());
                    if (isChecked) {
                        task.setIsChecked("false");
                    } else {
                        task.setIsChecked("true");
                    }
                    break;
                }
            }
        }

        /**
         * @return
         * @Author wanghf
         * @Description 批量执行任务
         * @Date 2019/1/23 下午2:37
         * @Param
         */
        private void operateExec(List<? extends Task> tasks, boolean isChecked, String type) {
            List<BuildParameter> parameters = new ArrayList<>();
            for (Task task : tasks) {
                if (task.isDeleted()) {
                    continue;
                }
                boolean taskChecked = Boolean.valueOf(task.getIsChecked());
                //判断是否 是执行单个任务
                if (isChecked && !taskChecked) {
                    continue;
                }
                Project project = GleaningCheckUtil.getProjectByName(task.getName());
                if (project != null) {
                    String context = null;
                    if (task instanceof CronTask) {
                        context = ((CronTask) task).getCalContext();
                    }
                    BuildParameter parameter = new BuildParameter(project, context, type);
                    parameters.add(parameter);
                    task.setDeleted(true);
                }

            }
            if (parameters.size() > 0) {
                GleaningCheckUtil.execTasks(parameters);
            }
        }

        @JavaScriptMethod
        public void operate(String opt, String type) {
            if (opt.equals(OperateEnum.RELOAD_FAILURE_TASK.toString())) {
                exceptionTasks.clear();
                exceptionTasks = GleaningCheckUtil.getAllExceptionTasks();
                return;
            } else if (opt.equals(OperateEnum.RELOAD_CRON_TASK.toString())) {
                cronTasks.clear();
                cronTasks = GleaningCheckUtil.getAllCronTasks();
                return;
            }
            List<? extends Task> tasks = getTasks(type);
            if (tasks != null && tasks.size() > 0) {
                if (opt.equals(OperateEnum.EXEC_ALL.toString())) {
                    operateExec(tasks, false, type);
                    return;
                } else if (opt.equals(OperateEnum.EXEC_CHECKED.toString())) {
                    operateExec(tasks, true, type);
                    return;
                }

                Iterator<? extends Task> iterator = tasks.iterator();
                while (iterator.hasNext()) {
                    Task task = iterator.next();
                    if (task.isDeleted()) {
                        continue;
                    }
                    if (opt.equals(OperateEnum.IGNORE_ALL.toString())) {
                        iterator.remove();
                    } else if (opt.equals(OperateEnum.IGNORE_CHECKED.toString())) {
                        if (task.getIsChecked().equals("false")) {
                            continue;
                        }
                        iterator.remove();
                    } else if (opt.equals(OperateEnum.CLEAR_CHECKED.toString())) {
                        task.setIsChecked("false");
                    }
                }
            }
        }
    }

    @Override
    public List<Action> createFor(View v) {
        return Collections.<Action>singletonList(new GleaningViewAction(v));
    }


    public static enum OperateEnum {

        CLEAR_CHECKED("clear_checked"),
        RELOAD_FAILURE_TASK("reload_failure_task"),
        RELOAD_CRON_TASK("reload_cron_task"),
        IGNORE_ALL("ignore_all"),
        IGNORE_CHECKED("ignore_checked"),
        EXEC_CHECKED("exec_checked"),
        EXEC_ALL("exec_all");


        private final String text;

        private OperateEnum(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

}
