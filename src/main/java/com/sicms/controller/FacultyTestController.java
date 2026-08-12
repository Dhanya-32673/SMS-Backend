package com.sicms.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/faculty/test")
public class FacultyTestController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> facultyTestEndpoint(Principal principal) {
        String username = principal != null ? principal.getName() : "FACULTY";
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Faculty authentication and authorization successful!",
                "user", username,
                "role", "ROLE_FACULTY"
        ));
    }
}
