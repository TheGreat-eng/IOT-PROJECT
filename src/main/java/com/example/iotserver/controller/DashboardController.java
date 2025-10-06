package com.example.iotserver.controller;

import com.example.iotserver.service.DeviceService;
import com.example.iotserver.service.SensorDataService;
import com.example.iotserver.repository.DeviceRepository;
import com.example.iotserver.entity.Device;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DeviceService deviceService;
    private final SensorDataService sensorDataService;
    private final DeviceRepository deviceRepository;

    /**
     * Get dashboard overview for a farm
     * GET /api/dashboard/farm/{farmId}
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<Map<String, Object>> getFarmDashboard(@PathVariable Long farmId) {
        Map<String, Object> dashboard = new HashMap<>();

        // Device statistics
        long totalDevices = deviceRepository.countByFarmId(farmId);
        long onlineDevices = deviceRepository.countByFarmIdAndStatus(
                farmId, Device.DeviceStatus.ONLINE);
        long offlineDevices = totalDevices - onlineDevices;

        dashboard.put("totalDevices", totalDevices);
        dashboard.put("onlineDevices", onlineDevices);
        dashboard.put("offlineDevices", offlineDevices);

        // Latest sensor data for all devices
        Map<String, Map<String, Object>> latestData = sensorDataService.getFarmLatestData(farmId);
        dashboard.put("latestSensorData", latestData);

        // Device list
        dashboard.put("devices", deviceService.getDevicesByFarm(farmId));

        return ResponseEntity.ok(dashboard);
    }

    /**
     * Get real-time stats
     * GET /api/dashboard/stats?farmId=1
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRealtimeStats(@RequestParam Long farmId) {
        Map<String, Object> stats = new HashMap<>();

        // Calculate averages from latest data
        Map<String, Map<String, Object>> latestData = sensorDataService.getFarmLatestData(farmId);

        double avgTemperature = latestData.values().stream()
                .filter(data -> data.containsKey("temperature"))
                .mapToDouble(data -> (Double) data.get("temperature"))
                .average()
                .orElse(0.0);

        double avgHumidity = latestData.values().stream()
                .filter(data -> data.containsKey("humidity"))
                .mapToDouble(data -> (Double) data.get("humidity"))
                .average()
                .orElse(0.0);

        double avgSoilMoisture = latestData.values().stream()
                .filter(data -> data.containsKey("soil_moisture"))
                .mapToDouble(data -> (Double) data.get("soil_moisture"))
                .average()
                .orElse(0.0);

        stats.put("avgTemperature", Math.round(avgTemperature * 10) / 10.0);
        stats.put("avgHumidity", Math.round(avgHumidity * 10) / 10.0);
        stats.put("avgSoilMoisture", Math.round(avgSoilMoisture * 10) / 10.0);
        stats.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(stats);
    }
}
