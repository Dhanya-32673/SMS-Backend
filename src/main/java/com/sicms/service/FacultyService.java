package com.sicms.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sicms.dto.FacultyAssignmentRequest;
import com.sicms.dto.FacultyAssignmentResponse;
import com.sicms.dto.FacultyCreateRequest;
import com.sicms.dto.FacultyResponse;
import com.sicms.dto.FacultyUpdateRequest;
import com.sicms.entity.AuthProvider;
import com.sicms.entity.Faculty;
import com.sicms.entity.FacultyAssignment;
import com.sicms.entity.Role;
import com.sicms.entity.Student;
import com.sicms.entity.StudentAcademicDetail;
import com.sicms.entity.User;
import com.sicms.repository.FacultyAssignmentRepository;
import com.sicms.repository.FacultyRepository;
import com.sicms.repository.RoleRepository;
import com.sicms.repository.UserRepository;

@Service
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final FacultyAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentPhotoService photoService;

    public FacultyService(FacultyRepository facultyRepository,
                          FacultyAssignmentRepository assignmentRepository,
                          UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          StudentPhotoService photoService) {
        this.facultyRepository = facultyRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.photoService = photoService;
    }

    @Transactional(readOnly = true)
    public Optional<Faculty> findFacultyByUserEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return userRepository.findByEmailIgnoreCase(email)
                .flatMap(user -> facultyRepository.findByUserId(user.getId()));
    }

    @Transactional(readOnly = true)
    public Faculty getFacultyByUserEmail(String email) {
        return findFacultyByUserEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Faculty record not found for current user"));
    }

    @Transactional(readOnly = true)
    public List<FacultyAssignmentResponse> getCurrentFacultyAssignments(String email) {
        Faculty faculty = getFacultyByUserEmail(email);
        return getFacultyAssignments(faculty.getId());
    }

    @Transactional(readOnly = true)
    public boolean hasAccessToAcademicScope(Faculty faculty, String branchGroup, String intermediateYear, String section, String academicYear) {
        if (faculty == null) {
            return false;
        }

        return assignmentRepository.findActiveByFacultyId(faculty.getId()).stream()
                .anyMatch(assignment -> equalsIgnoreCase(assignment.getBranchGroup(), branchGroup)
                        && equalsIgnoreCase(assignment.getIntermediateYear(), intermediateYear)
                        && equalsIgnoreCase(assignment.getSection(), section)
                        && (academicYear == null || academicYear.isBlank() || assignment.getAcademicYear() == null || equalsIgnoreCase(assignment.getAcademicYear(), academicYear)));
    }

    @Transactional(readOnly = true)
    public boolean hasAccessToStudent(Faculty faculty, Student student) {
        if (faculty == null || student == null) {
            return false;
        }

        // 1. Check if student was created by this faculty user
        if (faculty.getUser() != null && student.getCreatedBy() != null
                && faculty.getUser().getId().equals(student.getCreatedBy().getId())) {
            return true;
        }

        // 2. Check if student belongs to faculty's assigned sections
        if (student.getAcademicDetail() == null) {
            return false;
        }

        StudentAcademicDetail academicDetail = student.getAcademicDetail();
        return hasAccessToAcademicScope(
                faculty,
                academicDetail.getBranchGroup(),
                academicDetail.getIntermediateYear(),
                academicDetail.getSection(),
                academicDetail.getAcademicYear()
        );
    }

    @Transactional(readOnly = true)
    public List<String> getFacultyAssignedSectionFormattedNames(Long facultyId) {
        if (facultyId == null) {
            return List.of();
        }

        return assignmentRepository.findActiveByFacultyId(facultyId).stream()
                .map(a -> {
                    String group = a.getBranchGroup() != null ? a.getBranchGroup().trim() : "General";
                    String sec = a.getSection() != null ? a.getSection().replace("Section ", "").trim() : "A";
                    return group + "-" + sec;
                })
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @CacheEvict(value = {"faculty", "adminDashboard", "sections", "sectionsResponses"}, allEntries = true)
    @Transactional
    public FacultyResponse createFaculty(FacultyCreateRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists in user accounts");
        }

        Role facultyRole = roleRepository.findByRoleName("ROLE_FACULTY")
                .orElseThrow(() -> new RuntimeException("Role ROLE_FACULTY not found"));

        // Create User Login Account
        User user = new User();
        user.setFullName(request.getFirstName() + " " + (request.getMiddleName() != null ? request.getMiddleName() + " " : "") + request.getLastName());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(facultyRole);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEmailVerified(true);
        user.setAccountEnabled(true);
        user = userRepository.save(user);

        // Generate unique Faculty ID (e.g. FAC10001)
        String facultyId = "FAC" + System.currentTimeMillis() % 100000;

        Faculty faculty = new Faculty();
        faculty.setFacultyId(facultyId);
        faculty.setUser(user);
        faculty.setEmployeeId(request.getEmployeeId() != null ? request.getEmployeeId() : "EMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        faculty.setFirstName(request.getFirstName());
        faculty.setMiddleName(request.getMiddleName());
        faculty.setLastName(request.getLastName());
        faculty.setGender(request.getGender());
        faculty.setDateOfBirth(request.getDateOfBirth());
        faculty.setPhotoUrl(request.getPhotoUrl());
        faculty.setMobileNumber(request.getMobileNumber());
        faculty.setAlternateMobile(request.getAlternateMobile());
        faculty.setEmail(request.getEmail());
        faculty.setAddress(request.getAddress());
        faculty.setCity(request.getCity());
        faculty.setDistrict(request.getDistrict());
        faculty.setState(request.getState());
        faculty.setPinCode(request.getPinCode());
        faculty.setDesignation(request.getDesignation());
        faculty.setQualification(request.getQualification());
        faculty.setDepartment(request.getDepartment());
        faculty.setPrimaryGroup(request.getPrimaryGroup());
        faculty.setJoiningDate(request.getJoiningDate());
        faculty.setEmploymentType(request.getEmploymentType());
        faculty.setExperience(request.getExperience());
        faculty.setStatus("ACTIVE");

        faculty = facultyRepository.save(faculty);
        return mapToResponse(faculty);
    }

    @Cacheable(value = "faculty", key = "{#query, #group, #status, #pageable.pageNumber, #pageable.pageSize}")
    @Transactional(readOnly = true)
    public Page<FacultyResponse> searchFaculty(String query, String group, String status, Pageable pageable) {
        return facultyRepository.searchFaculty(query, group, status, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public FacultyResponse getFacultyById(Long id) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + id));
        return mapToResponse(faculty);
    }

    @Transactional(readOnly = true)
    public FacultyResponse getFacultyByUserId(Long userId) {
        Faculty faculty = facultyRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Faculty record not found for User ID: " + userId));
        return mapToResponse(faculty);
    }

    @Transactional
    public FacultyResponse updateFaculty(Long id, FacultyUpdateRequest request) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + id));

        faculty.setFirstName(request.getFirstName());
        faculty.setMiddleName(request.getMiddleName());
        faculty.setLastName(request.getLastName());
        faculty.setGender(request.getGender());
        faculty.setDateOfBirth(request.getDateOfBirth());
        if (request.getPhotoUrl() != null) faculty.setPhotoUrl(request.getPhotoUrl());
        faculty.setMobileNumber(request.getMobileNumber());
        faculty.setAlternateMobile(request.getAlternateMobile());
        faculty.setAddress(request.getAddress());
        faculty.setCity(request.getCity());
        faculty.setDistrict(request.getDistrict());
        faculty.setState(request.getState());
        faculty.setPinCode(request.getPinCode());
        faculty.setDesignation(request.getDesignation());
        faculty.setQualification(request.getQualification());
        faculty.setDepartment(request.getDepartment());
        faculty.setPrimaryGroup(request.getPrimaryGroup());
        faculty.setJoiningDate(request.getJoiningDate());
        faculty.setEmploymentType(request.getEmploymentType());
        faculty.setExperience(request.getExperience());
        if (request.getStatus() != null) faculty.setStatus(request.getStatus());

        User user = faculty.getUser();
        if (user != null) {
            user.setFullName(faculty.getFullName());
            userRepository.save(user);
        }

        faculty = facultyRepository.save(faculty);
        return mapToResponse(faculty);
    }

    @Transactional
    public void updateFacultyPhoto(Long id, String photoUrl) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + id));
        faculty.setPhotoUrl(photoUrl);
        facultyRepository.save(faculty);
    }

    @Transactional
    public FacultyAssignmentResponse addAssignment(Long facultyId, FacultyAssignmentRequest request) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + facultyId));

        // Check if section is already assigned to another faculty member
        List<FacultyAssignment> activeAssignments = assignmentRepository.findActiveAssignments(
                request.getBranchGroup(), request.getIntermediateYear(), request.getSection());

        String requestedSecClean = request.getSection() != null ? request.getSection().trim().toLowerCase().replace("section ", "") : "";

        for (FacultyAssignment fa : activeAssignments) {
            if (fa.isActive() && fa.getSection() != null) {
                String faSecClean = fa.getSection().trim().toLowerCase().replace("section ", "");
                if (faSecClean.equals(requestedSecClean)) {
                    Faculty assignedFaculty = fa.getFaculty();
                    String assignedName = assignedFaculty != null ? assignedFaculty.getFullName() : "another faculty";
                    if (assignedFaculty != null && assignedFaculty.getId().equals(facultyId)) {
                        throw new IllegalArgumentException("Section '" + request.getSection() + "' (" + request.getBranchGroup() + " " + request.getIntermediateYear() + ") is already assigned to this faculty member.");
                    } else {
                        throw new IllegalArgumentException("Section '" + request.getSection() + "' (" + request.getBranchGroup() + " " + request.getIntermediateYear() + ") is already assigned to " + assignedName + ".");
                    }
                }
            }
        }

        FacultyAssignment assignment = new FacultyAssignment();
        assignment.setFaculty(faculty);
        assignment.setBranchGroup(request.getBranchGroup());
        assignment.setIntermediateYear(request.getIntermediateYear());
        assignment.setSection(request.getSection());
        assignment.setAcademicYear(request.getAcademicYear());
        assignment.setSubjectName(request.getSubjectName());
        assignment.setActive(true);

        assignment = assignmentRepository.save(assignment);
        return mapToAssignmentResponse(assignment);
    }

    @Transactional
    @CacheEvict(value = {"faculty", "adminDashboard", "sections"}, allEntries = true)
    public void removeAssignment(Long facultyId, Long assignmentId) {
        FacultyAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + assignmentId));

        if (!assignment.getFaculty().getId().equals(facultyId)) {
            throw new IllegalArgumentException("Assignment does not belong to faculty ID: " + facultyId);
        }

        assignmentRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    public List<FacultyAssignmentResponse> getFacultyAssignments(Long facultyId) {
        return assignmentRepository.findByFacultyId(facultyId).stream()
                .map(this::mapToAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void toggleFacultyStatus(Long id, String status) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + id));
        faculty.setStatus(status);

        User user = faculty.getUser();
        if (user != null) {
            user.setAccountEnabled("ACTIVE".equalsIgnoreCase(status));
            userRepository.save(user);
        }

        facultyRepository.save(faculty);
    }

    @Transactional
    public void deleteFaculty(Long id) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + id));

        // 1. Delete all assignments
        List<FacultyAssignment> assignments = assignmentRepository.findByFacultyId(faculty.getId());
        if (!assignments.isEmpty()) {
            assignmentRepository.deleteAll(assignments);
        }

        // 2. Delete photo from storage if present
        if (faculty.getPhotoUrl() != null && !faculty.getPhotoUrl().isBlank()) {
            try {
                photoService.deletePhotoFile(faculty.getPhotoUrl());
            } catch (Exception e) {
                System.err.println(">>> FACULTY PHOTO DELETE NOTICE: " + e.getMessage());
            }
        }

        User user = faculty.getUser();

        // 3. Delete faculty entity
        facultyRepository.delete(faculty);

        // 4. Delete user account
        if (user != null) {
            userRepository.delete(user);
        }
    }

    private FacultyResponse mapToResponse(Faculty faculty) {
        FacultyResponse res = new FacultyResponse();
        res.setId(faculty.getId());
        res.setFacultyId(faculty.getFacultyId());
        res.setUserId(faculty.getUser() != null ? faculty.getUser().getId() : null);
        res.setEmployeeId(faculty.getEmployeeId());
        res.setFirstName(faculty.getFirstName());
        res.setMiddleName(faculty.getMiddleName());
        res.setLastName(faculty.getLastName());
        res.setFullName(faculty.getFullName());
        res.setGender(faculty.getGender());
        res.setDateOfBirth(faculty.getDateOfBirth());
        res.setPhotoUrl(faculty.getPhotoUrl());
        res.setMobileNumber(faculty.getMobileNumber());
        res.setAlternateMobile(faculty.getAlternateMobile());
        res.setEmail(faculty.getEmail());
        res.setAddress(faculty.getAddress());
        res.setCity(faculty.getCity());
        res.setDistrict(faculty.getDistrict());
        res.setState(faculty.getState());
        res.setPinCode(faculty.getPinCode());
        res.setDesignation(faculty.getDesignation());
        res.setQualification(faculty.getQualification());
        res.setDepartment(faculty.getDepartment());
        res.setPrimaryGroup(faculty.getPrimaryGroup());
        res.setJoiningDate(faculty.getJoiningDate());
        res.setEmploymentType(faculty.getEmploymentType());
        res.setExperience(faculty.getExperience());
        res.setStatus(faculty.getStatus());
        res.setCreatedAt(faculty.getCreatedAt());
        res.setUpdatedAt(faculty.getUpdatedAt());

        List<FacultyAssignmentResponse> assignments = assignmentRepository.findByFacultyId(faculty.getId())
                .stream()
                .map(this::mapToAssignmentResponse)
                .collect(Collectors.toList());
        res.setAssignments(assignments);

        return res;
    }

    private FacultyAssignmentResponse mapToAssignmentResponse(FacultyAssignment fa) {
        FacultyAssignmentResponse res = new FacultyAssignmentResponse();
        res.setId(fa.getId());
        res.setFacultyId(fa.getFaculty().getId());
        res.setFacultyName(fa.getFaculty().getFullName());
        res.setBranchGroup(fa.getBranchGroup());
        res.setIntermediateYear(fa.getIntermediateYear());
        res.setSection(fa.getSection());
        res.setAcademicYear(fa.getAcademicYear());
        res.setSubjectName(fa.getSubjectName());
        res.setActive(fa.isActive());
        res.setCreatedAt(fa.getCreatedAt());
        return res;
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().toLowerCase(Locale.ROOT).equals(right.trim().toLowerCase(Locale.ROOT));
    }
}
