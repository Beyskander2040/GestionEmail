package com.example.mail.Service;

import com.example.mail.Controlleur.MailControlleur;
import com.example.mail.Entity.Attachment;
import com.example.mail.Entity.Mail;
import com.example.mail.Repository.IAttachmentRepository;
import com.example.mail.Repository.IMailRepository;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Service
public class EmailService implements IEmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    @Autowired
    private IAttachmentRepository attachmentRepository;
    @Autowired
    private IMailRepository emailRepository;


    private List<Attachment> extractAttachmentsFromMultipart(MimeMultipart multipart) throws MessagingException, IOException {
        List<Attachment> attachments = new ArrayList<>();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                Attachment attachment = new Attachment();
                attachment.setFilename(bodyPart.getFileName());
                attachment.setContentType(bodyPart.getContentType());
                attachment.setData(IOUtils.toByteArray(bodyPart.getInputStream()));
                attachments.add(attachment);
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                attachments.addAll(extractAttachmentsFromMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }
        return attachments;
    }

    @Async
    @Override
    public boolean readAndSaveEmails1(String userEmail, String userPassword) {
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
            return true;
        } catch (Exception e) {
            logger.error("Error connecting to email server: " + e.getMessage());
            return false;
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
        List<Mail> emails = fetchEmailsFromServer(userEmail, userPassword);

        for (Mail email : emails) {
            email.setMailboxId(mailboxId);
        }

        emailRepository.saveAll(emails);
    }

    private List<Mail> fetchEmailsFromServer(String userEmail, String userPassword) {
        List<Mail> emails = new ArrayList<>();

        try {
            Properties properties = new Properties();
            properties.put("mail.store.protocol", "imaps");
            properties.put("mail.imaps.host", "imap.gmail.com");
            properties.put("mail.imaps.port", "993");
            properties.put("mail.imaps.ssl.trust", "*");

            Session emailSession = Session.getDefaultInstance(properties);
            Store store = emailSession.getStore("imaps");
            store.connect("imap.gmail.com", userEmail, userPassword);

            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            Message[] messages = emailFolder.getMessages();

            for (Message message : messages) {
                if (message instanceof MimeMessage) {
                    MimeMessage mimeMessage = (MimeMessage) message;

                    Mail mail = new Mail();
                    mail.setSubject(mimeMessage.getSubject());
                    mail.setSender(((InternetAddress) mimeMessage.getFrom()[0]).getAddress());
                    mail.setContent(mimeMessage.getContent().toString());
                    mail.setReceivedDate(mimeMessage.getReceivedDate());

                    emails.add(mail);
                }
            }

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
        Logger logger = LoggerFactory.getLogger(getClass());

        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", "imap.gmail.com");
        properties.put("mail.imaps.port", "993");
        properties.put("mail.imaps.ssl.enable", "true");

        Session emailSession = Session.getDefaultInstance(properties);
        Store store = null;
        Folder emailFolder = null;

        try {
            logger.info("Connecting to email server with email: {}", email);
            store = emailSession.getStore("imaps");
            store.connect("imap.gmail.com", email, password);

            emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            Message[] messages = emailFolder.getMessages();
            int totalMessages = messages.length;

            logger.info("Total messages to process: {}", totalMessages);

            // Create a thread pool to process emails concurrently
            int threadCount = 10; // Adjust the number of threads according to your needs
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            AtomicInteger processedCount = new AtomicInteger(0);

            for (int i = 0; i < messages.length; i++) {
                final int index = i; // Must be final or effectively final to be used in the lambda
                executor.submit(() -> {
                    try {
                        logger.debug("Processing email {} of {}", index + 1, totalMessages);
                        processAndSaveEmail(messages[index], mailboxId);

                        // Update progress
                        int processed = processedCount.incrementAndGet();
                        if (processed % 10 == 0 || processed == totalMessages) {
                            int progress = processed * 100 / totalMessages;
                            logger.info("Updating progress: {}% for mailbox ID: {}", progress, mailboxId);
                            updateProgress(mailboxId, progress);
                        }
                    } catch (Exception e) {
                        logger.error("Error processing email {} of {}: ", index + 1, totalMessages, e);
                    }
                });
            }

            // Shutdown the executor and wait for all threads to complete
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            logger.info("Email processing completed for mailbox ID: {}", mailboxId);

        } catch (Exception e) {
            logger.error("Error during email fetching: ", e);
        } finally {
            try {
                if (emailFolder != null) emailFolder.close(false);
                if (store != null) store.close();
            } catch (Exception e) {
                logger.error("Error closing email folder or store: ", e);
            }
        }
    }
    @Override
    public List<Mail> findByMailboxId(Long mailboxId) {
        return emailRepository.findByMailboxId(mailboxId);
    }

    public void checkAndLogNewEmails(String email, String password, Long mailboxId) {
        Date mostRecentEmailDate = emailRepository.findMostRecentEmailDateByMailboxId(mailboxId);
        if (mostRecentEmailDate == null) {
            logger.warn("Most recent email date is null for mailboxId: {}", mailboxId);
            // Handle null case or set a default date if needed
        }
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

    private String extractContentFromMultipart(MimeMultipart multipart) throws MessagingException, IOException {
        StringBuilder content = new StringBuilder();

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            String contentType = part.getContentType().toLowerCase();

            if (contentType.contains("text/plain") || contentType.contains("text/html")) {
                String charsetName = getCharset(part.getContentType());
                try (InputStream inputStream = part.getInputStream();
                     Reader reader = new InputStreamReader(inputStream, charsetName)) {
                    content.append(new BufferedReader(reader).lines().collect(Collectors.joining("\n")));
                } catch (UnsupportedEncodingException e) {
                    logger.error("Unsupported encoding: " + charsetName + ". Fallback to UTF-8.", e);
                    // Fallback to UTF-8
                    try (InputStream inputStream = part.getInputStream();
                         Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                        content.append(new BufferedReader(reader).lines().collect(Collectors.joining("\n")));
                    }
                }
            } else if (part.getContent() instanceof MimeMultipart) {
                content.append(extractContentFromMultipart((MimeMultipart) part.getContent()));
            }
        }

        return content.length() > 0 ? content.toString() : "No content found";
    }

    private String getCharset(String contentType) {
        try {
            ContentType ct = new ContentType(contentType);
            String charset = ct.getParameter("charset");

            if ("unicode-1-1-utf-7".equalsIgnoreCase(charset)) {
                return StandardCharsets.UTF_8.name(); // Fallback to UTF-8 if charset is unsupported
            }
            return charset != null ? charset : StandardCharsets.UTF_8.name();
        } catch (ParseException e) {
            return StandardCharsets.UTF_8.name(); // Fallback to UTF-8 in case of parsing error
        }
    }

}





