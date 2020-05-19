package org.jenkinsci.plugins.dependencyqueue;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Queue;
import hudson.model.queue.QueueTaskDispatcher;
import hudson.model.queue.CauseOfBlockage;
import jenkins.model.Jenkins;

@Extension
public class DependencyQueueDispatcher extends QueueTaskDispatcher {

    @Override
    public CauseOfBlockage canRun(Queue.Item itemInQuestion) {
        if (itemInQuestion.task instanceof AbstractProject) {

            int curPriority = getPriorityFromItm(itemInQuestion);
            if(curPriority == 1){
                return null;
            }

            //如果有执行队列没有跑满，可以直接跑
            Computer[] computers = Jenkins.getInstance().getComputers();
            for (Computer computer : computers) {
                if(isIdle(computer)) return null;
            }

            for (Queue.Item queuedItem : Jenkins.getInstance().getQueue().getItems()) {
                int queueTaskPriority = getPriorityFromItm(queuedItem);
                if(queueTaskPriority < curPriority){
                    //队列中有优先级更高的任务,不执行本次调度
                    String msg = String.format("任务[%s]的优先级高于[%s]的优先级,不执行!", queuedItem.task.getName(), itemInQuestion.task.getName());
                    return CauseOfBlockage.fromMessage(Messages._DependencyQueueDispatcher_UpstreamInQueue(msg));
                }
            }
            return null;
        }
        return super.canRun(itemInQuestion);
    }

    private boolean isIdle(Computer computer) {
        return computer.countIdle() > 0 ? true : false;
    }

    public BlockWhilePriorityQueuedProperty itemToProperty(Queue.Item itemInQuestion) {
        AbstractProject<?,?> projectInQuestion = (AbstractProject) itemInQuestion.task;
        BlockWhilePriorityQueuedProperty property = projectInQuestion.getProperty(BlockWhilePriorityQueuedProperty.class);
        return property;
    }

    public int getPriorityFromItm(Queue.Item itemInQuestion){
        BlockWhilePriorityQueuedProperty property = itemToProperty(itemInQuestion);
        if(null == property) return 3;
        return Integer.parseInt(property.getPriority());
    }
}
