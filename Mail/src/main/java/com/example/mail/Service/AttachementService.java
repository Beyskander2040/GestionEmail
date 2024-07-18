package com.example.mail.Service;

import com.example.mail.Entity.Attachment;
import com.example.mail.Repository.IAttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service

public class AttachementService implements IAttachementService{

    @Autowired
    private IAttachmentRepository attachmentRepository;
    @Override
    public List<Attachment> getAttachmentsForMail(Long mailId) {
        return attachmentRepository.findByMailId(mailId);
    }
}
