package com.example.iotserver.controller;

import com.example.iotserver.dto.FarmDTO;
import com.example.iotserver.service.FarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/farms")
@RequiredArgsConstructor
public class FarmController {

    private final FarmService farmService;

    /**
     * Create new farm
     * POST /api/farms
     */
    @PostMapping
    public ResponseEntity<FarmDTO> createFarm(
            @RequestBody FarmDTO dto,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        FarmDTO created = farmService.createFarm(userId, dto);
        return ResponseEntity.ok(created);
    }

    /**
     * Update farm
     * PUT /api/farms/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<FarmDTO> updateFarm(
            @PathVariable Long id,
            @RequestBody FarmDTO dto,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        FarmDTO updated = farmService.updateFarm(id, userId, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete farm
     * DELETE /api/farms/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFarm(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        farmService.deleteFarm(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get farm by ID
     * GET /api/farms/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<FarmDTO> getFarm(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        FarmDTO farm = farmService.getFarm(id, userId);
        return ResponseEntity.ok(farm);
    }

    /**
     * Get all farms owned by current user
     * GET /api/farms
     */
    @GetMapping
    public ResponseEntity<List<FarmDTO>> getUserFarms(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        List<FarmDTO> farms = farmService.getUserFarms(userId);
        return ResponseEntity.ok(farms);
    }

    /**
     * Get all farms user has access to (owned + member)
     * GET /api/farms/accessible
     */
    @GetMapping("/accessible")
    public ResponseEntity<List<FarmDTO>> getAccessibleFarms(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        List<FarmDTO> farms = farmService.getFarmsWithAccess(userId);
        return ResponseEntity.ok(farms);
    }

    // Helper method to extract user ID from authentication
    private Long getUserIdFromAuth(Authentication authentication) {
        // TODO: Implement based on your security configuration
        // For now, return a dummy value for testing
        if (authentication == null) {
            return 1L; // Default user for testing
        }

        // In production, get from JWT or session
        // Example: ((UserDetails) authentication.getPrincipal()).getId()
        return 1L;
    }
}
