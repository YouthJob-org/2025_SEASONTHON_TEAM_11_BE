package com.youthjob.api;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthCheckController {

    @GetMapping("health")
    public String healthCheck() {
        return "OK";
    }

    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody Map<String, Object> body) {
        return body; // 요청을 그대로 반환 -> 필터/escape 확인용
    }
}
