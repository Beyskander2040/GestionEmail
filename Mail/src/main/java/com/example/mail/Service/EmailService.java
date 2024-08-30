package com.example.mail.Service;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import jakarta.mail.Flags;
import jakarta.mail.*;


import com.example.mail.Controlleur.MailControlleur;
import com.example.mail.Entity.Attachment;
import com.example.mail.Entity.Mail;
import com.example.mail.Repository.IAttachmentRepository;
import com.example.mail.Repository.IMailRepository;
import com.sun.mail.imap.IMAPFolder;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.search.*;
import org.apache.commons.codec.language.bm.Lang;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.EntityNotFoundException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
private SessionService sessionService ;
    @Autowired
    private IMailRepository emailRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private static Store store;
    private static Session session;
    private static String currentUserEmail;

@Override
    public Long getUidFromDatabase(Long mailId) {
        // Retrieve UID from the database
        return emailRepository.findUidByMailId(mailId);
    }
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

            final Folder finalEmailFolder = emailFolder; // Effectively final reference

            Message[] messages = finalEmailFolder.getMessages();
            int totalMessages = messages.length;

            logger.info("Total messages to process: {}", totalMessages);

            // Create a thread pool to process emails concurrently
            int threadCount = 50; // Adjust the number of threads according to your needs
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            AtomicInteger processedCount = new AtomicInteger(0);

            for (int i = 0; i < messages.length; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        // Reopen the folder if closed
                        synchronized (finalEmailFolder) {
                            if (!finalEmailFolder.isOpen()) {
                                finalEmailFolder.open(Folder.READ_ONLY);
                            }
                        }

                        logger.debug("Processing email {} of {}", index + 1, totalMessages);
                        processAndSaveEmail(messages[index], mailboxId);

                        // Update progress
                        int processed = processedCount.incrementAndGet();
                        if (processed % 10 == 0 || processed == totalMessages) {
                            int progress = processed * 100 / totalMessages;
                            logger.info("Updating progress: {}% for mailbox ID: {}", progress, mailboxId);
                            updateProgress(mailboxId, progress);
                        }
                    } catch (FolderClosedException e) {
                        logger.error("Folder closed unexpectedly, cannot process email {}", index + 1, e);
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
                if (emailFolder != null && emailFolder.isOpen()) emailFolder.close(false);
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
    private static final int BATCH_SIZE = 100;

    public List<Mail> fetchNewEmails(String email, String password, Date mostRecentEmailDate, Long mailboxId) {
        List<Mail> allEmails = new ArrayList<>();
        int offset = 0;

        try {
            Store store = getStore(email, password);
            sessionService.saveSession(mailboxId, store); // Save session
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] messages;
            do {
                messages = fetchEmailsBatch(inbox, mostRecentEmailDate, offset, BATCH_SIZE);
                List<Mail> batchEmails = processMessages(messages, inbox, mailboxId);
                allEmails.addAll(batchEmails);
                offset += messages.length;
            } while (messages.length == BATCH_SIZE);

            inbox.close(false);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        return allEmails;
    }

    private Message[] fetchEmailsBatch(Folder inbox, Date mostRecentEmailDate, int offset, int batchSize) throws MessagingException {
        SearchTerm searchTerm = new ReceivedDateTerm(ComparisonTerm.GT, mostRecentEmailDate);
        Message[] allMessages = inbox.search(searchTerm);
        int end = Math.min(offset + batchSize, allMessages.length);
        return Arrays.copyOfRange(allMessages, offset, end);
    }



    private List<Mail> processMessages(Message[] messages, Folder inbox, Long mailboxId) {
        List<Mail> emailList = new ArrayList<>();

        for (Message message : messages) {
            try {
                // Extract UID from the message if possible
                String uid = getMessageUID(message); // Method to extract UID

                // If UID is not available, handle this case
                if (uid == null) {
                    continue; // Skip this email if UID is not found
                }

                Mail mail = new Mail();
                mail.setUid(uid); // Set real UID
                mail.setSubject(message.getSubject());
                mail.setSender(((InternetAddress) message.getFrom()[0]).getAddress());
                mail.setContent(extractContent(message));
                mail.setReceivedDate(message.getReceivedDate());
                mail.setMailboxId(mailboxId);

                emailList.add(mail);

                // Save each email to the database
                emailRepository.save(mail); // Save email to the database
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return emailList;
    }

    private String getMessageUID(Message message) throws MessagingException {
        String[] uidHeader = message.getHeader("Message-ID");
        if (uidHeader != null && uidHeader.length > 0) {
            String uid = uidHeader[0]; // Adjust extraction based on your header format
            logger.debug("Extracted UID: {}", uid); // Log extracted UID
            return uid;
        }
        return null;
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

        // Extract UID from the message
        String uid = getMessageUID(message); // Method to extract UID from Message
        if (uid != null) {
            email.setUid(uid); // Set UID if available
        } else {
            logger.warn("UID not found for email with subject: " + email.getSubject());
        }

        // Handle content
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

        logger.info("Saving email: " + email.getSubject() + " with UID: " + email.getUid());
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
    @Override
    public void archiveEmail(Long mailId, Long mailboxId) {
        Store store = sessionService.getSession(mailboxId);

        if (store == null) {
            throw new RuntimeException("No session found for mailbox ID: " + mailboxId);
        }

        try {
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Folder archiveFolder = getOrCreateFolder(store, "Archive");

            Message emailToMove = findMessageById(mailId, inbox);

            // Copy the message to the Archive folder
            emailToMove.setFlag(Flags.Flag.DELETED, true); // Mark the original message for deletion
            inbox.copyMessages(new Message[]{emailToMove}, archiveFolder);

            inbox.close(true); // Save changes

            // Optionally, remove the session if no longer needed
            // sessionService.removeSession(mailboxId);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public Folder getOrCreateFolder(Store store, String folderName) throws MessagingException {
        Folder folder = store.getFolder(folderName);
        if (!folder.exists()) {
            folder.create(Folder.HOLDS_MESSAGES);
        }
        return folder;
    }



    public Message findMessageById(Long mailId, Folder folder) throws MessagingException {
        String mailIdStr = mailId.toString();
        SearchTerm searchTerm = new MessageIDTerm(mailIdStr);

        folder.open(Folder.READ_ONLY);
        Message[] messages = folder.search(searchTerm);

        return messages.length > 0 ? messages[0] : null;
    }
    // Helper method to get the UID of a message





    public Store getStore(String email, String password) throws MessagingException {
        if (store == null || !store.isConnected() || !email.equals(currentUserEmail)) {
            Properties properties = new Properties();
            properties.put("mail.store.protocol", "imaps");
            properties.put("mail.imap.host", "imap.gmail.com");
            properties.put("mail.imap.port", "993");

            session = Session.getInstance(properties);
            store = session.getStore("imaps");
            store.connect("imap.gmail.com", email, password);
            currentUserEmail = email;
        }
        return store;
    }
@Override
    public List<Map<String, Object>> getTopDomainsWithAttachments() {
        String sql = "SELECT SUBSTRING_INDEX(mail.sender, '@', -1) AS domainName, " +
                "COUNT(attachment.id) AS attachmentCount " +
                "FROM mail " +
                "JOIN attachment ON mail.id = attachment.mail_id " +
                "GROUP BY domainName " +
                "ORDER BY attachmentCount DESC " +
                "LIMIT 10";
        return jdbcTemplate.queryForList(sql);
    }


}





