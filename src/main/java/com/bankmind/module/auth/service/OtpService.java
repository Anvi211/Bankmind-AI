package com.bankmind.module.auth.service;

import com.bankmind.module.auth.entity.OtpPurpose;
import com.bankmind.module.auth.entity.OtpToken;
import com.bankmind.module.auth.entity.User;
import com.bankmind.module.auth.repository.OtpTokenRepository;
import com.bankmind.util.OtpUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;

    public OtpService(OtpTokenRepository otpTokenRepository) {
        this.otpTokenRepository = otpTokenRepository;
    }

    @Transactional
    public String generateOtp(User user, OtpPurpose purpose) {
        // Invalidate previous unused OTP tokens for the same user and purpose
        otpTokenRepository.findByUserAndUsedFalse(user).forEach(token -> {
            if (token.getPurpose() == purpose) {
                token.setUsed(true);
                otpTokenRepository.save(token);
            }
        });

        String code = OtpUtil.generateOtp();
        OtpToken otpToken = OtpToken.builder()
                .code(code)
                .user(user)
                .purpose(purpose)
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(10)) // OTP expires in 10 minutes
                .createdAt(LocalDateTime.now())
                .build();

        otpTokenRepository.save(otpToken);
        return code;
    }

    @Transactional(readOnly = true)
    public boolean validateOtp(User user, String code, OtpPurpose purpose) {
        Optional<OtpToken> otpOpt = otpTokenRepository.findByCodeAndUserAndPurposeAndUsedFalse(code, user, purpose);
        if (otpOpt.isEmpty()) {
            return false;
        }
        OtpToken otpToken = otpOpt.get();
        return otpToken.getExpiresAt().isAfter(LocalDateTime.now());
    }

    @Transactional
    public void markOtpUsed(User user, String code, OtpPurpose purpose) {
        otpTokenRepository.findByCodeAndUserAndPurposeAndUsedFalse(code, user, purpose)
                .ifPresent(otpToken -> {
                    otpToken.setUsed(true);
                    otpTokenRepository.save(otpToken);
                });
    }

}
