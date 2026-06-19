package com.bankmind.module.auth.repository;

import com.bankmind.module.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByEmailAndTenantId(String email, Long tenantId);
    
    boolean existsByEmail(String email);
}
