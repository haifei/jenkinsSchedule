/*
 * The MIT License
 *
 * Copyright (c) 2011, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.model;

import hudson.model.ItemGroupMixIn;
import hudson.model.View;
import hudson.model.ViewGroup;
import java.util.Locale;
import java.util.logging.Level;

import jenkins.model.Jenkins;
import org.kohsuke.stapler.export.Exported;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Implements {@link ViewGroup} to be used as a "mix-in".
 * Not meant for a consumption from outside {@link ViewGroup}s.
 *
 * <h2>How to use this class</h2>
 * <ol>
 * <li>
 * Create three data fields in your class:
 * <pre>
 * private String primaryView;
 * private CopyOnWriteArrayList&lt;View> views;
 * private ViewsTabBar viewsTabBar;
 * </pre>
 * <li>
 * Define a transient field and store ViewGroupMixIn subype, then wire up getters and setters:
 * <pre>
 * private transient ViewGroupMixIn = new ViewGroupMixIn() {
 *     List&lt;View> views() { return views; }
 *     ...
 * }
 * </pre>
 * @author Kohsuke Kawaguchi
 * @see ItemGroupMixIn
 */
public abstract class ViewGroupMixIn {
    private final ViewGroup owner;

    /**
     * Returns all the views. This list must be concurrently iterable.
     */
    protected abstract List<View> views();
    protected abstract String primaryView();
    protected abstract void primaryView(String newName);

    protected ViewGroupMixIn(ViewGroup owner) {
        this.owner = owner;
    }

    public void addView(View v) throws IOException {
        v.owner = owner;
        views().add(v);
        owner.save();
    }

    public boolean canDelete(View view) {
        return !view.isDefault();  // Cannot delete primary view
    }

    public synchronized void deleteView(View view) throws IOException {
        if (views().size() <= 1)
            throw new IllegalStateException("Cannot delete last view");
        views().remove(view);
        owner.save();
    }

    public View getView(String name) {
        for (View v : views()) {
            if(v.getViewName().equals(name))
                return v;
        }
        if (name != null && !name.equals(primaryView())) {
            // Fallback to subview of primary view if it is a ViewGroup
            View pv = getPrimaryView();
            if (pv instanceof ViewGroup)
                return ((ViewGroup)pv).getView(name);
            if (pv instanceof AllView && AllView.DEFAULT_VIEW_NAME.equals(pv.name)) {
                // JENKINS-38606: primary view is the default AllView, is somebody using an old link to localized form?
                for (Locale l : Locale.getAvailableLocales()) {
                    if (name.equals(Messages._Hudson_ViewName().toString(l))) {
                        // why yes they are, let's keep that link working
                        return pv;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets the read-only list of all {@link View}s.
     */
    @Exported
    public Collection<View> getViews() {
        List<View> orig = views();
        List<View> copy = new ArrayList<View>(orig.size());
        for (View v : orig) {
            /**
             * @Author wanghf
             * @Description  修改依据用户创建的视图显示+ all 视图
             * @Date 2020/1/14 下午2:28
             * @Param
             * @return
             */
/*            if (v.hasPermission(View.READ))
                copy.add(v);*/

            if(v.getViewName().equals(AllView.DEFAULT_VIEW_NAME)||
                    v.getCreator()!=null && v.getCreator().equals(User.current().getId())){
                if (v.hasPermission(View.READ)){
                    copy.add(v);
                }
            }
        }
        Collections.sort(copy, View.SORTER);
        return copy;
    }

    /**
     * Returns the primary {@link View} that renders the top-page of Hudson.
     */
    @Exported
    public View getPrimaryView() {
        View v = getView(primaryView());
        if(v==null) // fallback
            v = views().get(0);
        return v;
    }

    public void onViewRenamed(View view, String oldName, String newName) {
        // If this view was the default view, change reference
        if (oldName.equals(primaryView())) {
            primaryView(newName);
        }
    }
}
