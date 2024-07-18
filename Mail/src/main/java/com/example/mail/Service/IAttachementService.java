package com.example.mail.Service;

import com.example.mail.Entity.Attachment;

import java.util.List;

public interface IAttachementService {
    List<Attachment> getAttachmentsForMail(Long mailId);
}
