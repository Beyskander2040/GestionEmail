package com.example.mail.Service;

import com.example.mail.Controlleur.MailControlleur;
import com.example.mail.Entity.Attachment;
import com.example.mail.Entity.EmailDTO;
import com.example.mail.Entity.Mail;
import com.example.mail.Repository.IAttachmentRepository;
import com.example.mail.Repository.IMailRepository;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
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
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.PostConstruct;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;
import javax.validation.constraints.Email;

import org.springframework.http.HttpMethod;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import javax.mail.UIDFolder;
import javax.mail.Folder;

@Service
public class EmailService implements IEmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    @Autowired
    private IAttachmentRepository attachmentRepository ;


    @Autowired
    private IMailRepository emailRepository;


    private SseEmitter emitter;
    private final Object emitterLock = new Object();
    private boolean isEmitterClosed = false;



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
    private void updateProgress(Long mailboxId, int progress) {
        MailControlleur.progressMap.put(mailboxId, progress);
    }



    @Async
    @Override

    public void readAndSaveEmails33(Long mailboxId, String email, String password) {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", "imap.gmail.com");
        properties.put("mail.imaps.port", "993");
        properties.put("mail.imaps.ssl.enable", "true");

        Session emailSession = Session.getDefaultInstance(properties);
        Store store = null;
        Folder emailFolder = null;

        try {
            store = emailSession.getStore("imaps");
            store.connect("imap.gmail.com", email, password);

            emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            Message[] messages = emailFolder.getMessages();
            int totalMessages = messages.length;

            for (int i = 0; i < messages.length; i++) {
                processAndSaveEmail(messages[i], mailboxId);
                // Update progress every 10 emails
                if ((i + 1) % 10 == 0 || (i + 1) == totalMessages) {
                    int progress = (i + 1) * 100 / totalMessages;
                    updateProgress(mailboxId, progress);
                }
            }

        } catch (Exception e) {
            // Handle exceptions
        } finally {
            try {
                if (emailFolder != null) emailFolder.close(false);
                if (store != null) store.close();
            } catch (Exception e) {
                // Handle exceptions
            }
        }
    }
    public SseEmitter getProgressEmitter() {
        synchronized (emitterLock) {
            emitter = new SseEmitter();
            isEmitterClosed = false; // Reset flag when a new emitter is created
        }
        return emitter;
    }


    private Mail processAndSaveEmail(Message message, Long mailboxId) throws Exception {
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
        if (email.getAttachments() != null && !email.getAttachments().isEmpty()) {
            for (Attachment attachment : email.getAttachments()) {
                attachmentRepository.save(attachment);
            }
        }

        return email;
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
    public List<EmailDTO> fetchAndReturnEmails(String userEmail, String userPassword) {
        // Logic to fetch emails from the mail server
        List<Mail> emails = fetchEmailsFromServer(userEmail, userPassword);

        // Convert to DTO
        return emails.stream().map(email -> {
            EmailDTO dto = new EmailDTO();
            BeanUtils.copyProperties(email, dto);
            return dto;
        }).collect(Collectors.toList());
    }
    @Override
    public List<Mail> fetchEmailsabc(String email, String password, int page, int size) {
        List<Mail> emails = new ArrayList<>();

        // Set up the properties for the IMAP connection
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imap.host", "imap.yourmailserver.com"); // Replace with your mail server
        properties.put("mail.imap.port", "993"); // Typically used for IMAP over SSL

        try {
            // Create a session with the mail server
            Session session = Session.getInstance(properties, null);
            Store store = session.getStore("imaps");
            store.connect(email, password);

            // Open the inbox folder
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // Get the message count and determine the range for pagination
            int messageCount = inbox.getMessageCount();
            int start = Math.max(0, messageCount - ((page + 1) * size));
            int end = Math.min(messageCount, start + size);

            // Fetch the messages in the specified range
            Message[] messages = inbox.getMessages(start + 1, end);
            for (Message message : messages) {
                Mail mail = new Mail();
                mail.setSubject(message.getSubject());
                mail.setSender(((MimeMessage) message).getFrom()[0].toString());
                mail.setContent(message.getContent().toString());
                mail.setReceivedDate(message.getReceivedDate());
                // Set any other fields you need

                emails.add(mail);
            }

            // Close connections
            inbox.close(false);
            store.close();
        } catch (Exception e) {
            e.printStackTrace();
            // Handle exceptions appropriately
        }

        return emails;
    }

    @Override
    public List<Mail> findByMailboxId(Long mailboxId) {
        return emailRepository.findByMailboxId(mailboxId);
    }
    public void checkAndLogNewEmails(String email, String password, Long mailboxId) {
        Date mostRecentEmailDate = emailRepository.findMostRecentEmailDateByMailboxId(mailboxId);
        logger.debug("Starting checkAndLogNewEmails method.");
        logger.debug("Most recent email date from database: {}", mostRecentEmailDate);

        List<Mail> newEmails = fetchNewEmails(email, password, mostRecentEmailDate, mailboxId);

        boolean newEmailsFound = false;
        for (Mail newEmail : newEmails) {
            if (newEmail.getUid() == null) {
                logger.warn("Email with null UID encountered: {}", newEmail);
                continue;
            }

            Optional<Mail> existingEmail = emailRepository.findByUidAndMailboxId(newEmail.getUid(), mailboxId);
            if (existingEmail.isEmpty()) {
                emailRepository.save(newEmail);
                logger.debug("Saved email with UID {} to the database.", newEmail.getUid());
                newEmailsFound = true;
            } else {
                logger.debug("Email with UID {} already exists in the database.", newEmail.getUid());
            }
        }

        if (newEmailsFound) {
            logger.info("New emails found:");
            for (Mail mail : newEmails) {
                System.out.println("Subject: " + mail.getSubject());
                System.out.println("From: " + mail.getSender());
                System.out.println("Received Date: " + mail.getReceivedDate());
                System.out.println("Content: " + mail.getContent());
                System.out.println("----------");
            }
        } else {
            logger.info("No new emails.");
        }
    }



    private List<Mail> fetchNewEmails(String email, String password, Date mostRecentEmailDate, Long mailboxId) {
        logger.debug("Starting fetchNewEmails method.");
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imap.host", "imap.gmail.com"); // Replace with your server
        properties.put("mail.imap.port", "993"); // Default IMAP SSL port

        Session session = Session.getInstance(properties);
        List<Mail> newEmails = new ArrayList<>();

        try {
            Store store = session.getStore("imaps");
            store.connect("imap.gmail.com", email, password);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            logger.debug("Connected to mail server and opened inbox.");

            // Fetch messages received after the most recent email date
            SearchTerm searchTerm = new ReceivedDateTerm(ComparisonTerm.GT, mostRecentEmailDate);
            Message[] messages = inbox.search(searchTerm);

            logger.debug("Fetched {} messages from the server.", messages.length);

            // Process and convert messages to your Mail objects using IMAP UID
            newEmails = processMessages(messages, inbox, mailboxId);

            inbox.close(false);
            store.close();
        } catch (MessagingException e) {
            logger.error("Error while fetching new emails.", e);
        }

        return newEmails;
    }

    private List<Mail> processMessages(Message[] messages, Folder inbox, Long mailboxId) {
        logger.debug("Starting processMessages method.");
        List<Mail> emailList = new ArrayList<>();

        if (inbox instanceof UIDFolder) {
            UIDFolder uidFolder = (UIDFolder) inbox;

            for (Message message : messages) {
                try {
                    long uid = uidFolder.getUID(message);

                    Mail mail = new Mail();
                    mail.setUid(String.valueOf(uid));
                    mail.setSubject(message.getSubject());
                    mail.setSender(((InternetAddress) message.getFrom()[0]).getAddress());
                    mail.setContent(extractContent(message)); // Use the extractContent method
                    mail.setReceivedDate(message.getReceivedDate());
                    mail.setMailboxId(mailboxId);

                    emailList.add(mail);

                    logger.debug("Processed email with UID {}.", mail.getUid());
                } catch (Exception e) {
                    logger.error("Error while processing message.", e);
                }
            }
        } else {
            logger.error("Folder does not support UID operations.");
        }

        return emailList;
    }



    @PostConstruct
    public void updateExistingMails() {
        Iterable<Mail> mailIterable = emailRepository.findAll();
        List<Mail> mails = StreamSupport.stream(mailIterable.spliterator(), false)
                .collect(Collectors.toList());
        for (Mail mail : mails) {
            if (mail.getUid() == null) {
                mail.setUid(generateUniqueUid());
                emailRepository.save(mail);
            }
        }
    }

    private String generateUniqueUid() {
        return UUID.randomUUID().toString();
    }
    public String extractContent(Message message) throws MessagingException {
        try {
            Object content = message.getContent();
            if (content instanceof String) {
                return (String) content;
            } else if (content instanceof MimeMultipart) {
                return extractContentFromMultipart((MimeMultipart) content);
            }
            return "Unsupported content type";
        } catch (IOException e) {
            throw new MessagingException("Error accessing message content", e);
        }
    }

    private String extractContentFromMultipart(MimeMultipart multipart) throws MessagingException {
        StringBuilder contentBuilder = new StringBuilder();
        try {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                Object bodyContent = bodyPart.getContent();

                if (bodyContent instanceof String) {
                    contentBuilder.append((String) bodyContent);
                } else if (bodyContent instanceof InputStream) {
                    try (InputStream inputStream = (InputStream) bodyContent;
                         ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                        contentBuilder.append(outputStream.toString());
                    } catch (IOException e) {
                        throw new MessagingException("Error reading content from InputStream", e);
                    }
                }
            }
        } catch (IOException e) {
            throw new MessagingException("Error processing multipart content", e);
        }
        return contentBuilder.toString();
    }
}




