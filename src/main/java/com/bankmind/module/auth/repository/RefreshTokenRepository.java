package com.bankmind.module.auth.repository;

import com.bankmind.module.auth.entity.RefreshToken;
import com.bankmind.module.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    
    List<RefreshToken> findByUserAndRevokedAtIsNull(User user);
    
    void deleteByUser(User user);
}
