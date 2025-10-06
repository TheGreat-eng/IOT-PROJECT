package com.example.iotserver.repository;

import com.example.iotserver.entity.Device;
import com.example.iotserver.entity.Device.DeviceStatus;
import com.example.iotserver.entity.Device.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByDeviceId(String deviceId);

    List<Device> findByFarmId(Long farmId);

    List<Device> findByFarmIdAndType(Long farmId, DeviceType type);

    List<Device> findByFarmIdAndStatus(Long farmId, DeviceStatus status);

    @Query("SELECT d FROM Device d WHERE d.farm.id = :farmId AND d.status = 'ONLINE'")
    List<Device> findOnlineDevicesByFarmId(Long farmId);

    @Query("SELECT d FROM Device d WHERE d.lastSeen < :threshold")
    List<Device> findStaleDevices(LocalDateTime threshold);

    boolean existsByDeviceId(String deviceId);

    long countByFarmId(Long farmId);

    long countByFarmIdAndStatus(Long farmId, DeviceStatus status);
}