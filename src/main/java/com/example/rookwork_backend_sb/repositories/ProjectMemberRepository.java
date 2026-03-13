package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.ProjectMember;
import com.example.rookwork_backend_sb.entities.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectMemberRepository
    extends JpaRepository<ProjectMember, ProjectMemberId> {
    List<ProjectMember> findAllByUserId(UUID userId);
    boolean existsById (ProjectMemberId id);
}
