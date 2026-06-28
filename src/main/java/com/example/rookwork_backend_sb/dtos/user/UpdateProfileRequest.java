package com.example.rookwork_backend_sb.dtos.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {
    private String profileName;
    private String picture;
    private String jobTitle;
    private String organization;
    private String location;
    private Boolean emailPublic;
    private Boolean jobTitlePublic;
    private Boolean organizationPublic;
    private Boolean locationPublic;
}
