package com.example.iotserver.service;

import com.example.iotserver.entity.User;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    User save(User user);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findById(Long id);

    // --- CÁC PHƯƠNG THỨC MỚI CHO ADMIN ---
    Page<User> findAllUsers(Pageable pageable);

    User lockUser(Long userId);

    User unlockUser(Long userId);
}