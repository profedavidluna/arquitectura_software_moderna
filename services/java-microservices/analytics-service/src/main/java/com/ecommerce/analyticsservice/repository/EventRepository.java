package com.ecommerce.analyticsservice.repository;

import com.ecommerce.analyticsservice.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findByEventType(String eventType, Pageable pageable);

    Page<Event> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<Event> findByEventTypeAndCreatedAtBetween(
            String eventType, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.eventType = :eventType AND e.createdAt BETWEEN :from AND :to")
    long countByEventTypeAndDateRange(
            @Param("eventType") String eventType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT DISTINCT e.userId FROM Event e WHERE e.createdAt BETWEEN :from AND :to AND e.userId IS NOT NULL")
    List<UUID> findDistinctActiveUsers(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(DISTINCT e.userId) FROM Event e WHERE e.createdAt BETWEEN :from AND :to AND e.userId IS NOT NULL")
    long countDistinctActiveUsers(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(DISTINCT e.sessionId) FROM Event e WHERE e.createdAt BETWEEN :from AND :to AND e.sessionId IS NOT NULL")
    long countDistinctSessions(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT e FROM Event e WHERE e.eventType = :eventType AND e.createdAt BETWEEN :from AND :to")
    List<Event> findAllByEventTypeAndDateRange(
            @Param("eventType") String eventType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
