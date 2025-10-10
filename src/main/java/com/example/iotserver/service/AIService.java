package com.example.iotserver.service;

import com.example.iotserver.dto.AIPredictionResponse;
import com.example.iotserver.dto.SensorDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final SensorDataService sensorDataService;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public AIPredictionResponse getPredictions(Long farmId) {
        try {
            // Lấy dữ liệu lịch sử làm đầu vào cho AI model
            // Ví dụ: lấy dữ liệu 24 giờ qua
            List<SensorDataDTO> historicalData = sensorDataService.getSensorDataRange(
                    "SOIL-001", // Cần một cơ chế để chọn deviceId phù hợp
                    java.time.Instant.now().minus(java.time.Duration.ofHours(24)),
                    java.time.Instant.now());

            // Tạo request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("farm_id", farmId);
            requestBody.put("historical_data", historicalData);

            log.info("Đang gửi request tới AI Service: {}", aiServiceUrl);

            // Gọi API của Python Service
            AIPredictionResponse response = restTemplate.postForObject(
                    aiServiceUrl,
                    requestBody,
                    AIPredictionResponse.class);

            log.info("✅ Nhận được phản hồi từ AI Service");
            return response;

        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi AI Service: {}", e.getMessage());
            // Trả về null hoặc một response mặc định khi có lỗi
            return null;
        }
    }
}