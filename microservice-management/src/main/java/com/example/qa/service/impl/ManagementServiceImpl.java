package com.example.qa.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.example.qa.config.ElasticsearchConfig;
import com.example.qa.config.DialogueManagementConfig;
import com.example.qa.dao.FeedbackMapper;
import com.example.qa.dao.HistoryMapper;
import com.example.qa.dao.StdqSimqMapper;
import com.example.qa.dao.StdqStdaMapper;
import com.example.qa.domain.dto.MQATreeNode;
import com.example.qa.domain.entity.Feedback;
import com.example.qa.domain.entity.History;
import com.example.qa.domain.entity.StdqStda;
import com.example.qa.domain.entity.StdqSimq;
import com.example.qa.service.ManagementService;
import com.example.qa.util.LogUtil;
import com.example.qa.util.RedisUtil;
import com.example.qa.util.RestClientUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: ManagementServiceImpl
 * @Author: lerry_li
 * @CreateDate: 2021/01/17
 * @Description
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ManagementServiceImpl implements ManagementService {

    private final ElasticsearchConfig ESConfig;
    private final DialogueManagementConfig dialogueManagementConfig;
    private final LogUtil logUtil;
    private final RestClientUtil restClientUtil;
    private final RedisUtil redisUtil;


    private final StdqSimqMapper stdqSimqMapper;
    private final StdqStdaMapper stdqStdaMapper;
    private final HistoryMapper historyMapper;
    private final FeedbackMapper feedbackMapper;

    /**
     * 全量同步，从mysql中同步一张表的所有数据到es对应的索引中
     *
     * @param tableIndexName 表名/索引名
     * @return 成功操作的数据总数
     */
    @Override
    public int totalSynchronize(String tableIndexName) throws IOException {
        int account = 0;
        //标准问-标准答表
        if (tableIndexName.equals(dialogueManagementConfig.getIndex().getStdqStda())) {
            //查询数据库中所有数据
            List<StdqStda> StdqStdaList = stdqStdaMapper.selectAll();
            //es client初始化
            RestHighLevelClient client = restClientUtil.getClient(ESConfig.getNode1().getHost(), ESConfig.getNode1().getPort());
            //删除原索引
            try {
                AcknowledgedResponse deleteIndexResponse = client.indices().delete(restClientUtil.getDeleteIndexRequest(tableIndexName), RequestOptions.DEFAULT);
                logUtil.traceAll().info("删除索引{} {}", tableIndexName, deleteIndexResponse.isAcknowledged());
            } catch (ElasticsearchException e) {
                e.printStackTrace();
                if (e.status() == RestStatus.NOT_FOUND) {
                    logUtil.traceAll().error("索引{}不存在，无法删除，将直接创建", tableIndexName);
                }
            }
            //创建新索引
            CreateIndexRequest createIndexRequest = restClientUtil.getCreateIndexRequest(tableIndexName);

            String jsonSource = readElasticsearchAPIJson(dialogueManagementConfig.getIndex().getStdqStda(), "index");

            //index setting,mappings,7.0以上版本弃用_doc
            createIndexRequest.source(jsonSource, XContentType.JSON);

            try {
                CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
                if (!createIndexResponse.isAcknowledged()) {
                    logUtil.traceAll().error("创建索引{}失败", tableIndexName);
                    return 0;
                }
            } catch (ElasticsearchException e) {
                e.printStackTrace();
                return 0;
            }

            logUtil.traceAll().info("创建索引{}成功", tableIndexName);

            //插入数据
            IndexRequest request = null;
            int size = StdqStdaList.size();
            for (StdqStda StdqStda : StdqStdaList) {
                Map<String, Object> jsonMap = new HashMap<>();
                jsonMap.put("qa_id", StdqStda.getQaId());
                jsonMap.put("standard_question", StdqStda.getStandardQuestion());
                jsonMap.put("category1", StdqStda.getCategory1());
                jsonMap.put("category2", StdqStda.getCategory2());
                jsonMap.put("standard_answer", StdqStda.getStandardAnswer());
                request = restClientUtil.getIndexRequest(tableIndexName, jsonMap);
                try {
                    IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);
                    account++;
                } catch (ElasticsearchException e) {
                    e.printStackTrace();
                }

            }
            logUtil.traceAll().info("从mysql中{}表查询到{}条数据，成功同步到es{}条数据", tableIndexName, size, account);
            //关闭client及时释放资源
            client.close();
        }
        //标准问-相似问表
        else if (tableIndexName.equals(dialogueManagementConfig.getIndex().getStdqSimq())) {
            //查询数据库中所有数据
            List<StdqSimq> StdqSimqList = stdqSimqMapper.selectAll();
            //es client初始化
            RestHighLevelClient client = restClientUtil.getClient(ESConfig.getNode1().getHost(), ESConfig.getNode1().getPort());

            //删除原索引
            try {
                AcknowledgedResponse deleteIndexResponse = client.indices().delete(restClientUtil.getDeleteIndexRequest(tableIndexName), RequestOptions.DEFAULT);
                logUtil.traceAll().info("删除索引{} {}", tableIndexName, deleteIndexResponse.isAcknowledged());
            } catch (ElasticsearchException e) {
                e.printStackTrace();
                if (e.status() == RestStatus.NOT_FOUND) {
                    logUtil.traceAll().error("索引{}不存在，无法删除，将直接创建", tableIndexName);
                }
            }

            //创建新索引
            CreateIndexRequest createIndexRequest = restClientUtil.getCreateIndexRequest(tableIndexName);

            //index setting,mappings,7.0以上版本启用_doc
            String jsonSource = readElasticsearchAPIJson(dialogueManagementConfig.getIndex().getStdqSimq(), "index");

            createIndexRequest.source(jsonSource, XContentType.JSON);
            try {
                CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
                if (!createIndexResponse.isAcknowledged()) {
                    logUtil.traceAll().error("创建索引{}失败", tableIndexName);
                    return 0;
                }
            } catch (ElasticsearchException e) {
                e.printStackTrace();
                return 0;
            }

            logUtil.traceAll().info("创建索引{}成功", tableIndexName);

            IndexRequest request = null;
            int size = StdqSimqList.size();
            for (StdqSimq StdqSimq : StdqSimqList) {
                Map<String, Object> jsonMap = new HashMap<>();
                jsonMap.put("qa_id", StdqSimq.getQaId());
                jsonMap.put("standard_question", StdqSimq.getStandardQuestion());
                jsonMap.put("similar_question", StdqSimq.getSimilarQuestion());
                request = restClientUtil.getIndexRequest(tableIndexName, jsonMap);
                try {
                    IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);
                    account++;
                } catch (ElasticsearchException e) {
                    e.printStackTrace();
                }
            }
            logUtil.traceAll().info("从mysql中{}表查询到{}条数据，成功插入es{}条数据", tableIndexName, size, account);
            //关闭client及时释放资源
            client.close();
        } else {
            logUtil.traceAll().error("表/索引名{}不在可以同步的表/索引中", tableIndexName);
        }

        return account;
    }

    /**
     * 批量导入，stdq_stda（作为事务处理，保证多表一致性）
     *
     * @param StdqStdaList 批量数据
     * @return 成功操作的数据总数
     */
    @Override
    @Transactional
    public int batchInsertIntoStdqStda(List<StdqStda> StdqStdaList) throws IOException {

        //若是stdq_stda表
        String tableIndexName = dialogueManagementConfig.getIndex().getStdqStda();

        //对于每个数据，首先插入到mysql，然后插入到es
        RestHighLevelClient client = restClientUtil.getClient(ESConfig.getNode1().getHost(), ESConfig.getNode1().getPort());
        IndexRequest request;

        List<StdqSimq> StdqSimqList = new ArrayList<>();
        //计数
        int count_mysql_t1 = 0;
        int count_es_i1 = 0;
        for (StdqStda StdqStda : StdqStdaList) {
            //判断qaId是否存在，mysql表中对该字段建立了唯一索引，这里再判断一次，对于不合法的qaId，该条数据跳过
            int qaId = StdqStda.getQaId();
            Example example = new Example(StdqSimq.class);
            example.createCriteria().andEqualTo("qaId", qaId);
            StdqStda qaIdData = stdqStdaMapper.selectOneByExample(example);
            if (qaIdData != null) {
                logUtil.traceAll().info("qaId={}已存在，跳过", qaId);
                continue;
            }
            //判断stdQ是否存在，stdQ和qaId本质上是一一对应的，对于不合法的stdQ，该条数据跳过
            String stdQ = StdqStda.getStandardQuestion();
            example = new Example(StdqSimq.class);
            example.createCriteria().andEqualTo("standardQuestion", stdQ);
            StdqStda stdQData = stdqStdaMapper.selectOneByExample(example);
            if (stdQData != null) {
                logUtil.traceAll().info("standardQuestion={}已存在，跳过", stdQ);
                continue;
            }

            //首先插入到mysql
            if (stdqStdaMapper.insert(StdqStda) > 0) {
                count_mysql_t1++;
            } else {
                continue;
            }
            //然后插入到es
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("qa_id", qaId);
            jsonMap.put("standard_question", StdqStda.getStandardQuestion());
            jsonMap.put("category1", StdqStda.getCategory1());
            jsonMap.put("category2", StdqStda.getCategory2());
            jsonMap.put("standard_answer", StdqStda.getStandardAnswer());
            request = restClientUtil.getIndexRequest(tableIndexName, jsonMap);
            try {
                client.index(request, RequestOptions.DEFAULT);
                count_es_i1++;
            } catch (ElasticsearchException e) {
                e.printStackTrace();
                continue;
            }

            //对于每一个qaId和stdQ，默认插入一个simQ与stdQ相同的数据到stdq_simq中
            StdqSimq StdqSimq = new StdqSimq();
            StdqSimq.setQaId(StdqStda.getQaId());
            StdqSimq.setStandardQuestion(StdqStda.getStandardQuestion());
            StdqSimq.setSimilarQuestion(StdqStda.getStandardQuestion());
            StdqSimqList.add(StdqSimq);

        }
        logUtil.traceAll().info("批量导入{}个数据到mysql表{}，同时同步{}个数据到es索引{}", count_mysql_t1, tableIndexName, count_es_i1, tableIndexName);
        batchInsertIntoStdqSimq(StdqSimqList);

        //关闭client及时释放资源
        client.close();

        return count_mysql_t1;

    }

    /**
     * 增量同步stdq_simq
     *
     * @param StdqSimqList 批量数据
     * @return 成功操作的数据总数
     */
    @Override
    public int batchInsertIntoStdqSimq(List<StdqSimq> StdqSimqList) throws IOException {

        String tableIndexName = dialogueManagementConfig.getIndex().getStdqSimq();

        //对于每个数据，首先插入到mysql，然后插入到es
        RestHighLevelClient client = restClientUtil.getClient(ESConfig.getNode1().getHost(), ESConfig.getNode1().getPort());
        IndexRequest request;
        int count_mysql = 0;
        int count_es = 0;
        for (StdqSimq StdqSimq : StdqSimqList) {
            Integer qaId = StdqSimq.getQaId();
            String stdQ = StdqSimq.getStandardQuestion();
            String simQ = StdqSimq.getSimilarQuestion();
            //根据qaId查询是否有效，以及标准问是否一致
            Example example = new Example(StdqStda.class);
            example.createCriteria().andEqualTo("qaId", qaId);
            StdqStda qaIdData = stdqStdaMapper.selectOneByExample(example);
            //若qaId不存在
            if (qaIdData == null) {
                logUtil.traceAll().info("qa_id={}无效，跳过", qaId);
                logUtil.recordInsertFailedData().info("(qaId不存在)qa_id={},standard_question={},similar_question={}", qaId, stdQ, simQ);
                continue;
            }
            //若要插入的数据的标准问和已有的不一致
            if (!stdQ.equals(qaIdData.getStandardQuestion())) {
                logUtil.traceAll().info("standard_question={}无效，跳过", stdQ);
                logUtil.recordInsertFailedData().info("(stdQ不一致)qa_id={},standard_question={},similar_question={}", qaId, stdQ, simQ);
                continue;
            }
            //若simQ已经存在，记录错误的数据，并跳过该次插入
            example = new Example(StdqSimq.class);
            example.createCriteria().andEqualTo("similarQuestion", simQ);
            StdqSimq simQData = stdqSimqMapper.selectOneByExample(example);
            if (simQData != null) {
                logUtil.traceAll().info("similar_question={}已存在，跳过", simQ);
                logUtil.recordInsertFailedData().info("(simQ已存在)qa_id={},standard_question={},similar_question={}", qaId, stdQ, simQ);
                continue;
            }
            //首先插入到mysql
            if (stdqSimqMapper.insert(StdqSimq) > 0) {
                count_mysql++;
            } else {
                continue;
            }
            //然后插入到es
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("qa_id", qaId);
            jsonMap.put("standard_question", StdqSimq.getStandardQuestion());
            jsonMap.put("similar_question", StdqSimq.getSimilarQuestion());
            request = restClientUtil.getIndexRequest(tableIndexName, jsonMap);
            try {
                client.index(request, RequestOptions.DEFAULT);
                count_es++;
            } catch (ElasticsearchException e) {
                e.printStackTrace();
            }

        }

        logUtil.traceAll().info("批量导入{}个数据到mysql表{}，同时同步{}个数据到es索引{}", count_mysql, tableIndexName, count_es, tableIndexName);

        //关闭client及时释放资源
        client.close();

        return count_mysql;
    }

    /**
     * 更新数据，stdq_stda（作为事务处理，保证多表一致性）
     *
     * @param stdqStda 一条数据
     * @return true/false
     */
    @Override
    @Transactional
    public boolean updateStdqStda(StdqStda stdqStda) throws IOException {
        //更新stdq_stda表要同步更新std_sim表，因为要保证一致性，qa_id和标准问是一一对应的
        String tableIndexName_stdq_stda = dialogueManagementConfig.getIndex().getStdqStda();
        String tableIndexName_std_sim = dialogueManagementConfig.getIndex().getStdqSimq();
        int qaId = stdqStda.getQaId();
        String stdq = stdqStda.getStandardQuestion();

        //1.1更新mysql的stdq_stda
        Example example = new Example(StdqStda.class);
        example.createCriteria().andEqualTo("qaId", qaId);
        int result = stdqStdaMapper.updateByExample(stdqStda, example);
        if (result == 0) {
            logUtil.traceAll().error("mysql表{}中qa_id={}的数据更新失败", tableIndexName_stdq_stda, qaId);
            return false;
        }

        //1.2更新es的stdq_stda
        RestHighLevelClient client = restClientUtil.getClient(ESConfig.getNode1().getHost(), ESConfig.getNode1().getPort());

        //根据qaId查找docId
        SearchRequest searchRequest = restClientUtil.getSearchRequest(tableIndexName_stdq_stda, "qa_id", qaId, 1);
        String docId = null;
        try {
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            docId = searchResponse.getHits().getHits()[0].getId();
        } catch (ElasticsearchException e) {
            e.printStackTrace();
            logUtil.traceAll().info("es索引{}中找不到对应qa_id={}的数据", tableIndexName_stdq_stda, qaId);
            return false;
        }

        //根据docId插入数据
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("qa_id", stdqStda.getQaId());
        jsonMap.put("standard_question", stdqStda.getStandardQuestion());
        jsonMap.put("category1", stdqStda.getCategory1());
        jsonMap.put("category2", stdqStda.getCategory2());
        jsonMap.put("standard_answer", stdqStda.getStandardAnswer());
        IndexRequest request = restClientUtil.getIndexRequest(tableIndexName_stdq_stda, jsonMap, docId);
        try {
            client.index(request, RequestOptions.DEFAULT);
            logUtil.traceAll().info("成功更新{}个qa_id={}的数据到mysql表{}，同时同步{}个数据到es索引{}", result, qaId, tableIndexName_stdq_stda, result, tableIndexName_stdq_stda);
        } catch (ElasticsearchException e) {
            e.printStackTrace();
            logUtil.traceAll().info("es索引{}中qa_id={}的数据更新失败", tableIndexName_stdq_stda, qaId);
            return false;
        }

        //2 更新stdq_simq中的对应的标准问，对于一个标准问，可以存在多个相似问，逐个更新
        example = new Example(StdqStda.class);
        example.createCriteria().andEqualTo("qaId", qaId);
        List<StdqSimq> tempList = stdqSimqMapper.selectByExample(example);
        int tempSize = tempList.size();
        if (tempSize == 0) {
            logUtil.traceAll().info("mysql表{}中qa_id={}的数据为空", tableIndexName_std_sim, qaId);
            return false;
        }
        //逐个更新
        int count_mysql_stdq_simq = 0;
        for (StdqSimq tempData : tempList) {
            if (tempData.getStandardQuestion().equals(tempData.getSimilarQuestion())) {
                tempData.setStandardQuestion(stdq);
                tempData.setSimilarQuestion(stdq);
            } else {
                tempData.setStandardQuestion(stdq);
            }
            int res = stdqSimqMapper.updateByExample(tempData, example);
            if (res == 0) {
                logUtil.traceAll().info("mysql表{}中id={}的数据更新标准问失败", tableIndexName_std_sim, tempData.getId());
                return false;
            }

            count_mysql_stdq_simq++;
        }

        //2.2更新es的stdq_simq
        SearchHit[] hits;
        //根据qaId查找docId
        SearchRequest searchRequest2 = restClientUtil.getSearchRequest(tableIndexName_std_sim, "qa_id", qaId, count_mysql_stdq_simq);
        try {
            SearchResponse searchResponse2 = client.search(searchRequest2, RequestOptions.DEFAULT);
            hits = searchResponse2.getHits().getHits();
        } catch (ElasticsearchException e) {
            e.printStackTrace();
            return false;
        }
        int count_es_std_simq = 0;
        for (SearchHit hit : hits) {
            String docId2 = hit.getId();
            //根据docId插入数据
            Map<String, Object> jsonMap2 = new HashMap<>();
            jsonMap2.put("qa_id", qaId);
            jsonMap2.put("standard_question", stdq);
            String simQ = (String) hit.getSourceAsMap().get("similar_question");
            //标准问和相似问相同的需要同时更新
            if (hit.getSourceAsMap().get("standard_question").equals(simQ)) {
                simQ = stdq;
            }
            jsonMap2.put("similar_question", simQ);
            IndexRequest request2 = restClientUtil.getIndexRequest(tableIndexName_std_sim, jsonMap2, docId2);
            try {
                client.index(request2, RequestOptions.DEFAULT);
                count_es_std_simq++;
            } catch (ElasticsearchException e) {
                e.printStackTrace();
                return false;
            }
        }
        logUtil.traceAll().info("成功更新{}个qa_id={}的数据到mysql表{}，同时同步{}个数据到es索引{}", count_mysql_stdq_simq, qaId, tableIndexName_std_sim, count_es_std_simq, tableIndexName_std_sim);

        //关闭client及时释放资源
        client.close();

        return true;
    }

    /**
     * 更新数据，stdq_simq
     *
     * @param StdqSimq 一条数据
     * @return true/false
     */
    @Override
    public boolean updateStdqSimq(StdqSimq StdqSimq) throws IOException {
        String tableIndexName = dialogueManagementConfig.getIndex().getStdqSimq();
        //首先更新mysql，然后更新es
        //更新mysql
        int result = stdqSimqMapper.updateByPrimaryKey(StdqSimq);
        if (result > 0) {
            logUtil.traceAll().info("mysql表{}更新成功", tableIndexName);
        } else {
            logUtil.traceAll().error("mysql表{}更新失败", tableIndexName);
            return false;
        }
        //更新es
        RestHighLevelClient client = restClientUtil.getClient(ESConfig.getNode1().getHost(), ESConfig.getNode1().getPort());

        //根据simQ查找docId
        SearchRequest searchRequest = restClientUtil.getSearchRequest(tableIndexName, "similar_question", StdqSimq.getSimilarQuestion(), 1);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        String docId = searchResponse.getHits().getHits()[0].getId();
        //根据docId插入数据
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("qa_id", StdqSimq.getQaId());
        jsonMap.put("standard_question", StdqSimq.getStandardQuestion());
        jsonMap.put("similar_question", StdqSimq.getSimilarQuestion());
        IndexRequest request = restClientUtil.getIndexRequest(tableIndexName, jsonMap, docId);
        try {
            client.index(request, RequestOptions.DEFAULT);
            logUtil.traceAll().info("es索引{}更新成功", tableIndexName);
        } catch (ElasticsearchException e) {
            e.printStackTrace();
            logUtil.traceAll().info("es索引{}更新失败", tableIndexName);
            return false;
        }

        //关闭client及时释放资源
        client.close();
        return true;
    }

    /**
     * 根据qaId查找一条数据,stdq_stda
     *
     * @param qaId qaId
     * @return QAKnowledgeBase
     */
    public StdqStda selectStdqStdaByQaId(int qaId) {
        Example example = new Example(StdqStda.class);
        example.createCriteria().andEqualTo("qaId", qaId);
        return stdqStdaMapper.selectOneByExample(example);
    }

    /**
     * 通过相似问similar_question查找数据，stdq_simq
     *
     * @param similarQuestion 相似问
     * @return StdqSimq
     */
    public StdqSimq selectStdqSimqBySimilarQuestion(String similarQuestion) {
        Example example = new Example(StdqSimq.class);
        example.createCriteria().andEqualTo("similarQuestion", similarQuestion);
        return stdqSimqMapper.selectOneByExample(example);
    }

    /**
     * 删除一条数据，stdq_stda（作为事务处理，保证多表一致性）
     *
     * @param StdqStda qaKnowledgeBase
     * @return true/false
     */
    @Override
    @Transactional
    public boolean deleteFromStdqStda(StdqStda StdqStda) throws IOException {

        String tableIndexName_stdq_stda = dialogueManagementConfig.getIndex().getStdqStda();
        String tableIndexName_stdq_simq = dialogueManagementConfig.getIndex().getStdqSimq();
        int qaId = StdqStda.getQaId();
        String stdQ = StdqStda.getStandardQuestion();

        //删除mysql中的数据
        int result = stdqStdaMapper.delete(StdqStda);
        if (result == 0) {
            logUtil.traceAll().info("删除mysql表{}中的qa_id={}的数据失败", tableIndexName_stdq_stda, qaId);
            return false;
        }
        logUtil.traceAll().info("删除mysql表{}中的qa_id={}的数据成功", tableIndexName_stdq_stda, qaId);

        //删除es中的数据
        RestHighLevelClient client = restClientUtil.getClient(ESConfig.getNode1().getHost(), ESConfig.getNode1().getPort());
        //根据qaId查找docId
        SearchRequest searchRequest = restClientUtil.getSearchRequest(tableIndexName_stdq_stda, "qa_id", qaId, 1);
        String docId = null;
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            docId = searchResponse.getHits().getHits()[0].getId();
        } catch (ElasticsearchException e) {
            e.printStackTrace();
            logUtil.traceAll().info("es索引{}中未找到qa_id={}的数据", tableIndexName_stdq_stda, qaId);
            return false;
        }
        //根据docId删除索引
        DeleteRequest deleteRequest = restClientUtil.getDeleteRequest(tableIndexName_stdq_stda, docId);
        DeleteResponse deleteResponse;
        try {
            deleteResponse = client.delete(deleteRequest, RequestOptions.DEFAULT);
            if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
                logUtil.traceAll().info("未找到索引{}中qa_id={},doc_id={}的数据，删除失败", tableIndexName_stdq_stda, qaId, docId);
                return false;
            }
        } catch (ElasticsearchException e) {
            e.printStackTrace();
            return false;
        }

        logUtil.traceAll().info("删除es索引{}中的数据成功,qa_id={},stdQ={}", tableIndexName_stdq_stda, qaId, stdQ);
        //删除stdq_simq中对应qaId的所有数据
        //删除mysql中的数据
        Example example = new Example(StdqSimq.class);
        example.createCriteria().andEqualTo("qaId", qaId);
        int result_mysql_stdq_simq = stdqSimqMapper.deleteByExample(example);
        if (result_mysql_stdq_simq == 0) {
            logUtil.traceAll().info("删除mysql表{}中qa_id={}的数据失败", tableIndexName_stdq_simq, qaId);
            return false;
        }
        logUtil.traceAll().info("成功删除{}个mysql表{}中qa_id={}的数据", result_mysql_stdq_simq, tableIndexName_stdq_simq, qaId);
        //删除es中的数据
        //根据qaId查找docId
        searchRequest = restClientUtil.getSearchRequest(tableIndexName_stdq_simq, "qa_id", qaId, result_mysql_stdq_simq);
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchException e) {
            e.printStackTrace();
            logUtil.traceAll().info("查找es索引{}中qa_id={}的数据失败", tableIndexName_stdq_simq, qaId);
            return false;
        }
        SearchHits searchHits = searchResponse.getHits();
        for (SearchHit hit : searchHits.getHits()) {
            docId = hit.getId();
            deleteRequest = restClientUtil.getDeleteRequest(tableIndexName_stdq_simq, docId);
            try {
                deleteResponse = client.delete(deleteRequest, RequestOptions.DEFAULT);
            } catch (ElasticsearchException e) {
                e.printStackTrace();
                logUtil.traceAll().info("删除es索引{}中qa_id={},doc_id={}的数据失败", tableIndexName_stdq_simq, qaId, docId);
                return false;
            }
            if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
                logUtil.traceAll().info("未找到索引{}中qa_id={},doc_id={}的数据，删除失败", tableIndexName_stdq_simq, qaId, docId);
                return false;
            }
        }

        logUtil.traceAll().info("成功删除{}个es索引{}中qa_id={},stdQ={}的数据", searchHits.totalHits, tableIndexName_stdq_simq, qaId, stdQ);
        //关闭client，及时释放资源
        client.close();

        return true;
    }

    /**
     * 删除一条数据，stdq_simq
     *
     * @param StdqSimq StdqSimq
     * @return true/false
     */
    @Override
    public boolean deleteFromStdqSimq(StdqSimq StdqSimq) throws IOException {

        String tableIndexName = dialogueManagementConfig.getIndex().getStdqSimq();
        String simQ = StdqSimq.getSimilarQuestion();
        String stdQ = StdqSimq.getStandardQuestion();
        int qaId = StdqSimq.getQaId();

        //删除mysql中的数据
        int result = stdqSimqMapper.delete(StdqSimq);
        if (result == 0) {
            logUtil.traceAll().info("删除mysql表{}中的数据失败，请检查各字段，qa_id={},stQ={},simQ={}", tableIndexName, qaId, stdQ, simQ);
            return false;
        }
        logUtil.traceAll().info("删除mysql表{}中的数据成功，qa_id={},stQ={},simQ={}", tableIndexName, qaId, stdQ, simQ);

        //删除es中的数据
        RestHighLevelClient client = restClientUtil.getClient(ESConfig.getNode1().getHost(), ESConfig.getNode1().getPort());
        //根据simQ查docId
        SearchRequest searchRequest = restClientUtil.getSearchRequest(tableIndexName, "similar_question", StdqSimq.getSimilarQuestion(), 1);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchException e) {
            e.printStackTrace();
            logUtil.traceAll().info("查找es索引{}中simQ={}的数据失败", tableIndexName, simQ);
            return false;
        }

        String docId = searchResponse.getHits().getHits()[0].getId();

        DeleteRequest deleteRequest = restClientUtil.getDeleteRequest(tableIndexName, docId);
        try {
            DeleteResponse deleteResponse = client.delete(deleteRequest, RequestOptions.DEFAULT);
            if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
                logUtil.traceAll().info("未找到索引{}中doc_id={},simQ={}的数据，删除失败", tableIndexName, docId, simQ);
                return false;
            }
        } catch (ElasticsearchException e) {
            e.printStackTrace();
            logUtil.traceAll().info("删除es索引{}中doc_id{},simQ={}的数据失败", tableIndexName, docId, simQ);
            return false;
        }
        logUtil.traceAll().info("删除索引{}中的doc_id={},simQ={}的数据成功", tableIndexName, docId, simQ);
        return true;
    }

    /**
     * 读取保存常用esAPI的json文件
     *
     * @param indexName 索引名
     * @param APIType   方法
     * @return jsonString
     */
    public String readElasticsearchAPIJson(String indexName, String APIType) {
        String jsonFile;
        switch (APIType) {
            case "index":
                jsonFile = String.format("%s/index_API/PUT-%s.json", dialogueManagementConfig.getElasticsearchAPIPath(), indexName);
                break;
            case "document":
                jsonFile = String.format("%s/document_API/PUT-%s.json", dialogueManagementConfig.getElasticsearchAPIPath(), indexName);
                break;
            case "search":
                jsonFile = String.format("%s/search_API/GET-%s.json", dialogueManagementConfig.getElasticsearchAPIPath(), indexName);
                break;
            default:
                jsonFile = null;
        }
        if (jsonFile == null) {
            logUtil.traceAll().error("没有对应{} API的操作", APIType);
            return null;
        }

        String jsonData = "";
        //读取多轮问答树json文件
        try {
            jsonData = FileUtils.readFileToString(new File(jsonFile), String.valueOf(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            logUtil.traceAll().error("读取esAPIJson文件{}出错", jsonFile);
            return null;
        }

        return jsonData;
    }

    /**
     * 更新多轮问答树到redis
     *
     * @return 成功更新的数量
     */
    @Override
    public int updateMultiTurnQATree() {
        String path = dialogueManagementConfig.getMultiTurnQA().getPath();
        //遍历多轮问答树的路径
        File dir = new File(dialogueManagementConfig.getMultiTurnQA().getPath());
        if (!dir.exists()) {
            logUtil.traceAll().error("多轮问答树的路径{}不存在", path);
            return 0;
        }
        if (!dir.isDirectory()) {
            logUtil.traceAll().error("多轮问答树的路径{}不是一个目录", path);
            return 0;
        }
        String[] files = dir.list();
        if (files == null) {
            logUtil.traceAll().error("{}路径下无任何文件", path);
            return 0;
        }
        //将文件转换为对象，更新到redis中

        //建立question到qaId的唯一映射
        HashMap<String, Integer> question2id = new HashMap<>();

        int NumsOfTreeNode = files.length;
        int accout = 0;
        for (int i = 0; i < NumsOfTreeNode; i++) {
            String filePath = dir + "/" + files[i];
            //将json文件转换为java对象
            MQATreeNode node = readFileToObject(filePath);
            if (node == null) {
                logUtil.traceAll().error("读取多轮问答树{}出错，跳过", filePath);
                continue;
            }
            int qaId = node.getQaId();
            question2id.put(node.getQuestion(), qaId);
            //设置多轮问答树的qaId的为key
            redisUtil.set(dialogueManagementConfig.getMQATreeKeyPrefix() + qaId, node);
            accout++;
        }

        redisUtil.set(dialogueManagementConfig.getMQAQuestion2idKey(), question2id);

        //返回更新的多轮问答树的总数
        logUtil.traceAll().info("已更新{}个多轮问答树到redis中", accout);
        return NumsOfTreeNode;
    }

    @Override
    public List<History> queryAllByUser(String msgId) {
        return historyMapper.selectAll();
    }

    @Override
    public List<Feedback> queryAllFeedback() {
        return feedbackMapper.selectAll();
    }

    @Override
    public List<StdqStda> queryStdQStdA(int limit, int i) {
        return stdqStdaMapper.selectAllWithLimit(limit, i);
    }

    @Override
    public int getStdqStdaTotal() {
        return stdqStdaMapper.getTotal();
    }

    @Override
    public List<StdqSimq> queryStdqSimq(int limit, int i) {
        return stdqSimqMapper.selectAllWithLimit(limit, i);
    }

    @Override
    public int getStdqSimqTotal() {
        return stdqSimqMapper.getTotal();
    }

    @Override
    public List<StdqStda> searchStdqStdqByStdq(String standardQuestion, int limit, int offset) {
        return stdqStdaMapper.searchByStdq("%" + standardQuestion + "%", limit, offset);
    }

    @Override
    public List<StdqSimq> searchStdqSimqByStdq(String standardQuestion, int limit, int offset) {
        return stdqSimqMapper.searchByStdQ("%" + standardQuestion + "%", limit, offset);
    }

    @Override
    public int getStdqStdaTotalBySearch(String standardQuestion) {
        return stdqStdaMapper.getTotalBySearch("%" + standardQuestion + "%");
    }

    @Override
    public int getStdqSimqTotalBySearch(String standardQuestion) {
        return stdqSimqMapper.getTotalBySearch("%" + standardQuestion + "%");
    }

    /**
     * 读取多轮问答树文件，转换为对象
     *
     * @return MQATreeNode
     */
    public MQATreeNode readFileToObject(String file) {
        String jsonData;
        //读取多轮问答树json文件
        try {
            jsonData = FileUtils.readFileToString(new File(file), String.valueOf(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        //转换为MQATreeNode多轮问答树对象
        return JSONObject.parseObject(jsonData, MQATreeNode.class);
    }
}
