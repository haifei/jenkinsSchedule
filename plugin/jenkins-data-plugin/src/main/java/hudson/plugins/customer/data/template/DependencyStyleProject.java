package hudson.plugins.customer.data.template;

import hudson.Extension;
import hudson.model.*;
import jenkins.model.Jenkins;
import org.jenkins.ui.icon.Icon;
import org.jenkins.ui.icon.IconSet;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * 2020-04-12 add by wanghf
 */
public class DependencyStyleProject extends Project<FreeStyleProject, FreeStyleBuild> implements TopLevelItem {

    public DependencyStyleProject(ItemGroup parent, String name) {
        super(parent, name);
    }

    @Override
    protected Class<FreeStyleBuild> getBuildClass() {
        return FreeStyleBuild.class;
    }

    public DependencyGeneratorDescriptor getDescriptor() {
        return (DependencyGeneratorDescriptor) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    @Restricted(NoExternalUse.class)
    public static DependencyGeneratorDescriptor DESCRIPTOR;

    @Extension(ordinal=1009)
    public static class DependencyGeneratorDescriptor
            extends AbstractProjectDescriptor {

        @Override
        public String getDisplayName() {
            return "创建依赖作业模板";
        }

        public DependencyGeneratorDescriptor() {
            DESCRIPTOR = this;
        }

        @Override
        public TopLevelItem newInstance(ItemGroup parent, String name) {
            return new DependencyStyleProject(parent, name);
        }

        @Override
        public String getDescription() {
            return "创建一个依赖作业,已经配置好告警,其它项需要根据需要填写!";
        }

        @Override
        public String getIconFilePathPattern() {
            return (Jenkins.RESOURCE_PATH + "/images/:size/warehouseproject.png").replaceFirst("^/", "");
        }

        @Override
        public String getIconClassName() {
            return "icon-warehouse-project";
        }

        static {
            IconSet.icons.addIcon(new Icon("icon-warehouse-project icon-sm", "16x16/warehouseproject.png", Icon.ICON_SMALL_STYLE));
            IconSet.icons.addIcon(new Icon("icon-warehouse-project icon-md", "24x24/warehouseproject.png", Icon.ICON_MEDIUM_STYLE));
            IconSet.icons.addIcon(new Icon("icon-warehouse-project icon-lg", "32x32/warehouseproject.png", Icon.ICON_LARGE_STYLE));
            IconSet.icons.addIcon(new Icon("icon-warehouse-project icon-xlg", "48x48/warehouseproject.png", Icon.ICON_XLARGE_STYLE));
        }
    }
}
