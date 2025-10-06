package com.example.iotserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorDataDTO {

    private String deviceId;
    private String sensorType;
    private Instant timestamp;
    private Map<String, Double> values; // Flexible for different sensor types

    // Common sensor fields
    private Double temperature;
    private Double humidity;
    private Double soilMoisture;
    private Double lightIntensity;
    private Double ph;

    // Device metadata
    private Long farmId;
    private String location;

    // Helper method to create from MQTT payload
    public static SensorDataDTO fromMqttPayload(String deviceId, Map<String, Object> payload) {
        SensorDataDTOBuilder builder = SensorDataDTO.builder()
                .deviceId(deviceId)
                .timestamp(Instant.now());

        if (payload.containsKey("temperature")) {
            builder.temperature(Double.valueOf(payload.get("temperature").toString()));
        }
        if (payload.containsKey("humidity")) {
            builder.humidity(Double.valueOf(payload.get("humidity").toString()));
        }
        if (payload.containsKey("soilMoisture")) {
            builder.soilMoisture(Double.valueOf(payload.get("soilMoisture").toString()));
        }
        if (payload.containsKey("lightIntensity")) {
            builder.lightIntensity(Double.valueOf(payload.get("lightIntensity").toString()));
        }
        if (payload.containsKey("ph")) {
            builder.ph(Double.valueOf(payload.get("ph").toString()));
        }
        if (payload.containsKey("sensorType")) {
            builder.sensorType(payload.get("sensorType").toString());
        }

        return builder.build();
    }
}
