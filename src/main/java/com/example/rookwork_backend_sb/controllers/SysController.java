package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.repositories.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/sys")
public class SysController {

    private final SystemSettingRepository systemSettingRepository;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> response = new HashMap<>();

        boolean isMaintenance = systemSettingRepository.findById("maintenance_mode")
                .map(setting -> "true".equalsIgnoreCase(setting.getSettingValue()))
                .orElse(false);

        response.put("maintenance", isMaintenance);

        return ResponseEntity.ok(response);
    }
}
