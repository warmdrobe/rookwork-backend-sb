package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.Invitation;
import com.example.rookwork_backend_sb.entities.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    List<Invitation> findByInvitedUser_IdAndStatus(UUID userId, InvitationStatus status);
    Optional<Invitation> findByProject_IdAndInvitedUser_Id(UUID projectId, UUID userId);
}