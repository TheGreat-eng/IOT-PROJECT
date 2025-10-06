package com.example.iotserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDTO {

    private Long id;
    private String deviceId;
    private String name;
    private String description;
    private String type;
    private String status;
    private Long farmId;
    private String farmName;
    private LocalDateTime lastSeen;
    private String metadata; // JSON string
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
