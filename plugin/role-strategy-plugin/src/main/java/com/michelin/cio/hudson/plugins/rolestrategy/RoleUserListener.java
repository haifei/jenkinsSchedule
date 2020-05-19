package com.michelin.cio.hudson.plugins.rolestrategy;

import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.model.User;
import hudson.model.listeners.UserListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * @author wanghf
 * @date 2020/4/20
 * @desc
 */
@Extension
public  final  class RoleUserListener extends UserListener {
    private static final Logger logger = LoggerFactory.getLogger(RoleUserListener.class);

    @Override
    public void addUser(User user) {
        RoleBasedAuthorizationStrategy instance = RoleBasedAuthorizationStrategy.getInstance();
        if(!instance.isContainsUser(user.getId())){
            try {
                //1. 如果用户所在业务组不是超级用户组则赋予,则赋予 全局角色 readOnly,以及对应的业务项目组
                if(instance.getSuperBusGroup()!=null){
                    List<String> superGroupList=Lists.newArrayList(instance.getSuperBusGroup().split(","));
                    if(!superGroupList.contains(user.getDeptCode())&&!superGroupList.contains(user.getDeptName())){
                        instance.grantUserRole(instance.GLOBAL, GlobalRole.READ.getRoleName(),user.getId());
                        //1.1 判断项目角色中是否已存在 该业务组角色
                        Role role=instance.getProjectRoleByBusGroup(user.getDeptCode(),true);
                        if(role!=null){
                            instance.grantRole(instance.PROJECT, role,user.getId());
                        }
                    }
                }else{
                    //2. 其他情况都赋予   全局角色 super_dev
                    instance.grantUserRole(instance.GLOBAL, GlobalRole.SUPERDEV.getRoleName(),user.getId());
                }

            } catch (IOException e) {
                logger.error("===== addUser error,"+e);
                e.printStackTrace();
            }
        }
    }
}
