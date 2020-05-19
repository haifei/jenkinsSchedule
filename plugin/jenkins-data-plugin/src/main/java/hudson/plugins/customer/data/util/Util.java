package hudson.plugins.customer.data.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
/**
 * 2020-04-12 add by wanghf
 */
public class Util {

    /**
     * 判断一个对象是否为空。它支持如下对象类型：
     * <ul>
     * <li>null : 一定为空
     * <li>字符串 : ""为空,多个空格也为空
     * <li>数组
     * <li>集合
     * <li>Map
     * <li>其他对象 : 一定不为空
     * </ul>
     *
     * @param obj 任意对象
     * @return 是否为空
     */
    public final static boolean isEmpty(final Object obj) {
        if (obj == null) {
            return true;
        }
        if (obj instanceof String) {
            return "".equals(String.valueOf(obj).trim());
        }
        if (obj.getClass().isArray()) {
            if (obj instanceof String[]) {
                return Array.getLength(obj) == 0 || "".equals(String.valueOf(Array.get(obj, 0)).trim());
            } else {
                return Array.getLength(obj) == 0;
            }
        }
        if (obj instanceof Collection) {
            return ((Collection<?>) obj).isEmpty();
        }
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).isEmpty();
        }
        return false;
    }

    /**
     * 拼接字符串
     */
    public static String getStrings(String[] strs) {
        StringBuffer sb = new StringBuffer();
        for (String str : strs) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 判断两个数组中是否存在相同的元素
     */
    public static boolean findSameElement(Integer[] sources, Integer[] targets) {
        int DEFAULT_VALUE = 0;
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        for (Integer source : sources)
            map.put(source, DEFAULT_VALUE);
        for (Integer target : targets) {
            if (map.containsKey(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 字符串数组转化到整形数组
     */
    public static Integer[] stringArrayToIntegerArray(String[] array) {
        Integer[] array_int = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            array_int[i] = Integer.parseInt(array[i]);
        }
        return array_int;
    }

}
