package com.example.iotserver.service;

import com.example.iotserver.repository.DeviceRepository;
import com.example.iotserver.repository.RuleExecutionLogRepository;
import com.example.iotserver.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final DeviceRepository deviceRepository;
    private final RuleRepository ruleRepository;
    private final RuleExecutionLogRepository logRepository;
    private final SensorDataService sensorDataService;

    public Map<String, Object> getDashboardSummary(Long farmId) {
        Map<String, Object> summary = new HashMap<>();

        // Thống kê thiết bị
        long totalDevices = deviceRepository.countByFarmId(farmId);
        long onlineDevices = deviceRepository.countByFarmIdAndStatus(farmId,
                com.example.iotserver.entity.Device.DeviceStatus.ONLINE);
        summary.put("totalDevices", totalDevices);
        summary.put("onlineDevices", onlineDevices);

        // Thống kê quy tắc
        long totalRules = ruleRepository.countByFarmId(farmId);
        long enabledRules = ruleRepository.countByFarmIdAndEnabled(farmId, true);
        summary.put("totalRules", totalRules);
        summary.put("enabledRules", enabledRules);

        // Lấy dữ liệu môi trường trung bình (từ service đã có)
        Map<String, Object> avgData = new HashMap<>();
        try {
            // Lấy dữ liệu mới nhất của TẤT CẢ thiết bị trong farm
            Map<String, Map<String, Object>> latestFarmData = sensorDataService.getFarmLatestData(farmId);

            // Lọc ra các giá trị khác null để tính trung bình
            double avgTemperature = latestFarmData.values().stream()
                    .filter(data -> data.containsKey("temperature") && data.get("temperature") != null)
                    .mapToDouble(data -> ((Number) data.get("temperature")).doubleValue())
                    .average()
                    .orElse(Double.NaN); // Dùng NaN nếu không có dữ liệu

            double avgHumidity = latestFarmData.values().stream()
                    .filter(data -> data.containsKey("humidity") && data.get("humidity") != null)
                    .mapToDouble(data -> ((Number) data.get("humidity")).doubleValue())
                    .average()
                    .orElse(Double.NaN);

            double avgLightIntensity = latestFarmData.values().stream()
                    .filter(data -> data.containsKey("light_intensity") && data.get("light_intensity") != null)
                    .mapToDouble(data -> ((Number) data.get("light_intensity")).doubleValue())
                    .average()
                    .orElse(Double.NaN);
            double avgSoilMoisture = latestFarmData.values().stream()
                    .filter(data -> data.containsKey("soil_moisture") && data.get("soil_moisture") != null)
                    .mapToDouble(data -> ((Number) data.get("soil_moisture")).doubleValue())
                    .average()
                    .orElse(Double.NaN);
            double avgSoilPH = latestFarmData.values().stream()
                    .filter(data -> data.containsKey("soilPH") && data.get("soilPH") != null)
                    .mapToDouble(data -> ((Number) data.get("soilPH")).doubleValue())
                    .average()
                    .orElse(Double.NaN);

            // Đưa giá trị vào map avgData (chỉ đưa vào nếu nó là số)
            if (!Double.isNaN(avgTemperature))
                avgData.put("avgTemperature", Math.round(avgTemperature * 10) / 10.0);
            if (!Double.isNaN(avgHumidity))
                avgData.put("avgHumidity", Math.round(avgHumidity * 10) / 10.0);
            if (!Double.isNaN(avgLightIntensity))
                avgData.put("avgLightIntensity", Math.round(avgLightIntensity));
            if (!Double.isNaN(avgSoilMoisture))
                avgData.put("avgSoilMoisture", Math.round(avgSoilMoisture * 10) / 10.0);
            if (!Double.isNaN(avgSoilPH))
                avgData.put("avgSoilPH", Math.round(avgSoilPH * 100) / 100.0); // pH lấy 2 chữ số thập phân

        } catch (Exception e) {
            // Ghi log lỗi nhưng không làm crash ứng dụng
            // logger.error("Không thể tính dữ liệu môi trường trung bình", e);
        }

        summary.put("averageEnvironment", avgData);
        // ===================================

        return summary;
    }
}