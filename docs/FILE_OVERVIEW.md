# Tổng quan chi tiết cấu trúc dự án IoT Server

Tài liệu này mô tả **từng thư mục và tệp** trong dự án `IOT-PROJECT`, nêu rõ chức năng, công nghệ sử dụng và mối liên hệ giữa các thành phần. Thứ tự sắp xếp đi từ cấp thư mục gốc tới các lớp chi tiết trong mã nguồn Java.

## 1. Thư mục gốc của repository

| Thành phần | Mục đích |
| --- | --- |
| `HELP.md` | Tập hợp liên kết tài liệu Spring Boot/Maven hữu ích cho việc phát triển và vận hành dự án. 【F:HELP.md†L1-L34】 |
| `docker-compose.yml` | Định nghĩa stack dịch vụ (MySQL, InfluxDB, Mosquitto MQTT, Redis) phục vụ backend; cung cấp biến môi trường và ánh xạ volume. 【F:docker-compose.yml†L1-L70】 |
| `pom.xml` | Khai báo cấu hình Maven, phụ thuộc (Spring Boot, Security, MQTT, InfluxDB, JWT, WebSocket...), phiên bản Java 17 và plugin build. 【F:pom.xml†L1-L116】 |
| `mvnw` / `mvnw.cmd` | Maven Wrapper cho phép build chạy Maven nhất quán mà không cần cài Maven hệ thống. |
| `ctest.data/` | Chứa script kiểm thử tự động cho module cảnh báo sức khỏe cây (Plant Health). 【F:ctest.data/test_plant_health.py†L1-L215】 |
| `mosquitto/` | Cấu hình, dữ liệu và log cho MQTT broker cục bộ dùng khi chạy docker-compose. 【F:mosquitto/config/mosquitto.conf†L1-L33】【fb057d†L1-L21】 |
| `mqtt-test/` | Bộ script Python kiểm thử tương tác MQTT thủ công (fake thiết bị bơm, gửi payload soil moisture). 【F:mqtt-test/fake_pump.py†L1-L84】【F:mqtt-test/test_mqtt.py†L1-L109】 |
| `simulator/` | Trình mô phỏng cảm biến IoT gửi dữ liệu MQTT, cùng file `requirements.txt` khai báo thư viện. 【F:simulator/sensor_simulator.py†L1-L204】【F:simulator/requirements.txt†L1-L1】 |
| `src/` | Mã nguồn chính Spring Boot (Java) và tài nguyên cấu hình. Xem chi tiết ở các mục sau. |

## 2. Thư mục `ctest.data`

- `test_plant_health.py`: Script Python điều phối 7 ca kiểm thử tự động cho hệ thống cảnh báo cây, gửi dữ liệu qua MQTT và đọc kết quả API REST `plant-health`. Bao gồm nhiều hàm test theo từng quy tắc, logic mô phỏng thời gian/ngữ cảnh. 【F:ctest.data/test_plant_health.py†L1-L215】

## 3. Thư mục `mosquitto`

- `config/mosquitto.conf`: Cấu hình broker MQTT (cổng 1883 và 9001, cho phép anonymous, bật logging/persistence). 【F:mosquitto/config/mosquitto.conf†L1-L33】
- `data/mosquitto.db`: Tập tin persistence do Mosquitto tạo, lưu trạng thái message/retained (không chỉnh tay).
- `log/mosquitto.log`: Log khởi động/tắt broker, xác nhận port lắng nghe và lưu snapshot. 【fb057d†L1-L21】

## 4. Thư mục `mqtt-test`

- `fake_pump.py`: Thiết bị bơm giả lập; subscribe kênh điều khiển `device/<ID>/control`, phản hồi trạng thái về `device/<ID>/status`. Hữu ích để kiểm thử Rule Engine bật/tắt thiết bị. 【F:mqtt-test/fake_pump.py†L1-L84】
- `test_mqtt.py`: Script gửi mẫu dữ liệu soil moisture thông qua MQTT và hướng dẫn xác thực log/DB để đảm bảo rule hoạt động. 【F:mqtt-test/test_mqtt.py†L1-L109】
- `testold.txt`: Bản trước của script kiểm thử, giữ lại tham khảo (payload soil moisture 22%). 【F:mqtt-test/testold.txt†L1-L104】

## 5. Thư mục `simulator`

- `sensor_simulator.py`: Ứng dụng mô phỏng nhiều loại cảm biến (DHT22, soil, light, pH), phát dữ liệu định kỳ và gửi trạng thái thiết bị qua MQTT. Có cả logic mô phỏng ngày/đêm, sự kiện tưới. 【F:simulator/sensor_simulator.py†L1-L200】【F:simulator/sensor_simulator.py†L200-L400】
- `requirements.txt`: Khai báo phụ thuộc `paho-mqtt` cho simulator. 【F:simulator/requirements.txt†L1-L1】

## 6. Cấu trúc `src/main/resources`

- `application.properties`: Cấu hình Spring (MySQL, JPA, encoding, JWT, logging, InfluxDB, MQTT, OpenWeather, actuator). Các giá trị placeholder hỗ trợ docker-compose. 【F:src/main/resources/application.properties†L1-L76】

## 7. Gói Java `com.example.iotserver`

### 7.1 Tập tin khởi động

- `IotserverApplication.java`: Điểm vào Spring Boot, bật `@EnableScheduling` để kích hoạt scheduler. 【F:src/main/java/com/example/iotserver/IotserverApplication.java†L1-L16】

### 7.2 Cấu hình (`config`)

- `CorsConfig.java`: Khai báo `CorsFilter` cho phép mọi origin, header, method trong môi trường phát triển. 【F:src/main/java/com/example/iotserver/config/CorsConfig.java†L1-L44】
- `InfluxDBConfig.java`: Đọc cấu hình InfluxDB từ properties, cung cấp `InfluxDBClient`, `WriteApiBlocking`, và `ObjectMapper` hỗ trợ thời gian. 【F:src/main/java/com/example/iotserver/config/InfluxDBConfig.java†L1-L44】
- `MqttConfig.java`: Thiết lập factory MQTT Paho, adapter inbound (subscribe `sensor/+/data`, `device/+/status`) và outbound handler để publish lệnh điều khiển. 【F:src/main/java/com/example/iotserver/config/MqttConfig.java†L1-L70】
- `PasswordEncoderConfig.java`: Đăng ký `BCryptPasswordEncoder`. 【F:src/main/java/com/example/iotserver/config/PasswordEncoderConfig.java†L1-L14】
- `SecurityConfig.java`: Cấu hình chuỗi filter Spring Security (disable CSRF, cho phép public endpoint auth/ws, thêm `JwtAuthenticationFilter`, session stateless, cung cấp `AuthenticationManager`). 【F:src/main/java/com/example/iotserver/config/SecurityConfig.java†L1-L46】
- `WebSocketConfig.java`: Bật STOMP WebSocket, broker `/topic`/`/queue`, endpoint `/ws` với SockJS. 【F:src/main/java/com/example/iotserver/config/WebSocketConfig.java†L1-L26】

### 7.3 Bộ điều khiển REST (`controller`)

- `AuthController.java`: API đăng ký, đăng nhập, trả về JWT và thông tin người dùng; dùng `UserService`, `JwtUtil`, `PasswordEncoder`. 【F:src/main/java/com/example/iotserver/controller/AuthController.java†L1-L71】
- `DashboardController.java`: Tổng hợp thống kê farm (thiết bị, dữ liệu cảm biến, danh sách thiết bị) và tính trung bình theo thời gian thực. 【F:src/main/java/com/example/iotserver/controller/DashboardController.java†L1-L66】
- `DeviceController.java`: CRUD thiết bị, truy xuất dữ liệu cảm biến (mới nhất, theo khoảng, aggregated) và gửi lệnh điều khiển qua service. 【F:src/main/java/com/example/iotserver/controller/DeviceController.java†L1-L112】
- `FarmController.java`: Quản lý farm theo người dùng hiện tại (tạo, cập nhật, xóa, liệt kê, lấy danh sách farm truy cập được). 【F:src/main/java/com/example/iotserver/controller/FarmController.java†L1-L92】
- `PlantHealthController.java`: API phân tích sức khỏe cây trồng (hiện tại, lịch sử, phân tích chi tiết, resolve cảnh báo). 【F:src/main/java/com/example/iotserver/controller/PlantHealthController.java†L1-L122】
- `RuleController.java`: CRUD quy tắc tự động, bật/tắt, lấy log thực thi, thống kê, trigger thủ công. 【F:src/main/java/com/example/iotserver/controller/RuleController.java†L1-L120】
- `WeatherController.java`: Truy vấn thời tiết hiện tại/dự báo, trigger cập nhật, phân tích ảnh hưởng tới tưới tiêu. 【F:src/main/java/com/example/iotserver/controller/WeatherController.java†L1-L78】

### 7.4 DTO (`dto`)

- `DeviceDTO.java`: Mô tả thiết bị kèm dữ liệu cảm biến mới nhất và metadata; có helper `calculateDerivedFields`. 【F:src/main/java/com/example/iotserver/dto/DeviceDTO.java†L1-L53】
- `FarmDTO.java`: Đại diện thông tin farm, thống kê thiết bị, dữ liệu trung bình và thời gian hoạt động. 【F:src/main/java/com/example/iotserver/dto/FarmDTO.java†L1-L49】
- `PlantHealthDTO.java`: Gói thông tin điểm sức khỏe, cảnh báo hoạt động, thống kê mức độ, enum trạng thái. 【F:src/main/java/com/example/iotserver/dto/PlantHealthDTO.java†L1-L97】
- `RuleDTO.java`: Chuyển đổi quy tắc + điều kiện + hành động sang dạng trao đổi JSON. 【F:src/main/java/com/example/iotserver/dto/RuleDTO.java†L1-L63】
- `RuleExecutionLogDTO.java`: Thể hiện log thực thi (trạng thái, thời điểm, chi tiết). 【F:src/main/java/com/example/iotserver/dto/RuleExecutionLogDTO.java†L1-L22】
- `SensorDataDTO.java`: Mẫu dữ liệu cảm biến (timestamp, giá trị, aggregate) + helper parse MQTT/Influx. 【F:src/main/java/com/example/iotserver/dto/SensorDataDTO.java†L1-L83】
- `WeatherDTO.java`: Thời tiết hiện tại, dự báo, gợi ý và icon. 【F:src/main/java/com/example/iotserver/dto/WeatherDTO.java†L1-L38】
- `dto/request` (`LoginRequest`, `RegisterRequest`): Payload xác thực với validation annotation. 【F:src/main/java/com/example/iotserver/dto/request/LoginRequest.java†L1-L18】【F:src/main/java/com/example/iotserver/dto/request/RegisterRequest.java†L1-L22】
- `dto/response` (`ApiResponse`, `AuthResponse`, `PageResponse`): Khuôn mẫu response thống nhất và thông tin đăng nhập. 【F:src/main/java/com/example/iotserver/dto/response/ApiResponse.java†L1-L34】【F:src/main/java/com/example/iotserver/dto/response/AuthResponse.java†L1-L12】【F:src/main/java/com/example/iotserver/dto/response/PageResponse.java†L1-L16】

### 7.5 Thực thể JPA (`entity`)

- `Device.java`: Entity thiết bị (ID, loại, trạng thái, metadata, timestamps). 【F:src/main/java/com/example/iotserver/entity/Device.java†L1-L64】
- `Farm.java`: Farm sở hữu bởi user, chứa danh sách thiết bị. 【F:src/main/java/com/example/iotserver/entity/Farm.java†L1-L49】
- `PlantHealthAlert.java`: Lưu cảnh báo sức khỏe cây, enum loại/mức độ, JSON điều kiện, thời điểm xử lý. 【F:src/main/java/com/example/iotserver/entity/PlantHealthAlert.java†L1-L131】
- `Rule.java`: Quy tắc tự động với danh sách điều kiện (`RuleCondition`) và hành động (`RuleAction`). 【F:src/main/java/com/example/iotserver/entity/Rule.java†L1-L82】
- `RuleCondition.java`: Điều kiện thuộc quy tắc (loại sensor/time/device/weather, toán tử, logical operator). 【F:src/main/java/com/example/iotserver/entity/RuleCondition.java†L1-L65】
- `RuleExecutionLog.java`: Log kết quả chạy rule (trạng thái, JSON chi tiết). 【F:src/main/java/com/example/iotserver/entity/RuleExecutionLog.java†L1-L53】
- `User.java`: Người dùng hệ thống (username/email/password/role), quan hệ với farm. 【F:src/main/java/com/example/iotserver/entity/User.java†L1-L52】
- `Weather.java`: Dữ liệu thời tiết lưu cache trong DB, liên kết farm. 【F:src/main/java/com/example/iotserver/entity/Weather.java†L1-L52】
- `Zone.java`: Phân vùng trong farm với thông tin diện tích. 【F:src/main/java/com/example/iotserver/entity/Zone.java†L1-L45】

### 7.6 Enum (`enums`)

- `DeviceStatus`, `DeviceType`, `UserRole`: Enum phụ trợ biểu diễn trạng thái thiết bị, loại thiết bị và vai trò người dùng. 【F:src/main/java/com/example/iotserver/enums/DeviceStatus.java†L1-L4】【F:src/main/java/com/example/iotserver/enums/DeviceType.java†L1-L8】【F:src/main/java/com/example/iotserver/enums/UserRole.java†L1-L6】

### 7.7 Repository (`repository`)

Các interface JPA truy cập database:
- `DeviceRepository` (tìm thiết bị theo farm, trạng thái, đếm, truy vấn thiết bị offline). 【F:src/main/java/com/example/iotserver/repository/DeviceRepository.java†L1-L25】
- `FarmRepository` (tìm farm theo owner, truy vấn quyền truy cập). 【F:src/main/java/com/example/iotserver/repository/FarmRepository.java†L1-L22】
- `PlantHealthAlertRepository` (truy vấn cảnh báo chưa xử lý, thống kê theo severity, xóa cảnh báo cũ). 【F:src/main/java/com/example/iotserver/repository/PlantHealthAlertRepository.java†L1-L33】
- `RuleExecutionLogRepository` (phân trang log, đếm, xóa log cũ). 【F:src/main/java/com/example/iotserver/repository/RuleExecutionLogRepository.java†L1-L26】
- `RuleRepository` (tìm quy tắc theo farm/priority, đếm enabled). 【F:src/main/java/com/example/iotserver/repository/RuleRepository.java†L1-L25】
- `UserRepository` (tìm/kiểm tra user theo username/email). 【F:src/main/java/com/example/iotserver/repository/UserRepository.java†L1-L15】
- `WeatherRepository` (lấy dữ liệu thời tiết mới nhất, xóa dữ liệu cũ). 【F:src/main/java/com/example/iotserver/repository/WeatherRepository.java†L1-L19】
- `ZoneRepository` (liệt kê zone theo farm). 【F:src/main/java/com/example/iotserver/repository/ZoneRepository.java†L1-L6】

### 7.8 Bảo mật (`security`)

- `CustomUserDetailsService`: Tải thông tin người dùng theo email để xác thực Spring Security. 【F:src/main/java/com/example/iotserver/security/CustomUserDetailsService.java†L1-L24】
- `JwtAuthenticationFilter`: Trích xuất JWT từ header, xác thực token và thiết lập `SecurityContext`. 【F:src/main/java/com/example/iotserver/security/JwtAuthenticationFilter.java†L1-L55】
- `JwtUtil`: Tạo/giải mã/validate JWT dựa trên khóa bí mật cấu hình. 【F:src/main/java/com/example/iotserver/security/JwtUtil.java†L1-L58】

### 7.9 Scheduler (`scheduler`)

- `PlantHealthScheduler`: Nhiệm vụ định kỳ dọn dẹp cảnh báo đã xử lý sau 30 ngày. 【F:src/main/java/com/example/iotserver/scheduler/PlantHealthScheduler.java†L1-L33】
- `RuleScheduler`: Chạy rule engine mỗi 30 giây và dọn dẹp log hằng ngày. 【F:src/main/java/com/example/iotserver/scheduler/RuleScheduler.java†L1-L32】

### 7.10 Service (`service`)

- `DeviceService`: Quản lý thiết bị, tương tác MQTT điều khiển, ánh xạ DTO, kiểm tra thiết bị offline. 【F:src/main/java/com/example/iotserver/service/DeviceService.java†L1-L107】【F:src/main/java/com/example/iotserver/service/DeviceService.java†L200-L249】
- `FarmService`: CRUD farm gắn với user. 【F:src/main/java/com/example/iotserver/service/FarmService.java†L1-L73】
- `MqttGateway`: Interface gateway gửi message ra topic MQTT. 【F:src/main/java/com/example/iotserver/service/MqttGateway.java†L1-L6】
- `MqttMessageHandler`: Xử lý message inbound từ MQTT (sensor data/status), cập nhật DB và lưu InfluxDB. 【F:src/main/java/com/example/iotserver/service/MqttMessageHandler.java†L1-L71】
- `PlantHealthService`: Triển khai 7 quy tắc phân tích sức khỏe, lưu cảnh báo, thống kê điểm, dọn dẹp lịch sử. 【F:src/main/java/com/example/iotserver/service/PlantHealthService.java†L1-L102】【F:src/main/java/com/example/iotserver/service/PlantHealthService.java†L200-L298】
- `RuleEngineService`: Máy suy luận đọc quy tắc, kiểm tra điều kiện (cảm biến, thời gian, thiết bị, thời tiết), thực thi hành động (MQTT, thông báo) và lưu log. 【F:src/main/java/com/example/iotserver/service/RuleEngineService.java†L1-L122】【F:src/main/java/com/example/iotserver/service/RuleEngineService.java†L200-L288】
- `RuleService`: CRUD quy tắc ở tầng nghiệp vụ, ánh xạ entity ↔ DTO, truy xuất log. 【F:src/main/java/com/example/iotserver/service/RuleService.java†L1-L123】
- `SensorDataService`: Lưu/đọc dữ liệu từ InfluxDB (latest, range, aggregate, farm-level). 【F:src/main/java/com/example/iotserver/service/SensorDataService.java†L1-L118】【F:src/main/java/com/example/iotserver/service/SensorDataService.java†L200-L273】
- `UserService` & `impl/UserServiceImpl`: Interface và triển khai thao tác người dùng qua `UserRepository`. 【F:src/main/java/com/example/iotserver/service/UserService.java†L1-L13】【F:src/main/java/com/example/iotserver/service/impl/UserServiceImpl.java†L1-L32】
- `WeatherService`: Đồng bộ dữ liệu từ OpenWeatherMap, cache vào DB, cung cấp gợi ý và cleanup định kỳ. 【F:src/main/java/com/example/iotserver/service/WeatherService.java†L1-L116】【F:src/main/java/com/example/iotserver/service/WeatherService.java†L200-L244】
- `WebSocketService`: Phát dữ liệu cảm biến, cảnh báo, trạng thái thiết bị qua STOMP topic. 【F:src/main/java/com/example/iotserver/service/WebSocketService.java†L1-L36】

### 7.11 Ngoại lệ (`exception`)

- `GlobalExceptionHandler`: Chuẩn hóa phản hồi lỗi validation, runtime, not found, và lỗi chung. 【F:src/main/java/com/example/iotserver/exception/GlobalExceptionHandler.java†L1-L38】
- `ResourceNotFoundException`: Ngoại lệ chuyên dụng báo tài nguyên không tồn tại. 【F:src/main/java/com/example/iotserver/exception/ResourceNotFoundException.java†L1-L9】

## 8. Các thành phần khác

- `src/test/`: Hiện không chứa test Java (trống) – có thể sử dụng để bổ sung unit/integration test trong tương lai.

---

Tài liệu này nhằm giúp nhanh chóng nắm bắt kiến trúc và vị trí chức năng của từng tệp trong dự án, hỗ trợ việc bảo trì, mở rộng cũng như onboarding thành viên mới.
