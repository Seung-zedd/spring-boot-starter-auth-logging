package com.example.springboot_starter_auth.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
// BoilerPlate Class
public class EnvironmentUtil {

    private final Environment environment;

    public boolean isLocalEnvironment() {
        return Arrays.asList(environment.getActiveProfiles()).contains("local");
    }

    public boolean isDevEnvironment() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }



    public boolean isHttpEnvironment() {
        // Local: HTTP, Dev/Prod: HTTPS
        // dev is used for integrated web server test with HTTPS
        return Arrays.asList(environment.getActiveProfiles()).contains("local");
    }

}
