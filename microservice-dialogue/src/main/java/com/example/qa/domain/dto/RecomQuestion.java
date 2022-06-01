package com.example.qa.domain.dto;

import lombok.Data;

/**
 * @ClassName: RecomQuestion
 * @Author: lerry_li
 * @CreateDate: 2021/01/12
 * @Description 其他相似问题推荐
 */
@Data
public class RecomQuestion {
    //相似问
    private String simQ;
    //标准问
    private String stdQ;
    //置信度
    private Float confidence;
}
