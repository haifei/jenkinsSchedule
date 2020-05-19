package hudson.model.listeners;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.User;
import org.jvnet.tiger_types.Types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author wanghf
 * @date 2020/4/20
 * @desc
 */
public class UserListener implements ExtensionPoint {

    private static final Logger LOGGER = Logger.getLogger(UserListener.class.getName());

    public void addUser(User user) {
    }

    public static void fireAddUser(User user) {
        for (UserListener l : all()) {
            try {
                l.addUser(user);
            } catch (Throwable e) {
                report(e);
            }
        }
    }

    /**
     * Returns all the registered {@link UserListener}s.
     */
    public static ExtensionList<UserListener> all() {
        return ExtensionList.lookup(UserListener.class);
    }

    private static void report(Throwable e) {
        LOGGER.log(Level.WARNING, "UserListener failed",e);
    }
}
