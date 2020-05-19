package hudson.plugins.customer.data.template;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.model.listeners.ItemListener;

import java.util.logging.Logger;

/**
 * 2020-04-12 add by wanghf
 * 创建有依赖的数仓作业创建事件
 */
@Extension
public class DependencyJobCreateListener extends ItemListener {
    private final static Logger LOGGER = Logger.getLogger(DependencyJobCreateListener.class.getName());
    private final String templateName = "dependency-job-template";

    /*使用数仓模板来替换创建的自由格式模板*/
    @Override
    public void onCreated(Item item) {
        if(item instanceof DependencyStyleProject){
            JenkinsCreateProjectUtil.onCreated(templateName, (TopLevelItem) item);
        }
    }

    /*忽略即可( 必须加上,否则死循环 )*/
    public void onCopied(Item src, Item item) {
        return;
    }

}
