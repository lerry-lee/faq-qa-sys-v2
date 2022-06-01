package com.example.qa.dao;

import com.example.qa.domain.entity.StdqSimq;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface StdqSimqMapper extends Mapper<StdqSimq> {
    @Select("select count(id) from stdq_simq")
    int getTotal();

    @Select("select id,qa_id,standard_question,similar_question from stdq_simq LIMIT #{limit} OFFSET #{offset}")
    @Results({
            @Result(property = "id", column = "id", id = true),
            @Result(property = "qaId", column = "qa_id"),
            @Result(property = "standardQuestion", column = "standard_question"),
            @Result(property = "similarQuestion", column = "similar_question"),
    })
    List<StdqSimq> selectAllWithLimit(int limit, int offset);

    @Select("select id,qa_id,standard_question,similar_question from stdq_simq " +
            "where standard_question like #{standardQuestion} LIMIT #{limit} OFFSET #{offset}")
    @Results({
            @Result(property = "id", column = "id", id = true),
            @Result(property = "qaId", column = "qa_id"),
            @Result(property = "standardQuestion", column = "standard_question"),
            @Result(property = "similarQuestion", column = "similar_question"),
    })
    List<StdqSimq> searchByStdQ(String standardQuestion,int limit, int offset);

    @Select("select count(id) from stdq_simq where standard_question like #{standardQuestion}")
    int getTotalBySearch(String standardQuestion);
}