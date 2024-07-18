package com.example.mail.Service;

import com.example.mail.Entity.Attachment;
import com.example.mail.Entity.Mail;
import com.example.mail.Repository.IMailRepository;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;

@Service
public class EmailService implements IEmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private IMailRepository emailRepository;

    @Async
    @Override
    public void readAndSaveEmails() {
        logger.info("readAndSaveEmails method called");

        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", "imap.gmail.com");
        properties.put("mail.imaps.port", "993");
        properties.put("mail.imaps.ssl.enable", "true");

        Session emailSession = Session.getDefaultInstance(properties);
        Store store = null;
        try {
            logger.info("Attempting to connect to the email server...");
            store = emailSession.getStore("imaps");
            store.connect("imap.gmail.com", "skanderbey2040@gmail.com", "yzwv pqyl kexm vqyk");
            logger.info("Successfully connected to the email server.");
        } catch (Exception e) {
            logger.error("Error connecting to email server: " + e.getMessage());
            // Handle or log the exception as needed
            return; // Exit method if connection fails
        }

        Folder emailFolder = null;
        try {
            emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            logger.info("Fetching unseen emails...");
            Message[] messages = emailFolder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            for (Message message : messages) {
                Mail email = new Mail();
                email.setSubject(message.getSubject());
                email.setSender(message.getFrom()[0].toString());
                email.setReceivedDate(message.getReceivedDate());

                Object content = message.getContent();
                if (content instanceof String) {
                    email.setContent((String) content);
                } else if (content instanceof MimeMultipart) {
                    MimeMultipart multipart = (MimeMultipart) content;
                    email.setContent(extractContentFromMultipart(multipart));

                    // Extract attachments
                    List<Attachment> attachments = extractAttachmentsFromMultipart(multipart);
                    for (Attachment attachment : attachments) {
                        attachment.setMail(email); // Link attachment to email
                    }
                    email.setAttachments(attachments); // Set attachments in email entity
                } else {
                    email.setContent("Unsupported content type");
                }

                logger.info("Saving email: " + email.getSubject());
                emailRepository.save(email);
            }
        } catch (Exception e) {
            logger.error("Error reading or saving emails: " + e.getMessage());
            // Handle or log the exception as needed
        } finally {
            try {
                if (emailFolder != null) {
                    emailFolder.close(false);
                }
                if (store != null) {
                    store.close();
                }
            } catch (MessagingException e) {
                logger.error("Error closing email folder or store: " + e.getMessage());
            }
        }
    }


    @Override
    public Page<Mail> getEmails(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return emailRepository.findAll(pageable);
    }

        private String extractContentFromMultipart(Multipart multipart) throws MessagingException, IOException {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                content.append(bodyPart.getContent());
            } else if (bodyPart.isMimeType("text/html")) {
                content.append(bodyPart.getContent());
            } else if (bodyPart.getContent() instanceof Multipart) {
                content.append(extractContentFromMultipart((Multipart) bodyPart.getContent()));
            }
        }
        return content.toString();
    }

    private List<Attachment> extractAttachmentsFromMultipart(MimeMultipart multipart) throws MessagingException, IOException {
        List<Attachment> attachments = new ArrayList<>();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                Attachment attachment = new Attachment();
                attachment.setFilename(bodyPart.getFileName());
                attachment.setContentType(bodyPart.getContentType());
                attachment.setData(IOUtils.toByteArray(bodyPart.getInputStream())); // Example using Apache Commons IO
                attachments.add(attachment);
            } else if (bodyPart.getContent() instanceof Multipart) {
                attachments.addAll(extractAttachmentsFromMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }
        return attachments;
    }


}
