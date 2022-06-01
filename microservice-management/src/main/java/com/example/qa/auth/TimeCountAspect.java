package com.example.qa.auth;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;


/**
 * @ClassName: CheckLoginAspect
 * @Author: lerry_li
 * @CreateDate: 2021/06/13
 * @Description
 */
@Aspect
@Component
@Slf4j
public class TimeCountAspect {

    @Around("@annotation(com.example.qa.auth.TimeCount)")
    public Object checkLogin(ProceedingJoinPoint joinPoint) throws Throwable {
        //1.假设查过了
        StopWatch stopWatch = new StopWatch();

        log.info("aop开始...");
        stopWatch.start();
        Object result = joinPoint.proceed();
        stopWatch.stop();
        log.info("aop结束...,耗时{}ms", stopWatch.getTotalTimeMillis());
        return result;
    }
}
