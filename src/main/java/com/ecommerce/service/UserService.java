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

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final OtpUtil otpUtil;

    // In-memory storage for OTPs
    private final Map<String, String> otpStorage = new HashMap<>();
    private final Map<String, SignupRequest> pendingUsers = new HashMap<>();

    @Transactional
    public ApiResponse registerUser(SignupRequest request) {
        // Check if email already exists
        if (userRepository.existsByUseremail(request.getEmail())) {
            return new ApiResponse(false, "Email already exists: " + request.getEmail());
        }

        // Generate OTP
        String otp = otpUtil.generateOtp();
        otpStorage.put(request.getEmail(), otp);
        pendingUsers.put(request.getEmail(), request);

        // Send OTP email
        emailService.sendOtpEmail(request.getEmail(), otp);

        return new ApiResponse(true, "OTP has been sent to the registered email: " + request.getEmail());
    }

    @Transactional
    public ApiResponse verifyOtpAndCreateUser(String email, String otp) {
        String storedOtp = otpStorage.get(email);

        if (storedOtp == null) {
            return new ApiResponse(false, "OTP expired or not found");
        }

        if (!storedOtp.equals(otp)) {
            return new ApiResponse(false, "Invalid OTP");
        }

        SignupRequest request = pendingUsers.get(email);
        if (request == null) {
            return new ApiResponse(false, "User data not found");
        }

        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setUseremail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAddress(request.getAddress());
        user.setGender(request.getGender());

        userRepository.save(user);

        // Cleanup
        otpStorage.remove(email);
        pendingUsers.remove(email);

        return new ApiResponse(true, "User registered successfully");
    }

    public AuthResponse login(LoginRequest request) {
        Optional<User> userOptional = userRepository.findByUseremail(request.getEmail());

        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found with email: " + request.getEmail());
        }

        User user = userOptional.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(user.getUseremail(), "USER");

        return new AuthResponse(token, user.getUseremail(), user.getUsername());
    }

    public User getUserByEmail(String email) {
        return userRepository.findByUseremail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public ApiResponse initiatePasswordReset(String email) {
        if (!userRepository.existsByUseremail(email)) {
            return new ApiResponse(false, "Email not found");
        }

        String otp = otpUtil.generateNumericOtp(6);
        otpStorage.put(email, otp);

        emailService.sendOtpEmail(email, otp);

        return new ApiResponse(true, "OTP has been sent to " + email);
    }

    public ApiResponse verifyResetOtp(String email, String otp) {
        String storedOtp = otpStorage.get(email);

        if (storedOtp == null) {
            return new ApiResponse(false, "OTP expired or not found");
        }

        if (!storedOtp.equals(otp)) {
            return new ApiResponse(false, "Invalid OTP");
        }

        return new ApiResponse(true, "OTP verified successfully");
    }

    @Transactional
    public ApiResponse resetPassword(String email, String newPassword) {
        User user = getUserByEmail(email);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Cleanup OTP
        otpStorage.remove(email);

        return new ApiResponse(true, "Password updated successfully");
    }
}