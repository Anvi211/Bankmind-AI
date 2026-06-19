package com.bankmind.module.auth.repository;

import com.bankmind.module.auth.entity.LoginEvent;
import com.bankmind.module.auth.entity.LoginOutcome;
import com.bankmind.module.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginEventRepository extends JpaRepository<LoginEvent, Long> {
    
    Page<LoginEvent> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<LoginEvent> findByIpAddressAndCreatedAtAfter(String ipAddress, LocalDateTime dateTime);
    
    long countByEmailAndOutcomeAndCreatedAtAfter(String email, LoginOutcome outcome, LocalDateTime dateTime);
    
    long countByIpAddressAndOutcomeAndCreatedAtAfter(String ipAddress, LoginOutcome outcome, LocalDateTime dateTime);
}
