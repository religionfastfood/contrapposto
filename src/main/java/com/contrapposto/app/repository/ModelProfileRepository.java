package com.contrapposto.app.repository;

import com.contrapposto.app.model.ModelProfile;
import com.contrapposto.app.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ModelProfileRepository extends JpaRepository<ModelProfile, Long> {

    List<ModelProfile> findByUser_SubscriptionStatusIn(Collection<SubscriptionStatus> statuses);

    List<ModelProfile> findByUser_SubscriptionStatusInAndCityIgnoreCase(
            Collection<SubscriptionStatus> statuses, String city);
}
