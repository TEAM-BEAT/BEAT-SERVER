package com.beat.domain.user.api;

import org.springframework.web.bind.annotation.GetMapping;

public class HealthCheckController {
    @GetMapping("/health-check")
    public String healthcheck() {
        return "OK";
    }
}
