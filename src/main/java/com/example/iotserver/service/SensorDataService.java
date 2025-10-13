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
import java.time.LocalDateTime;
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
            if (data.getSoilPH() != null) {
                point.addField("soilPH", data.getSoilPH());
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
        try {
            // ✅ THÊM LOG DEBUG
            log.info("🔍 [InfluxDB Query] Đang lấy dữ liệu mới nhất cho device: {}", deviceId);

            String query = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: -1h) " +
                            "|> filter(fn: (r) => r[\"device_id\"] == \"%s\") " +
                            "|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\") " +
                            "|> last()",
                    influxDBConfig.getBucket(), deviceId);

            // ✅ THÊM LOG DEBUG
            log.info("🔍 [InfluxDB Query] Query: {}", query);

            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(query, influxDBConfig.getOrg());

            // ✅ THÊM LOG DEBUG
            log.info("🔍 [InfluxDB Query] Số lượng tables trả về: {}", tables != null ? tables.size() : 0);

            if (tables.isEmpty()) {
                log.warn("❌ [InfluxDB Query] Không có dữ liệu cho device: {}", deviceId);
                return null;
            }

            SensorDataDTO sensorData = new SensorDataDTO();
            sensorData.setDeviceId(deviceId);

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    String field = (String) record.getValueByKey("_field");
                    Object value = record.getValue();
                    Instant time = record.getTime();

                    // ✅ THÊM LOG DEBUG
                    log.info("🔍 [InfluxDB Query] Field: {}, Value: {}, Time: {}", field, value, time);

                    if (time != null) {
                        sensorData.setTimestamp(time);
                    }

                    if (value instanceof Number) {
                        double doubleValue = ((Number) value).doubleValue();

                        switch (field) {
                            case "temperature":
                                sensorData.setTemperature(doubleValue);
                                break;
                            case "humidity":
                                sensorData.setHumidity(doubleValue);
                                break;
                            case "soil_moisture":
                                sensorData.setSoilMoisture(doubleValue);
                                // ✅ THÊM LOG QUAN TRỌNG
                                log.info("✅ [InfluxDB Query] Tìm thấy soil_moisture: {}", doubleValue);
                                break;
                            case "light_intensity":
                                sensorData.setLightIntensity(doubleValue);
                                break;
                            case "soilPH":
                                sensorData.setSoilPH(doubleValue);
                                break;
                        }
                    }
                }
            }

            // ✅ THÊM LOG QUAN TRỌNG
            log.info(
                    "✅ [InfluxDB Query] Dữ liệu cuối cùng: soilMoisture={}, temperature={}, humidity={}, lightIntensity={}, soilPH={}",
                    sensorData.getSoilMoisture(),
                    sensorData.getTemperature(),
                    sensorData.getHumidity(),
                    sensorData.getLightIntensity(),
                    sensorData.getSoilPH());

            return sensorData;

        } catch (Exception e) {
            log.error("❌ [InfluxDB Query] Lỗi khi truy vấn dữ liệu cảm biến: {}", e.getMessage(), e);
            return null;
        }
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

    /**
     * Lấy dữ liệu cảm biến mới nhất theo farmId
     */
    public SensorDataDTO getLatestSensorDataByFarmId(Long farmId) {
        try {
            String query = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: -1h) " +
                            "|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\") " +
                            "|> filter(fn: (r) => r[\"farm_id\"] == \"%s\") " +
                            "|> last()",
                    influxDBConfig.getBucket(),
                    farmId);

            log.debug("🔍 [InfluxDB] Query for farmId {}: {}", farmId, query);

            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(query);

            if (tables == null || tables.isEmpty()) {
                log.warn("⚠️ [InfluxDB] Không có dữ liệu cho farmId: {}", farmId);
                return null;
            }

            // Parse dữ liệu
            SensorDataDTO data = new SensorDataDTO();
            data.setFarmId(farmId);
            data.setTimestamp(Instant.now());

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    String field = (String) record.getField();
                    Object value = record.getValue();

                    switch (field) {
                        case "temperature":
                            data.setTemperature(((Number) value).doubleValue());
                            break;
                        case "humidity":
                            data.setHumidity(((Number) value).doubleValue());
                            break;
                        case "soil_moisture":
                            data.setSoilMoisture(((Number) value).doubleValue());
                            break;
                        case "light_intensity":
                            data.setLightIntensity(((Number) value).doubleValue());
                            break;
                        case "soil_ph":
                            data.setSoilPH(((Number) value).doubleValue());
                            break;
                    }
                }
            }

            log.info("✅ [InfluxDB] Lấy dữ liệu thành công cho farmId: {}", farmId);
            return data;

        } catch (Exception e) {
            log.error("❌ [InfluxDB] Lỗi khi lấy dữ liệu farmId {}: {}", farmId, e.getMessage());
            return null;
        }
    }

    /**
     * Lấy dữ liệu cảm biến tại thời điểm cụ thể
     * (Dùng cho quy tắc 5: độ ẩm dao động)
     */
    public SensorDataDTO getSensorDataAt(Long farmId, LocalDateTime dateTime) {
        try {
            String query = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s, stop: %s) " +
                            "|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\") " +
                            "|> filter(fn: (r) => r[\"farm_id\"] == \"%s\") " +
                            "|> last()",
                    influxDBConfig.getBucket(),
                    dateTime.minusMinutes(30).toString() + "Z",
                    dateTime.plusMinutes(30).toString() + "Z",
                    farmId);

            log.debug("🔍 [InfluxDB] Query for farmId {}: {}", farmId, query);

            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(query);

            if (tables == null || tables.isEmpty()) {
                log.warn("⚠️ [InfluxDB] Không có dữ liệu cho farmId: {}", farmId);
                return null;
            }

            // Parse dữ liệu
            SensorDataDTO data = new SensorDataDTO();
            data.setFarmId(farmId);
            data.setTimestamp(Instant.now());

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    String field = (String) record.getField();
                    Object value = record.getValue();

                    switch (field) {
                        case "temperature":
                            data.setTemperature(((Number) value).doubleValue());
                            break;
                        case "humidity":
                            data.setHumidity(((Number) value).doubleValue());
                            break;
                        case "soil_moisture":
                            data.setSoilMoisture(((Number) value).doubleValue());
                            break;
                        case "light_intensity":
                            data.setLightIntensity(((Number) value).doubleValue());
                            break;
                        case "soil_ph":
                            data.setSoilPH(((Number) value).doubleValue());
                            break;
                    }
                }
            }

            log.info("✅ [InfluxDB] Lấy dữ liệu thành công cho farmId: {}", farmId);
            return data;

        } catch (Exception e) {
            log.error("❌ [InfluxDB] Lỗi khi lấy dữ liệu farmId {}: {}", farmId, e.getMessage());
            return null;
        }
    }
}
