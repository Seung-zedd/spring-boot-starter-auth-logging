package com.example.springboot_starter_auth.global.util;

import java.util.UUID;

// BoilerPlate Class
public class UuidUtil {
    public static String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}
