package com.ekko.order_service.domain.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class OrderNumberGenerator {

    private static final String PREFIX = "EKK";
    private static final String SUFFIX_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SUFFIX_LENGTH = 4;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return PREFIX + "-" + datePart + "-" + randomSuffix();
    }

    private String randomSuffix() {
        StringBuilder sb = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            sb.append(SUFFIX_CHARS.charAt(RANDOM.nextInt(SUFFIX_CHARS.length())));
        }
        return sb.toString();
    }
}