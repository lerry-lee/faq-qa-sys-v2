# 管理微服务

## 服务名
  
`microservice-management`

## 功能

负责对后台数据的管理，具体包含以下功能：

- 全量同步MySQL的stdq_stda表/stdq_simq表中的数据到Elasticsearch同名的索引中

- 批量插入数据到stdq_stda表/stdq_simq表，同时同步到Elasticsearch

- 更新一条数据到stdq_stda表/stdq_simq表，同时同步到Elasticsearch

- 删除一条数据到stdq_stda表/stdq_simq表，同时同步到Elasticsearch

- 插入未识别的问题到unknown_question表

## 对外开放端口
  
`88xx`,当启动多个微服务实例时，建议在该范围内设置端口号

## 数据库设计

### 标准问-标准答 stdq_stda

| 字段名 | 字段类型 | 是否可为空 | 键 | 注释 |
| ------| ------ | ------ | ------| ------ |
| id | int(11) | NO | PRI |  |
| qa_id | int(11) | NO | UNI | 标准问-标准答的唯一标识id，确保多表数据关联一致性 |
| standard_question | text | NO |  | 标准问 |
| category1 | varchar(255) | NO |  | 一级类别 |
| category2 | varchar(255) | NO |  | 二级类别 |
| standard_answer | text | NO |  | 标准答 |

### 标准问-相似问 stdq_simq

| 字段名 | 字段类型 | 是否可为空 | 键 | 注释 |
| ------| ------ | ------ | ------| ------ |
| id | int(11) | NO | PRI |  |
| qa_id | int(11) | NO |  | 标准问-标准答的唯一标识id，确保多表数据关联一致性 |
| standard_question | text | NO |  | 标准问 |
| similar_question | text | NO |  | 相似问 |

### 反馈表 feedback

| 字段名      | 字段类型        | 是否可为空 | 键 | 注释 |
|----------|-------------| ------ | ------| ------ |
| id       | int(11)     | NO | PRI |  |
| question | text        | NO |  | 用户问题 |
| type     | varchar(50) | NO |  | 反馈类型 |
| reason   | text        | NO |  | 反馈原因 |

### 历史记录表 history

| 字段名          | 字段类型       | 是否可为空 | 键 | 注释 |
|--------------|------------| ------ | ------| ---- |
| id           | int(11)    | NO | PRI |  |
| msg_id       | varchar(50)  | NO |  | 消息id，区分不同用户 |
| type         | varchar(50) | NO |  | 类型 |
| content_text | text       | NO |  | 内容 |
| position     | varchar(50)  | NO |  | 位置，区分机器人还是用户 |
| created_at   | bigint     | NO |  | 创建时间 |

### 表关系说明

标准问-标准答`stdq_stda`和标准问-相似问`stdq_simq`存在依赖关系如下，
- `stdq_stda`中的唯一的`qa_id`对应一条标准问-标准答数据  
  对于每一个`qa_id`的数据，在`stdq_simq`中默认需要包含一条相同`qa_id`和标准问的数据，其中相似问和标准问相同（因为检索的是相似问，当一条标准问没有人工创建相似问时，需要将标准问作为相似问检索，否则会找不到该数据）。

- `stdq_simq`中的`qa_id`必须全部且仅为`stdq_stda`中的`qa_id`

- `stdq_simq`中的相似问字段应是唯一的  
  可以人工创建多个某`qa_id`对应的标准问的相似问，但重复的相似问属于冗余数据。

# 启动步骤

在启动微服务应用之前，需要确保对应的数据库和微服务组件已经正常启动

## 1. 配置数据库服务器

确保对应的数据库服务器MySQL,Elasticsearch,Redis已经正常启动，并修改`application.yml`中对应的服务器地址

```yaml
spring:
  #mysql连接
  datasource:
    url: jdbc:mysql://localhost:3306/qadb?characterEncoding=utf8&useSSL=false
    username: root
    password: root
  #redis连接
  redis:
    host: 127.0.0.1
    port: 6379
    database: 15
#es连接
elasticsearch:
  #节点1(若有集群，可往后追加，并修改配置类ElasticsearchConfig.java)
  node-1:
    host: localhost
    port: 9200
```

## 2. 配置Nacos服务器

确保服务发现与注册中心Nacos服务器已经正常启动，并修改`application.yml`中对应的服务器地址

```yaml
spring:
  cloud:
    nacos:
      discovery:
        # 指定nacos server的地址
        server-addr: localhost:8848
```

## 3. 配置Sentinel控制台

确保Sentinel控制台已经正常启动，并修改`application.yml`中对应的控制台地址

```yaml
spring:
  cloud:
    sentinel:
  #    filter:
  #      # 打开/关闭对springMVC端点的保护
  #      enabled: true
      transport:
        # 指定sentinel控制台
        dashboard: localhost:8080
```

## 4. 配置RocketMQ Name Server的地址

确保Rocket MQ Name Server已经正常启动，并修改`application.yml`中对应的地址

```yaml
rocketmq:
  name-server: localhost:9876
```

## 5. 配置常见的用户自定义参数

编辑`application.yml`

```yaml
#管理配置选项
managements:
  #es索引，用于和MySQL表同步，默认mysql表和es索引同名
  index:
    #标准问-标准答
    stdq-stda: stdq_stda
    #标准问-相似问
    stdq-simq: stdq_simq
  #存放常用elasticsearch API的路径
  elasticsearch-API-path: data/elasticsearch_API
  #多轮问答树路径
  multi-turn-qa:
    path: data/multi_turn_qa
```

## 6. 启动微服务

运行jar包或运行启动类`MicroserviceManagementApplication.java`，启动成功后可以对接口进行测试
