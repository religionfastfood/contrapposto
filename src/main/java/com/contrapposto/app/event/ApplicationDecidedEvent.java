package com.contrapposto.app.event;

// See ApplicationSubmittedEvent for why this carries only the id.
public record ApplicationDecidedEvent(Long applicationId) {
}
