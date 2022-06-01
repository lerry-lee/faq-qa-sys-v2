package com.example.qa.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;

/**
 * @ClassName: ManagementConfig
 * @Author: lerry_li
 * @CreateDate: 2021/06/06
 * @Description
 */
@Configuration
@ConfigurationProperties(prefix = "managements")
@Data
public class DialogueManagementConfig {

    private MultiTurnQA multiTurnQA;

    //redis中多轮问答树的key前缀
    private final String MQATreeKeyPrefix = "MQATreeNode_";
    //redis中question映射id的key
    private final String MQAQuestion2idKey = "MQA_question2id";

    @Data
    public static class MultiTurnQA {
        private String path;
    }

    private Index index;

    private String elasticsearchAPIPath;

    public static class Index {
        @Getter
        private String stdqStda;
        @Getter
        private String stdqSimq;
        @Getter
        @Setter
        private HashSet<String> indexNames;

        public Index() {
            this.indexNames = new HashSet<>();
        }

        public void setStdqStda(String stdqStda) {
            this.stdqStda = stdqStda;
            this.indexNames.add(this.stdqStda);
        }

        public void setStdqSimq(String stdqSimq) {
            this.stdqSimq = stdqSimq;
            this.indexNames.add(this.stdqSimq);
        }
    }
}
