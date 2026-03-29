package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.ProjectMember;
import com.example.rookwork_backend_sb.entities.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectMemberRepository
    extends JpaRepository<ProjectMember, ProjectMemberId> {
    List<ProjectMember> findAllByUser_Id(UUID userId);
    List<ProjectMember> findAllByProject_Id(UUID projectId);
    boolean existsById (ProjectMemberId id);
}
