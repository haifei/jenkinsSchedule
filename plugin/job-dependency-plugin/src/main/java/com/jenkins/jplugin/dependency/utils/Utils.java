package com.jenkins.jplugin.dependency.utils;

import com.jenkins.jplugin.dependency.exception.JobDependencyException;
import org.apache.commons.lang.StringUtils;
import com.jenkins.jplugin.dependency.exception.JobDependencyRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 2020-04-12 add by wanghf
 */
public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static boolean isEmpty(Object object) {
        if (object == null) {
            return true;
        } else if (object instanceof String) {
            return StringUtils.isEmpty((String) object);
        } else if (object instanceof Collection) {
            return ((Collection) object).isEmpty();
        } else if (object instanceof Map) {
            return ((Map) object).isEmpty();
        }
        return false;
    }

    public static List<String> trimForList(List<String> list) {
        List<String> trimList = new ArrayList<>();
        for (String str : list)
            trimList.add(str.trim());
        return trimList;
    }

    public static boolean hasValueForList(Collection<String> lists, String str) {
        for (String _str : lists) {
            if (str.equals(_str)) {
                return true;
            }
        }
        return false;
    }

    public static String getStrings(String[] strs) {
        StringBuffer sb = new StringBuffer();
        for (String str : strs) {
            sb.append(str);
        }
        return sb.toString();
    }

    public static String trimCommo(String str){
        return str.replace(",","");
    }

    /**
     * 两个参数的个数应该是一样的
     *
     * @param oldStrs
     * @param newStrs
     * @return
     */
    public static Map<String, String> getReviewdMap(String[] oldStrs, String[] newStrs) throws JobDependencyException {

        Map<String, String> reviewdMap = new HashMap<>();

        if (oldStrs.length != newStrs.length) {
            throw new JobDependencyException(String.format("参数长度不一致.oldStr:%s newStr:%s", oldStrs.length, newStrs.length));
        }

        for (int i = 0; i < oldStrs.length; i++) {
            if (!oldStrs[i].equals(newStrs[i])) {
                reviewdMap.put(oldStrs[i], newStrs[i]);
            }
        }
        logger.info(String.format("Job的名字已经重命名.%s", reviewdMap));
        return reviewdMap;
    }

    public static boolean containValueInCollection(List<String> lists, String str) {
        for (String _str : lists) {
            if (_str.contains(str)) {
                return true;
            }
        }
        return false;
    }

    public static int stringConvertInteger(String data) {
        int _data = 0;
        try {
            _data = Integer.parseInt(data);
        } catch (NumberFormatException e) {
            logger.error(String.format("数字转化异常.data:%s", data), e);
            throw new JobDependencyRuntimeException(String.format("数字转化异常.data:%s", data));
        }
        return _data;
    }

}
