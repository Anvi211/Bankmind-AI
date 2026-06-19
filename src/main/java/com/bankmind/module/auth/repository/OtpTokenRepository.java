package com.bankmind.module.auth.repository;

import com.bankmind.module.auth.entity.OtpPurpose;
import com.bankmind.module.auth.entity.OtpToken;
import com.bankmind.module.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    
    Optional<OtpToken> findByCode(String code);
    
    Optional<OtpToken> findByCodeAndUserAndPurposeAndUsedFalse(String code, User user, OtpPurpose purpose);
    
    List<OtpToken> findByUserAndUsedFalse(User user);
}
