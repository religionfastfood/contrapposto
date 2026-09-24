package com.contrapposto.app.event;

// Carries only the id, not the EventApplication itself -- the entity's lazy associations
// (event, model) would throw LazyInitializationException if touched after the publishing
// transaction commits, which is exactly when the @Async listener runs.
public record ApplicationSubmittedEvent(Long applicationId) {
}
