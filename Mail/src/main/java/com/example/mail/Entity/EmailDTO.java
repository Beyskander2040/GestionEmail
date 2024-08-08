package com.example.mail.Entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmailDTO {
    private Long id;
    private String subject;
    private String sender;
    private String content;
    private Date receivedDate;
    private List<Attachment> attachments;
    private Integer userId;

    private Long mailboxId;
}
