package com.example.rookwork_backend_sb.dtos.issues;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueTypeResponse {
    private UUID id;
    private String name;
    private String description;
    private String iconKey;
    private String color;

    @JsonProperty("isSystem")
    private boolean isSystem;
}
