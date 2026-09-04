package com.contrapposto.app.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModelProfileRequest {

    @Size(max = 2000, message = "Bio must be at most 2000 characters")
    private String bio;

    @Size(max = 255, message = "Contact info must be at most 255 characters")
    private String contactInfo;

    @Size(max = 500, message = "Social media links must be at most 500 characters")
    private String socialMediaLinks;

    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;
}
