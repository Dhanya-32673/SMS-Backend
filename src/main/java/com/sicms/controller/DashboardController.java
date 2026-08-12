package com.sicms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicms.dto.AdminDashboardSummaryResponse;
import com.sicms.dto.FacultyDashboardSummaryResponse;
import com.sicms.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardSummaryResponse> getAdminDashboardSummary() {
        AdminDashboardSummaryResponse summary = dashboardService.getAdminSummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/faculty/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<FacultyDashboardSummaryResponse> getFacultyDashboardSummary(@AuthenticationPrincipal UserDetails userDetails) {
        FacultyDashboardSummaryResponse summary = dashboardService.getFacultySummaryForUser(userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.ok(summary);
    }
}
