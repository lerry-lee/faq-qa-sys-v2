package com.example.qa.service.model;

import com.example.qa.domain.dto.Answer;
import com.example.qa.domain.dto.RecomQuestion;
import com.example.qa.domain.dto.MQATreeNode;
import com.example.qa.response.CodeMsg;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: DialogueStatusModel
 * @Author: lerry_li
 * @CreateDate: 2021/01/13
 * @Description 对话状态的领域模型
 */
@Data
@AllArgsConstructor
@Builder
public class DialogueStatusModel implements Serializable {
    //用户ID
    private Integer userId;
    //机器人ID
    private Integer robotId;
    //用户问题
    private String question;
    //本轮问答的状态码和解释
    private CodeMsg codeMsg;
    //回答：回答的内容、对应的相似问、标准问、置信度
    private Answer answer;
    //其他相似问题
    private List<RecomQuestion> recomQuestions;
    //是否处于多轮问答中
    private boolean isMultiRound;
    //多轮问答树节点
    private MQATreeNode MQATreeNode;
    //历史记录
    private List<List<String>> history;

    public DialogueStatusModel() {
        this.answer = new Answer();
        this.recomQuestions = new ArrayList<>();
        this.MQATreeNode = new MQATreeNode();
        this.history = new ArrayList<>();
    }
}
