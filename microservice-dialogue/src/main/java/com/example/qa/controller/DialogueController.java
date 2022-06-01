package com.example.qa.controller;

import com.example.qa.config.DialogueConfig;
import com.example.qa.controller.viewObject.DialogueResultVO;
import com.example.qa.response.CommonReturnType;
import com.example.qa.service.DialogueService;
import com.example.qa.service.model.DialogueStatusModel;
import com.example.qa.util.LogUtil;
import com.example.qa.util.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @ClassName: DialogueController
 * @Author: lerry_li
 * @CreateDate: 2021/01/17
 * @Description
 */
@Api(tags = "对话")
@RestController
@RequestMapping("/dialogue")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")   //处理跨域请求
public class DialogueController {

    private final static String ContentType = "application/x-www-form-urlencoded";

    @Autowired
    private DialogueConfig dialogueConfig;

    @Autowired
    private DialogueService dialogueService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private LogUtil logUtil;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @ApiOperation("提问问题")
    @GetMapping(value = "/ask")
    public CommonReturnType ask(@ApiParam("用户问题") @RequestParam(name = "question") String question,
                                @ApiParam("用户id") @RequestParam(name = "user_id") Integer userId,
                                @ApiParam("机器人id") @RequestParam(name = "robot_id", required = false, defaultValue = "1") Integer robotId) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        //异步插入聊天记录（user）
        sendMessage("msg_" + userId, question, "text", "right", new Date().getTime());

        //首先检查redis中有没有用户的对话状态
        DialogueStatusModel statusModel = (DialogueStatusModel) redisUtil.get(dialogueConfig.getDialogueStatusKeyPrefix() + userId);
        //没有则为用户创建一个对话状态
        if (statusModel == null) {
            statusModel = new DialogueStatusModel();
            statusModel.setUserId(userId);
        }
        //有则更新问题和robotId
        statusModel.setQuestion(question);
        statusModel.setRobotId(robotId);
        //调用service回答
        statusModel = dialogueService.answer(statusModel);
        //更新对话状态到redis
        String key = dialogueConfig.getDialogueStatusKeyPrefix() + statusModel.getUserId();
        redisUtil.set(key, statusModel);
        redisUtil.expire(key, dialogueConfig.getStatus().getExpireTime());
        //创建视图对象
        DialogueResultVO vo = new DialogueResultVO();
        BeanUtils.copyProperties(statusModel, vo);

        stopWatch.stop();
        logUtil.traceAll().info("(userId={},robotId={})当前用户提问\"{}\"，处理耗时{}ms", userId, robotId, question, stopWatch.getTotalTimeMillis());

        //异步插入聊天记录（robot）
        sendMessage("msg_" + userId, statusModel.getAnswer().getContent(), "text", "left", new Date().getTime());

        return CommonReturnType.create(vo, statusModel.getCodeMsg());
    }

    /**
     * 发送消息到rocketmq
     *
     */
    private void sendMessage(String msgId, String question, String type, String position, Long createdAt) {
        List<String> historys = new ArrayList<>(5);
        historys.add(msgId);
        historys.add(type);
        historys.add(question);
        historys.add(position);
        historys.add(String.valueOf(createdAt));
        this.rocketMQTemplate.convertAndSend("add-history", historys);
    }
}
