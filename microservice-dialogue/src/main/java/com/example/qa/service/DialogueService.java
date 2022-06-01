package com.example.qa.service;

import com.example.qa.service.model.DialogueStatusModel;

import java.io.IOException;

/**
 * @ClassName: DialogueService
 * @Author: lerry_li
 * @CreateDate: 2021/01/17
 * @Description 对话服务
 */
public interface DialogueService {
    /**
     * 回答用户提问
     * @param dialogueStatusModel 初始的对话状态
     * @return 完成的对话状态
     */
    DialogueStatusModel answer(DialogueStatusModel dialogueStatusModel) throws IOException;
}
