package com.ecommerce.service;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.AuthResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.SignupRequest;
import com.ecommerce.model.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.util.JwtUtil;
import com.ecommerce.util.OtpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final OtpUtil otpUtil;

    // FIXED: Using ConcurrentHashMap for thread-safety
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, SignupRequest> pendingUsers = new ConcurrentHashMap<>();
    private final Map<String, Long> otpTimestamps = new ConcurrentHashMap<>();

    private static final long OTP_EXPIRY_TIME = 10 * 60 * 1000; // 10 minutes

    @Transactional
    public ApiResponse registerUser(SignupRequest request) {
        try {
            log.info("Registration attempt for email: {}", request.getEmail());

            // Check if email already exists
            if (userRepository.existsByUseremail(request.getEmail())) {
                log.warn("Email already exists: {}", request.getEmail());
                return new ApiResponse(false, "Email already exists: " + request.getEmail());
            }

            // Generate OTP
            String otp = otpUtil.generateNumericOtp(6);
            log.info("Generated OTP for {}: {}", request.getEmail(), otp);

            otpStorage.put(request.getEmail(), otp);
            pendingUsers.put(request.getEmail(), request);
            otpTimestamps.put(request.getEmail(), System.currentTimeMillis());

            // Send OTP email
            try {
                emailService.sendOtpEmail(request.getEmail(), otp);
                log.info("OTP email sent successfully to: {}", request.getEmail());
            } catch (Exception e) {
                log.error("Failed to send OTP email to: {}", request.getEmail(), e);
                // Continue anyway - OTP is stored, user can retry
            }

            return new ApiResponse(true, "OTP has been sent to the registered email: " + request.getEmail());
        } catch (Exception e) {
            log.error("Registration failed for email: {}", request.getEmail(), e);
            return new ApiResponse(false, "Registration failed: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse verifyOtpAndCreateUser(String email, String otp) {
        try {
            log.info("OTP verification attempt for email: {}", email);

            String storedOtp = otpStorage.get(email);
            Long timestamp = otpTimestamps.get(email);

            if (storedOtp == null || timestamp == null) {
                log.warn("OTP expired or not found for email: {}", email);
                return new ApiResponse(false, "OTP expired or not found. Please register again.");
            }

            // Check OTP expiry
            if (System.currentTimeMillis() - timestamp > OTP_EXPIRY_TIME) {
                log.warn("OTP expired for email: {}", email);
                otpStorage.remove(email);
                pendingUsers.remove(email);
                otpTimestamps.remove(email);
                return new ApiResponse(false, "OTP has expired. Please register again.");
            }

            if (!storedOtp.equals(otp)) {
                log.warn("Invalid OTP provided for email: {}", email);
                return new ApiResponse(false, "Invalid OTP. Please try again.");
            }

            SignupRequest request = pendingUsers.get(email);
            if (request == null) {
                log.warn("User data not found for email: {}", email);
                return new ApiResponse(false, "User data not found. Please register again.");
            }

            // Create user
            User user = new User();
            user.setUsername(request.getUsername());
            user.setUseremail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setAddress(request.getAddress());
            user.setGender(request.getGender());

            userRepository.save(user);
            log.info("User registered successfully: {}", email);

            // Cleanup
            otpStorage.remove(email);
            pendingUsers.remove(email);
            otpTimestamps.remove(email);

            return new ApiResponse(true, "User registered successfully! You can now login.");
        } catch (Exception e) {
            log.error("OTP verification failed for email: {}", email, e);
            return new ApiResponse(false, "Verification failed: " + e.getMessage());
        }
    }

    public AuthResponse login(LoginRequest request) {
        try {
            log.info("Login attempt for email: {}", request.getEmail());

            Optional<User> userOptional = userRepository.findByUseremail(request.getEmail());

            if (userOptional.isEmpty()) {
                log.warn("User not found with email: {}", request.getEmail());
                throw new RuntimeException("Invalid email or password");
            }

            User user = userOptional.get();

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                log.warn("Invalid password for email: {}", request.getEmail());
                throw new RuntimeException("Invalid email or password");
            }

            String token = jwtUtil.generateToken(user.getUseremail(), "USER");
            log.info("Login successful for email: {}", request.getEmail());

            return new AuthResponse(token, user.getUseremail(), user.getUsername());
        } catch (Exception e) {
            log.error("Login failed for email: {}", request.getEmail(), e);
            throw e;
        }
    }

    public User getUserByEmail(String email) {
        return userRepository.findByUseremail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public ApiResponse initiatePasswordReset(String email) {
        try {
            log.info("Password reset initiated for email: {}", email);

            if (!userRepository.existsByUseremail(email)) {
                log.warn("Email not found for password reset: {}", email);
                return new ApiResponse(false, "Email not found");
            }

            String otp = otpUtil.generateNumericOtp(6);
            log.info("Generated reset OTP for {}: {}", email, otp);

            otpStorage.put(email, otp);
            otpTimestamps.put(email, System.currentTimeMillis());

            try {
                emailService.sendOtpEmail(email, otp);
                log.info("Reset OTP email sent successfully to: {}", email);
            } catch (Exception e) {
                log.error("Failed to send reset OTP email to: {}", email, e);
            }

            return new ApiResponse(true, "OTP has been sent to " + email);
        } catch (Exception e) {
            log.error("Password reset initiation failed for email: {}", email, e);
            return new ApiResponse(false, "Failed to initiate password reset: " + e.getMessage());
        }
    }

    public ApiResponse verifyResetOtp(String email, String otp) {
        try {
            log.info("Reset OTP verification attempt for email: {}", email);

            String storedOtp = otpStorage.get(email);
            Long timestamp = otpTimestamps.get(email);

            if (storedOtp == null || timestamp == null) {
                log.warn("Reset OTP expired or not found for email: {}", email);
                return new ApiResponse(false, "OTP expired or not found");
            }

            // Check OTP expiry
            if (System.currentTimeMillis() - timestamp > OTP_EXPIRY_TIME) {
                log.warn("Reset OTP expired for email: {}", email);
                otpStorage.remove(email);
                otpTimestamps.remove(email);
                return new ApiResponse(false, "OTP has expired. Please request a new one.");
            }

            if (!storedOtp.equals(otp)) {
                log.warn("Invalid reset OTP provided for email: {}", email);
                return new ApiResponse(false, "Invalid OTP");
            }

            log.info("Reset OTP verified successfully for email: {}", email);
            return new ApiResponse(true, "OTP verified successfully");
        } catch (Exception e) {
            log.error("Reset OTP verification failed for email: {}", email, e);
            return new ApiResponse(false, "Verification failed: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse resetPassword(String email, String newPassword) {
        try {
            log.info("Password reset for email: {}", email);

            User user = getUserByEmail(email);
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            // Cleanup OTP
            otpStorage.remove(email);
            otpTimestamps.remove(email);

            log.info("Password reset successfully for email: {}", email);
            return new ApiResponse(true, "Password updated successfully");
        } catch (Exception e) {
            log.error("Password reset failed for email: {}", email, e);
            return new ApiResponse(false, "Failed to reset password: " + e.getMessage());
        }
    }
}