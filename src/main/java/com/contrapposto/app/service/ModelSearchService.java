package com.contrapposto.app.service;

import com.contrapposto.app.model.ModelProfile;

import java.util.List;
import java.util.Optional;

public interface ModelSearchService {

    /**
     * Models visible to organizer search: subscription active, optionally filtered by city.
     *
     * @param city exact city match (case-insensitive), or blank/null for no filter
     */
    List<ModelProfile> search(String city);

    /**
     * A single model's profile, only if it's currently visible to organizer search.
     */
    Optional<ModelProfile> findVisibleProfile(Long modelProfileId);
}
