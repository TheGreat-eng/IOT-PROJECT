package com.example.iotserver.controller;

import com.example.iotserver.dto.DeviceDTO;
import com.example.iotserver.dto.SensorDataDTO;
import com.example.iotserver.dto.response.ApiResponse;
import com.example.iotserver.service.DeviceService;
import com.example.iotserver.service.SensorDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final SensorDataService sensorDataService;

    /**
     * Create new device
     * POST /api/devices?farmId=1
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DeviceDTO>> createDevice(
            @RequestParam Long farmId,
            @RequestBody DeviceDTO dto) {
        DeviceDTO created = deviceService.createDevice(farmId, dto);
        return ResponseEntity.ok(ApiResponse.success("Device created successfully", created));
    }

    /**
     * Update device
     * PUT /api/devices/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DeviceDTO>> updateDevice(
            @PathVariable Long id,
            @RequestBody DeviceDTO dto) {
        DeviceDTO updated = deviceService.updateDevice(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Device updated successfully", updated));
    }

    /**
     * Delete device
     * DELETE /api/devices/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.ok(ApiResponse.success("Device deleted successfully", null));
    }

    /**
     * Get device by ID
     * GET /api/devices/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeviceDTO>> getDevice(@PathVariable Long id) {
        DeviceDTO device = deviceService.getDevice(id);
        return ResponseEntity.ok(ApiResponse.success(device));
    }

    /**
     * Get device with latest data
     * GET /api/devices/{deviceId}/full
     */
    @GetMapping("/{deviceId}/full")
    public ResponseEntity<ApiResponse<DeviceDTO>> getDeviceWithData(@PathVariable String deviceId) {
        DeviceDTO device = deviceService.getDeviceWithLatestData(deviceId);
        return ResponseEntity.ok(ApiResponse.success(device));
    }

    /**
     * Get all devices for a farm
     * GET /api/devices?farmId=1
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DeviceDTO>>> getDevicesByFarm(
            @RequestParam Long farmId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "false") boolean withData) {

        List<DeviceDTO> devices;
        if (type != null) {
            devices = deviceService.getDevicesByFarmAndType(farmId, type);
        } else if (withData) {
            devices = deviceService.getDevicesByFarmWithData(farmId);
        } else {
            devices = deviceService.getDevicesByFarm(farmId);
        }

        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    /**
     * Get online devices
     * GET /api/devices/online?farmId=1
     */
    @GetMapping("/online")
    public ResponseEntity<ApiResponse<List<DeviceDTO>>> getOnlineDevices(@RequestParam Long farmId) {
        List<DeviceDTO> devices = deviceService.getOnlineDevices(farmId);
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    /**
     * Control device (turn on/off)
     * POST /api/devices/{deviceId}/control
     * Body: {"action": "turn_on", "duration": 300}
     */
    @PostMapping("/{deviceId}/control")
    public ResponseEntity<ApiResponse<Map<String, String>>> controlDevice(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> command) {
        String action = (String) command.get("action");
        deviceService.controlDevice(deviceId, action, command);

        return ResponseEntity.ok(ApiResponse.success(
                "Command sent successfully",
                Map.of("status", "success", "message", "Command sent to device " + deviceId)));
    }

    /**
     * Get latest sensor data for a device
     * GET /api/devices/{deviceId}/data/latest
     */
    @GetMapping("/{deviceId}/data/latest")
    public ResponseEntity<ApiResponse<SensorDataDTO>> getLatestData(@PathVariable String deviceId) {
        SensorDataDTO data = sensorDataService.getLatestSensorData(deviceId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Get sensor data for time range
     * GET /api/devices/{deviceId}/data?start=...&end=...
     */
    @GetMapping("/{deviceId}/data")
    public ResponseEntity<ApiResponse<List<SensorDataDTO>>> getSensorDataRange(
            @PathVariable String deviceId,
            @RequestParam String start,
            @RequestParam String end) {
        Instant startTime = Instant.parse(start);
        Instant endTime = Instant.parse(end);

        List<SensorDataDTO> data = sensorDataService.getSensorDataRange(deviceId, startTime, endTime);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Get aggregated data for charts
     * GET /api/devices/{deviceId}/data/aggregated?field=temperature&window=1h
     */
    @GetMapping("/{deviceId}/data/aggregated")
    public ResponseEntity<ApiResponse<List<SensorDataDTO>>> getAggregatedData(
            @PathVariable String deviceId,
            @RequestParam String field,
            @RequestParam(defaultValue = "mean") String aggregation,
            @RequestParam(defaultValue = "1h") String window) {
        List<SensorDataDTO> data = sensorDataService.getAggregatedData(
                deviceId, field, aggregation, window);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
