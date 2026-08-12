package com.sicms.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sicms.dto.FacultyAssignmentRequest;
import com.sicms.dto.FacultyAssignmentResponse;
import com.sicms.dto.FacultyCreateRequest;
import com.sicms.dto.FacultyResponse;
import com.sicms.dto.FacultyUpdateRequest;
import com.sicms.service.FacultyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/faculty")
public class FacultyController {

    private final FacultyService facultyService;
    private final com.sicms.service.StudentPhotoService photoService;

    public FacultyController(FacultyService facultyService, com.sicms.service.StudentPhotoService photoService) {
        this.facultyService = facultyService;
        this.photoService = photoService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<FacultyResponse>> searchFaculty(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, size, sort);
        Page<FacultyResponse> result = facultyService.searchFaculty(query, group, status, pageable);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FacultyResponse> createFaculty(@Valid @RequestBody FacultyCreateRequest request) {
        FacultyResponse response = facultyService.createFaculty(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<FacultyResponse> getFacultyById(@PathVariable Long id) {
        FacultyResponse response = facultyService.getFacultyById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FacultyResponse> updateFaculty(@PathVariable Long id,
            @Valid @RequestBody FacultyUpdateRequest request) {
        FacultyResponse response = facultyService.updateFaculty(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * ADMIN ONLY: Upload or update faculty profile photo
     */
    @PostMapping(value = "/{id}/photo", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> uploadFacultyPhoto(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String publicUrl = photoService.uploadFacultyPhoto(id, file);
        facultyService.updateFacultyPhoto(id, publicUrl);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> toggleFacultyStatus(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "INACTIVE");
        facultyService.toggleFacultyStatus(id, status);
        return ResponseEntity.ok(Map.of("message", "Faculty status updated to " + status));
    }

    @PostMapping("/{id}/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FacultyAssignmentResponse> addAssignment(@PathVariable Long id,
            @Valid @RequestBody FacultyAssignmentRequest request) {
        FacultyAssignmentResponse response = facultyService.addAssignment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<List<FacultyAssignmentResponse>> getFacultyAssignments(@PathVariable Long id) {
        List<FacultyAssignmentResponse> list = facultyService.getFacultyAssignments(id);
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}/assignments/{assignmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeAssignment(@PathVariable Long id, @PathVariable Long assignmentId) {
        facultyService.removeAssignment(id, assignmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<List<FacultyAssignmentResponse>> getCurrentFacultyAssignments(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        List<FacultyAssignmentResponse> list = facultyService.getCurrentFacultyAssignments(userDetails.getUsername());
        return ResponseEntity.ok(list);
    }

    /**
     * ADMIN ONLY: Permanently delete faculty member, assignments, storage files,
     * and user account
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFaculty(@PathVariable Long id) {
        facultyService.deleteFaculty(id);
        return ResponseEntity.noContent().build();
    }
}
