package com.example.qa.domain.entity;

import javax.persistence.*;

import io.swagger.annotations.ApiModel;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "feedback")
@ApiModel("反馈")
public class Feedback {
    @Id
    @GeneratedValue(generator = "JDBC")
    private Integer id;

    private String type;

    private String question;

    private String reason;
}