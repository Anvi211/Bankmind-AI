package com.bankmind.module.auth.repository;

import com.bankmind.module.auth.entity.RefreshToken;
import com.bankmind.module.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Query("""
        SELECT rt
        FROM RefreshToken rt
        JOIN FETCH rt.user
        WHERE rt.token = :token
    """)
    Optional<RefreshToken> findByToken(@Param("token") String token);

    List<RefreshToken> findByUserAndRevokedAtIsNull(User user);

    void deleteByUser(User user);
}