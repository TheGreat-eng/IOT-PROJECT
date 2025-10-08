"""
Script test gửi dữ liệu MQTT cho SmartFarm
Chạy: python test_mqtt.py
"""

import paho.mqtt.client as mqtt
import json
import time
from datetime import datetime

# ========== CẤU HÌNH ==========
BROKER = "localhost"
PORT = 1883
TOPIC = "sensor/DEV-12345678/data"

# ========== CALLBACK FUNCTIONS ==========
def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("✅ Đã kết nối MQTT broker thành công!")
    else:
        print(f"❌ Lỗi kết nối. Code: {rc}")

def on_publish(client, userdata, mid):
    print(f"✅ Đã gửi tin nhắn ID: {mid}")

# ========== TẠO CLIENT ==========
print("🔄 Đang kết nối MQTT broker...")
client = mqtt.Client()
client.on_connect = on_connect
client.on_publish = on_publish

try:
    client.connect(BROKER, PORT, 60)
    client.loop_start()
    time.sleep(1)  # Đợi kết nối
    
    # ========== DỮ LIỆU TEST ==========
    print("\n" + "="*50)
    print("📤 GỬING DỮ LIỆU TEST")
    print("="*50)
    
    # Test 1: Dữ liệu THỎA điều kiện (soil_moisture < 30)
    test_data_1 = {
        "temperature": 28.5,
        "humidity": 65.0,
        "soilMoisture": 25.0,  # ✅ < 30 → Điều kiện ĐÃ THỎA MÃN
        "lightIntensity": 15000,
        "ph": 6.5,
        "sensorType": "DHT22"
    }
    
    print(f"\n📍 Topic: {TOPIC}")
    print(f"📦 Payload:")
    print(json.dumps(test_data_1, indent=2))
    print(f"⏰ Thời gian: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    # Gửi tin nhắn
    result = client.publish(TOPIC, json.dumps(test_data_1))
    
    if result.rc == mqtt.MQTT_ERR_SUCCESS:
        print("\n✅ Đã gửi dữ liệu thành công!")
        print("\n📊 KẾT QUẢ MONG ĐỢI:")
        print("  1. Dữ liệu sẽ được lưu vào InfluxDB")
        print("  2. Backend nhận qua MQTT")
        print("  3. Sau tối đa 30 giây, Rule Engine sẽ kiểm tra")
        print("  4. Vì soil_moisture = 25 < 30 → Quy tắc sẽ CHẠY")
        print("  5. Thiết bị DEV-PUMP-001 sẽ được BẬT")
        print("\n🔍 CÁCH KIỂM TRA:")
        print("  - Xem log Spring Boot (console)")
        print("  - Kiểm tra database: SELECT * FROM rule_execution_logs")
        print("  - Xem InfluxDB: http://localhost:8086")
        
        print("\n⏳ Đợi 35 giây để Scheduler chạy...")
        for i in range(35, 0, -5):
            print(f"   {i} giây nữa...", end="\r")
            time.sleep(5)
        
        print("\n\n✅ Đã hết thời gian chờ!")
        print("👉 Kiểm tra kết quả trong log Spring Boot và database nhé!")
        
    else:
        print(f"❌ Lỗi gửi tin nhắn. Code: {result.rc}")
    
    # Ngắt kết nối
    client.loop_stop()
    client.disconnect()
    print("\n🔌 Đã ngắt kết nối MQTT")
    
except Exception as e:
    print(f"\n❌ LỖI: {e}")
    print("\n🔧 KHẮC PHỤC:")
    print("  1. Kiểm tra MQTT broker đang chạy:")
    print("     docker ps | grep mosquitto")
    print("  2. Kiểm tra port 1883 có mở không:")
    print("     netstat -an | findstr 1883")
    print("  3. Khởi động lại MQTT:")
    print("     docker restart smartfarm-mosquitto")

print("\n" + "="*50)
print("🎉 HOÀN THÀNH!")
print("="*50)