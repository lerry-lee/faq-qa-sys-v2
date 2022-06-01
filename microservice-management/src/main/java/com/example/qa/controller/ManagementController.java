package com.example.qa.controller;

import com.example.qa.config.DialogueManagementConfig;
import com.example.qa.domain.entity.Feedback;
import com.example.qa.domain.entity.StdqSimq;
import com.example.qa.domain.entity.StdqStda;
import com.example.qa.response.CommonReturnType;
import com.example.qa.response.LayuiType;
import com.example.qa.service.ManagementService;
import com.example.qa.util.LogUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * @ClassName: ManagerController
 * @Author: lerry_li
 * @CreateDate: 2021/01/17
 * @Description
 */
@Api(tags = "管理")
@RestController
@RequestMapping("/management")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")   //处理跨域请求
public class ManagementController {

    private final static String ContentType = "application/x-www-form-urlencoded";

    @Autowired
    private ManagementService managementService;

    @Autowired
    private DialogueManagementConfig dialogueManagementConfig;

    @Autowired
    private LogUtil logUtil;

    /**
     * 全量同步，将mysql中的一张表全部同步到redis中
     */
    @ApiOperation("全量同步")
    @RequestMapping(value = "/total_synchronize", method = RequestMethod.GET)
    public CommonReturnType totalSynchronize(@ApiParam("表/索引名") @RequestParam(name = "table_index_name") String tableIndexName) throws IOException {

        //检查表/索引名是否有效
        if (!dialogueManagementConfig.getIndex().getIndexNames().contains(tableIndexName)) {
            logUtil.traceAll().error("{}不在可以同步的表/索引中", tableIndexName);
            return CommonReturnType.failed(String.format("%s不在可以同步的表/索引中", tableIndexName));
        }

        //统计耗时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int account = managementService.totalSynchronize(tableIndexName);
        stopWatch.stop();

        if (account == 0) {
            return CommonReturnType.failed(String.format("mysql表%s中0条数据被同步", tableIndexName));
        }

        return CommonReturnType.success(String.format("成功同步mysql表%s中%d条数据到es索引%s，耗时%dms", tableIndexName, account, tableIndexName, stopWatch.getTotalTimeMillis()));
    }


    /**
     * 批量导入，stdq_stda
     * 对于新导入的每一条数据，需要创建一条新数据到stdq_simq中，其中qaId和标准问对应、标准问和相似问相同
     */
    @ApiOperation("批量导入-stdq_stda")
    @RequestMapping(value = "/batch_insert_into_stdq_stda", method = RequestMethod.POST, consumes = "application/json")
    public CommonReturnType batchInsertIntoStdQStdA(@ApiParam("批量的数据") @RequestBody List<StdqStda> stdQStdAList) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        //导入数据到stdq_stda
        int result = managementService.batchInsertIntoStdqStda(stdQStdAList);
        stopWatch.stop();
        String tableIndexName = dialogueManagementConfig.getIndex().getStdqStda();
        if (result == 0) {
            return CommonReturnType.failed(String.format("0条数据被导入到mysql表%s", tableIndexName));
        }

        return CommonReturnType.success(String.format("成功导入%d条数据到mysql表%s，耗时%dms", result, tableIndexName, stopWatch.getTotalTimeMillis()));
    }


    /**
     * 批量导入，stdq_simq
     */
    @ApiOperation("批量导入-stdq_simq")
    @RequestMapping(value = "/batch_insert_into_stdq_simq", method = RequestMethod.POST, consumes = "application/json")
    public CommonReturnType batchInsertIntoStdQSimQ(
            @ApiParam("批量的数据") @RequestBody List<StdqSimq> stdQSimQList) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int result = managementService.batchInsertIntoStdqSimq(stdQSimQList);
        stopWatch.stop();
        String tableIndexName = dialogueManagementConfig.getIndex().getStdqSimq();

        if (result == 0) {
            return CommonReturnType.failed(String.format("0条数据被导入到mysql表%s", tableIndexName));
        }
        logUtil.traceAll().info("成功导入{}条数据到mysql表{}和es索引{}，耗时{}ms", result, dialogueManagementConfig.getIndex().getStdqSimq(), dialogueManagementConfig.getIndex().getStdqSimq(), stopWatch.getTotalTimeMillis());
        return CommonReturnType.success(String.format("成功导入%d条数据到mysql表%s，耗时%dms", result, tableIndexName, stopWatch.getTotalTimeMillis()));
    }

    /**
     * 更新stdq_stda的一条数据
     */
    @ApiOperation("更新数据-stdq_stda")
    @RequestMapping(value = "/update_stdq_stda", method = RequestMethod.POST, consumes = ContentType)
    public CommonReturnType updateQAKnowledgeBase(
            @ApiParam("qa_id") @RequestParam(name = "qa_id") Integer qaId,
            @ApiParam("标准问") @RequestParam(name = "standard_question", required = false) String standardQuestion,
            @ApiParam("一级类别") @RequestParam(name = "category1", required = false) String category1,
            @ApiParam("二级类别") @RequestParam(name = "category2", required = false) String category2,
            @ApiParam("标准答") @RequestParam(name = "standard_answer", required = false) String standardAnswer) throws IOException {

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        //首先查找qa_id是否有效
        StdqStda data = managementService.selectStdqStdaByQaId(qaId);

        if (data == null) {
            logUtil.traceAll().error("qa_id={}有误，找不到对应的数据", qaId);
            return CommonReturnType.failed(String.format("qa_id=%d有误，找不到对应的数据", qaId));
        }
        if (standardQuestion != null) {
            data.setStandardQuestion(standardQuestion);
        }
        if (category1 != null) {
            data.setCategory1(category1);
        }
        if (category2 != null) {
            data.setCategory2(category2);
        }
        if (standardAnswer != null) {
            data.setStandardAnswer(standardAnswer);
        }
        if (managementService.updateStdqStda(data)) {
            stopWatch.stop();
            logUtil.traceAll().info("更新成功，耗时{}ms", stopWatch.getTotalTimeMillis());
            return CommonReturnType.success(String.format("更新成功，耗时%dms", stopWatch.getTotalTimeMillis()));
        }


        return CommonReturnType.failed("更新失败");
    }

    /**
     * 更新stdq_simq的一条数据（只能更新相似问）
     */
    @ApiOperation("更新数据-stdq_simq")
    @RequestMapping(value = "/update_stdq_simq", method = RequestMethod.POST, consumes = ContentType)
    public CommonReturnType updateStdQSimQ(
            @ApiParam("qa_id") @RequestParam(name = "qa_id") Integer qaId,
            @ApiParam("标准问") @RequestParam(name = "standard_question") String standardQuestion,
            @ApiParam("(原)旧相似问") @RequestParam(name = "old_similar_question") String oldSimilarQuestion,
            @ApiParam("(改)新相似问") @RequestParam(name = "new_similar_question") String newSimilarQuestion) throws IOException {

        if (oldSimilarQuestion.equals(standardQuestion)) {
            logUtil.traceAll().info("标准问和相似问相同，不能更新该条数据");
            return CommonReturnType.failed("标准问和相似问相同，不能更新该条数据");
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        //首先根据原相似问old_similar_question查找要更新的这条数据
        StdqSimq stdQSimQ = managementService.selectStdqSimqBySimilarQuestion(oldSimilarQuestion);
        //若找不到
        if (stdQSimQ == null) {
            logUtil.traceAll().info("old_similar_question={}有误，找不到对应的数据", oldSimilarQuestion);
            return CommonReturnType.failed(String.format("old_similar_question=“%s”有误，找不到对应的数据", oldSimilarQuestion));
        }

        //然后检查stdQ是否一致
        if (!stdQSimQ.getStandardQuestion().equals(standardQuestion)) {
            logUtil.traceAll().info("数据库中的standard_question={}，而参数中的standard_question={}，标准问不一致", stdQSimQ.getStandardQuestion(), standardQuestion);
            return CommonReturnType.failed(String.format("standard_question=”%s“有误", standardQuestion));
        }

        //然后检查qaId与qaId是否一致
        if (!stdQSimQ.getQaId().equals(qaId)) {
            logUtil.traceAll().info("数据库中qa_id={}，而参数中的qa_id={}，qaId不一致", stdQSimQ.getQaId(), qaId);
            return CommonReturnType.failed(String.format("qa_id=%d有误", qaId));
        }

        //更新相似问
        stdQSimQ.setSimilarQuestion(newSimilarQuestion);

        if (managementService.updateStdqSimq(stdQSimQ)) {
            stopWatch.stop();
            logUtil.traceAll().info("更新成功，耗时{}ms", stopWatch.getTotalTimeMillis());
            return CommonReturnType.success(String.format("更新成功，耗时%dms", stopWatch.getTotalTimeMillis()));
        }
        return CommonReturnType.failed("更新失败");
    }


    /**
     * 从stdq_stda中删除一条数据，需要同步mysql和es，同时需要将stdq_simq中的对应数据删除
     */
    @ApiOperation("删除数据-stdq_simq")
    @RequestMapping(value = "/delete_from_stdq_stda", method = RequestMethod.POST, consumes = ContentType)
    public CommonReturnType deleteFromQAKnowledgeBase(
            @ApiParam("qa_id") @RequestParam(name = "qa_id") int qaId,
            @ApiParam("标准问") @RequestParam(name = "standard_question") String standardQuestion,
            @ApiParam("一级类别") @RequestParam(name = "category1") String category1,
            @ApiParam("二级类别") @RequestParam(name = "category2") String category2,
            @ApiParam("标准答") @RequestParam(name = "standard_answer") String standardAnswer) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        StdqStda stdQStdA = new StdqStda();
        stdQStdA.setQaId(qaId);
        stdQStdA.setStandardQuestion(standardQuestion);
        stdQStdA.setCategory1(category1);
        stdQStdA.setCategory2(category2);
        stdQStdA.setStandardAnswer(standardAnswer);

        if (managementService.deleteFromStdqStda(stdQStdA)) {
            stopWatch.stop();
            return CommonReturnType.success(String.format("删除成功，耗时%dms", stopWatch.getTotalTimeMillis()));
        }

        return CommonReturnType.failed("删除失败，请检查各字段是否正确");
    }


    /**
     * 删除stdq_simq中的数据
     */
    @ApiOperation("删除数据-stdq_simq")
    @RequestMapping(value = "/delete_from_stdq_simq", method = RequestMethod.POST, consumes = ContentType)
    public CommonReturnType deleteFromStdQSimQ(
            @ApiParam("qa_id") @RequestParam(name = "qa_id") int qaId,
            @ApiParam("标准问") @RequestParam(name = "standard_question") String standardQuestion,
            @ApiParam("相似问") @RequestParam(name = "similar_question") String similarQuestion) throws IOException {

        if (standardQuestion.equals(similarQuestion)) {
            logUtil.traceAll().info("stdQ=\"{}\"和simQ=\"{}\"相同，不能删除", standardQuestion, similarQuestion);
            return CommonReturnType.failed(String.format("stdQ='%s'和simQ='%s'相同，不能删除", standardQuestion, similarQuestion));
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        StdqSimq stdQSimQ = new StdqSimq();
        stdQSimQ.setQaId(qaId);
        stdQSimQ.setStandardQuestion(standardQuestion);
        stdQSimQ.setSimilarQuestion(similarQuestion);

        if (managementService.deleteFromStdqSimq(stdQSimQ)) {
            stopWatch.stop();
            return CommonReturnType.success(String.format("删除成功，耗时%dms", stopWatch.getTotalTimeMillis()));
        }

        return CommonReturnType.failed("删除失败，请检查各字段是否正确");

    }

    @ApiOperation("更新多轮问答树")
    @RequestMapping(value = "/update_multi_turn_qa_tree", method = RequestMethod.GET)
    public CommonReturnType updateMultiTurnQATree() {
        int account = managementService.updateMultiTurnQATree();
        if (account == 0) {
            return CommonReturnType.failed(null);
        }
        return CommonReturnType.success(String.format("成功更新%d个多轮问答树到redis", account));
    }


    @ApiOperation("查询所有反馈记录")
    @RequestMapping(value = "/query_all_feedback", method = RequestMethod.GET)
    public LayuiType queryAllFeedback() {
        List<Feedback> feedbacks = managementService.queryAllFeedback();
        if (feedbacks.isEmpty()) {
            return LayuiType.create(1, "查询失败(数据为空)", 0, null);
        }
        return LayuiType.create(0, "", feedbacks.size(), feedbacks);
    }


    /**
     * 查询stdq_stda中的数据
     *
     */
    @ApiOperation("查询数据-stdq_stda")
    @RequestMapping(value = "/query_stdq_stda", method = RequestMethod.GET)
    public LayuiType queryStdQStdA(
            @RequestParam(name = "page") int page,
            @RequestParam(name = "limit") int limit) {

        List<StdqStda> stdQStdAList = managementService.queryStdQStdA(limit, (page - 1) * limit);
        int total = managementService.getStdqStdaTotal();

        if (stdQStdAList.isEmpty()) {
            return LayuiType.create(1, "查询失败(表stdq_stda数据为空)", 0, null);
        }

        return LayuiType.create(0, "", total, stdQStdAList);
    }

    /**
     * 查询stdq_simq中的数据
     *
     */
    @ApiOperation("查询数据-stdq_simq")
    @RequestMapping(value = "/query_stdq_simq", method = RequestMethod.GET)
    public LayuiType queryStdQSimQ(
            @RequestParam(name = "page") int page,
            @RequestParam(name = "limit") int limit) {

        List<StdqSimq> stdQSimQList = managementService.queryStdqSimq(limit, (page - 1) * limit);
        int total = managementService.getStdqSimqTotal();

        if (stdQSimQList.isEmpty()) {
            return LayuiType.create(1, "查询失败(表stdq_simq数据为空)", 0, null);
        }

        return LayuiType.create(0, "", total, stdQSimQList);
    }

    /**
     * 按条件搜索问答对stdq_stda
     */
    @ApiOperation("按条件搜索问答对stdq_stda")
    @RequestMapping(value = "/search_stdq_stda", method = RequestMethod.POST, consumes = ContentType)
    public LayuiType queryStdQStdAByStdQ(
            @RequestParam(name = "standard_question") String standardQuestion,
            @RequestParam(name = "page") int page,
            @RequestParam(name = "limit") int limit) {

        if (standardQuestion == null || standardQuestion.length() == 0) {
            return LayuiType.create(1, "查询失败(标准问为空)，耗时%dms", 0, null);
        }

        List<StdqStda> list = managementService.searchStdqStdqByStdq(standardQuestion, limit, (page - 1) * limit);
        int total = managementService.getStdqStdaTotalBySearch(standardQuestion);

        if (list.isEmpty()) {
            return LayuiType.create(1, "查询失败(数据为空)", 0, null);
        }

        return LayuiType.create(0, "", total, list);
    }

    /**
     * 按条件搜索问答对stdq_simq
     */
    @ApiOperation("按条件搜索问答对stdq_stda")
    @RequestMapping(value = "/search_stdq_simq", method = RequestMethod.POST, consumes = ContentType)
    public LayuiType queryStdQSimQByStdQ(
            @RequestParam(name = "standard_question") String standardQuestion,
            @RequestParam(name = "page") int page,
            @RequestParam(name = "limit") int limit) {

        if (standardQuestion == null || standardQuestion.length() == 0) {
            return LayuiType.create(1, "查询失败(标准问为空)，耗时%dms", 0, null);
        }

        List<StdqSimq> list = managementService.searchStdqSimqByStdq(standardQuestion, limit, (page - 1) * limit);
        int total = managementService.getStdqSimqTotalBySearch(standardQuestion);

        if (list.isEmpty()) {
            return LayuiType.create(1, "查询失败(数据为空)", 0, null);
        }

        return LayuiType.create(0, "", total, list);
    }
}
