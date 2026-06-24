package com.example.rookwork_backend_sb.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class UserSummary {
    public UUID id;
    public String profileName;
    public String picture;
    public String email;
    public String jobTitle;
    public String language;
    public String timezone;
    public String organization;
    public String location;
    public boolean emailPublic;
    public boolean jobTitlePublic;
    public boolean organizationPublic;
    public boolean locationPublic;
    public boolean notifyIssueAssigned;
    public boolean notifyMentioned;
    public boolean notifyProjectUpdates;
    public boolean notifyDailyDigest;
}
