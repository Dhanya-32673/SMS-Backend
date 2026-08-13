package com.sicms.controller;

import com.sicms.dto.SetupRequest;
import com.sicms.dto.UserDto;
import com.sicms.service.SetupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/setup")
public class SetupController {

    private final SetupService setupService;

    public SetupController(SetupService setupService) {
        this.setupService = setupService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getSetupStatus() {
        boolean exists = setupService.adminExists();
        return ResponseEntity.ok(Map.of("adminExists", exists));
    }

    @PostMapping("/admin")
    public ResponseEntity<UserDto> createInitialAdmin(@Valid @RequestBody SetupRequest request) {
        UserDto adminUser = setupService.createInitialAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUser);
    }
}
