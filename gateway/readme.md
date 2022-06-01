# 服务网关

使用Spring Cloud Gateway，它可以为微服务架构提供一种简单有效统一的API路由管理方式，底层基于WebFlux框架实现的，而WebFlux框架底层则使用了高性能的Reactor模式通信框架Netty

# 启动网关

运行jar包或启动类，成功后可通过网关访问微服务

# 具体的配置步骤

Spring Cloud Alibaba和各组件的[版本说明](https://github.com/alibaba/spring-cloud-alibaba/wiki/%E7%89%88%E6%9C%AC%E8%AF%B4%E6%98%8E)

1. 新建一个微服务项目作为网关

如用IDEA新建Spring Boot应用，初始化时勾选gateway

2. 加依赖

    1. 添加Spring Cloud Alibaba依赖
   ```xml
   <dependencyManagement>
       <dependencies>
           <!--整合spring cloud-->
           <dependency>
               <groupId>org.springframework.cloud</groupId>
               <artifactId>spring-cloud-dependencies</artifactId>
               <version>Hoxton.SR8</version>
               <type>pom</type>
               <scope>import</scope>
           </dependency>
           <!--整合spring cloud alibaba-->
           <dependency>
               <groupId>com.alibaba.cloud</groupId>
               <artifactId>spring-cloud-alibaba-dependencies</artifactId>
               <version>2.2.5.RELEASE</version>
               <type>pom</type>
           <scope>import</scope>
           </dependency>
       </dependencies>
   </dependencyManagement>
   ```

    2. 添加Nacos依赖
   ```xml
   <dependencies>
       <!--整合Nacos-->
       <dependency>
           <groupId>com.alibaba.cloud</groupId>
           <artifactId>spring-cloud-starter-alibaba-Nacos-discovery</artifactId>
       </dependency>
   </dependencies>
   ```

    3. 添加actuator依赖
   ```xml
   <!--actuator-->
   <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
   </dependency>
   ```

3. 写配置

编辑`application.yml`

```yaml
server:
  port: 8600
spring:
  application:
    # 注册到nacos的服务名称
    name: gateway
  cloud:
    nacos:
      discovery:
        #nacos server地址
        server-addr: localhost:8848
    gateway:
      discovery:
        locator:
          #让gateway通过服务发现组件找到其他的微服务
          enabled: true
#actuator暴露健康端点
management:
  endpoints:
    web:
      exposure:
        include: '*'
  endpoint:
    health:
      show-details: always
```