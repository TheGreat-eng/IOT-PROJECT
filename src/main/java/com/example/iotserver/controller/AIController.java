package com.example.iotserver.controller;

import com.example.iotserver.dto.AIPredictionResponse;
import com.example.iotserver.dto.response.ApiResponse;
import com.example.iotserver.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @GetMapping("/predictions")
    public ResponseEntity<ApiResponse<AIPredictionResponse>> getAIPredictions(@RequestParam Long farmId) {
        AIPredictionResponse predictions = aiService.getPredictions(farmId);
        if (predictions == null) {
            return ResponseEntity.status(503).body(ApiResponse.error("AI Service không khả dụng"));
        }
        return ResponseEntity.ok(ApiResponse.success("Lấy dữ liệu dự đoán thành công", predictions));
    }
}