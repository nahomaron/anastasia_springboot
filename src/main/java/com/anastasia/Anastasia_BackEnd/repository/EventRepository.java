package com.anastasia.Anastasia_BackEnd.repository;

import com.anastasia.Anastasia_BackEnd.model.event.EventEntity;
import com.anastasia.Anastasia_BackEnd.model.event.EventManagerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {
    List<EventManagerEntity> findAllManagersByEventId(Long eventId);
}
