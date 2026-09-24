package com.contrapposto.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_applications")
@Getter
@Setter
@NoArgsConstructor
public class EventApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    private User model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationInitiator initiatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(length = 1000)
    private String message;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime respondedAt;

    public EventApplication(Event event, User model, ApplicationInitiator initiatedBy, String message) {
        this.event = event;
        this.model = model;
        this.initiatedBy = initiatedBy;
        this.message = message;
    }
}
