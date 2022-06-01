package com.example.qa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.oas.annotations.EnableOpenApi;
import tk.mybatis.spring.annotation.MapperScan;

@EnableOpenApi
@SpringBootApplication
@MapperScan("com.example.qa.dao")
public class MicroserviceManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroserviceManagementApplication.class, args);
    }

}
