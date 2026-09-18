package com.contrapposto.app.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EventRequest {

    @NotNull(message = "Event type is required")
    private Long eventTypeId;

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;

    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location must be at most 255 characters")
    private String location;

    @NotNull(message = "Date and time are required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;

    @DecimalMin(value = "0", message = "Price cannot be negative")
    private BigDecimal priceAmount;

    @Size(max = 500, message = "External link must be at most 500 characters")
    // rendered as a clickable <a href> on the public event page, so restrict to http(s) --
    // otherwise a javascript: URI would execute in any visitor's session when clicked
    @Pattern(regexp = "^$|^https?://.+", message = "External link must start with http:// or https://")
    private String externalLink;
}
