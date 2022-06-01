package com.example.qa.service;

import com.example.qa.domain.entity.Feedback;
import com.example.qa.domain.entity.History;
import com.example.qa.domain.entity.StdqStda;
import com.example.qa.domain.entity.StdqSimq;

import java.io.IOException;
import java.util.List;

/**
 * @ClassName: ManagementService
 * @Author: lerry_li
 * @CreateDate: 2021/01/17
 * @Description
 */
public interface ManagementService {

    /**
     * 全量同步，从mysql中同步一张表的所有数据到es对应的索引中
     *
     * @param tableIndexName 表名/索引名
     * @return 成功操作的数据总数
     */
    int totalSynchronize(String tableIndexName) throws IOException;

    /**
     * 批量导入，stdq_stda（作为事务处理，保证多表一致性）
     *
     * @param stdQStdAList 批量数据
     * @return 成功操作的数据总数
     */
    int batchInsertIntoStdqStda(List<StdqStda> stdQStdAList) throws IOException;


    /**
     * 批量导入，stdq_simq
     *
     * @param stdQSimQList 批量数据
     * @return 成功操作的数据总数
     */
    int batchInsertIntoStdqSimq(List<StdqSimq> stdQSimQList) throws IOException;

    /**
     * 更新数据，stdq_stda（作为事务处理，保证多表一致性）
     *
     * @param stdQStdA 一条数据
     * @return true/false
     */
    boolean updateStdqStda(StdqStda stdQStdA) throws IOException;

    /**
     * 更新数据，stdq_simq
     *
     * @param stdQSimQ 一条数据
     * @return true/false
     */
    boolean updateStdqSimq(StdqSimq stdQSimQ) throws IOException;


    /**
     * 删除一条数据，stdq_stda（作为事务处理，保证多表一致性）
     * @param stdQStdA qaKnowledgeBase
     * @return true/false
     */
    boolean deleteFromStdqStda(StdqStda stdQStdA) throws IOException;


    /**
     * 删除一条数据，stdq_simq
     * @param stdQSimQ stdQSimQ
     * @return true/false
     */
    boolean deleteFromStdqSimq(StdqSimq stdQSimQ) throws IOException;

    StdqSimq selectStdqSimqBySimilarQuestion(String similarQuestion);

    StdqStda selectStdqStdaByQaId(int qaId);

    /**
     * 更新多轮问答树到redis
     * @return 成功更新的数量
     */
    int updateMultiTurnQATree();

    /**
     * 查询某个用户的聊天记录
     *
     */
    List<History> queryAllByUser(String msgId);

    /**
     * 查询所有反馈记录
     *
     */
    List<Feedback> queryAllFeedback();

    /**
     * 全量查询stdq_stda
     *
     */
    List<StdqStda> queryStdQStdA(int limit, int i);

    /**
     * 获取数据总数stdq_stda
     *
     */
    int getStdqStdaTotal();

    /**
     * 全量查询stdq_simq
     *
     */
    List<StdqSimq> queryStdqSimq(int limit, int i);

    /**
     * 获取数据总数stdq_simq
     *
     */
    int getStdqSimqTotal();

    /**
     * 根据stdq搜索stdq_stda
     *
     */
    List<StdqStda> searchStdqStdqByStdq(String standardQuestion,int limit, int offset);

    /**
     * 根据stdq搜索stdq_simq
     *
     */
    List<StdqSimq> searchStdqSimqByStdq(String standardQuestion,int limit, int offset);

    /**
     * 获取数据总数stdq_stda根据模糊词
     *
     */
    int getStdqStdaTotalBySearch(String standardQuestion);

    /**
     * 获取数据总数stdq_simq根据模糊词
     *
     */
    int getStdqSimqTotalBySearch(String standardQuestion);
}
