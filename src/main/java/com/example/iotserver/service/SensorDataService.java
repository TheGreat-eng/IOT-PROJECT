package com.example.iotserver.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.example.iotserver.config.InfluxDBConfig;
import com.example.iotserver.dto.SensorDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SensorDataService {

    private final WriteApiBlocking writeApi;
    private final InfluxDBClient influxDBClient;
    private final InfluxDBConfig influxDBConfig;

    /**
     * Save sensor data to InfluxDB
     */
    public void saveSensorData(SensorDataDTO data) {
        try {
            Point point = Point.measurement("sensor_data")
                    .addTag("device_id", data.getDeviceId())
                    .addTag("sensor_type", data.getSensorType())
                    .addTag("farm_id", String.valueOf(data.getFarmId()))
                    .time(data.getTimestamp(), WritePrecision.MS);

            // Add fields based on sensor type
            if (data.getTemperature() != null) {
                point.addField("temperature", data.getTemperature());
            }
            if (data.getHumidity() != null) {
                point.addField("humidity", data.getHumidity());
            }
            if (data.getSoilMoisture() != null) {
                point.addField("soil_moisture", data.getSoilMoisture());
            }
            if (data.getLightIntensity() != null) {
                point.addField("light_intensity", data.getLightIntensity());
            }
            if (data.getPh() != null) {
                point.addField("ph", data.getPh());
            }

            writeApi.writePoint(point);
            log.debug("Saved sensor data for device: {}", data.getDeviceId());

        } catch (Exception e) {
            log.error("Error saving sensor data to InfluxDB: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save sensor data", e);
        }
    }

    /**
     * Get latest sensor data for a device
     */
    public SensorDataDTO getLatestSensorData(String deviceId) {
        String flux = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: -1h) " +
                        "|> filter(fn: (r) => r[\"device_id\"] == \"%s\") " +
                        "|> last()",
                influxDBConfig.getBucket(), deviceId);

        Map<String, Object> rawData = executeQuery(flux);
        if (rawData.isEmpty()) {
            return null;
        }

        return SensorDataDTO.fromInfluxRecord(rawData);
    }

    /**
     * Get sensor data for a time range
     */
    public List<SensorDataDTO> getSensorDataRange(
            String deviceId,
            Instant start,
            Instant end) {
        String flux = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: %s, stop: %s) " +
                        "|> filter(fn: (r) => r[\"device_id\"] == \"%s\") " +
                        "|> sort(columns: [\"_time\"])",
                influxDBConfig.getBucket(),
                start.toString(),
                end.toString(),
                deviceId);

        List<Map<String, Object>> rawDataList = executeQueryList(flux);
        return rawDataList.stream()
                .map(SensorDataDTO::fromInfluxRecord)
                .collect(Collectors.toList());
    }

    /**
     * Get aggregated sensor data (for charts)
     */
    public List<SensorDataDTO> getAggregatedData(
            String deviceId,
            String field,
            String aggregation, // mean, max, min
            String window // 1m, 5m, 1h, 1d
    ) {
        String flux = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: -7d) " +
                        "|> filter(fn: (r) => r[\"device_id\"] == \"%s\") " +
                        "|> filter(fn: (r) => r[\"_field\"] == \"%s\") " +
                        "|> aggregateWindow(every: %s, fn: %s, createEmpty: false)",
                influxDBConfig.getBucket(),
                deviceId,
                field,
                window,
                aggregation);

        List<Map<String, Object>> rawDataList = executeQueryList(flux);
        return rawDataList.stream()
                .map(data -> {
                    SensorDataDTO dto = SensorDataDTO.fromInfluxRecord(data);
                    dto.setAvgValue((Double) data.get("_value"));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all devices data for a farm
     */
    public Map<String, Map<String, Object>> getFarmLatestData(Long farmId) {
        String flux = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: -1h) " +
                        "|> filter(fn: (r) => r[\"farm_id\"] == \"%s\") " +
                        "|> last()",
                influxDBConfig.getBucket(),
                farmId);

        List<Map<String, Object>> results = executeQueryList(flux);
        Map<String, Map<String, Object>> deviceDataMap = new HashMap<>();

        for (Map<String, Object> record : results) {
            String deviceId = (String) record.get("device_id");
            deviceDataMap.putIfAbsent(deviceId, new HashMap<>());

            String field = record.get("_field").toString();
            Object value = record.get("_value");

            deviceDataMap.get(deviceId).put(field, value);
            deviceDataMap.get(deviceId).put("device_id", deviceId);
            deviceDataMap.get(deviceId).put("timestamp", record.get("_time"));
        }

        return deviceDataMap;
    }

    // Helper methods
    private Map<String, Object> executeQuery(String flux) {
        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux, influxDBConfig.getOrg());

        if (tables.isEmpty() || tables.get(0).getRecords().isEmpty()) {
            return new HashMap<>();
        }

        return fluxRecordToMap(tables.get(0).getRecords().get(0));
    }

    private List<Map<String, Object>> executeQueryList(String flux) {
        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux, influxDBConfig.getOrg());

        List<Map<String, Object>> results = new ArrayList<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                results.add(fluxRecordToMap(record));
            }
        }

        return results;
    }

    private Map<String, Object> fluxRecordToMap(FluxRecord record) {
        Map<String, Object> map = new HashMap<>();
        map.put("_time", record.getTime());
        map.put("_value", record.getValue());
        map.put("_field", record.getField());
        map.putAll(record.getValues());
        return map;
    }
}
