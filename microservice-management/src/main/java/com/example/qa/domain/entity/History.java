package com.example.qa.domain.entity;

import javax.persistence.*;

import io.swagger.annotations.ApiModel;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "history")
@ApiModel("聊天记录")
public class History {
    @Id
    @GeneratedValue(generator = "JDBC")
    private Integer id;

    @Column(name = "msg_id")
    private String msgId;

    private String type;

    private String position;

    @Column(name = "created_at")
    private Long createdAt;

    @Column(name = "content_text")
    private String contentText;
}