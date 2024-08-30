package com.example.mail.Service;

import jakarta.mail.Store;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service

public class SessionService {
    private final Map<Long, Store> sessionStore = new ConcurrentHashMap<>();

    public void saveSession(Long mailboxId, Store store) {
        sessionStore.put(mailboxId, store);
    }

    public Store getSession(Long mailboxId) {
        return sessionStore.get(mailboxId);
    }

    public void removeSession(Long mailboxId) {
        sessionStore.remove(mailboxId);
    }
}
