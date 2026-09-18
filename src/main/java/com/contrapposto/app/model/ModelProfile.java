package com.contrapposto.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "model_profiles")
@Getter
@Setter
@NoArgsConstructor
public class ModelProfile {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @Column(length = 2000)
    private String bio;

    @Column
    private String contactInfo;

    @Column
    private String socialMediaLinks;

    @Column
    private String city;

    @ElementCollection
    @CollectionTable(name = "model_profile_photos", joinColumns = @JoinColumn(name = "model_profile_id"))
    @OrderColumn(name = "position")
    @Column(name = "photo_url")
    private List<String> photoUrls = new ArrayList<>();

    // Deliberately leaves id null — @MapsId derives it from `user` at persist time.
    // Pre-setting it would make Spring Data's isNew() check treat this as an existing
    // row and call merge() instead of persist(), breaking the derived-id association.
    public ModelProfile(User user) {
        this.user = user;
    }
}
