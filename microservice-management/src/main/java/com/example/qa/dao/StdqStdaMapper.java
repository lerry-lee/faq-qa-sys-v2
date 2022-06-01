package com.example.qa.dao;

import com.example.qa.domain.entity.StdqStda;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface StdqStdaMapper extends Mapper<StdqStda> {
    @Select("select count(id) from stdq_stda")
    int getTotal();

    @Select("select id,qa_id,standard_question,category1,category2,standard_answer from stdq_stda LIMIT #{limit} OFFSET #{offset}")
    @Results({
            @Result(property = "id", column = "id", id = true),
            @Result(property = "qaId", column = "qa_id"),
            @Result(property = "standardQuestion", column = "standard_question"),
            @Result(property = "category1", column = "category1"),
            @Result(property = "category2", column = "category2"),
            @Result(property = "standardAnswer", column = "standard_answer")
    })
    List<StdqStda> selectAllWithLimit(int limit, int offset);

    @Select("select id,qa_id,standard_question,category1,category2,standard_answer from stdq_stda " +
            "where standard_question like #{standardQuestion} LIMIT #{limit} OFFSET #{offset}")
    @Results({
            @Result(property = "id", column = "id", id = true),
            @Result(property = "qaId", column = "qa_id"),
            @Result(property = "standardQuestion", column = "standard_question"),
            @Result(property = "category1", column = "category1"),
            @Result(property = "category2", column = "category2"),
            @Result(property = "standardAnswer", column = "standard_answer")
    })
    List<StdqStda> searchByStdq(String standardQuestion,int limit, int offset);

    @Select("select count(id) from stdq_stda where standard_question like #{standardQuestion}")
    int getTotalBySearch(String standardQuestion);
}