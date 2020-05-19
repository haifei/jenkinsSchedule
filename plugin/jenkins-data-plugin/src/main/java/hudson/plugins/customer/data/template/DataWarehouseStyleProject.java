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
public class DataWarehouseStyleProject extends Project<FreeStyleProject, FreeStyleBuild> implements TopLevelItem {

    public DataWarehouseStyleProject(ItemGroup parent, String name) {
        super(parent, name);
    }

    @Override
    protected Class<FreeStyleBuild> getBuildClass() {
        return FreeStyleBuild.class;
    }

    @Override
    public DataWareHuseGeneratorDescriptor getDescriptor() {
        return (DataWareHuseGeneratorDescriptor) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    @Restricted(NoExternalUse.class)
    public static DataWareHuseGeneratorDescriptor DESCRIPTOR;

    @Extension(ordinal = 1010)
    public static class DataWareHuseGeneratorDescriptor
            extends AbstractProjectDescriptor {

        @Override
        public String getDisplayName() {
            return "创建周期作业模板";
        }

        public DataWareHuseGeneratorDescriptor() {
            DESCRIPTOR = this;
        }

        @Override
        public TopLevelItem newInstance(ItemGroup parent, String name) {
            return new DataWarehouseStyleProject(parent, name);
        }

        @Override
        public String getDescription() {
            return "创建一个周期运行的作业,已经配置好告警,其它项需要根据需要填写!";
        }

        @Override
        public String getIconFilePathPattern() {
            return (Jenkins.RESOURCE_PATH + "/images/:size/crontabProject.png").replaceFirst("^/", "");
        }

        @Override
        public String getIconClassName() {
            return "icon-crontab-project";
        }

        static {
            IconSet.icons.addIcon(new Icon("icon-crontab-project icon-sm", "16x16/crontabProject.png", Icon.ICON_SMALL_STYLE));
            IconSet.icons.addIcon(new Icon("icon-crontab-project icon-md", "24x24/crontabProject.png", Icon.ICON_MEDIUM_STYLE));
            IconSet.icons.addIcon(new Icon("icon-crontab-project icon-lg", "32x32/crontabProject.png", Icon.ICON_LARGE_STYLE));
            IconSet.icons.addIcon(new Icon("icon-crontab-project icon-xlg", "48x48/crontabProject.png", Icon.ICON_XLARGE_STYLE));
        }
    }
}