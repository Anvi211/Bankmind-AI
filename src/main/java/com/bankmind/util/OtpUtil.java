package com.bankmind.util;

import java.security.SecureRandom;

public class OtpUtil {

    private static final SecureRandom random = new SecureRandom();

    private OtpUtil() {}

    public static String generateOtp() {
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
    }
}
