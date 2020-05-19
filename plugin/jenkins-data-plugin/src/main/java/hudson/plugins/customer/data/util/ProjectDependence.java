package hudson.plugins.customer.data.util;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 2020-04-12 add by wanghf
 */
public class ProjectDependence implements Serializable {
    public Set<String> getDownstreamOwnersForBuild(AbstractBuild<?, ?> build, BuildListener listener) {
        Set<String> tmp = new LinkedHashSet<>();
        List<AbstractProject> projects = build.getParent().getDownstreamProjects();
        for (AbstractProject p : projects) {
            try {
                tmp.addAll(getDownstreamOwnersForProject(p));
            } catch (StackOverflowError e1) {
                listener.getLogger().println(String.format("任务[%s]获取下游owners发生StackOverflowError, " +
                        "下游任务DAG图发生了相互依赖，请检查下游任务依赖关系！", p.getName()));
            } catch (Exception e) {
                listener.getLogger().println("获取下游owners发生错误：" + e.getMessage());
            }
        }
        return removeDuplicate(tmp);
    }

    public Set<String> removeDuplicate(Set<String> owners) {
        Set<String> tmp = new LinkedHashSet<>();
        for (String owner : owners) {
            if (null != owner
                    && !"null".equals(owner)
                    && !"".equals(owner)) {
                if (owner.contains(",")) {
                    for (String o : owner.split(",")) {
                        tmp.add(o);
                    }
                } else if (!owner.trim().isEmpty()) {
                    tmp.add(owner);
                }
            }
        }
        return tmp;
    }

    public Set<String> getDownstreamOwnersForProject(AbstractProject project) {
        Set<String> tmp = new LinkedHashSet<>();
        List<AbstractProject> projects = project.getDownstreamProjects();
        if (projects.size() == 0) {
            tmp.add(project.getOwnerName());
            return tmp;
        }
        for (AbstractProject p : projects) {
            tmp.addAll(getDownstreamOwnersForProject(p));
        }
        return tmp;
    }

}
