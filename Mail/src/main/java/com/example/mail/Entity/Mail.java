package com.example.mail.Entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.xml.registry.infomodel.User;
import java.util.Date;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Mail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 255)
    private String subject;
    private String sender;
    private String userEmail; // Add this field


    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;
    @Temporal(TemporalType.TIMESTAMP)
    private Date receivedDate;
    @OneToMany(mappedBy = "mail", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Attachment> attachments;


    private Long mailboxId;



}
