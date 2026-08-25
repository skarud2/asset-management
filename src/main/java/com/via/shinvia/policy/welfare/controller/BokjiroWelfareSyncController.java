package com.via.shinvia.policy.welfare.controller;


import com.via.shinvia.policy.welfare.service.BokjiroWelfareSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/welfare-support/sync")
@RequiredArgsConstructor
public class BokjiroWelfareSyncController {

    private final BokjiroWelfareSyncService syncService;

    @PostMapping("/bokjiro/national")
    public ResponseEntity<Map<String, Object>> syncNational() {
        int savedCount = syncService.synchronizeNational();

        return ResponseEntity.ok(Map.of(
                "sourceType", "BOKJIRO_NATIONAL",
                "savedCount", savedCount
        ));
    }

    @PostMapping("/bokjiro/local")
    public ResponseEntity<Map<String, Object>> syncLocal() {
        int savedCount = syncService.synchronizeLocal();

        return ResponseEntity.ok(Map.of(
                "sourceType", "BOKJIRO_LOCAL",
                "savedCount", savedCount
        ));
    }

    @PostMapping("/bokjiro/all")
    public ResponseEntity<Map<String, Object>> syncAll() {
        int savedCount = syncService.synchronizeAll();

        return ResponseEntity.ok(Map.of(
                "sourceType", "ALL",
                "savedCount", savedCount
        ));
    }
}