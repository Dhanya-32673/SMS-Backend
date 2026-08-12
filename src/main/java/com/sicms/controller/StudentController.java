package com.sicms.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
import org.springframework.web.multipart.MultipartFile;

import com.sicms.dto.CreateStudentRequest;
import com.sicms.dto.PaginatedStudentResponse;
import com.sicms.dto.StudentIdCardResponse;
import com.sicms.dto.StudentResponse;
import com.sicms.dto.StudentSearchResponse;
import com.sicms.dto.UpdateStudentRequest;
import com.sicms.entity.StudentStatus;
import com.sicms.service.StudentPhotoService;
import com.sicms.service.StudentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final StudentPhotoService studentPhotoService;

    @Autowired
    public StudentController(StudentService studentService, StudentPhotoService studentPhotoService) {
        this.studentService = studentService;
        this.studentPhotoService = studentPhotoService;
    }

    /**
     * ADMIN ONLY: Add new student record
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<StudentResponse> createStudent(
            @Valid @RequestBody CreateStudentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String createdByEmail = userDetails != null ? userDetails.getUsername() : null;
        StudentResponse response = studentService.createStudent(request, createdByEmail, isFaculty(userDetails));
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * ADMIN ONLY: Get server-side paginated & filtered list of students
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<PaginatedStudentResponse<com.sicms.dto.StudentSummaryResponse>> getStudents(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer currentYear,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PaginatedStudentResponse<com.sicms.dto.StudentSummaryResponse> response = studentService.getStudents(
                page, size, sortBy, sortDir, department, academicYear, currentYear, section, status, search,
                userDetails != null ? userDetails.getUsername() : null,
                isFaculty(userDetails)
        );
        return ResponseEntity.ok(response);
    }

    /**
     * ADMIN & FACULTY: Search students for search tab / autocomplete
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<List<StudentSearchResponse>> searchStudents(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false, defaultValue = "") String query) {
        List<StudentSearchResponse> results = studentService.searchStudents(query, userDetails != null ? userDetails.getUsername() : null, isFaculty(userDetails));
        return ResponseEntity.ok(results);
    }

    /**
     * ADMIN & FACULTY: Get student detailed profile
     */
    @GetMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable String studentId,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        StudentResponse response = studentService.getStudentByPublicId(studentId, userDetails != null ? userDetails.getUsername() : null, isFaculty(userDetails));
        return ResponseEntity.ok(response);
    }

    /**
     * ADMIN ONLY: Update student information
     */
    @PutMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable String studentId,
            @Valid @RequestBody UpdateStudentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        StudentResponse response = studentService.updateStudent(studentId, request, userDetails != null ? userDetails.getUsername() : null, isFaculty(userDetails));
        return ResponseEntity.ok(response);
    }

    /**
     * ADMIN ONLY: Soft deactivate student record
     */
    @PatchMapping("/{studentId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateStudent(@PathVariable String studentId) {
        studentService.deactivateStudent(studentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * ADMIN ONLY: Upload or change student profile photo
     */
    @PostMapping(value = "/{studentId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<Void> uploadStudentPhoto(
            @PathVariable String studentId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        String publicUrl = studentPhotoService.uploadStudentPhoto(studentId, file);
        studentService.updatePhotoUrl(studentId, publicUrl, userDetails != null ? userDetails.getUsername() : null, isFaculty(userDetails));
        return ResponseEntity.ok().build();
    }

    /**
     * ADMIN & FACULTY: Get data required for Student ID Card & QR Code
     */
    @GetMapping("/{studentId}/id-card")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<StudentIdCardResponse> getStudentIdCardData(@PathVariable String studentId,
                                                                       @AuthenticationPrincipal UserDetails userDetails) {
        StudentIdCardResponse response = studentService.getStudentIdCard(studentId, userDetails != null ? userDetails.getUsername() : null, isFaculty(userDetails));
        return ResponseEntity.ok(response);
    }

    /**
     * ADMIN ONLY: Permanently delete student, certificates from storage, and all records
     */
    @DeleteMapping("/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStudent(@PathVariable String studentId) {
        studentService.deleteStudent(studentId);
        return ResponseEntity.noContent().build();
    }

    private boolean isFaculty(UserDetails userDetails) {
        return userDetails != null && userDetails.getAuthorities().stream().anyMatch(authority -> "ROLE_FACULTY".equalsIgnoreCase(authority.getAuthority()));
    }
}
