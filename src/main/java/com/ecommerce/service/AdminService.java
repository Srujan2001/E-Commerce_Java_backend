package com.ecommerce.service;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.AuthResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.SignupRequest;
import com.ecommerce.model.Admin;
import com.ecommerce.repository.AdminRepository;
import com.ecommerce.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Value("${admin.authorized.email}")
    private String authorizedEmail;

    // In-memory storage for pending admin requests
    private final Map<String, SignupRequest> pendingAdmins = new HashMap<>();

    @Transactional
    public ApiResponse registerAdmin(SignupRequest request, String baseUrl) {
        // Check if email already exists
        if (adminRepository.existsByAdminEmail(request.getEmail())) {
            return new ApiResponse(false, "Email already exists: " + request.getEmail());
        }

        // Generate token for approval
        String token = UUID.randomUUID().toString();
        pendingAdmins.put(token, request);

        // Send approval email to authorized admin
        String confirmUrl = baseUrl + "/api/admin/confirm/" + token;
        String rejectUrl = baseUrl + "/api/admin/reject/" + token;

        emailService.sendAdminApprovalRequest(
                authorizedEmail,
                request.getUsername(),
                request.getEmail(),
                request.getPhone(),
                request.getAddress(),
                confirmUrl,
                rejectUrl
        );

        return new ApiResponse(true, "A confirmation email has been sent to the authorized admin for approval.");
    }

    @Transactional
    public ApiResponse confirmAdminRegistration(String token) {
        SignupRequest request = pendingAdmins.remove(token);

        if (request == null) {
            return new ApiResponse(false, "Invalid or expired confirmation link");
        }

        // Create admin
        Admin admin = new Admin();
        admin.setAdminUsername(request.getUsername());
        admin.setAdminEmail(request.getEmail());
        admin.setAdminPassword(passwordEncoder.encode(request.getPassword()));
        admin.setAddress(request.getAddress());
        admin.setPhone(request.getPhone());
        admin.setIsApproved(true);

        adminRepository.save(admin);

        // Send approval email to user
        emailService.sendAdminApprovalEmail(
                request.getEmail(),
                request.getUsername(),
                request.getEmail(),
                request.getPhone()
        );

        return new ApiResponse(true, "Admin registered successfully and email sent to the user.");
    }

    @Transactional
    public ApiResponse rejectAdminRegistration(String token) {
        SignupRequest request = pendingAdmins.remove(token);

        if (request == null) {
            return new ApiResponse(false, "Invalid or expired rejection link");
        }

        // Send rejection email to user
        emailService.sendAdminRejectionEmail(request.getEmail(), request.getUsername());

        return new ApiResponse(true, "Admin registration request has been rejected and user notified.");
    }

    public AuthResponse login(LoginRequest request) {
        Optional<Admin> adminOptional = adminRepository.findByAdminEmail(request.getEmail());

        if (adminOptional.isEmpty()) {
            throw new RuntimeException("Admin not found with email: " + request.getEmail());
        }

        Admin admin = adminOptional.get();

        if (!admin.getIsApproved()) {
            throw new RuntimeException("Admin account is not approved yet");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getAdminPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(admin.getAdminEmail(), "ADMIN");

        return new AuthResponse(token, admin.getAdminEmail(), admin.getAdminUsername());
    }

    public Admin getAdminByEmail(String email) {
        return adminRepository.findByAdminEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }

    @Transactional
    public ApiResponse updateProfile(String email, String username, String address, String phone) {
        Admin admin = getAdminByEmail(email);
        admin.setAdminUsername(username);
        admin.setAddress(address);
        admin.setPhone(phone);
        adminRepository.save(admin);

        return new ApiResponse(true, "Profile updated successfully");
    }

    @Transactional
    public ApiResponse resetPassword(String email, String newPassword) {
        Admin admin = getAdminByEmail(email);
        admin.setAdminPassword(passwordEncoder.encode(newPassword));
        adminRepository.save(admin);

        return new ApiResponse(true, "Password updated successfully");
    }
}