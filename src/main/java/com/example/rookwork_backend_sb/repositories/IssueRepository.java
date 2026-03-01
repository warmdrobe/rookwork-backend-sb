package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.Entities.Issue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueRepository extends JpaRepository<Issue, UUID> {

    Optional<Issue> findById(UUID uuid);

}
