package com.sicms.controller;

import com.sicms.dto.AssignStudentsRequest;
import com.sicms.dto.CreateSectionRequest;
import com.sicms.dto.SectionResponse;
import com.sicms.dto.StudentResponse;
import com.sicms.entity.AcademicGroup;
import com.sicms.service.AcademicGroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic")
public class AcademicGroupController {

    private final AcademicGroupService groupService;

    public AcademicGroupController(AcademicGroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping("/groups")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<List<AcademicGroup>> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @PostMapping("/groups")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AcademicGroup> createGroup(@Valid @RequestBody AcademicGroup group) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.createGroup(group));
    }

    @DeleteMapping("/groups/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        groupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sections")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<List<SectionResponse>> getAllSections() {
        return ResponseEntity.ok(groupService.getSectionResponses());
    }

    @PostMapping("/sections")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SectionResponse> createSection(@Valid @RequestBody CreateSectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.createSection(request));
    }

    @PutMapping("/sections/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SectionResponse> updateSection(
            @PathVariable Long id,
            @Valid @RequestBody CreateSectionRequest request) {
        return ResponseEntity.ok(groupService.updateSection(id, request));
    }

    @DeleteMapping("/sections/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSection(@PathVariable Long id) {
        groupService.deleteSection(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sections/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<List<StudentResponse>> getSectionMembers(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.getSectionMembers(id));
    }

    @PostMapping("/sections/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignStudentsToSection(
            @PathVariable Long id,
            @RequestBody AssignStudentsRequest request) {
        groupService.assignStudentsToSection(id, request.getStudentIds());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/sections/{id}/members/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeStudentFromSection(
            @PathVariable Long id,
            @PathVariable String studentId) {
        groupService.removeStudentFromSection(id, studentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sections/{id}/remove-students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeStudentsFromSection(
            @PathVariable Long id,
            @RequestBody List<String> studentIds) {
        groupService.removeStudentsFromSection(id, studentIds);
        return ResponseEntity.ok().build();
    }
}
