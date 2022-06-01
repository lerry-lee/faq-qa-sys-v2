package com.example.qa.service.similarity.impl;

import com.alibaba.fastjson.JSON;
import com.example.qa.config.SimilarityConfig;
import com.example.qa.service.similarity.SimilarityService;
import com.example.qa.util.HttpUtil;
import com.example.qa.util.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: SimilarityServiceImpl
 * @Author: lerry_li
 * @CreateDate: 2021/01/17
 * @Description
 */
@Service
public class SimilarityServiceImpl implements SimilarityService {

    @Autowired
    private SimilarityConfig similarityConfig;

    @Autowired
    private LogUtil logUtil;

    @Autowired
    private HttpUtil httpUtil;

    /**
     * 计算两个文本列表的相似度
     * 说明：一一对应
     *
     * @param textList1 文本1的列表
     * @param textList2 文本2的列表
     * @return List<SimilarityDataModel>
     */
    @Override
    public List<Float> similarityCalculation(List<String> textList1, List<String> textList2) {
        List<Float> scores = new ArrayList<>();
        //输入处理
        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        for (int i = 0; i < textList1.size(); i++) {
            params.add("text_list1", textList1.get(i));
            params.add("text_list2", textList2.get(i));
        }

        //调用深度学习模型计算
        System.setProperty("java.net.preferIPv6Addresses", "true");
        String responseBody = httpUtil.sendHttpPostRequest(similarityConfig.getModel1().getRequestUrl(), params);

        if (responseBody == null) {
            logUtil.traceAll().error("responseBody is null");
            return scores;
        }
        Map<String, List<BigDecimal>> data = (Map<String, List<BigDecimal>>) JSON.parse(responseBody);
        List<BigDecimal> origin_scores = data.get("scores");
        for (BigDecimal origin_score : origin_scores) {
            scores.add(origin_score.floatValue());
        }
        return scores;
    }
}
