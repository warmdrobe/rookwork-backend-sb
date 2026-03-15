package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDesc (UUID userId);
    List<Notification> findByUserIdAndIsReadFalse(UUID userId);
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);
    long countByUserIdAndIsReadFalse(UUID userId);
    List<Notification> findAllByUserId(UUID userId);
}
