package com.example.qa.domain.entity;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "stdq_stda")
@ApiModel("标准问-标准答")
public class StdqStda {
    @Id
    @GeneratedValue(generator = "JDBC")
    private Integer id;

    /**
     * 标准问-标准答的唯一标识id，确保多表数据关联一致性
     */
    @Column(name = "qa_id")
    @JsonProperty(value = "qa_id")
    private Integer qaId;

    /**
     * 一级类别
     */
    private String category1;

    /**
     * 二级类别
     */
    private String category2;

    /**
     * 标准问
     */
    @Column(name = "standard_question")
    @JsonProperty(value = "standard_question")
    private String standardQuestion;

    /**
     * 标准答
     */
    @Column(name = "standard_answer")
    @JsonProperty(value = "standard_answer")
    private String standardAnswer;
}