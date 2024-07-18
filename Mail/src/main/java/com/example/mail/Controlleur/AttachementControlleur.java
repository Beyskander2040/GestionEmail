package com.example.mail.Controlleur;

import com.example.mail.Entity.Attachment;
import com.example.mail.Service.IAttachementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attachments")
@CrossOrigin(origins = "http://localhost:4200")
public class AttachementControlleur {

    @Autowired
    private IAttachementService attachementService;

    @GetMapping("/mail/{mailId}")
    public ResponseEntity<List<Attachment>> getAttachmentsForMail(@PathVariable Long mailId) {
        List<Attachment> attachments = attachementService.getAttachmentsForMail(mailId);
        return ResponseEntity.ok(attachments);
    }
}
