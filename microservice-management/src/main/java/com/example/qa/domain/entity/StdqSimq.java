package com.example.qa.domain.entity;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "stdq_simq")
@ApiModel("标准问-相似问")
public class StdqSimq {
    @Id
    @GeneratedValue(generator = "JDBC")
    private Integer id;

    /**
     * 标准问的唯一标识，用于多表数据关联一致性
     */
    @Column(name = "qa_id")
    @JsonProperty(value = "qa_id")
    private Integer qaId;

    /**
     * 标准问
     */
    @Column(name = "standard_question")
    @JsonProperty(value = "standard_question")
    private String standardQuestion;

    /**
     * 相似问
     */
    @Column(name = "similar_question")
    @JsonProperty(value = "similar_question")
    private String similarQuestion;
}