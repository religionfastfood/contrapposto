package com.contrapposto.app.service;

// Public-safe summary of the model accepted for an event -- first name only, not the model's full
// display name, since this is shown to anonymous visitors on public event pages (unlike the
// organizer-only model search/profile view, which shows full profile details).
public record AssignedModelView(String firstName, String photoUrl) {
}
