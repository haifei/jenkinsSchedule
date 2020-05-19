package hudson.plugins.customer.data.template;

import hudson.model.FreeStyleProject;
import hudson.model.TopLevelItem;
import hudson.model.User;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 * 2020-04-12 add by wanghf
 */
public class JenkinsCreateProjectUtil implements Serializable{

    private final static Logger LOGGER = Logger.getLogger(JenkinsCreateProjectUtil.class.getName());
    private static Jenkins jenkins = Jenkins.getInstance();

    /*使用数仓模板来替换创建的自由格式模板*/
    public static void onCreated(String templateName, TopLevelItem createItem) {
        LOGGER.info("create job " + createItem.getName() + "...");

        TopLevelItem templateItem = jenkins.getItem(templateName);
        String createItemName = createItem.getName();

        if(null == templateItem){
            LOGGER.warning(String.format("template job [%s] 不存在! 使用自由模板", templateName));
            return;
        }
        //删除原始item
        try {
            jenkins.onDeleted(createItem);
        } catch (IOException e) {
            LOGGER.warning(String.format("delete job [%s] 失败: \n%s.", createItemName, e.getMessage()));
            e.printStackTrace();
        }


        try {
            //从模板copy
            jenkins.copy(templateItem, createItemName);
        } catch (IOException e) {
            LOGGER.warning(String.format("copy template job [%s] 失败: \n%s.", templateName, e.getMessage()));
            e.printStackTrace();
        }
        /*
         *  2020-04-07
         *   修改作业 创建者 以及 联系人
         */
        String userId= User.current().getId();
        ((FreeStyleProject)jenkins.getItem(createItemName)).setOwnerName(userId);
        ((FreeStyleProject)jenkins.getItem(createItemName)).setContacts(userId);

        LOGGER.info(String.format("使用 template job [%s] 创建 job[%s] success.", templateName, createItemName));
    }
}
