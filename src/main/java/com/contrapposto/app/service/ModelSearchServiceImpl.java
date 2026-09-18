package com.contrapposto.app.service;

import com.contrapposto.app.model.ModelProfile;
import com.contrapposto.app.model.SubscriptionStatus;
import com.contrapposto.app.repository.ModelProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
public class ModelSearchServiceImpl implements ModelSearchService {

    private final ModelProfileRepository modelProfileRepository;

    public ModelSearchServiceImpl(ModelProfileRepository modelProfileRepository) {
        this.modelProfileRepository = modelProfileRepository;
    }

    @Override
    public List<ModelProfile> search(String city) {
        if (StringUtils.hasText(city)) {
            return modelProfileRepository.findByUser_SubscriptionStatusInAndCityIgnoreCase(
                    SubscriptionStatus.active(), city.trim());
        }
        return modelProfileRepository.findByUser_SubscriptionStatusIn(SubscriptionStatus.active());
    }

    @Override
    public Optional<ModelProfile> findVisibleProfile(Long modelProfileId) {
        return modelProfileRepository.findById(modelProfileId)
                .filter(profile -> profile.getUser().getSubscriptionStatus().isActive());
    }
}
