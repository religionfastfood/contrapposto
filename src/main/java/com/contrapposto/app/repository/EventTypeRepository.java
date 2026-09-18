package com.contrapposto.app.repository;

import com.contrapposto.app.model.EventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventTypeRepository extends JpaRepository<EventType, Long> {

    List<EventType> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
