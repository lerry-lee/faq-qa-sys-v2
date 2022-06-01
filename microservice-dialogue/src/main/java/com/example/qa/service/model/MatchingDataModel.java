package com.example.qa.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: MatchingDataModel
 * @Author: lerry_li
 * @CreateDate: 2021/01/17
 * @Description 匹配数据的领域模型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MatchingDataModel {

    //docId
    private String id;
    //问答知识库的qa_id
    private Integer qaId;
    //标准问
    private String standardQuestion;
    //一级类别
    private String category1;
    //二级类别
    private String category2;
    //标准答
    private String standardAnswer;
    //相似问
    private String similarQuestion;
    //相关度得分
    private Float relevanceScore;
    //相似度得分
    private Float similarityScore;
    //置信度
    private Float confidence;

}
