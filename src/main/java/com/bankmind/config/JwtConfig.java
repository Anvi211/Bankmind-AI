package com.bankmind.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${bankmind.security.jwt.secret}")
    private String secret;

    @Value("${bankmind.security.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${bankmind.security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public String getSecret() {
        return secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }
}