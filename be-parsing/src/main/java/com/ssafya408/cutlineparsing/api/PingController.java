package com.ssafya408.cutlineparsing.api;

import com.ssafya408.cutlineparsing.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
public class PingController {
    
    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ping() {
        Map<String, Object> pingData = new HashMap<>();
        pingData.put("message", "pong");
        pingData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        pingData.put("service", "cutline-parsing");
        pingData.put("status", "healthy");
        
        return ResponseEntity.ok(ApiResponse.success(pingData));
    }
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("status", "UP");
        healthData.put("service", "cutline-parsing");
        healthData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        healthData.put("uptime", "파싱 서비스가 정상적으로 실행 중입니다.");
        
        return ResponseEntity.ok(ApiResponse.success(healthData));
    }
}
