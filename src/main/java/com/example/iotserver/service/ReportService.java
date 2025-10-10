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
        // Logic tính trung bình có thể được thêm vào SensorDataService
        // hoặc tính toán ở đây dựa trên dữ liệu lấy về.
        // Tạm thời để trống
        summary.put("averageEnvironment", avgData);

        return summary;
    }
}