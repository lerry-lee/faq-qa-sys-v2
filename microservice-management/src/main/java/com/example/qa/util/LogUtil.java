package com.example.qa.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * @ClassName: LogUtil
 * @Author: lerry_li
 * @CreateDate: 2021/01/13
 * @Description
 */
@Component
public class LogUtil {
    /**
     * 详细追踪，用于调试
     */
    public Logger traceAll() {
        return LogManager.getLogger("TRACE_ALL");
    }

    /**
     * 记录未识别的问题到文件
     */
    public Logger recordUnrecognizedQuestion() {
        return LogManager.getLogger("Record_Unrecognized_Question");
    }

    /**
     * 记录插入失败的数据
     */
    public Logger recordInsertFailedData() {
        return LogManager.getLogger("Record_Insert_Failed_Data");
    }
}
