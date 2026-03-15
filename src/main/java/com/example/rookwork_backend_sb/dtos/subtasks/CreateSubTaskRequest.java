package com.example.rookwork_backend_sb.dtos.subtasks;

import lombok.Data;

@Data
public class CreateSubTaskRequest {
    private String subtaskName;
    private String subtaskDescription;
}