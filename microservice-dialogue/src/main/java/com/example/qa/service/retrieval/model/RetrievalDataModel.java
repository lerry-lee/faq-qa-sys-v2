package com.example.qa.service.retrieval.model;

import lombok.Data;

/**
 * @ClassName: RetrievalDataModel
 * @Author: lerry_li
 * @CreateDate: 2021/01/17
 * @Description 检索数据的领域模型
 */
@Data
public class RetrievalDataModel {
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
}
