package com.contrapposto.app.service;

import com.contrapposto.app.model.EventType;
import com.contrapposto.app.repository.EventRepository;
import com.contrapposto.app.repository.EventTypeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class EventTypeServiceImplTest {

    private final EventTypeRepository eventTypeRepository = Mockito.mock(EventTypeRepository.class);
    private final EventRepository eventRepository = Mockito.mock(EventRepository.class);
    private final EventTypeServiceImpl service = new EventTypeServiceImpl(eventTypeRepository, eventRepository);

    {
        when(eventTypeRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void findAll_delegatesToRepositoryOrderedByName() {
        List<EventType> types = List.of(new EventType("Gesture"), new EventType("Portrait"));
        when(eventTypeRepository.findAllByOrderByNameAsc()).thenReturn(types);

        assertThat(service.findAll()).isEqualTo(types);
    }

    @Test
    void create_newName_trimsAndSaves() {
        when(eventTypeRepository.existsByNameIgnoreCase("Croquis")).thenReturn(false);

        EventType created = service.create("  Croquis  ");

        assertThat(created.getName()).isEqualTo("Croquis");
        verify(eventTypeRepository).save(argThat(t -> t.getName().equals("Croquis")));
    }

    @Test
    void create_duplicateNameCaseInsensitive_throws() {
        when(eventTypeRepository.existsByNameIgnoreCase("gesture")).thenReturn(true);

        assertThatThrownBy(() -> service.create("gesture"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        verify(eventTypeRepository, never()).save(any());
    }

    @Test
    void create_blankName_throws() {
        assertThatThrownBy(() -> service.create("   "))
                .isInstanceOf(IllegalArgumentException.class);
        verify(eventTypeRepository, never()).save(any());
    }

    @Test
    void delete_notInUse_deletes() {
        EventType type = new EventType("Gesture");
        when(eventTypeRepository.findById(1L)).thenReturn(Optional.of(type));
        when(eventRepository.existsByEventType(type)).thenReturn(false);

        service.delete(1L);

        verify(eventTypeRepository).delete(type);
    }

    @Test
    void delete_inUse_throwsAndDoesNotDelete() {
        EventType type = new EventType("Gesture");
        when(eventTypeRepository.findById(1L)).thenReturn(Optional.of(type));
        when(eventRepository.existsByEventType(type)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Gesture");
        verify(eventTypeRepository, never()).delete(any());
    }

    @Test
    void delete_notFound_throws() {
        when(eventTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }
}
