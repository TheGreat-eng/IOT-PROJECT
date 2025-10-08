#!/usr/bin/env python3
"""
Fake Pump Device - Giả lập máy bơm nhận lệnh MQTT
"""

import paho.mqtt.client as mqtt
import json
import time
from datetime import datetime

BROKER = "localhost"
PORT = 1883
DEVICE_ID = "DEV-PUMP-001"

pump_state = "OFF"

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print(f"\n{'='*60}")
        print(f"✅ [{DEVICE_ID}] Đã kết nối MQTT Broker")
        print(f"{'='*60}")
        
        # Subscribe để nhận lệnh điều khiển
        client.subscribe(f"device/{DEVICE_ID}/control")
        print(f"📡 Đang lắng nghe topic: device/{DEVICE_ID}/control")
        
        # Gửi status ONLINE
        status_msg = {
            "deviceId": DEVICE_ID,
            "status": "ONLINE",
            "state": pump_state,
            "timestamp": datetime.now().isoformat()
        }
        client.publish(f"device/{DEVICE_ID}/status", json.dumps(status_msg))
        print(f"✅ Đã gửi status: ONLINE, state: {pump_state}\n")
    else:
        print(f"❌ Lỗi kết nối MQTT: {rc}")

def on_message(client, userdata, msg):
    global pump_state
    
    print(f"\n{'='*60}")
    print(f"📥 NHẬN LỆNH TỪ BACKEND")
    print(f"{'='*60}")
    print(f"📍 Topic: {msg.topic}")
    
    try:
        payload = json.loads(msg.payload.decode())
        print(f"📦 Payload:")
        print(json.dumps(payload, indent=2, ensure_ascii=False))
        
        action = payload.get("action", "").upper()
        
        if action == "TURN_ON" or action == "ON":
            duration = payload.get("duration", 60)
            pump_state = "ON"
            print(f"\n💧 BẬT MÁY BƠM")
            print(f"⏱️  Thời gian: {duration} giây")
            
            # Gửi feedback về backend
            feedback = {
                "deviceId": DEVICE_ID,
                "status": "ONLINE",
                "state": "ON",
                "duration": duration,
                "timestamp": datetime.now().isoformat()
            }
            client.publish(f"device/{DEVICE_ID}/status", json.dumps(feedback))
            print(f"✅ Đã gửi trạng thái: MÁY BƠM ĐANG BẬT\n")
            
        elif action == "TURN_OFF" or action == "OFF":
            pump_state = "OFF"
            print(f"\n🛑 TẮT MÁY BƠM")
            
            # Gửi feedback về backend
            feedback = {
                "deviceId": DEVICE_ID,
                "status": "ONLINE",
                "state": "OFF",
                "timestamp": datetime.now().isoformat()
            }
            client.publish(f"device/{DEVICE_ID}/status", json.dumps(feedback))
            print(f"✅ Đã gửi trạng thái: MÁY BƠM ĐÃ TẮT\n")
        else:
            print(f"⚠️  Lệnh không xác định: {action}\n")
            
    except Exception as e:
        print(f"❌ Lỗi xử lý message: {e}\n")

# Setup MQTT client
client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message

print(f"{'='*60}")
print(f"🔌 FAKE PUMP DEVICE - {DEVICE_ID}")
print(f"{'='*60}")
print(f"🔗 Đang kết nối tới: {BROKER}:{PORT}...")

try:
    client.connect(BROKER, PORT, 60)
    print(f"⏳ Đang chờ lệnh điều khiển...\n")
    print(f"{'='*60}\n")
    client.loop_forever()
except KeyboardInterrupt:
    print(f"\n\n{'='*60}")
    print(f"👋 Dừng Fake Pump Device")
    print(f"{'='*60}\n")
    client.disconnect()
except Exception as e:
    print(f"❌ Lỗi: {e}")