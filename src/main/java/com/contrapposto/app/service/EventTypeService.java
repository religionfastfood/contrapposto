package com.contrapposto.app.service;

import com.contrapposto.app.model.EventType;

import java.util.List;

public interface EventTypeService {

    List<EventType> findAll();

    EventType create(String name);

    void delete(Long id);
}
