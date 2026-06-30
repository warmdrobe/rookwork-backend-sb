package com.example.rookwork_backend_sb.dtos.projectstatus;

import com.example.rookwork_backend_sb.entities.StatusCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStatusResponse {
    private UUID id;
    private String statusName;
    private String color;
    private int position;
    private StatusCategory statusCategory;
    private long version;
}
