package com.michelin.cio.hudson.plugins.rolestrategy;

/**
 * 2020-04-20
 * 调度系统中的超级用户
 */
public enum GlobalRole {
    /**
     * 超级管理员
     */
    ADMIN("admin"),

    /**
     * 对作业只有只读权限
     */
    READ("read-only"),
    /**
     * 超级开发者
     */
    SUPERDEV("super-dev");

    private String roleName;

    GlobalRole(String roleName) {
        this.roleName = roleName;
    }


    public String getRoleName() {
        return roleName;
    }
}
