package com.example.qa.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: Answer
 * @Author: lerry_li
 * @CreateDate: 2021/01/12
 * @Description 回答（对应置信度最高的问题）
 */
@Data
public class Answer {
    //答案内容
    private String content;
    //对应的相似问
    private String simQ;
    //相似问对应的标准问
    private String stdQ;
    //置信度
    private Float confidence;
    //多轮问答选项
    private List<String> options;

    public Answer() {
        this.options = new ArrayList<>();
    }
}
