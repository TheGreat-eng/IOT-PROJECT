package com.example.iotserver.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.iotserver.enums.DeviceStatus;
import com.example.iotserver.enums.DeviceType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private DeviceType type; // SENSOR, PUMP, FAN, LIGHT

    @Column(unique = true)
    private String deviceCode; // Mã thiết bị duy nhất

    @Enumerated(EnumType.STRING)
    private DeviceStatus status; // ACTIVE, INACTIVE, ERROR

    @ManyToOne
    @JoinColumn(name = "zone_id")
    private Zone zone;

    private Boolean isControllable; // Có điều khiển được không

    @CreationTimestamp
    private LocalDateTime createdAt;
}
