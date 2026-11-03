package dev.fatestay.research.server;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/api/v1/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "fate-stay-research");
    }

    public record HealthResponse(String status, String application) {}
}
