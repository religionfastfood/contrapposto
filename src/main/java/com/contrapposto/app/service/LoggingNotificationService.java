package com.contrapposto.app.service;

import com.contrapposto.app.model.EventApplication;
import com.contrapposto.app.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// Stand-in until real email (Spring Mail/SES) is wired up -- logs what would have been sent.
@Service
public class LoggingNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationService.class);

    @Override
    public void notifySubmitted(EventApplication application, User recipient) {
        log.info("[notification] {} for event '{}' submitted by {} -- notifying {}",
                application.getInitiatedBy(), application.getEvent().getTitle(),
                application.getInitiatedBy(), recipient.getEmail());
    }

    @Override
    public void notifyDecided(EventApplication application, User recipient) {
        log.info("[notification] {} for event '{}' was {} -- notifying {}",
                application.getInitiatedBy(), application.getEvent().getTitle(),
                application.getStatus(), recipient.getEmail());
    }
}
