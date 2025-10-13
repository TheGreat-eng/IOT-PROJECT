// File: src/main/java/com/example/iotserver/service/AIService.java

package com.example.iotserver.service;

import com.example.iotserver.dto.AIPredictionResponse;
import com.example.iotserver.dto.SensorDataDTO;
import com.example.iotserver.entity.Device;
import com.example.iotserver.repository.DeviceRepository; // <-- THÊM IMPORT
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional; // <-- THÊM IMPORT

@Service
@Slf4j
@RequiredArgsConstructor
public class AIService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final SensorDataService sensorDataService;
    private final DeviceRepository deviceRepository; // <-- THÊM VÀO CONSTRUCTOR

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public AIPredictionResponse getPredictions(Long farmId) {
        try {
            // SỬA LOGIC Ở ĐÂY: Tìm thiết bị cảm biến đất trong farm
            Optional<Device> soilSensor = deviceRepository
                    .findByFarmIdAndType(farmId, Device.DeviceType.SENSOR_SOIL_MOISTURE)
                    .stream()
                    .findFirst();

            if (soilSensor.isEmpty()) {
                log.warn("Không tìm thấy cảm biến đất nào trong farm {} để lấy dữ liệu cho AI", farmId);
                return null;
            }

            String deviceId = soilSensor.get().getDeviceId();
            log.info("Sử dụng device {} để lấy dữ liệu cho AI của farm {}", deviceId, farmId);

            // Lấy dữ liệu lịch sử làm đầu vào cho AI model
            List<SensorDataDTO> historicalData = sensorDataService.getSensorDataRange(
                    deviceId, // <-- SỬ DỤNG deviceId ĐỘNG
                    java.time.Instant.now().minus(java.time.Duration.ofHours(24)),
                    java.time.Instant.now());

            // ... phần còn lại giữ nguyên
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("farm_id", farmId);
            requestBody.put("historical_data", historicalData);

            log.info("Đang gửi request tới AI Service: {}", aiServiceUrl);

            AIPredictionResponse response = restTemplate.postForObject(
                    aiServiceUrl,
                    requestBody,
                    AIPredictionResponse.class);

            log.info("✅ Nhận được phản hồi từ AI Service");
            return response;

        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi AI Service: {}", e.getMessage());
            return null;
        }
    }
}