package com.contrapposto.app.config;

import com.contrapposto.app.model.EventType;
import com.contrapposto.app.repository.EventTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventTypeSeederTest {

    @Mock
    private EventTypeRepository eventTypeRepository;

    @Test
    void seedsAllFiveDefaultsWhenNoneExist() {
        when(eventTypeRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);

        new EventTypeSeeder(eventTypeRepository).run();

        ArgumentCaptor<EventType> captor = ArgumentCaptor.forClass(EventType.class);
        verify(eventTypeRepository, times(5)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(EventType::getName)
                .containsExactly("Gesture", "Short Pose", "Long Pose", "Portrait", "Open Studio");
    }

    @Test
    void skipsTypesThatAlreadyExist() {
        when(eventTypeRepository.existsByNameIgnoreCase("Gesture")).thenReturn(true);
        when(eventTypeRepository.existsByNameIgnoreCase("Short Pose")).thenReturn(false);
        when(eventTypeRepository.existsByNameIgnoreCase("Long Pose")).thenReturn(true);
        when(eventTypeRepository.existsByNameIgnoreCase("Portrait")).thenReturn(true);
        when(eventTypeRepository.existsByNameIgnoreCase("Open Studio")).thenReturn(true);

        new EventTypeSeeder(eventTypeRepository).run();

        ArgumentCaptor<EventType> captor = ArgumentCaptor.forClass(EventType.class);
        verify(eventTypeRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Short Pose");
    }

    @Test
    void alwaysRunsRegardlessOfDevGate_hasNoConditionalOnPropertyAnnotation() {
        assertThat(EventTypeSeeder.class.isAnnotationPresent(
                org.springframework.boot.autoconfigure.condition.ConditionalOnProperty.class)).isFalse();
    }
}
