package com.bankmind.module.auth.service;

import com.bankmind.module.auth.entity.RefreshToken;
import com.bankmind.module.auth.entity.User;
import com.bankmind.module.auth.repository.RefreshTokenRepository;
import com.bankmind.security.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    @Value("${bankmind.security.jwt.refresh-expiration-ms:86400000}")
    private long refreshExpirationMs;

    public TokenService(RefreshTokenRepository refreshTokenRepository, JwtProvider jwtProvider) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProvider = jwtProvider;
    }

    public String generateAccessToken(User user) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("ROLE_" + user.getRole().name())
                .build();
        return jwtProvider.generateToken(userDetails, user.getTenantId());
    }

    @Transactional
    public String generateRefreshToken(User user) {
        // Revoke/Delete previous tokens to avoid cluttering DB (Single Session/Device enforcement)
        refreshTokenRepository.deleteByUser(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("ROLE_" + user.getRole().name())
                .build();
        String tokenStr = jwtProvider.generateRefreshToken(userDetails, user.getTenantId());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenStr)
                .user(user)
                .expiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000))
                .createdAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(refreshToken);
        return tokenStr;
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(refreshToken);
        });
    }

    @Transactional(readOnly = true)
    public boolean validateRefreshToken(String token) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(token);
        if (refreshTokenOpt.isEmpty()) {
            return false;
        }
        RefreshToken refreshToken = refreshTokenOpt.get();
        if (refreshToken.getRevokedAt() != null) {
            return false;
        }
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername(refreshToken.getUser().getEmail())
                .password(refreshToken.getUser().getPasswordHash())
                .authorities("ROLE_" + refreshToken.getUser().getRole().name())
                .build();
        return jwtProvider.isTokenValid(token, userDetails);
    }
}
