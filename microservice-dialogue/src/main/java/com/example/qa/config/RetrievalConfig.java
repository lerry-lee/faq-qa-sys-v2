package com.example.qa.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;

/**
 * @ClassName: RetrievalServiceConfig
 * @Author: lerry_li
 * @CreateDate: 2021/01/17
 * @Description
 */
@Configuration
@ConfigurationProperties(prefix = "retrieval")
@Data
public class RetrievalConfig {

    private Index index;
    private Search search;

    public static class Index {
        @Getter
        private String stdQStdA;
        @Getter
        private String stdQSimQ;
        @Getter
        @Setter
        private HashSet<String> indexNames;

        public Index() {
            this.indexNames = new HashSet<>();
        }

        public void setStdQStdA(String stdQStdA) {
            this.stdQStdA = stdQStdA;
            this.indexNames.add(this.stdQStdA);
        }

        public void setStdQSimQ(String stdQSimQ) {
            this.stdQSimQ = stdQSimQ;
            this.indexNames.add(this.stdQSimQ);
        }
    }

    @Data
    public static class Search {
        private Integer size;
    }
}
