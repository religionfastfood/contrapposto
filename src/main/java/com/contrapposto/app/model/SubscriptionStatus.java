package com.contrapposto.app.model;

import java.util.List;

public enum SubscriptionStatus {
    NONE,    // newly registered, no subscription initiated
    TRIAL,   // within free trial period
    ACTIVE,  // active paid subscription
    LAPSED;  // subscription expired or cancelled

    private static final List<SubscriptionStatus> ACTIVE_STATUSES = List.of(TRIAL, ACTIVE);

    // statuses that count as "actively subscribed", e.g. for gating search visibility
    public static List<SubscriptionStatus> active() {
        return ACTIVE_STATUSES;
    }

    public boolean isActive() {
        return ACTIVE_STATUSES.contains(this);
    }
}
