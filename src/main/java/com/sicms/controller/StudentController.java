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
import com.sicms.entity.Student;
import com.sicms.entity.StudentStatus;
import com.sicms.exception.StudentNotFoundException;
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
     * ADMIN & FACULTY: Get server-side paginated & filtered list of students
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<PaginatedStudentResponse<com.sicms.dto.StudentSummaryResponse>> getStudents(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String campus,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String branchGroup,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer currentYear,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        String effectiveGroup = (group != null && !group.isBlank()) ? group.trim()
                : ((branchGroup != null && !branchGroup.isBlank()) ? branchGroup.trim()
                : (department != null && !department.isBlank() ? department.trim() : null));

        PaginatedStudentResponse<com.sicms.dto.StudentSummaryResponse> response = studentService.getStudents(
                page, size, sortBy, sortDir, campus, effectiveGroup, academicYear, currentYear, section, status, search,
                userDetails != null ? userDetails.getUsername() : null,
                isFaculty(userDetails)
        );
        return ResponseEntity.ok(response);
    }

    /**
     * ADMIN & FACULTY: Get distinct available groups from database
     */
    @GetMapping("/groups")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<List<String>> getDistinctGroups() {
        return ResponseEntity.ok(studentService.getDistinctGroups());
    }

    /**
     * AUTHENTICATED STUDENT: Get currently logged-in student's own profile
     */
    @GetMapping({"/me", "/profile/me"})
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT') or hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_STUDENT', 'ADMIN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<StudentResponse> getCurrentStudentProfile(@AuthenticationPrincipal Object principal, java.security.Principal fallbackPrincipal) {
        String identifier = null;
        if (principal instanceof com.sicms.security.CustomUserDetails cud) {
            identifier = cud.getEmail();
        } else if (principal instanceof UserDetails ud) {
            identifier = ud.getUsername();
        } else if (principal instanceof String str) {
            identifier = str;
        } else if (fallbackPrincipal != null) {
            identifier = fallbackPrincipal.getName();
        }

        if (identifier == null || identifier.isBlank() || "anonymousUser".equalsIgnoreCase(identifier)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        StudentResponse response = studentService.getStudentByEmailOrUserId(identifier);
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
     * ADMIN & FACULTY: Upload or change student profile photo
     */
    @PostMapping(value = "/{studentId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<java.util.Map<String, String>> uploadStudentPhoto(
            @PathVariable String studentId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        boolean facultyScoped = isFaculty(userDetails);

        Student student = studentService.loadStudentForCurrentUser(studentId, userEmail, facultyScoped)
                .orElseThrow(() -> new StudentNotFoundException("Student with ID '" + studentId + "' not found."));

        String canonicalId = student.getStudentId();
        String publicUrl = studentPhotoService.uploadStudentPhoto(canonicalId, file);
        studentService.updatePhotoUrl(canonicalId, publicUrl, userEmail, facultyScoped);

        return ResponseEntity.ok(java.util.Map.of(
                "photoUrl", publicUrl,
                "studentId", canonicalId,
                "message", "Student photo updated successfully"
        ));
    }

    /**
     * Get student photo bytes directly with fallback & caching
     */
    @GetMapping("/{studentId}/photo")
    public ResponseEntity<byte[]> getStudentPhoto(
            @PathVariable String studentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        StudentResponse student = studentService.getStudentByPublicId(studentId, userDetails != null ? userDetails.getUsername() : null, isFaculty(userDetails));
        if (student == null || student.getProfilePhotoUrl() == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = studentPhotoService.getPhotoBytes(student.getProfilePhotoUrl());
        if (bytes == null || bytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        String contentType = studentPhotoService.getPhotoContentType(student.getProfilePhotoUrl());
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-cache, must-revalidate")
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
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
        System.out.println(">>> [DELETE CONTROLLER] DELETE /api/students/" + studentId);
        studentService.deleteStudent(studentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * ADMIN ONLY: Bulk delete multiple students by IDs safely in one transaction
     */
    @DeleteMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> deleteStudentsBulk(@RequestBody List<String> studentIds) {
        System.out.println(">>> [BULK DELETE CONTROLLER] DELETE /api/students/bulk Payload: " + studentIds);
        if (studentIds == null || studentIds.isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "No student IDs provided for deletion", "count", 0));
        }
        int deletedCount = studentService.deleteStudentsBulk(studentIds);
        System.out.println(">>> [BULK DELETE CONTROLLER] Response deleted count: " + deletedCount);
        return ResponseEntity.ok(java.util.Map.of(
                "message", deletedCount + " student" + (deletedCount != 1 ? "s" : "") + " deleted successfully",
                "count", deletedCount
        ));
    }

    private boolean isFaculty(UserDetails userDetails) {
        return userDetails != null && userDetails.getAuthorities().stream().anyMatch(authority -> "ROLE_FACULTY".equalsIgnoreCase(authority.getAuthority()));
    }
}
