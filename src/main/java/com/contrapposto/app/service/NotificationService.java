package com.contrapposto.app.service;

import com.contrapposto.app.model.EventApplication;
import com.contrapposto.app.model.User;

public interface NotificationService {

    /**
     * A new application or invitation was submitted; notify the party that needs to respond.
     */
    void notifySubmitted(EventApplication application, User recipient);

    /**
     * An application or invitation was accepted or declined; notify the party that initiated it.
     */
    void notifyDecided(EventApplication application, User recipient);
}
