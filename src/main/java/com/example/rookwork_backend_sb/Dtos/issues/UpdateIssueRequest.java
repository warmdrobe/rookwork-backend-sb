package com.example.rookwork_backend_sb.Dtos.issues;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateIssueRequest {
    public String issueName;
    public String description;
    public String priority;
    public String status;
    public String parentId;
    public LocalDate deadline;

}
