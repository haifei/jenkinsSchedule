package com.michelin.cio.hudson.plugins.rolestrategy;

/**
 * @author wanghf
 * @date 2020/5/9
 * @desc
 */
public enum ProjectRole {
    /**
     * 项目开发角色, 对自己业务组有查看权限
     */
    ProjectDEV("project_dev");

    private String roleName;

    ProjectRole(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return roleName;
    }
}
