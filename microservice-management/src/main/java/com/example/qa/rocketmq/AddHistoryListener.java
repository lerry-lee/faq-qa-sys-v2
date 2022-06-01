package com.example.qa.rocketmq;

import com.example.qa.dao.HistoryMapper;
import com.example.qa.domain.entity.History;
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
@RocketMQMessageListener(consumerGroup = "add-history", topic = "add-history")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class AddHistoryListener implements RocketMQListener<List<String>> {

    private final HistoryMapper historyMapper;

    @Override
    public void onMessage(List<String> historys) {
        //从MQ受到待消费的消息，这里是识别有问题的quesiton、type和reason
        History history = History.builder()
                .msgId(historys.get(0))
                .type(historys.get(1))
                .contentText(historys.get(2))
                .position(historys.get(3))
                .createdAt(Long.valueOf(historys.get(4)))
                .build();
        //接下来需要将这些插入到表中
        this.historyMapper.insert(history);
        log.info("从MQ获取消息：聊天记录\"{}\"，已插入到MySQL表history中", history.getContentText());
    }
}
