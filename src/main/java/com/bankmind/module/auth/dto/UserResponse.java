package com.bankmind.module.auth.dto;

import com.bankmind.module.auth.entity.Role;
import com.bankmind.module.auth.entity.UserStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private Long tenantId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Role role;
    private UserStatus status;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
