package com.example.rookwork_backend_sb.dtos.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitionDto {
    private UUID id;
    private UUID fromStatusId;
    private String fromStatusName;
    private UUID toStatusId;
    private String toStatusName;
}
