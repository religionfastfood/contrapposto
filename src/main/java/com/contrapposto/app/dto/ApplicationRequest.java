package com.contrapposto.app.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApplicationRequest {

    @Size(max = 1000, message = "Message must be at most 1000 characters")
    private String message;
}
