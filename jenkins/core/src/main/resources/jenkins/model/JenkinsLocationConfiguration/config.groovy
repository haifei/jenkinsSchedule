package jenkins.model.JenkinsLocationConfiguration

import hudson.Functions

def f=namespace(lib.FormTagLib)

f.section(title:_("Jenkins Location")) {
    f.entry(title:_("Jenkins URL"), field:"url") {
        f.textbox(default: Functions.inferHudsonURL(request))
    }
    f.entry(title:_("System Admin e-mail address"), field:"adminAddress") {
        f.textbox()
    }
    f.entry(title:_("视图显示的条数"), field:"viewPageSize") {
        f.textbox()
    }
}
