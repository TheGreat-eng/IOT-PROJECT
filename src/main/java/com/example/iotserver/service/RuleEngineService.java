package com.example.iotserver.service;

import com.example.iotserver.dto.SensorDataDTO;
import com.example.iotserver.dto.WeatherDTO;
import com.example.iotserver.entity.Rule;
import com.example.iotserver.entity.RuleCondition;
import com.example.iotserver.entity.RuleExecutionLog;
import com.example.iotserver.repository.RuleExecutionLogRepository;
import com.example.iotserver.repository.RuleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleEngineService {

    private final RuleRepository ruleRepository;
    private final RuleExecutionLogRepository logRepository;
    private final SensorDataService sensorDataService;
    private final DeviceService deviceService;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;
    private final WeatherService weatherService;

    /**
     * Chạy tất cả quy tắc đang kích hoạt
     */
    @Transactional
    public void executeAllRules() {
        long startTime = System.currentTimeMillis();

        List<Rule> enabledRules = ruleRepository.findAllEnabledRules();
        log.debug("Đang kiểm tra {} quy tắc đang kích hoạt", enabledRules.size());

        int successCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (Rule rule : enabledRules) {
            try {
                boolean executed = executeRule(rule);
                if (executed) {
                    successCount++;
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                failedCount++;
                log.error("Lỗi khi thực thi quy tắc {}: {}", rule.getName(), e.getMessage());
            }
        }

        long executionTime = System.currentTimeMillis() - startTime;
        log.info("Hoàn thành kiểm tra quy tắc: {} thành công, {} bỏ qua, {} lỗi ({}ms)",
                successCount, skippedCount, failedCount, executionTime);
    }

    /**
     * Thực thi một quy tắc cụ thể
     */
    @Transactional
    public boolean executeRule(Rule rule) {
        long startTime = System.currentTimeMillis();

        log.debug("Đang kiểm tra quy tắc: {}", rule.getName());

        try {
            // Bước 1: Kiểm tra điều kiện
            Map<String, Object> conditionContext = new HashMap<>();
            boolean allConditionsMet = evaluateConditions(rule, conditionContext);

            long executionTime = System.currentTimeMillis() - startTime;

            // Bước 2: Nếu điều kiện đúng → Thực hiện hành động
            if (allConditionsMet) {
                log.info("✅ Quy tắc '{}' - Điều kiện ĐÃ THỎA MÃN", rule.getName());

                List<String> performedActions = performActions(rule);

                // Cập nhật thống kê
                rule.setLastExecutedAt(LocalDateTime.now());
                rule.setExecutionCount(rule.getExecutionCount() + 1);
                ruleRepository.save(rule);

                // Lưu log thành công
                saveExecutionLog(rule, RuleExecutionLog.ExecutionStatus.SUCCESS,
                        true, conditionContext, performedActions, null, executionTime);

                return true;
            } else {
                log.debug("⏭️ Quy tắc '{}' - Điều kiện CHƯA THỎA MÃN", rule.getName());

                // Lưu log bỏ qua
                saveExecutionLog(rule, RuleExecutionLog.ExecutionStatus.SKIPPED,
                        false, conditionContext, Collections.emptyList(), null, executionTime);

                return false;
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("❌ Lỗi khi thực thi quy tắc '{}': {}", rule.getName(), e.getMessage(), e);

            // Lưu log lỗi
            saveExecutionLog(rule, RuleExecutionLog.ExecutionStatus.FAILED,
                    null, null, null, e.getMessage(), executionTime);

            return false;
        }
    }

    /**
     * Kiểm tra tất cả điều kiện của quy tắc
     */
    private boolean evaluateConditions(Rule rule, Map<String, Object> context) {
        if (rule.getConditions().isEmpty()) {
            log.warn("Quy tắc '{}' không có điều kiện nào", rule.getName());
            return false;
        }

        // Sắp xếp theo thứ tự
        List<RuleCondition> sortedConditions = rule.getConditions().stream()
                .sorted(Comparator.comparing(RuleCondition::getOrderIndex))
                .collect(Collectors.toList());

        boolean result = true;
        RuleCondition.LogicalOperator nextOperator = RuleCondition.LogicalOperator.AND;

        for (int i = 0; i < sortedConditions.size(); i++) {
            RuleCondition condition = sortedConditions.get(i);
            boolean conditionMet = evaluateSingleCondition(condition, context);

            // Kết hợp với điều kiện trước đó
            if (i == 0) {
                result = conditionMet;
            } else {
                if (nextOperator == RuleCondition.LogicalOperator.AND) {
                    result = result && conditionMet;
                } else {
                    result = result || conditionMet;
                }
            }

            // Lưu operator cho lần tiếp theo
            nextOperator = condition.getLogicalOperator();

            log.debug("  Điều kiện {}: {} {} {} = {}",
                    i + 1, condition.getField(), condition.getOperator(),
                    condition.getValue(), conditionMet);
        }

        return result;
    }

    /**
     * Kiểm tra một điều kiện đơn
     */
    private boolean evaluateSingleCondition(RuleCondition condition, Map<String, Object> context) {
        switch (condition.getType()) {
            case SENSOR_VALUE:
                return evaluateSensorCondition(condition, context);
            case TIME_RANGE:
                return evaluateTimeCondition(condition, context);
            case DEVICE_STATUS:
                return evaluateDeviceStatusCondition(condition, context);
            case WEATHER: // ✅ THÊM MỚI
                return evaluateWeatherCondition(condition, context);
            default:
                log.warn("Loại điều kiện không được hỗ trợ: {}", condition.getType());
                return false;
        }
    }

    /**
     * Kiểm tra điều kiện về giá trị cảm biến
     */
    private boolean evaluateSensorCondition(RuleCondition condition, Map<String, Object> context) {
        try {
            String deviceId = condition.getDeviceId();

            log.info("🔍 [Rule Check] deviceId: {}, field: {}, operator: {}, value: {}",
                    deviceId, condition.getField(), condition.getOperator(), condition.getValue());

            if (deviceId == null || deviceId.isEmpty()) {
                log.warn("❌ [Rule Check] Thiếu deviceId cho điều kiện cảm biến");
                return false;
            }

            SensorDataDTO sensorData = sensorDataService.getLatestSensorData(deviceId);

            log.info("🔍 [Rule Check] Sensor data từ InfluxDB: {}", sensorData != null ? "CÓ DỮ LIỆU" : "NULL");

            if (sensorData == null) {
                log.warn("❌ [Rule Check] Không có dữ liệu cảm biến cho thiết bị: {}", deviceId);
                return false;
            }

            Double actualValue = getSensorValue(sensorData, condition.getField());

            log.info("🔍 [Rule Check] actualValue: {}, expectedValue: {}", actualValue, condition.getValue());

            if (actualValue == null) {
                log.warn("❌ [Rule Check] Không tìm thấy giá trị cho trường: {}", condition.getField());
                return false;
            }

            Double expectedValue = Double.parseDouble(condition.getValue());
            context.put(condition.getField(), actualValue);
            context.put(condition.getField() + "_expected", expectedValue);

            boolean result = compareValues(actualValue, condition.getOperator(), expectedValue);

            log.info("🔍 [Rule Check] So sánh: {} {} {} = {}",
                    actualValue, condition.getOperator(), expectedValue, result);

            return result;

        } catch (Exception e) {
            log.error("❌ [Rule Check] Lỗi: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Kiểm tra điều kiện về thời gian
     */
    private boolean evaluateTimeCondition(RuleCondition condition, Map<String, Object> context) {
        try {
            LocalTime now = LocalTime.now();
            context.put("current_time", now.toString());

            // Format: "06:00-18:00" hoặc "06:00"
            String value = condition.getValue();

            if (value.contains("-")) {
                // Khoảng thời gian
                String[] parts = value.split("-");
                LocalTime start = LocalTime.parse(parts[0].trim());
                LocalTime end = LocalTime.parse(parts[1].trim());

                boolean inRange = now.isAfter(start) && now.isBefore(end);
                context.put("time_range", value);
                context.put("in_time_range", inRange);

                return inRange;
            } else {
                // Thời gian cụ thể
                LocalTime target = LocalTime.parse(value.trim());
                return now.isAfter(target) || now.equals(target);
            }

        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra điều kiện thời gian: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Kiểm tra điều kiện về trạng thái thiết bị
     */
    private boolean evaluateDeviceStatusCondition(RuleCondition condition, Map<String, Object> context) {
        try {
            String deviceId = condition.getDeviceId();
            if (deviceId == null || deviceId.isEmpty()) {
                return false;
            }

            var device = deviceService.getDeviceWithLatestData(deviceId);
            String status = device.getStatus();

            context.put("device_" + deviceId + "_status", status);

            return status.equalsIgnoreCase(condition.getValue());

        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra trạng thái thiết bị: {}", e.getMessage());
            return false;
        }
    }

    /**
     * So sánh giá trị
     */
    private boolean compareValues(Double actual, RuleCondition.Operator operator, Double expected) {
        switch (operator) {
            case EQUALS:
                return Math.abs(actual - expected) < 0.01;
            case NOT_EQUALS:
                return Math.abs(actual - expected) >= 0.01;
            case GREATER_THAN:
                return actual > expected;
            case GREATER_THAN_OR_EQUAL:
                return actual >= expected;
            case LESS_THAN:
                return actual < expected;
            case LESS_THAN_OR_EQUAL:
                return actual <= expected;
            default:
                return false;
        }
    }

    /**
     * Lấy giá trị cảm biến theo tên trường
     */
    private Double getSensorValue(SensorDataDTO data, String field) {
        switch (field.toLowerCase()) {
            case "temperature":
                return data.getTemperature();
            case "humidity":
                return data.getHumidity();
            case "soil_moisture":
            case "soilmoisture":
                return data.getSoilMoisture();
            case "light_intensity":
            case "lightintensity":
                return data.getLightIntensity();
            case "ph":
                return data.getPh();
            default:
                return null;
        }
    }

    /**
     * Thực hiện các hành động
     */
    private List<String> performActions(Rule rule) {
        List<String> performedActions = new ArrayList<>();

        for (Rule.RuleAction action : rule.getActions()) {
            try {
                String result = performSingleAction(rule, action);
                performedActions.add(result);
                log.info("  ✓ Đã thực hiện: {}", result);
            } catch (Exception e) {
                String error = "Lỗi khi thực hiện hành động: " + e.getMessage();
                performedActions.add(error);
                log.error("  ✗ {}", error);
            }
        }

        return performedActions;
    }

    /**
     * Thực hiện một hành động đơn
     */
    private String performSingleAction(Rule rule, Rule.RuleAction action) {
        switch (action.getType()) {
            case TURN_ON_DEVICE:
                return turnOnDevice(action);
            case TURN_OFF_DEVICE:
                return turnOffDevice(action);
            case SEND_NOTIFICATION:
                return sendNotification(rule, action);
            case SEND_EMAIL:
                return sendEmail(rule, action);
            default:
                return "Loại hành động không được hỗ trợ: " + action.getType();
        }
    }

    /**
     * Bật thiết bị
     */
    private String turnOnDevice(Rule.RuleAction action) {
        Map<String, Object> command = new HashMap<>();
        command.put("action", "turn_on");
        if (action.getDurationSeconds() != null) {
            command.put("duration", action.getDurationSeconds());
        }

        deviceService.controlDevice(action.getDeviceId(), "turn_on", command);

        return String.format("Đã bật thiết bị %s trong %d giây",
                action.getDeviceId(),
                action.getDurationSeconds() != null ? action.getDurationSeconds() : 0);
    }

    /**
     * Tắt thiết bị
     */
    private String turnOffDevice(Rule.RuleAction action) {
        Map<String, Object> command = new HashMap<>();
        command.put("action", "turn_off");

        deviceService.controlDevice(action.getDeviceId(), "turn_off", command);

        return String.format("Đã tắt thiết bị %s", action.getDeviceId());
    }

    /**
     * Gửi thông báo
     */
    private String sendNotification(Rule rule, Rule.RuleAction action) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "RULE_TRIGGERED");
        notification.put("ruleName", rule.getName());
        notification.put("message", action.getMessage());
        notification.put("timestamp", LocalDateTime.now().toString());

        webSocketService.sendAlert(rule.getFarm().getId(), notification);

        return "Đã gửi thông báo: " + action.getMessage();
    }

    /**
     * Gửi email
     */
    private String sendEmail(Rule rule, Rule.RuleAction action) {
        // TODO: Triển khai gửi email thực tế
        log.info("TODO: Gửi email - {}", action.getMessage());
        return "Đã gửi email: " + action.getMessage();
    }

    /**
     * Lưu log thực thi
     */
    private void saveExecutionLog(Rule rule, RuleExecutionLog.ExecutionStatus status,
            Boolean conditionsMet, Map<String, Object> conditionContext,
            List<String> actions, String errorMessage, long executionTime) {
        try {
            RuleExecutionLog log = RuleExecutionLog.builder()
                    .rule(rule)
                    .executedAt(LocalDateTime.now())
                    .status(status)
                    .conditionsMet(conditionsMet)
                    .conditionDetails(
                            conditionContext != null ? objectMapper.writeValueAsString(conditionContext) : null)
                    .actionsPerformed(actions != null ? objectMapper.writeValueAsString(actions) : null)
                    .errorMessage(errorMessage)
                    .executionTimeMs(executionTime)
                    .build();

            logRepository.save(log);

        } catch (JsonProcessingException e) {
            log.error("Lỗi khi lưu execution log: {}", e.getMessage());
        }
    }

    private boolean evaluateWeatherCondition(RuleCondition condition, Map<String, Object> context) {
        try {
            Long farmId = condition.getRule().getFarm().getId();
            WeatherDTO weather = weatherService.getCurrentWeather(farmId);

            if (weather == null) {
                log.warn("Không có dữ liệu thời tiết cho farm {}", farmId);
                return false;
            }

            String field = condition.getField().toLowerCase();
            Double actualValue = null;

            switch (field) {
                case "rain_amount":
                case "rain":
                    actualValue = weather.getRainAmount();
                    break;
                case "temperature":
                    actualValue = weather.getTemperature();
                    break;
                case "humidity":
                    actualValue = weather.getHumidity();
                    break;
                case "wind_speed":
                    actualValue = weather.getWindSpeed();
                    break;
                default:
                    log.warn("Trường thời tiết không được hỗ trợ: {}", field);
                    return false;
            }

            if (actualValue == null) {
                return false;
            }

            Double expectedValue = Double.parseDouble(condition.getValue());
            context.put("weather_" + field, actualValue);
            context.put("weather_" + field + "_expected", expectedValue);

            boolean result = compareValues(actualValue, condition.getOperator(), expectedValue);

            log.info("🌤️ Kiểm tra thời tiết: {} {} {} = {}",
                    actualValue, condition.getOperator(), expectedValue, result);

            return result;

        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra điều kiện thời tiết: {}", e.getMessage());
            return false;
        }
    }

}