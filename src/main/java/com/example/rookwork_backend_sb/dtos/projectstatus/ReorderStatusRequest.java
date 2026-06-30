package com.example.rookwork_backend_sb.dtos.projectstatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ReorderStatusRequest {

    @NotNull(message = "statusOrders must not be null")
    @Valid
    private List<StatusOrder> statusOrders;

    @Data
    public static class StatusOrder {

        @NotNull(message = "statusId is required")
        private UUID statusId;

        @Min(value = 1, message = "position must be >= 1")
        private int position;

        /** Optimistic-lock version — must match current DB value to detect conflicts. */
        @NotNull(message = "version is required")
        private Long version;
    }
}
