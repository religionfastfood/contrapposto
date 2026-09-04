package com.contrapposto.app.model;

public enum SubscriptionStatus {
    NONE,    // newly registered, no subscription initiated
    TRIAL,   // within free trial period
    ACTIVE,  // active paid subscription
    LAPSED   // subscription expired or cancelled
}
