package com.example.qa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import springfox.documentation.oas.annotations.EnableOpenApi;

@EnableOpenApi
@SpringBootApplication
@EnableFeignClients
public class MicroserviceDialogueApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroserviceDialogueApplication.class, args);
    }

}
