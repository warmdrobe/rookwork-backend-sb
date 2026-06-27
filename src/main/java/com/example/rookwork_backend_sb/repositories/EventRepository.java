package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByProjectId(UUID projectId);

    @Query("SELECT DISTINCT e FROM Event e LEFT JOIN e.guests g WHERE e.user.id = :userId OR g.id = :userId")
    List<Event> findByUserIdOrGuestId(@Param("userId") UUID userId);
}
