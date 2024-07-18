package com.example.mail.Repository;

import com.example.mail.Entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IAttachmentRepository extends JpaRepository<Attachment,Long> {

    List<Attachment> findByMailId(Long mailId);
}
