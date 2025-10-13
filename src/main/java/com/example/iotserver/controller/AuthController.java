package com.example.iotserver.controller;

import com.example.iotserver.dto.request.LoginRequest;
import com.example.iotserver.dto.request.RegisterRequest;
import com.example.iotserver.dto.response.AuthResponse;
import com.example.iotserver.entity.User;
import com.example.iotserver.enums.UserRole;
import com.example.iotserver.security.JwtUtil;
import com.example.iotserver.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "1. Authentication", description = "API cho việc Đăng ký và Đăng nhập")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "Đăng ký tài khoản mới", description = "Tạo một tài khoản người dùng mới trong hệ thống.") // <--
                                                                                                                    // THÊM
    @ApiResponses(value = { // <-- THÊM
            @ApiResponse(responseCode = "200", description = "Đăng ký thành công", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc Email đã tồn tại", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // Kiểm tra email đã tồn tại
        if (userService.existsByEmail(request.getEmail())) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Email đã được sử dụng");
            return ResponseEntity.badRequest().body(error);
        }

        // Tạo user mới
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getEmail()); // ✅ FIX: Thêm username
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhone());
        user.setRole(UserRole.FARMER); // ✅ FIX: Đổi thành UserRole.FARMER

        User savedUser = userService.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Đăng ký thành công");
        response.put("userId", savedUser.getId());
        response.put("email", savedUser.getEmail());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Đăng nhập vào hệ thống", description = "Xác thực người dùng và trả về JWT token.") // <-- THÊM
    @ApiResponses(value = { // <-- THÊM
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Sai email hoặc mật khẩu", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        // Tìm user theo email
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không đúng"));

        // Kiểm tra password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Email hoặc mật khẩu không đúng");
        }

        // Tạo JWT token
        String token = jwtUtil.generateToken(user.getEmail());

        // Tạo response
        AuthResponse response = AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole()) // <-- THAY ĐỔI Ở ĐÂY, đơn giản hơn nhiều
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "API hoạt động OK!");
        return ResponseEntity.ok(response);
    }
}