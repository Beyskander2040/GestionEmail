package com.example.mail.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "attachments")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String contentType;

    @Lob
    private byte[] data; // Store attachment content as byte array, or use a different approach based on your requirements

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mail_id", nullable = false)
    @JsonBackReference
    private Mail mail;
}