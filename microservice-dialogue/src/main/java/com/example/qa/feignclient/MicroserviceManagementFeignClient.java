package com.example.qa.feignclient;

import com.example.qa.response.CommonReturnType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @ClassName: MicroserviceManagementFeignClient
 * @Author: lerry_li
 * @CreateDate: 2021/06/13
 * @Description
 */
@FeignClient(name = "microservice-management")
public interface MicroserviceManagementFeignClient {
    @GetMapping("/user/get_chat_history")
    CommonReturnType getChatHistory(@RequestParam(name = "user_id") Integer user_id);
}
