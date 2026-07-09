package com.example.rookwork_backend_sb.dtos.issues;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateIssueTypeRequest {
    @NotBlank(message = "Issue type name must not be blank")
    @Size(max = 100, message = "Issue type name must not exceed 100 characters")
    private String name;
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    private String iconKey;
    private String color;
}
