package com.example.iotserver.controller;

import com.example.iotserver.dto.AIPredictionResponse;
import com.example.iotserver.dto.response.ApiResponse;
import com.example.iotserver.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "7. AI Predictions", description = "API dự đoán AI (Machine Learning)")
public class AIController {

    private final AIService aiService;

    @GetMapping("/predictions")
    @Operation(summary = "Lấy dự đoán từ AI/ML model")
    public ResponseEntity<ApiResponse<AIPredictionResponse>> getAIPredictions(
            @Parameter(description = "ID nông trại") @RequestParam Long farmId) {
        AIPredictionResponse predictions = aiService.getPredictions(farmId);
        if (predictions == null) {
            return ResponseEntity.status(503).body(ApiResponse.error("AI Service không khả dụng"));
        }
        return ResponseEntity.ok(ApiResponse.success("Lấy dữ liệu dự đoán thành công", predictions));
    }
}