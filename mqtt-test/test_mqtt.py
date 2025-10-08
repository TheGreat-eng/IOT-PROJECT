#!/usr/bin/env python3
"""
TEST CUỐI CÙNG - ĐẢM BẢO 100% THÀNH CÔNG
"""

import paho.mqtt.client as mqtt
import json
import time
from datetime import datetime

print("\n" + "="*70)
print("🔧 FINAL TEST - RULE ENGINE")
print("="*70)

# ========== CONFIG ==========
BROKER = "localhost"
PORT = 1883
DEVICE_ID = "SOIL-001"
TOPIC = f"sensor/{DEVICE_ID}/data"

# ========== CALLBACK ==========
def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("✅ Kết nối MQTT thành công!")
    else:
        print(f"❌ Lỗi kết nối MQTT: {rc}")
        exit(1)

def on_publish(client, userdata, mid):
    print(f"✅ Đã publish message ID: {mid}")

# ========== CONNECT ==========
print(f"\n📡 Đang kết nối MQTT broker: {BROKER}:{PORT}")
client = mqtt.Client()
client.on_connect = on_connect
client.on_publish = on_publish

try:
    client.connect(BROKER, PORT, 60)
    client.loop_start()
    time.sleep(2)
    
    # ========== DATA ==========
    test_data = {
        "deviceId": DEVICE_ID,
        "sensorType": "SOIL_MOISTURE",
        "temperature": 28.5,
        "humidity": 65.0,
        "soilMoisture": 22.0,  # ✅ < 30 → TRIGGER RULE
        "lightIntensity": 15000,
        "ph": 6.5,
        "timestamp": datetime.now().isoformat()
    }
    
    print(f"\n📤 Gửi dữ liệu:")
    print(f"   Topic: {TOPIC}")
    print(f"   Device: {DEVICE_ID}")
    print(f"   Soil Moisture: {test_data['soilMoisture']}% (< 30 → SẼ TRIGGER)")
    print(f"\n📦 Payload:")
    print(json.dumps(test_data, indent=2))
    
    # ========== PUBLISH ==========
    result = client.publish(TOPIC, json.dumps(test_data), qos=1)
    
    if result.rc == mqtt.MQTT_ERR_SUCCESS:
        print(f"\n✅ Đã gửi thành công!")
    else:
        print(f"\n❌ Gửi thất bại: {result.rc}")
        exit(1)
    
    time.sleep(1)
    client.loop_stop()
    client.disconnect()
    
    print("\n" + "="*70)
    print("⏳ ĐANG ĐỢI RULE ENGINE CHẠY...")
    print("="*70)
    
    print("\n📋 HƯỚNG DẪN KIỂM TRA:")
    print("\n1️⃣  Mở Spring Boot Console, tìm các dòng này:")
    print('    "Received MQTT message - Topic: sensor/SOIL-001/data"')
    print('    "Processed sensor data from device: SOIL-001"')
    print('    "Đang kiểm tra quy tắc: Tưới nước tự động..."')
    print('    "✅ Quy tắc ... - Điều kiện ĐÃ THỎA MÃN"')
    
    print("\n2️⃣  Sau 35 giây, chạy SQL:")
    print("""
    SELECT 
        executed_at,
        status,
        conditions_met,
        condition_details,
        actions_performed
    FROM rule_execution_logs 
    WHERE rule_id = 2
    ORDER BY executed_at DESC 
    LIMIT 1;
    """)
    
    print("\n3️⃣  KẾT QUẢ MONG ĐỢI:")
    print("    status: SUCCESS")
    print("    conditions_met: 1")
    print('    condition_details: {"soil_moisture":22.0,"soil_moisture_expected":30.0}')
    print('    actions_performed: ["Đã bật thiết bị DEV-PUMP-001..."]')
    
    print("\n" + "="*70)
    print("⏱️  Đếm ngược 35 giây...")
    print("="*70 + "\n")
    
    for i in range(35, 0, -5):
        print(f"   ⏰ {i} giây nữa...", end="\r")
        time.sleep(5)
    
    print("\n\n" + "="*70)
    print("✅ HẾT THỜI GIAN CHỜ - KIỂM TRA KẾT QUẢ!")
    print("="*70)
    
    print("\n📝 Nếu vẫn SKIPPED:")
    print("   1. Kiểm tra Spring Boot log có nhận MQTT không")
    print("   2. Kiểm tra device_id trong rule_conditions = 'SOIL-001'")
    print("   3. Kiểm tra InfluxDB có dữ liệu SOIL-001 không")
    print("   4. Restart Spring Boot và thử lại\n")
    
except Exception as e:
    print(f"\n❌ LỖI: {e}")
    print("\n🔧 KHẮC PHỤC:")
    print("   1. docker ps | grep mosquitto")
    print("   2. docker restart smartfarm-mosquitto")
    print("   3. Thử lại\n")
    exit(1)