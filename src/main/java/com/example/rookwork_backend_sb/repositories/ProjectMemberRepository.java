package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.Entities.ProjectMember;
import com.example.rookwork_backend_sb.Entities.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository
    extends JpaRepository<ProjectMember, ProjectMemberId> {}
