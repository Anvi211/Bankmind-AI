package com.bankmind.module.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_events", indexes = {
    @Index(name = "idx_le_user", columnList = "user_id, created_at"),
    @Index(name = "idx_le_ip", columnList = "ip_address, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginOutcome outcome;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
