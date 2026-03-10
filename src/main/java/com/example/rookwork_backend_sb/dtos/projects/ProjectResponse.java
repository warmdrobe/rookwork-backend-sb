package com.example.rookwork_backend_sb.dtos.projects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"id", "projectName", "isPrivate", "ownerName", "createdAt", "updatedAt"})
public class ProjectResponse {
    public UUID id;
    public String projectName;
    @JsonProperty("isPrivate")
    public boolean isPrivate;
    public String ownerName;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
