package com.contrapposto.app.repository;

import com.contrapposto.app.model.EventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EventTypeRepositoryTest {

    @Autowired
    private EventTypeRepository eventTypeRepository;

    @Test
    void findAllByOrderByNameAsc_returnsAlphabeticalOrder() {
        eventTypeRepository.save(new EventType("Portrait"));
        eventTypeRepository.save(new EventType("Gesture"));
        eventTypeRepository.save(new EventType("Long Pose"));

        List<EventType> found = eventTypeRepository.findAllByOrderByNameAsc();

        assertThat(found).extracting(EventType::getName).containsExactly("Gesture", "Long Pose", "Portrait");
    }

    @Test
    void existsByNameIgnoreCase_matchesRegardlessOfCase() {
        eventTypeRepository.save(new EventType("Gesture"));

        assertThat(eventTypeRepository.existsByNameIgnoreCase("gesture")).isTrue();
        assertThat(eventTypeRepository.existsByNameIgnoreCase("GESTURE")).isTrue();
        assertThat(eventTypeRepository.existsByNameIgnoreCase("Croquis")).isFalse();
    }

    @Test
    void save_exactDuplicateName_violatesUniqueConstraint() {
        eventTypeRepository.save(new EventType("Gesture"));

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> eventTypeRepository.saveAndFlush(new EventType("Gesture")));
    }
}
