package hudson.model;

import hudson.Extension;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * <br>
 * <b>功能:</b><br>
 * <b>作者:</b> yashiro <br>
 * <b>日期:</b> 2017/6/1 <br>
 */
public class OwnerView extends MyView {

    public static final String DEFAULT_VIEW_OWNER_NAME = "Owner";
    public static final String OWNER_VIEW_DISPLAY_NAME = "Owner View";

    @DataBoundConstructor
    public OwnerView(String name) {
        super(name);
    }

    public OwnerView(String name, ViewGroup owner) {
        super(name, owner);
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public String getDisplayName() {
        return DEFAULT_VIEW_OWNER_NAME.equals(name) ? DEFAULT_VIEW_OWNER_NAME : name;
    }

    @Override
    public Collection<TopLevelItem> getItems() {
        List<TopLevelItem> items = new ArrayList<TopLevelItem>();
        User user = User.current();
        if (user == null) {
            throw new RuntimeException("当前没有用户登录,请登录后重试.");
        }
        for (TopLevelItem item : getOwnerItemGroup().getItems()) {
            for (Job job : item.getAllJobs()) {
                String owner = job.getOwnerName();
                if (StringUtils.isNotEmpty(owner)) {
                    if (owner.contains(user.getId())) {
                        items.add(item);
                    }
                }
            }
        }
        return Collections.unmodifiableList(items);
    }

    @Extension
    @Symbol("owner")
    public static final class DescriptorImpl extends ViewDescriptor {
        @Override
        public boolean isApplicableIn(ViewGroup owner) {
            return true;
        }

        public String getDisplayName() {
            return OWNER_VIEW_DISPLAY_NAME;
        }
    }
}
