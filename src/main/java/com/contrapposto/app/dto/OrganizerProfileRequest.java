package com.contrapposto.app.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrganizerProfileRequest {

    @Size(max = 150, message = "Name must be at most 150 characters")
    private String displayName;

    @Size(max = 2000, message = "Organization info must be at most 2000 characters")
    private String orgInfo;

    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;
}
