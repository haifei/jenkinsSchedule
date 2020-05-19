package hudson.plugins.customer.data.watcherItem;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.User;
import hudson.model.listeners.ItemListener;
import hudson.plugins.customer.data.alarmsdkv2.AlarmV2;
import hudson.plugins.customer.data.common.Constants;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Notify whenever Job configuration changes.
 * <p>
 * Sends Notifiy to the list of recipients on following events: onRenamed,
 * onUpdated and onDeleted.
 * <p>
 * Created by wanghf on  2018/09/22.
 */
@Extension
public class WatcherItemNotifiy extends ItemListener {
    private static final Logger logger = LoggerFactory.getLogger(WatcherItemNotifiy.class);

    @Override
    public void onRenamed(Item item, String oldName, String newName) {
        if (!(item instanceof Job<?, ?>)) {
            return;
        }
        final Job<?, ?> job = (Job<?, ?>) item;
        String receiver = ((Job) item).getOwnerName();
        User user = User.current();
        String loginUser = user.getId();
        if (isOwnerModify(loginUser, receiver)) {
            return;
        }
        Map<String, Object> contentMap = new HashMap<>();
        String message = String.format("[job任务调度] 作业名称被修改，[%s] -> [%s], 修改人: [%s]. 请注意!", oldName, newName, loginUser);
        contentMap.put("content", message);
        //发送钉钉
        contentMap.put("mobiles", User.get(receiver).getPhone());
        new AlarmV2(receiver, contentMap).sendDingDing();
        logger.info(message);
    }

    @Override
    public void onUpdated(Item item) {
        if (!(item instanceof Job<?, ?>)) {
            return;
        }
        String receiver = ((Job) item).getOwnerName();
        User user = User.current();
        String loginUser = user.getId();
        if (isOwnerModify(loginUser, receiver)) {
            return;
        }
        String jobName = ((Job) item).getDisplayName();

        Map<String, Object> contentMap = new HashMap<>();
        String message = String.format("[job任务调度] 作业 [%s] 配置被 [%s] 修改. 请注意!", jobName, loginUser);

        contentMap.put("content", message);
        //发送钉钉
        contentMap.put("mobiles", User.get(receiver).getPhone());
        new AlarmV2(receiver, contentMap).sendDingDing();
        logger.info(message);
    }

    @Override
    public void onDeleted(Item item) {
        if (!(item instanceof Job<?, ?>)) {
            return;
        }
        String receiver = ((Job) item).getOwnerName();
        User user = User.current();
        String loginUser = user.getId();
        if (isOwnerModify(loginUser, receiver)) {
            return;
        }
        String jobName = item.getDisplayName();

        if (StringUtils.isNotEmpty(receiver) || StringUtils.isNotEmpty(User.get(receiver).getEmplid())) {
            return;
        }

        Map<String, Object> contentMap = new HashMap<>();
        String message = String.format("[job任务调度] 作业 [%s] 被 [%s] 删除. 请注意!", jobName, loginUser);
        contentMap.put("content", message);


        contentMap.put("content", message);
        //发送钉钉
        contentMap.put("mobiles", User.get(receiver).getPhone());
        new AlarmV2(receiver, contentMap).sendDingDing();
        logger.info(message);
    }

    private boolean isOwnerModify(String loginUser, String jobOwner) {
        if (loginUser.equals(Constants.AdminUser)) {
            return true;
        } else if (loginUser.equals(jobOwner)) {
            return true;
        } else {
            return false;
        }
    }

}
