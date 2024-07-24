package com.example.mail.Service;

import com.example.mail.Entity.Attachment;
import com.example.mail.Entity.Mail;
import com.example.mail.Repository.IAttachmentRepository;
import com.example.mail.Repository.IMailRepository;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.springframework.web.client.RestTemplate;


@Service
public class EmailService implements IEmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    @Autowired
    private IAttachmentRepository attachmentRepository ;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private IMailRepository emailRepository;
    @Autowired
    private RestTemplate restTemplate;

//    public List<Mail> getEmails(int page, int size) {
//        Pageable pageable = PageRequest.of(page, size);
//        Page<Mail> emailPage = emailRepository.findAll(pageable);
//        return emailPage.getContent();
//    }

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


    @Async
    @Override
    public boolean  readAndSaveEmails1(String userEmail, String userPassword) {
        logger.info("Validating credentials for user: {}", userEmail);

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
            store.connect("imap.gmail.com", userEmail, userPassword);
            logger.info("Successfully connected to the email server.");
            return true; // Credentials are valid
        } catch (Exception e) {
            logger.error("Error connecting to email server: " + e.getMessage());
            return false; // Invalid credentials
        } finally {
            if (store != null) {
                try {
                    store.close();
                } catch (MessagingException e) {
                    logger.error("Error closing email store: " + e.getMessage());
                }
            }
        }
    }
    @Override
    public List<Mail> getEmailsByUserEmail(String userEmail) {
        return emailRepository.findByUserEmail(userEmail);
    }

    @Override
    public List<Mail> getEmailsByUserId(Integer userId) {
        return emailRepository.findByUserId(userId);
    }

    public List<Mail> findByUserEmail(String userEmail) {
        return emailRepository.findByUserEmail(userEmail);
    }
    @Override
    @Async
    public void readAndSaveEmailsForMailbox(String userEmail, String userPassword, Integer userId) {
        logger.info("readAndSaveEmails method called for user: {}", userEmail);

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
            store.connect("imap.gmail.com", userEmail, userPassword);
            logger.info("Successfully connected to the email server.");
        } catch (Exception e) {
            logger.error("Error connecting to email server: " + e.getMessage());
            return;
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
                email.setUserId(userId);

                Object content = message.getContent();
                if (content instanceof String) {
                    email.setContent((String) content);
                } else if (content instanceof MimeMultipart) {
                    MimeMultipart multipart = (MimeMultipart) content;
                    email.setContent(extractContentFromMultipart(multipart));

                    List<Attachment> attachments = extractAttachmentsFromMultipart(multipart);
                    for (Attachment attachment : attachments) {
                        attachment.setMail(email);
                    }
                    email.setAttachments(attachments);
                } else {
                    email.setContent("Unsupported content type");
                }

                logger.info("Saving email: " + email.getSubject());
                emailRepository.save(email);
            }
        } catch (Exception e) {
            logger.error("Error reading or saving emails: " + e.getMessage());
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



    public void readAndSaveEmailsforeachMailBox(String userEmail, String userPassword, Long mailboxId) {
        // Logic to read emails
        List<Mail> emails = fetchEmailsFromServer(userEmail, userPassword);

        // Set mailboxId for each email
        for (Mail email : emails) {
            email.setMailboxId(mailboxId);
        }

        // Save emails
        emailRepository.saveAll(emails);
    }
    private List<Mail> fetchEmailsFromServer(String userEmail, String userPassword) {
        List<Mail> emails = new ArrayList<>();

        try {
            // Set mail properties
            Properties properties = new Properties();
            properties.put("mail.store.protocol", "imaps");
            properties.put("mail.imaps.host", "imap.gmail.com");
            properties.put("mail.imaps.port", "993");
            properties.put("mail.imaps.ssl.trust", "*");

            // Create session and store
            Session emailSession = Session.getDefaultInstance(properties);
            Store store = emailSession.getStore();
            store.connect("imap.gmail.com", userEmail, userPassword);

            // Open inbox folder
            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            // Fetch messages
            Message[] messages = emailFolder.getMessages();

            for (Message message : messages) {
                if (message instanceof MimeMessage) {
                    MimeMessage mimeMessage = (MimeMessage) message;

                    // Create Mail entity from MimeMessage
                    Mail mail = new Mail();
                    mail.setSubject(mimeMessage.getSubject());
                    mail.setSender(((InternetAddress) mimeMessage.getFrom()[0]).getAddress());
                    mail.setContent(mimeMessage.getContent().toString());
                    mail.setReceivedDate(mimeMessage.getReceivedDate());

                    emails.add(mail);
                }
            }

            // Close connections
            emailFolder.close(false);
            store.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return emails;
    }


    @Async
    @Override
    public void readAndSaveEmails33(Long mailboxId, String userEmail, String userPassword) {
        logger.info("readAndSaveEmails33 method called for mailboxId: {}", mailboxId);

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
            store.connect("imap.gmail.com", userEmail, userPassword);
            logger.info("Successfully connected to the email server.");
        } catch (Exception e) {
            logger.error("Error connecting to email server: " + e.getMessage());
            return;
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
                email.setMailboxId(mailboxId); // Set the mailboxId

                Object content = message.getContent();
                if (content instanceof String) {
                    email.setContent((String) content);
                } else if (content instanceof MimeMultipart) {
                    MimeMultipart multipart = (MimeMultipart) content;
                    email.setContent(extractContentFromMultipart(multipart));

                    List<Attachment> attachments = extractAttachmentsFromMultipart(multipart);
                    email.setAttachments(new ArrayList<>());
                    for (Attachment attachment : attachments) {
                        attachment.setMail(email);
                        email.getAttachments().add(attachment);
                    }
                } else {
                    email.setContent("Unsupported content type");
                }

                logger.info("Saving email: " + email.getSubject());
                emailRepository.save(email); // Save the Mail entity first

                // Save the attachments after saving the email
                if(email.getAttachments() != null && !email.getAttachments().isEmpty()) {
                    for (Attachment attachment : email.getAttachments()) {
                        attachmentRepository.save(attachment);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error reading or saving emails: " + e.getMessage());
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
    public List<Mail> fetchEmails( String userEmail, String userPassword, int page, int size) {
        logger.info("fetchEmails method called for mailboxId: {}, page: {}, size: {}", page, size);

        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", "imap.gmail.com");
        properties.put("mail.imaps.port", "993");
        properties.put("mail.imaps.ssl.enable", "true");

        Session emailSession = Session.getDefaultInstance(properties);
        Store store = null;
        List<Mail> emails = new ArrayList<>();

        try {
            logger.info("Attempting to connect to the email server...");
            store = emailSession.getStore("imaps");
            store.connect("imap.gmail.com", userEmail, userPassword);
            logger.info("Successfully connected to the email server.");

            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            logger.info("Fetching unseen emails...");
            Message[] messages = emailFolder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

            int start = page * size;
            int end = Math.min(start + size, messages.length);

            for (int i = start; i < end; i++) {
                Message message = messages[i];
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
                    // Attachments are not processed here
                } else {
                    email.setContent("Unsupported content type");
                }

                // Do not save the email to the database
                emails.add(email);
            }
        } catch (Exception e) {
            logger.error("Error fetching emails: " + e.getMessage());
        } finally {
            try {
                if (store != null) {
                    store.close();
                }
            } catch (MessagingException e) {
                logger.error("Error closing store: " + e.getMessage());
            }
        }

        return emails;
    }

}




