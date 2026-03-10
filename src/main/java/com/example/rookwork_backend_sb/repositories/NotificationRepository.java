package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUser_IdOrderByCreatedAtDesc (UUID userId);

    List<Notification> findByUser_IdAndIsReadFalse(UUID userId);
    List<Notification> findAllByUser_Id(UUID userId);
}
