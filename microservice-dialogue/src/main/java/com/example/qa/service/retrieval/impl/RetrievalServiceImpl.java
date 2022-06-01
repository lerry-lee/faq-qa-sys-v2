package com.example.qa.service.retrieval.impl;

import com.example.qa.config.ElasticsearchConfig;
import com.example.qa.config.RetrievalConfig;
import com.example.qa.service.retrieval.RetrievalService;
import com.example.qa.service.retrieval.model.RetrievalDataModel;
import com.example.qa.util.LogUtil;
import com.example.qa.util.RestClientUtil;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: RetrievalServiceImpl
 * @Author: lerry_li
 * @CreateDate: 2021/01/17
 * @Description
 */
@Service
public class RetrievalServiceImpl implements RetrievalService {

    @Autowired
    private ElasticsearchConfig ESConfig;

    @Autowired
    private RetrievalConfig RetrievalConfig;

    @Autowired
    private LogUtil logUtil;

    @Autowired
    private RestClientUtil restClientUtil;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 搜索一个问题，返回最相关的相似问及相关的标准问等信息
     *
     * @param question 待搜索的问题
     * @return RetrievalDataModel
     */
    @Override
    public List<RetrievalDataModel> searchSimilarQuestions(String question) throws IOException {

        List<RetrievalDataModel> retrievalDataModelList = new ArrayList<>(RetrievalConfig.getSearch().getSize());
        RestHighLevelClient client;
        //初始化rest client
        try {
            client = restClientUtil.getClient(ESConfig.getNode1().getHost(), ESConfig.getNode1().getPort());
        } catch (ElasticsearchException e) {
            e.printStackTrace();
            return null;
        }
        //创建searchRequest
        SearchRequest request = restClientUtil.getSearchRequest(RetrievalConfig.getIndex().getStdQSimQ(), "similar_question", question, RetrievalConfig.getSearch().getSize());

        //以同步方式搜索问题，等待搜索结果
        SearchResponse response;
        try {
            response = client.search(request, RequestOptions.DEFAULT);
        } catch (ElasticsearchException e) {
            e.printStackTrace();
            return null;
        }
        //状态
        RestStatus status = response.status();
        //耗时
        TimeValue took = response.getTook();

        SearchHits hits = response.getHits();
        long totalHits = hits.getTotalHits();
        if (totalHits == 0) {
            logUtil.recordUnrecognizedQuestion().info("未识别的问题\"{}\"", question);
            //插入未识别的问题到数据库，异步执行
            List<String> question_type_reason = new ArrayList<>(3);
            question_type_reason.add(question);
            question_type_reason.add("未识别");
            this.rocketMQTemplate.convertAndSend("add-feedback", question_type_reason);
            return retrievalDataModelList;
        }
        //遍历docs中的数据
        SearchHit[] searchHits = hits.getHits();

        for (SearchHit hit : searchHits) {
            //docId
            String id = hit.getId();
            //相关度得分
            float score = hit.getScore();
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            RetrievalDataModel retrievalDataModel = new RetrievalDataModel();
            retrievalDataModel.setId(id);
            retrievalDataModel.setRelevanceScore(score);
            retrievalDataModel.setStandardQuestion((String) sourceAsMap.get("standard_question"));
            retrievalDataModel.setSimilarQuestion((String) sourceAsMap.get("similar_question"));
            Integer qaId = (Integer) sourceAsMap.get("qa_id");
            retrievalDataModel.setQaId(qaId);

            //根据qaId搜索问答知识库，一个qaId只能对应一个标准问
            request = restClientUtil.getSearchRequest(RetrievalConfig.getIndex().getStdQStdA(), "qa_id", qaId, 1);
            try {
                response = client.search(request, RequestOptions.DEFAULT);
            } catch (ElasticsearchException e) {
                e.printStackTrace();
                return null;
            }

            SearchHit hit_ = response.getHits().getHits()[0];
            //遍历docs中的数据
            Map<String, Object> sourceAsMap_ = hit_.getSourceAsMap();
            retrievalDataModel.setCategory1((String) sourceAsMap_.get("category1"));
            retrievalDataModel.setCategory2((String) sourceAsMap_.get("category2"));
            retrievalDataModel.setStandardAnswer((String) sourceAsMap_.get("standard_answer"));

            retrievalDataModelList.add(retrievalDataModel);
        }
        client.close();
        return retrievalDataModelList;
    }

    /**
     * 索引docs（插入docs到索引中）
     *
     * @param indexName   索引名
     * @param jsonMapList docs
     * @return 成功操作的数量
     */
    @Override
    public Integer insertDocs(String indexName, List<Map<String, Object>> jsonMapList) throws IOException {
        RestHighLevelClient client = restClientUtil.getClient(ESConfig.getNode1().getHost(), ESConfig.getNode1().getPort());
        int account = 0;
        for (Map<String, Object> jsonMap : jsonMapList) {
            IndexRequest request = restClientUtil.getIndexRequest(indexName, jsonMap);
            client.index(request, RequestOptions.DEFAULT);
            account++;
        }
        client.close();
        logUtil.traceAll().info("成功插入{}个数据到索引{}中", account, indexName);

        return account;
    }
}
