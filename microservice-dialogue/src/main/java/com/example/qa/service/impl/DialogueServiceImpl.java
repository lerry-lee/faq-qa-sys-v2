package com.example.qa.service.impl;

import com.example.qa.config.DialogueConfig;
import com.example.qa.controller.viewObject.DialogueResultVO;
import com.example.qa.domain.dto.Answer;
import com.example.qa.domain.dto.MQATreeNode;
import com.example.qa.domain.dto.RecomQuestion;
import com.example.qa.response.CodeMsg;
import com.example.qa.service.DialogueService;
import com.example.qa.service.model.DialogueStatusModel;
import com.example.qa.service.model.MatchingDataModel;
import com.example.qa.service.retrieval.RetrievalService;
import com.example.qa.service.retrieval.model.RetrievalDataModel;
import com.example.qa.service.similarity.SimilarityService;
import com.example.qa.util.LogUtil;
import com.example.qa.util.RedisUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * @ClassName: DialogueServiceImpl
 * @Author: lerry_li
 * @CreateDate: 2021/01/17
 * @Description
 */
@Service
public class DialogueServiceImpl implements DialogueService {
    @Autowired
    private DialogueConfig dialogueConfig;

    @Autowired
    private LogUtil logUtil;

    @Autowired
    private RetrievalService retrievalService;

    @Autowired
    private SimilarityService similarityService;

    @Autowired
    private RedisUtil redisUtil;


    /**
     * 回答用户提问
     *
     * @param statusModel 初始的对话状态
     * @return 完成的对话状态
     */
    @Override
    public DialogueStatusModel answer(DialogueStatusModel statusModel) throws IOException {

        //清空原answer
        statusModel.setAnswer(new Answer());
        //清空原recomQuestion
        statusModel.setRecomQuestions(new ArrayList<>());

        Integer userId = statusModel.getUserId();
        Integer robotId = statusModel.getRobotId();
        String question = statusModel.getQuestion();

        //1.是否需要检索
        //1.1 若处于多轮问答中，则不需要检索
        if (statusModel.isMultiRound()) {
            //处理多轮问答
            DialogueStatusModel statusModelAfterProcess = processMultiRound(statusModel);
            //若成功处理，直接返回
            if (statusModelAfterProcess.getCodeMsg() == CodeMsg.SUCCESS_MULTI) {
                return statusModelAfterProcess;
            }
        }
        //1.2 查看redis中是否有对应的热点数据
        HashMap<String, String> hotDataQuestion2id = null;
        if (dialogueConfig.getHotData().getOpen()) {
            hotDataQuestion2id = (HashMap<String, String>) redisUtil.get(dialogueConfig.getHotDataQuestion2idKey());
            if (hotDataQuestion2id != null && hotDataQuestion2id.containsKey(question)) {
                //根据id对应的key找到对应的缓存数据
                DialogueResultVO dialogueResultVO = (DialogueResultVO) redisUtil.get(dialogueConfig.getHotDataKeyPrefix() + hotDataQuestion2id.get(question));
                if (dialogueResultVO != null) {
                    //将缓存中的数据拷贝到领域模型中
                    BeanUtils.copyProperties(dialogueResultVO, statusModel);
                    //判断是多轮还是单轮
                    HashMap<String, Integer> MQAQuestion2Id = (HashMap<String, Integer>) redisUtil.get(dialogueConfig.getMQAQuestion2idKey());
                    String stdQuestion = dialogueResultVO.getAnswer().getStdQ();
                    //若多轮问答树为空或者不包含该数据对应的标准问，则设置状态为单轮
                    if (MQAQuestion2Id == null || !MQAQuestion2Id.containsKey(stdQuestion)) {
                        //更新对话状态多轮
                        statusModel.setMQATreeNode(null);
                        statusModel.setMultiRound(false);
                        //设置状态码
                        statusModel.setCodeMsg(CodeMsg.SUCCESS_SINGLE);
                        logUtil.traceAll().info("(userId={},robotId={})当前用户的问题命中缓存数据\"{}\"，redis中多轮问答树不包含该问题对应的标准问\"{}\",状态判定为单轮", userId, robotId, question, stdQuestion);
                    }
                    //否则设置状态为多轮
                    else {
                        //设置状态码
                        statusModel.setCodeMsg(CodeMsg.SUCCESS_MULTI);
                        //更新对话状态
                        statusModel.setMultiRound(true);
                        MQATreeNode currNode = (MQATreeNode) redisUtil.get(dialogueConfig.getMQATreeKeyPrefix() + MQAQuestion2Id.get(stdQuestion));
                        statusModel.setMQATreeNode(currNode);
                        logUtil.traceAll().info("(userId={},robotId={})当前用户的问题命中缓存数据\"{}\"，redis中多轮问答树包含该问题对应的标准问\"{}\",状态判定为多轮", userId, robotId, question, stdQuestion);
                    }

                    return statusModel;
                }
            }
        }

        //否则需要进行检索
        //2.检索
        List<RetrievalDataModel> retrievalDataModelList = retrievalService.searchSimilarQuestions(question);
        //判断es是否正常工作，若有异常则返回
        if (retrievalDataModelList == null) {
            statusModel.setCodeMsg(CodeMsg.ELASTICSEARCH_EXCEPTION);
            return statusModel;
        }
        int total_counts = retrievalDataModelList.size();
        //若未识别该问题
        if (total_counts == 0) {
            //更新对话状态
            statusModel.setMultiRound(false);
            statusModel.setMQATreeNode(null);
            //设置状态码
            statusModel.setCodeMsg(CodeMsg.UNRECOGNIZED_QUESTION);
            statusModel.getAnswer().setContent("抱歉，小新还在学习中，您说的我还无法理解～");

            return statusModel;
        }
        //3.语义相似度计算
        List<String> questionList = new ArrayList<>(total_counts);
        List<String> similarQuestionList = new ArrayList<>(total_counts);
        for (int i = 0; i < total_counts; i++) {
            questionList.add(question);
            similarQuestionList.add(retrievalDataModelList.get(i).getSimilarQuestion());
        }
        List<Float> similarityScoreList = similarityService.similarityCalculation(questionList, similarQuestionList);

        //若相似度模型返回为空，则忽略相似度
        if (similarityScoreList == null || similarityScoreList.size() == 0) {
            logUtil.traceAll().error("(userId={},robotId={})相似度模型返回为空，请检查模型是否启动、url/参数是否正确", userId, robotId);

            //设置状态码
            statusModel.setCodeMsg(CodeMsg.SIMILARITY_NULL_EXCEPTION);

//            return statusModel;

            //填充语义相似度为0
            similarityScoreList = new ArrayList<>();
            for (int i = 0; i < total_counts; i++) {
                similarityScoreList.add(0F);
            }
//
            logUtil.traceAll().info("(userId={},robotId={})当前用户提问\"{}\"，相似度模型返回为null，只使用es检索返回的结果", userId, robotId, question);

        }
        //4.问答处理
        //4.1组装matchingDataModel
        List<MatchingDataModel> matchingDataModelList = new ArrayList<>(total_counts);
        for (int i = 0; i < total_counts; i++) {
            matchingDataModelList.add(new MatchingDataModel());
            BeanUtils.copyProperties(retrievalDataModelList.get(i), matchingDataModelList.get(i));
            matchingDataModelList.get(i).setSimilarityScore(similarityScoreList.get(i));
        }
        //4.2综合相关度得分和相似度得分，两者加权求和为置信度，按置信度排序
        matchingDataModelList.sort(new Comparator<MatchingDataModel>() {
            @Override
            public int compare(MatchingDataModel o1, MatchingDataModel o2) {
                //综合相关度得分和相似度得分，加权求和
                Float confidence1 = o1.getConfidence();
                if (confidence1 == null) {
                    Float relevanceScore1 = o1.getRelevanceScore();
                    Float similarityScore1 = o1.getSimilarityScore();
                    confidence1 = dialogueConfig.getConfidenceRank().getWeights().getRelevanceWeight() * relevanceScore1 + dialogueConfig.getConfidenceRank().getWeights().getSimilarityWeight() * similarityScore1;
                    o1.setConfidence(confidence1);
                }
                Float confidence2 = o2.getConfidence();
                if (confidence2 == null) {
                    Float relevanceScore2 = o2.getRelevanceScore();
                    Float similarityScore2 = o2.getSimilarityScore();
                    confidence2 = dialogueConfig.getConfidenceRank().getWeights().getRelevanceWeight() * relevanceScore2 + dialogueConfig.getConfidenceRank().getWeights().getSimilarityWeight() * similarityScore2;
                    o2.setConfidence(confidence2);
                }

                return confidence2.compareTo(confidence1);
            }
        });
        //4.3填充结果
        //记录识别为多轮的标记
        boolean isRecognizeMultiRound = false;
        for (int i = 0; i < dialogueConfig.getConfidenceRank().getSize() && i < total_counts; i++) {
            MatchingDataModel matchingDataModel = matchingDataModelList.get(i);
            //置信度最高的结果作为answer
            if (i == 0) {
                String bestAnswer = matchingDataModel.getStandardAnswer();
                //标准问同步到status
                statusModel.getAnswer().setStdQ(matchingDataModel.getStandardQuestion());
                statusModel.getAnswer().setConfidence(matchingDataModel.getConfidence());
                statusModel.getAnswer().setSimQ(matchingDataModel.getSimilarQuestion());
                //若识别为多轮，则进入首轮多轮问答处理
                if (bestAnswer.equals("多轮")) {
                    statusModel = firstProcessMultiRound(statusModel);
                    //判断是否成功处理首轮多轮
                    if (statusModel.getCodeMsg() != CodeMsg.SUCCESS_MULTI) {
                        return statusModel;
                    }
                    isRecognizeMultiRound = true;
                }
                //如果未识别为多轮，则设置答案，否则使用多轮首轮的处理结果
                if (!isRecognizeMultiRound) {
                    statusModel.getAnswer().setContent(bestAnswer);
                }
            }
            //其他的作为相关问题推荐
            else {
                RecomQuestion recomQuestion = new RecomQuestion();
                recomQuestion.setSimQ(matchingDataModel.getSimilarQuestion());
                recomQuestion.setStdQ(matchingDataModel.getStandardQuestion());
                recomQuestion.setConfidence(matchingDataModel.getConfidence());
                statusModel.getRecomQuestions().add(recomQuestion);
            }
        }

        //更新对话状态
        //如果未识别为多轮，则设置多轮状态为false
        if (!isRecognizeMultiRound) {
            statusModel.setMultiRound(false);
            //设置状态码
            statusModel.setCodeMsg(CodeMsg.SUCCESS_SINGLE);
        } else {
            statusModel.setCodeMsg(CodeMsg.SUCCESS_MULTI);
        }

        //5.问答处理过后，考虑将数据加入缓存
        if (dialogueConfig.getHotData().getOpen()) {
            DialogueResultVO vo = new DialogueResultVO();
            BeanUtils.copyProperties(statusModel, vo);
            //更新hotDataQuestion2id和hotData
            if (hotDataQuestion2id == null) {
                hotDataQuestion2id = new HashMap<>();
            }
            //判断redis是否缓存热点数据
            //生成随机不重复id token
            String hotDataIdToken = UUID.randomUUID().toString().replace("-", "");
            //将question映射到id token上
            hotDataQuestion2id.put(question, hotDataIdToken);
            redisUtil.set(dialogueConfig.getHotDataQuestion2idKey(), hotDataQuestion2id);
            String hotDataKey = dialogueConfig.getHotDataKeyPrefix() + hotDataIdToken;
            redisUtil.set(hotDataKey, vo);
            redisUtil.expire(hotDataKey, dialogueConfig.getHotData().getExpireTime());
        }

        return statusModel;
    }


    /**
     * 多轮问答首轮处理
     *
     * @param statusModel 对话状态
     * @return DialogueStatusModel
     */
    public DialogueStatusModel firstProcessMultiRound(DialogueStatusModel statusModel) {
        String standardQuestion = statusModel.getAnswer().getStdQ();
        //首先到redis中查找多轮问答树的question2id的键值对
        HashMap<String, Integer> question2id = (HashMap<String, Integer>) redisUtil.get(dialogueConfig.getMQAQuestion2idKey());
        //若question2id不存在
        if (question2id == null) {
            logUtil.traceAll().info("(userId={},robotId={})redis中多轮问答树为空", statusModel.getUserId(), statusModel.getRobotId());

            //设置状态码
            statusModel.setCodeMsg(CodeMsg.MULTI_ROUND_QA_NULL);

            return statusModel;
        }
        //若question2id中没有对应的key
        if (!question2id.containsKey(standardQuestion)) {
            logUtil.traceAll().info("(userId={},robotId={})没有找到根节点的question为\"{}\"的多轮问答树", statusModel.getUserId(), statusModel.getRobotId(), standardQuestion);

            //设置状态码
            statusModel.setCodeMsg(CodeMsg.MULTI_ROUND_QA_NOT_FOUND);

            return statusModel;
        }
        //根据question对应的id，从redis中获取对应的多轮问答树
        Integer MQATreeId = question2id.get(standardQuestion);
        MQATreeNode node = (MQATreeNode) redisUtil.get(dialogueConfig.getMQATreeKeyPrefix() + MQATreeId);
        //填充statusModel
        statusModel.getAnswer().setContent(node.getAnswer());

        //清空选项
        statusModel.getAnswer().setOptions(new ArrayList<>());
        //填充选项
        List<MQATreeNode> nodes = node.getChildNodes();

        if (nodes == null | nodes.size() == 0) {
            logUtil.traceAll().error("(userId={},robotId={})子结点为空！请检查\"{}\"的多轮问答树是否正确读取", statusModel.getUserId(), statusModel.getRobotId(), standardQuestion);

            //设置状态码

            statusModel.setCodeMsg(CodeMsg.MULTI_ROUND_QA_CHILD_NODE_NULL);

            return statusModel;
        }

        for (MQATreeNode childNode : node.getChildNodes()) {
            statusModel.getAnswer().getOptions().add(childNode.getQuestion());
        }

        //更新对话状态
        statusModel.setMultiRound(true);
        statusModel.setMQATreeNode(node);
        statusModel.setCodeMsg(CodeMsg.SUCCESS_MULTI);
        logUtil.traceAll().info("(userId={},robotId={})当前用户提问\"{}\"，进入首轮多轮问答", statusModel.getUserId(), statusModel.getRobotId(), statusModel.getQuestion());

        return statusModel;
    }


    /**
     * 多轮问答中间过程处理
     *
     * @param statusModel 对话状态
     * @return DialogueStatusModel
     */
    public DialogueStatusModel processMultiRound(DialogueStatusModel statusModel) {
        String question = statusModel.getQuestion();
        MQATreeNode node = statusModel.getMQATreeNode();
        boolean getIntoNextMulti = false;
        //遍历子结点，找到用户提问命中的选项
        for (MQATreeNode child_node : node.getChildNodes()) {
            if (child_node.getQuestion().equals(question)) {
                node = child_node;
                getIntoNextMulti = true;
                break;
            }
        }
        //若未命中多轮问答选项时，不进入下一轮多轮问答，返回
        if (!getIntoNextMulti) {
            logUtil.traceAll().info("(userId={},robotId={})当前用户提问\"{}\"，未命中多轮问答的选项", statusModel.getUserId(), statusModel.getRobotId(), question);
            //更新状态
            statusModel.setCodeMsg(CodeMsg.OPTIONS_NOT_HIT);
            return statusModel;
        }
        //处理多轮问答数据
        //填充answer属性
        String answer_content = node.getAnswer();
        statusModel.getAnswer().setContent(answer_content);
        statusModel.getAnswer().setConfidence(1.0000F);
        //清空原options选项
        statusModel.getAnswer().setOptions(new ArrayList<>());
        //若当前多轮问答树节点没有子结点，表示当前的多轮问答已经结束
        if (node.getChildNodes() == null || node.getChildNodes().size() == 0) {
            statusModel.setMultiRound(false);
        }
        //否则遍历当前节点的子结点，填充options属性
        else {
            statusModel.setMultiRound(true);
            //填充选项
            for (MQATreeNode childNode : node.getChildNodes()) {
                statusModel.getAnswer().getOptions().add(childNode.getQuestion());
            }
        }
        //更新对话状态
        statusModel.setMQATreeNode(node);

        logUtil.traceAll().info("(userId={},robotId={})当前用户提问\"{}\"，命中多轮问答的选项", statusModel.getUserId(), statusModel.getRobotId(), question);

        //设置状态码
        statusModel.setCodeMsg(CodeMsg.SUCCESS_MULTI);

        return statusModel;
    }
}
