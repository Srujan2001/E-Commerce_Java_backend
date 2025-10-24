package com.ecommerce.controller;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.AuthResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.SignupRequest;
import com.ecommerce.model.Admin;
import com.ecommerce.service.AdminService;
import com.ecommerce.util.OtpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final OtpUtil otpUtil;

    // In-memory storage for password reset OTPs
    private final Map<String, String> resetOtpStorage = new HashMap<>();

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody SignupRequest request,
                                                HttpServletRequest httpRequest) {
        try {
            String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName() +
                    ":" + httpRequest.getServerPort() + "/api";
            ApiResponse response = adminService.registerAdmin(request, baseUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Registration failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Registration failed: " + e.getMessage()));
        }
    }

    @GetMapping("/confirm/{token}")
    public ResponseEntity<String> confirmRegistration(@PathVariable String token) {
        ApiResponse response = adminService.confirmAdminRegistration(token);
        if (response.isSuccess()) {
            return ResponseEntity.ok("<h2 style='color: green;'>Success! Admin registered.</h2>");
        }
        return ResponseEntity.badRequest().body("<h2 style='color: red;'>Error: " + response.getMessage() + "</h2>");
    }

    @GetMapping("/reject/{token}")
    public ResponseEntity<String> rejectRegistration(@PathVariable String token) {
        ApiResponse response = adminService.rejectAdminRegistration(token);
        if (response.isSuccess()) {
            return ResponseEntity.ok("<h2 style='color: orange;'>Admin registration rejected.</h2>");
        }
        return ResponseEntity.badRequest().body("<h2 style='color: red;'>Error: " + response.getMessage() + "</h2>");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = adminService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        try {
            String email = authentication.getName();
            Admin admin = adminService.getAdminByEmail(email);
            return ResponseEntity.ok(new ApiResponse(true, "Profile fetched", admin));
        } catch (Exception e) {
            log.error("Failed to fetch profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to fetch profile"));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse> updateProfile(@RequestBody Map<String, String> updates,
                                                     Authentication authentication) {
        try {
            String email = authentication.getName();
            String username = updates.get("username");
            String address = updates.get("address");
            String phone = updates.get("phone");

            ApiResponse response = adminService.updateProfile(email, username, address, phone);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to update profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to update profile"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            // Generate and store OTP logic here
            String otp = otpUtil.generateNumericOtp(6);
            resetOtpStorage.put(email, otp);
            // Send OTP email (implement email service call)
            return ResponseEntity.ok(new ApiResponse(true, "OTP sent to " + email));
        } catch (Exception e) {
            log.error("Failed to send OTP", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to send OTP"));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        String storedOtp = resetOtpStorage.get(email);

        if (storedOtp != null && storedOtp.equals(otp)) {
            return ResponseEntity.ok(new ApiResponse(true, "OTP verified"));
        }
        return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid OTP"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String newPassword = request.get("password");

            ApiResponse response = adminService.resetPassword(email, newPassword);
            resetOtpStorage.remove(email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to reset password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to reset password"));
        }
    }
}