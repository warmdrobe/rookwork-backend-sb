package com.example.rookwork_backend_sb.dtos.issues;

import lombok.Data;
import java.util.UUID;

@Data
public class AssigneeResponse {
    public UUID id;
    public String profileName;
    public String picture;
}
