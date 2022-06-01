package com.example.qa.rocketmq;

import com.example.qa.dao.FeedbackMapper;
import com.example.qa.domain.entity.Feedback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: lerry_li
 * @CreateDate: 2021/06/14
 * @Description
 */
@Service
@RocketMQMessageListener(consumerGroup = "add-feedback", topic = "add-feedback")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class AddFeedbackListener implements RocketMQListener<List<String>> {

    private final FeedbackMapper feedbackMapper;

    @Override
    public void onMessage(List<String> question_type_reason) {
        //从MQ受到待消费的消息，这里是识别有问题的quesiton、type和reason
        Feedback feedback = Feedback.builder()
                .question(question_type_reason.get(0))
                .type(question_type_reason.get(1))
                .reason(question_type_reason.size() >= 3 ? question_type_reason.get(2) : null)
                .build();
        //接下来需要将这些插入到表中
        this.feedbackMapper.insert(feedback);
        log.info("从MQ获取消息：识别有问题的\"{}\"，已插入到MySQL表feedback中", feedback.getQuestion());
    }
}
