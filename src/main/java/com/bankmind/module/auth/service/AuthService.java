package com.bankmind.module.auth.service;

import com.bankmind.exception.DuplicateEmailException;
import com.bankmind.exception.ResourceNotFoundException;
import com.bankmind.exception.ValidationException;
import com.bankmind.module.auth.entity.*;
import com.bankmind.module.auth.repository.LoginEventRepository;
import com.bankmind.module.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final LoginEventRepository loginEventRepository;
    private final OtpService otpService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       LoginEventRepository loginEventRepository,
                       OtpService otpService,
                       TokenService tokenService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.loginEventRepository = loginEventRepository;
        this.otpService = otpService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(String fullName, String email, String password, String phoneNumber, Long tenantId) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email already registered: " + email);
        }

        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .phoneNumber(phoneNumber)
                .tenantId(tenantId != null ? tenantId : 1L) // Default to tenant 1 per Phase 0 spec
                .role(Role.CUSTOMER)
                .status(UserStatus.PENDING)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        // Generate email verification OTP
        otpService.generateOtp(savedUser, OtpPurpose.EMAIL_VERIFICATION);

        return savedUser;
    }

    @Transactional
    public void verifyEmail(String email, String otpCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ValidationException("Email is already verified");
        }

        boolean isValid = otpService.validateOtp(user, otpCode, OtpPurpose.EMAIL_VERIFICATION);
        if (!isValid) {
            throw new ValidationException("Invalid or expired OTP");
        }

        otpService.markOtpUsed(user, otpCode, OtpPurpose.EMAIL_VERIFICATION);


        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public Map<String, String> login(String email, String password, String ipAddress, String userAgent) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            LoginEvent event = LoginEvent.builder()
                    .user(null)
                    .email(email)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .outcome(LoginOutcome.WRONG_PASSWORD)
                    .createdAt(LocalDateTime.now())
                    .build();
            loginEventRepository.save(event);
            throw new ValidationException("Invalid email or password");
        }

        User user = userOpt.get();

        if (user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.DISABLED) {
            LoginEvent event = LoginEvent.builder()
                    .user(user)
                    .email(email)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .outcome(LoginOutcome.ACCOUNT_LOCKED)
                    .createdAt(LocalDateTime.now())
                    .build();
            loginEventRepository.save(event);
            throw new ValidationException("Account is locked or disabled");
        }

        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            LoginEvent event = LoginEvent.builder()
                    .user(user)
                    .email(email)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .outcome(LoginOutcome.ACCOUNT_NOT_VERIFIED)
                    .createdAt(LocalDateTime.now())
                    .build();
            loginEventRepository.save(event);
            throw new ValidationException("Email is not verified");
        }

        // Brute-force check: check failed attempts in the last 15 minutes
        LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);
        long failedAttempts = loginEventRepository.countByEmailAndOutcomeAndCreatedAtAfter(
                email, LoginOutcome.WRONG_PASSWORD, fifteenMinutesAgo);
        if (failedAttempts >= 5) {
            user.setStatus(UserStatus.LOCKED);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            LoginEvent event = LoginEvent.builder()
                    .user(user)
                    .email(email)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .outcome(LoginOutcome.ACCOUNT_LOCKED)
                    .createdAt(LocalDateTime.now())
                    .build();
            loginEventRepository.save(event);
            throw new ValidationException("Account has been locked due to too many failed attempts");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            LoginEvent event = LoginEvent.builder()
                    .user(user)
                    .email(email)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .outcome(LoginOutcome.WRONG_PASSWORD)
                    .createdAt(LocalDateTime.now())
                    .build();
            loginEventRepository.save(event);

            long newFailedAttempts = failedAttempts + 1;
            if (newFailedAttempts >= 5) {
                user.setStatus(UserStatus.LOCKED);
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);

                LoginEvent lockoutEvent = LoginEvent.builder()
                        .user(user)
                        .email(email)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .outcome(LoginOutcome.ACCOUNT_LOCKED)
                        .createdAt(LocalDateTime.now())
                        .build();
                loginEventRepository.save(lockoutEvent);
                throw new ValidationException("Account has been locked due to too many failed attempts");
            }

            throw new ValidationException("Invalid email or password");
        }

        // Login success path
        LoginEvent event = LoginEvent.builder()
                .user(user)
                .email(email)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .outcome(LoginOutcome.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
        loginEventRepository.save(event);

        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        return tokens;
    }

    @Transactional
    public void logout(String refreshToken) {
        tokenService.revokeRefreshToken(refreshToken);
    }
}
