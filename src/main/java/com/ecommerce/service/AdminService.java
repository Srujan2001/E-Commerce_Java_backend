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

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    // FIXED: Using ConcurrentHashMap for thread-safety
    private final ConcurrentHashMap<String, SignupRequest> pendingAdmins = new ConcurrentHashMap<>();

    @Transactional
    public ApiResponse registerAdmin(SignupRequest request, String baseUrl) {
        try {
            log.info("Admin registration attempt for email: {}", request.getEmail());

            // Check if email already exists
            if (adminRepository.existsByAdminEmail(request.getEmail())) {
                log.warn("Admin email already exists: {}", request.getEmail());
                return new ApiResponse(false, "Email already exists: " + request.getEmail());
            }

            // Generate token for approval
            String token = UUID.randomUUID().toString();
            pendingAdmins.put(token, request);
            log.info("Admin registration token generated: {}", token);

            // Send approval email to authorized admin
            String confirmUrl = baseUrl + "/admin/confirm/" + token;
            String rejectUrl = baseUrl + "/admin/reject/" + token;

            try {
                emailService.sendAdminApprovalRequest(
                        authorizedEmail,
                        request.getUsername(),
                        request.getEmail(),
                        request.getPhone() != null ? request.getPhone() : "Not provided",
                        request.getAddress() != null ? request.getAddress() : "Not provided",
                        confirmUrl,
                        rejectUrl
                );
                log.info("Admin approval email sent to: {}", authorizedEmail);
            } catch (Exception e) {
                log.error("Failed to send admin approval email", e);
                // Continue anyway - admin can approve manually
            }

            return new ApiResponse(true, "Registration request submitted successfully. Awaiting admin approval.");
        } catch (Exception e) {
            log.error("Admin registration failed for email: {}", request.getEmail(), e);
            return new ApiResponse(false, "Registration failed: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse confirmAdminRegistration(String token) {
        try {
            log.info("Admin registration confirmation for token: {}", token);

            SignupRequest request = pendingAdmins.remove(token);

            if (request == null) {
                log.warn("Invalid or expired confirmation token: {}", token);
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
            log.info("Admin registered successfully: {}", request.getEmail());

            // Send approval email to user
            try {
                emailService.sendAdminApprovalEmail(
                        request.getEmail(),
                        request.getUsername(),
                        request.getEmail(),
                        request.getPhone() != null ? request.getPhone() : "Not provided"
                );
                log.info("Admin approval notification sent to: {}", request.getEmail());
            } catch (Exception e) {
                log.error("Failed to send approval notification", e);
            }

            return new ApiResponse(true, "Admin registered successfully and email sent to the user.");
        } catch (Exception e) {
            log.error("Admin confirmation failed for token: {}", token, e);
            return new ApiResponse(false, "Confirmation failed: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse rejectAdminRegistration(String token) {
        try {
            log.info("Admin registration rejection for token: {}", token);

            SignupRequest request = pendingAdmins.remove(token);

            if (request == null) {
                log.warn("Invalid or expired rejection token: {}", token);
                return new ApiResponse(false, "Invalid or expired rejection link");
            }

            // Send rejection email to user
            try {
                emailService.sendAdminRejectionEmail(request.getEmail(), request.getUsername());
                log.info("Admin rejection notification sent to: {}", request.getEmail());
            } catch (Exception e) {
                log.error("Failed to send rejection notification", e);
            }

            return new ApiResponse(true, "Admin registration request has been rejected and user notified.");
        } catch (Exception e) {
            log.error("Admin rejection failed for token: {}", token, e);
            return new ApiResponse(false, "Rejection failed: " + e.getMessage());
        }
    }

    public AuthResponse login(LoginRequest request) {
        try {
            log.info("Admin login attempt for email: {}", request.getEmail());

            Optional<Admin> adminOptional = adminRepository.findByAdminEmail(request.getEmail());

            if (adminOptional.isEmpty()) {
                log.warn("Admin not found with email: {}", request.getEmail());
                throw new RuntimeException("Invalid email or password");
            }

            Admin admin = adminOptional.get();

            if (!admin.getIsApproved()) {
                log.warn("Admin account not approved: {}", request.getEmail());
                throw new RuntimeException("Admin account is not approved yet");
            }

            if (!passwordEncoder.matches(request.getPassword(), admin.getAdminPassword())) {
                log.warn("Invalid password for admin email: {}", request.getEmail());
                throw new RuntimeException("Invalid email or password");
            }

            String token = jwtUtil.generateToken(admin.getAdminEmail(), "ADMIN");
            log.info("Admin login successful: {}", request.getEmail());

            return new AuthResponse(token, admin.getAdminEmail(), admin.getAdminUsername());
        } catch (Exception e) {
            log.error("Admin login failed for email: {}", request.getEmail(), e);
            throw e;
        }
    }

    public Admin getAdminByEmail(String email) {
        return adminRepository.findByAdminEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }

    @Transactional
    public ApiResponse updateProfile(String email, String username, String address, String phone) {
        try {
            log.info("Admin profile update for email: {}", email);

            Admin admin = getAdminByEmail(email);
            admin.setAdminUsername(username);
            admin.setAddress(address);
            admin.setPhone(phone);
            adminRepository.save(admin);

            log.info("Admin profile updated successfully: {}", email);
            return new ApiResponse(true, "Profile updated successfully");
        } catch (Exception e) {
            log.error("Admin profile update failed for email: {}", email, e);
            return new ApiResponse(false, "Failed to update profile: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse resetPassword(String email, String newPassword) {
        try {
            log.info("Admin password reset for email: {}", email);

            Admin admin = getAdminByEmail(email);
            admin.setAdminPassword(passwordEncoder.encode(newPassword));
            adminRepository.save(admin);

            log.info("Admin password reset successfully: {}", email);
            return new ApiResponse(true, "Password updated successfully");
        } catch (Exception e) {
            log.error("Admin password reset failed for email: {}", email, e);
            return new ApiResponse(false, "Failed to reset password: " + e.getMessage());
        }
    }
}