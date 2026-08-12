package com.sicms.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/test")
public class AdminTestController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> adminTestEndpoint(Principal principal) {
        String username = principal != null ? principal.getName() : "ADMIN";
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Admin authentication and authorization successful!",
                "user", username,
                "role", "ROLE_ADMIN"
        ));
    }
}
