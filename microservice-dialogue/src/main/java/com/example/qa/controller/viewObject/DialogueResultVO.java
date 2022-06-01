package com.example.qa.controller.viewObject;

import com.example.qa.domain.dto.Answer;
import com.example.qa.domain.dto.RecomQuestion;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: DialogueResultVO
 * @Author: lerry_li
 * @CreateDate: 2021/01/13
 * @Description 对话结果的视图对象
 */
@Data
public class DialogueResultVO {
    //用户问题
    private String question;
    //回答：回答的内容、对应的相似问，标准问、置信度
    private Answer answer;
    //其他相似问题
    private List<RecomQuestion> recomQuestions;

    public DialogueResultVO() {
        this.answer = new Answer();
        this.recomQuestions = new ArrayList<>();
    }
}
