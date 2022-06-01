package com.example.qa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName: ElasticsearchConfig
 * @Author: lerry_li
 * @CreateDate: 2021/01/17
 * @Description
 */
@Configuration
@ConfigurationProperties(prefix = "elasticsearch")
@Data
public class ElasticsearchConfig {
    private Node node1;

    @Data
    public static class Node {
        private String host;
        private Integer port;
    }
}
