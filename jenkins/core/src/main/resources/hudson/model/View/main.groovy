package hudson.model.View;

t=namespace(lib.JenkinsTagLib)
st=namespace("jelly:stapler")

if (items == null) {
    p(_('broken'))
} else if (items.isEmpty()) {
    if (app.items.size() != 0) {
        set("views",my.owner.views);
        set("currentView",my);
        include(my.owner.viewsTabBar, "viewTabs");
    }
    include(my,"noJob.jelly");
} else {
    /**
     * 2020-04-30 add by wanghf
     *  限制 All 视图显示的条数， 具体看AllView.getPageItems()
     */
    t.projectView(jobs: items,pageJobs: pageItems ,showViewTabs: true, columnExtensions: my.columns, indenter: my.indenter, itemGroup: my.owner.itemGroup) {
        set("views",my.owner.views);
        set("currentView",my);
        if (my.owner.class == hudson.model.MyViewsProperty.class) {
            include(my.owner?.myViewsTabBar, "myViewTabs");
        } else {
            include(my.owner.viewsTabBar,"viewTabs");
        }
    }
}