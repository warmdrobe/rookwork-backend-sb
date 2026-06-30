package com.example.rookwork_backend_sb.dtos.issues;

import lombok.Data;

@Data
public class CreateIssueTypeRequest {
    private String name;
    private String description;
    private String iconKey;
    private String color;
}
