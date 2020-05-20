# JenkinsSchedule
JenkinsSchedule 是基于jenkins 实现的分布式调度平台

# 特性
  * 简单易用
  * 支持分布式优先调度
  * 基于状态、条件的依赖触发
  * 基于角色实现模块功能、作业权限的控制
  * 实现任务节点当次执行与任务节点下游自动执行
  * 开放式的告警接口
  
# 编译
## 配置mavn 镜像为 Jenkins 
修改 ${MAVN_HOME}/conf/setttings.xml
```
</pluginGroups>
    <pluginGroup>org.jenkins-ci.tools</pluginGroup>
</pluginGroups>
  
  
<mirrors>
     <mirror>
      <id>repo.jenkins-ci.org</id>
      <url>https://repo.jenkins-ci.org/public/</url>
      <mirrorOf>m.g.o-public</mirrorOf>
    </mirror>
<mirrors>
    
   
    
</profiles>
    <profile>
      <id>jenkins</id>
      <activation>
        <activeByDefault>true</activeByDefault> <!-- change this to false, if you don't like to have it on per default -->
      </activation>
      <repositories>
        <repository>
          <id>repo.jenkins-ci.org</id>
          <url>https://repo.jenkins-ci.org/public/</url>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>repo.jenkins-ci.org</id>
          <url>https://repo.jenkins-ci.org/public/</url>
        </pluginRepository>
      </pluginRepositories>
    </profile>
</profiles>
```

## 执行编译
```
git clone https://github.com/apache/flink.git
cd flink
mvn clean package -DskipTests 
```

# Getting Started
## 启动jenkins-master(主服务)
```
cd flink/bin
sh jenkins-master.sh start
```

## 启动jenkins-slave 
```
cd flink/bin
sh jenkins-slave.sh start
```

* 浏览器访问  [http://localhost:8080](http://localhost:8080), 默认管理员用户:  admin/adminQAZ
![登录界面](https://github.com/haifei/jenkinsSchedule/blob/master/build/images/login.jpg)
![主界面](https://github.com/haifei/jenkinsSchedule/blob/master/build/images/main.jpg)
![作业界面](https://github.com/haifei/jenkinsSchedule/blob/master/build/images/job.jpg)
        
# 文档
   
# 关于
  * Jenkins ：[https://www.jenkins.io/](https://www.jenkins.io/) 
  
# 问题反馈
  * 报告issue: [https://github.com/haifei/jenkinsSchedule/issues](https://github.com/haifei/jenkinsSchedule/issues)
  

