package com.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "admin_id", columnDefinition = "BINARY(16)")
    private UUID adminId;

    @NotBlank(message = "Username is required")
    @Column(name = "admin_username", nullable = false)
    private String adminUsername;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(name = "admin_email", nullable = false, unique = true)
    private String adminEmail;

    @NotBlank(message = "Password is required")
    @Column(name = "admin_password", nullable = false)
    private String adminPassword;

    @Column(name = "address")
    private String address;

    @Column(name = "phone")
    private String phone;

    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}