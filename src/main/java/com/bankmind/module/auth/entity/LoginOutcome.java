package com.bankmind.module.auth.entity;

public enum LoginOutcome {
    SUCCESS,
    WRONG_PASSWORD,
    ACCOUNT_LOCKED,
    OTP_FAILED,
    ACCOUNT_NOT_VERIFIED
}
