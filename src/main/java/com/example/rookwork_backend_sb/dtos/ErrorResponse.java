package com.example.rookwork_backend_sb.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;


@Getter
@Setter
@Builder
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;
    private Instant timestamp;
}