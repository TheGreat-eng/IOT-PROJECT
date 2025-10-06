package com.example.iotserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.iotserver.dto.SensorDataDTO;
import com.example.iotserver.entity.Device;
import com.example.iotserver.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqttMessageHandler {

    private final DeviceRepository deviceRepository;
    private final SensorDataService sensorDataService;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
        try {
            MessageHeaders headers = message.getHeaders();
            String topic = (String) headers.get("mqtt_receivedTopic");
            String payload = message.getPayload().toString();

            log.info("Received MQTT message - Topic: {}, Payload: {}", topic, payload);

            if (topic.startsWith("sensor/")) {
                handleSensorData(topic, payload);
            } else if (topic.startsWith("device/")) {
                handleDeviceStatus(topic, payload);
            }

        } catch (Exception e) {
            log.error("Error handling MQTT message: {}", e.getMessage(), e);
        }
    }

    private void handleSensorData(String topic, String payload) {
        try {
            // Extract deviceId from topic: sensor/{deviceId}/data
            String deviceId = topic.split("/")[1];

            // Parse JSON payload
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);

            // Create DTO
            SensorDataDTO sensorData = SensorDataDTO.fromMqttPayload(deviceId, data);

            // Update device last seen
            deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
                device.setLastSeen(LocalDateTime.now());
                device.setStatus(Device.DeviceStatus.ONLINE);
                deviceRepository.save(device);

                sensorData.setFarmId(device.getFarm().getId());
            });

            // Save to InfluxDB
            sensorDataService.saveSensorData(sensorData);

            // TODO: Send via WebSocket to connected clients

            log.info("Processed sensor data from device: {}", deviceId);

        } catch (Exception e) {
            log.error("Error processing sensor data: {}", e.getMessage(), e);
        }
    }

    private void handleDeviceStatus(String topic, String payload) {
        try {
            // Extract deviceId from topic: device/{deviceId}/status
            String deviceId = topic.split("/")[1];

            @SuppressWarnings("unchecked")
            Map<String, Object> status = objectMapper.readValue(payload, Map.class);

            deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
                String statusStr = status.get("status").toString();
                device.setStatus(Device.DeviceStatus.valueOf(statusStr.toUpperCase()));
                device.setLastSeen(LocalDateTime.now());
                deviceRepository.save(device);

                log.info("Updated device status: {} - {}", deviceId, statusStr);
            });

        } catch (Exception e) {
            log.error("Error processing device status: {}", e.getMessage(), e);
        }
    }
}