package com.example.qa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName: SimilarityServiceConfig
 * @Author: lerry_li
 * @CreateDate: 2021/01/17
 * @Description
 */
@Configuration
@ConfigurationProperties(prefix = "similarity")
@Data
public class SimilarityConfig {
    private Model model1;

    @Data
    public static class Model {
        private String requestUrl;
    }
}
