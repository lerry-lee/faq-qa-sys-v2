package com.example.qa.controller;

import com.example.qa.feignclient.MicroserviceManagementFeignClient;
import com.example.qa.response.CommonReturnType;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName: UserController
 * @Author: lerry_li
 * @CreateDate: 2021/06/13
 * @Description
 */
@Api(tags = "用户")
@RestController
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")   //处理跨域请求
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserController {
    private final MicroserviceManagementFeignClient managementFeignClient;

    @GetMapping("/get_chat_history")
    public CommonReturnType getChatHistory(@RequestParam(name = "user_id") Integer user_id) {
        return this.managementFeignClient.getChatHistory(user_id);
    }
}
