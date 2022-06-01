package com.example.qa.controller;

import com.example.qa.auth.TimeCount;
import com.example.qa.response.CommonReturnType;
import com.example.qa.service.ManagementService;
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
@RestController
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")   //处理跨域请求
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Api(tags = "用户")
public class UserController {

    @Autowired
    private ManagementService managementService;

    @TimeCount
    @GetMapping("/get_chat_history")
    public CommonReturnType getChatHistory(@RequestParam(name = "user_id") Integer user_id) {
        return CommonReturnType.success(managementService.queryAllByUser("msg_" + user_id));
    }
}
