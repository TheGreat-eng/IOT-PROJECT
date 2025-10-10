package com.example.iotserver.controller;

import com.example.iotserver.dto.response.ApiResponse;
import com.example.iotserver.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Lấy dữ liệu tóm tắt cho dashboard hoặc báo cáo
     * GET /api/reports/summary?farmId=1
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(@RequestParam Long farmId) {
        Map<String, Object> summary = reportService.getDashboardSummary(farmId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // Các API khác cho báo cáo có thể được thêm vào đây
    // Ví dụ: Lấy lịch sử tưới nước, lượng điện tiêu thụ...
}