package com.example.rookwork_backend_sb.dtos.projects;

import lombok.Data;

@Data
public class UpdateProjectRequest {
    public String projectName;
    public Boolean isPrivate;
}
