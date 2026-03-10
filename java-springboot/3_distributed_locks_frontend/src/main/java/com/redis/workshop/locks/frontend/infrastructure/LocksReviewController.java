package com.redis.workshop.locks.frontend.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LocksReviewController {

    @GetMapping("/api/review/purchase-service")
    public Map<String, String> purchaseServiceReview() throws IOException {
        ClassPathResource resource = new ClassPathResource("workshop-files/PurchaseService.java");
        return Map.of(
            "fileName", "PurchaseService.java",
            "language", "java",
            "content", resource.getContentAsString(StandardCharsets.UTF_8)
        );
    }
}
