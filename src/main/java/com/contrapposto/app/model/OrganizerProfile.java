package com.contrapposto.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "organizer_profiles")
@Getter
@Setter
@NoArgsConstructor
public class OrganizerProfile {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @Column
    private String displayName;

    @Column(length = 2000)
    private String orgInfo;

    @Column
    private String city;

    // Deliberately leaves id null — @MapsId derives it from `user` at persist time.
    // Pre-setting it would make Spring Data's isNew() check treat this as an existing
    // row and call merge() instead of persist(), breaking the derived-id association.
    public OrganizerProfile(User user) {
        this.user = user;
    }
}
