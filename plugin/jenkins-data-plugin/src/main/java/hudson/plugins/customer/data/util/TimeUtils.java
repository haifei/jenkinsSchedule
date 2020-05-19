package hudson.plugins.customer.data.util;

/**
 * 2020-04-12 add by wanghf
 */
public class TimeUtils {

    private static final long DAY = 1000 * 60 * 60 * 24;
    private static final long HOUR = 1000 * 60 * 60;
    private static final long MINUTE = 1000 * 60;
    private static final long SECONDS = 1000;

    public static String parse(Double duration) {

        if (duration <= 0)
            return "";

        //day
        if (duration > DAY) {
            return String.format("%.1f", duration / DAY) + " days";
        }
        //hour
        else if (duration > HOUR) {
            return String.format("%.1f", duration / HOUR) + " hours";
        }
        //minute
        else if (duration > MINUTE) {
            return String.format("%.1f", duration / MINUTE) + " mins";
        }
        //seconds
        else {
            return String.format("%.1f", duration / SECONDS) + " secs";
        }

    }

}
