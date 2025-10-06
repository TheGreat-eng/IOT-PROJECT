package com.example.iotserver.service;

import com.example.iotserver.dto.DeviceDTO;
import com.example.iotserver.entity.Device;
import com.example.iotserver.entity.Farm;
import com.example.iotserver.repository.DeviceRepository;
import com.example.iotserver.repository.FarmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final FarmRepository farmRepository;

    @Transactional
    public DeviceDTO createDevice(Long farmId, DeviceDTO dto) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new RuntimeException("Farm not found"));

        String deviceId = dto.getDeviceId() != null ? dto.getDeviceId() : generateDeviceId();

        if (deviceRepository.existsByDeviceId(deviceId)) {
            throw new RuntimeException("Device ID already exists");
        }

        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setName(dto.getName());
        device.setDescription(dto.getDescription());
        device.setType(Device.DeviceType.valueOf(dto.getType()));
        device.setStatus(Device.DeviceStatus.OFFLINE);
        device.setFarm(farm);
        device.setMetadata(dto.getMetadata());

        Device saved = deviceRepository.save(device);
        log.info("Created device: {} for farm: {}", saved.getDeviceId(), farmId);

        return mapToDTO(saved);
    }

    @Transactional
    public DeviceDTO updateDevice(Long deviceId, DeviceDTO dto) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (dto.getName() != null)
            device.setName(dto.getName());
        if (dto.getDescription() != null)
            device.setDescription(dto.getDescription());
        if (dto.getMetadata() != null)
            device.setMetadata(dto.getMetadata());
        if (dto.getStatus() != null) {
            device.setStatus(Device.DeviceStatus.valueOf(dto.getStatus()));
        }

        Device updated = deviceRepository.save(device);
        log.info("Updated device: {}", updated.getDeviceId());

        return mapToDTO(updated);
    }

    @Transactional
    public void deleteDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        deviceRepository.delete(device);
        log.info("Deleted device: {}", device.getDeviceId());
    }

    public DeviceDTO getDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        return mapToDTO(device);
    }

    public List<DeviceDTO> getDevicesByFarm(Long farmId) {
        return deviceRepository.findByFarmId(farmId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<DeviceDTO> getDevicesByFarmAndType(Long farmId, String type) {
        Device.DeviceType deviceType = Device.DeviceType.valueOf(type);
        return deviceRepository.findByFarmIdAndType(farmId, deviceType)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<DeviceDTO> getOnlineDevices(Long farmId) {
        return deviceRepository.findOnlineDevicesByFarmId(farmId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Control device (send MQTT command)
     */
    @Transactional
    public void controlDevice(String deviceId, String action, Map<String, Object> params) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        // Validate device is an actuator
        if (!isActuator(device.getType())) {
            throw new RuntimeException("Device is not controllable");
        }

        // TODO: Send MQTT command to device
        // Topic: device/{deviceId}/command
        // Payload: {action: "turn_on", duration: 300}

        log.info("Sent command to device {}: {} with params: {}", deviceId, action, params);
    }

    /**
     * Check stale devices and mark as offline
     */
    @Transactional
    public void checkStaleDevices() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<Device> staleDevices = deviceRepository.findStaleDevices(threshold);

        for (Device device : staleDevices) {
            if (device.getStatus() == Device.DeviceStatus.ONLINE) {
                device.setStatus(Device.DeviceStatus.OFFLINE);
                deviceRepository.save(device);
                log.warn("Device {} marked as offline due to inactivity", device.getDeviceId());
            }
        }
    }

    // Helper methods
    private String generateDeviceId() {
        return "DEV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean isActuator(Device.DeviceType type) {
        return type == Device.DeviceType.ACTUATOR_PUMP ||
                type == Device.DeviceType.ACTUATOR_FAN ||
                type == Device.DeviceType.ACTUATOR_LIGHT;
    }

    private DeviceDTO mapToDTO(Device device) {
        DeviceDTO dto = new DeviceDTO();
        dto.setId(device.getId());
        dto.setDeviceId(device.getDeviceId());
        dto.setName(device.getName());
        dto.setDescription(device.getDescription());
        dto.setType(device.getType().name());
        dto.setStatus(device.getStatus().name());
        dto.setFarmId(device.getFarm().getId());
        dto.setFarmName(device.getFarm().getName());
        dto.setLastSeen(device.getLastSeen());
        dto.setMetadata(device.getMetadata());
        dto.setCreatedAt(device.getCreatedAt());
        dto.setUpdatedAt(device.getUpdatedAt());
        return dto;
    }
}
