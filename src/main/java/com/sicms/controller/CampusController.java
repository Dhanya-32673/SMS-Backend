package com.sicms.controller;

import com.sicms.dto.AssignStudentsRequest;
import com.sicms.dto.CampusResponse;
import com.sicms.dto.CreateCampusRequest;
import com.sicms.dto.StudentResponse;
import com.sicms.service.CampusService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/campuses", "/api/academic/campuses"})
public class CampusController {

    private final CampusService campusService;

    public CampusController(CampusService campusService) {
        this.campusService = campusService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<List<CampusResponse>> getCampuses() {
        return ResponseEntity.ok(campusService.getAllCampusResponses());
    }

    @GetMapping("/names")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<List<String>> getCampusNames() {
        return ResponseEntity.ok(campusService.getOfficialCampusNames());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<CampusResponse> getCampus(@PathVariable Long id) {
        return ResponseEntity.ok(campusService.getCampusResponseById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CampusResponse> createCampus(@Valid @RequestBody CreateCampusRequest request) {
        CampusResponse created = campusService.createCampus(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CampusResponse> updateCampus(
            @PathVariable Long id,
            @Valid @RequestBody CreateCampusRequest request) {
        return ResponseEntity.ok(campusService.updateCampus(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCampus(@PathVariable Long id) {
        campusService.deleteCampus(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/{id}/students", "/{id}/members"})
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<List<StudentResponse>> getCampusStudents(@PathVariable Long id) {
        return ResponseEntity.ok(campusService.getCampusStudents(id));
    }

    @PostMapping({"/{id}/students", "/{id}/assign", "/{id}/members"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> assignStudentsToCampus(
            @PathVariable Long id,
            @RequestBody AssignStudentsRequest request) {
        List<String> studentIds = request != null && request.getStudentIds() != null
                ? request.getStudentIds()
                : new ArrayList<>();
        int count = campusService.assignStudentsToCampus(id, studentIds);
        return ResponseEntity.ok(Map.of(
                "message", "Students assigned to campus successfully",
                "assignedCount", count
        ));
    }

    @DeleteMapping({"/{id}/students/{studentId}", "/{id}/members/{studentId}"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeStudentFromCampus(
            @PathVariable Long id,
            @PathVariable String studentId) {
        campusService.removeStudentFromCampus(id, studentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/remove-students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> removeStudentsFromCampus(
            @PathVariable Long id,
            @RequestBody List<String> studentIds) {
        campusService.removeStudentsFromCampus(id, studentIds);
        return ResponseEntity.ok(Map.of("message", "Students removed from campus successfully"));
    }
}
