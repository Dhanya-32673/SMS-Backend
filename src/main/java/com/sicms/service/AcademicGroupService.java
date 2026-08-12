package com.sicms.service;

import com.sicms.dto.CreateSectionRequest;
import com.sicms.dto.SectionResponse;
import com.sicms.dto.StudentResponse;
import com.sicms.entity.AcademicGroup;
import com.sicms.entity.AcademicSection;
import com.sicms.entity.Student;
import com.sicms.entity.StudentAcademicDetail;
import com.sicms.exception.StudentNotFoundException;
import com.sicms.entity.FacultyAssignment;
import com.sicms.repository.AcademicGroupRepository;
import com.sicms.repository.AcademicSectionRepository;
import com.sicms.repository.FacultyAssignmentRepository;
import com.sicms.repository.StudentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AcademicGroupService {

    private final AcademicGroupRepository groupRepository;
    private final AcademicSectionRepository sectionRepository;
    private final StudentRepository studentRepository;
    private final FacultyAssignmentRepository assignmentRepository;

    public AcademicGroupService(
            AcademicGroupRepository groupRepository,
            AcademicSectionRepository sectionRepository,
            StudentRepository studentRepository,
            FacultyAssignmentRepository assignmentRepository
    ) {
        this.groupRepository = groupRepository;
        this.sectionRepository = sectionRepository;
        this.studentRepository = studentRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public List<AcademicGroup> getAllGroups() {
        return groupRepository.findAll();
    }

    @Transactional
    public AcademicGroup createGroup(AcademicGroup group) {
        if (groupRepository.existsByCode(group.getCode())) {
            throw new IllegalArgumentException("Group code " + group.getCode() + " already exists");
        }
        return groupRepository.save(group);
    }

    @Transactional
    public void deleteGroup(Long id) {
        AcademicGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Academic Group with ID " + id + " not found."));

        long studentCount = studentRepository.countByAcademicDetail_BranchGroupIgnoreCase(group.getCode());
        if (studentCount > 0) {
            throw new IllegalArgumentException("Cannot delete Group '" + group.getCode() + "' because " + studentCount + " student(s) are assigned to it.");
        }

        groupRepository.delete(group);
    }

    @Cacheable(value = "sections")
    @Transactional(readOnly = true)
    public List<AcademicSection> getAllSections() {
        return sectionRepository.findAll();
    }

    @Cacheable(value = "sectionsResponses")
    @Transactional(readOnly = true)
    public List<SectionResponse> getSectionResponses() {
        List<AcademicSection> sections = sectionRepository.findAll();
        List<FacultyAssignment> activeAssignments = assignmentRepository.findAll().stream()
                .filter(FacultyAssignment::isActive)
                .collect(Collectors.toList());

        Map<String, Long> countMap = new HashMap<>();
        try {
            List<Object[]> rows = studentRepository.countStudentsGroupedBySection();
            for (Object[] row : rows) {
                String grp = row[0] != null ? row[0].toString() : "";
                String yr = row[1] != null ? row[1].toString() : "";
                String sec = row[2] != null ? row[2].toString() : "";
                Long count = (Long) row[3];
                countMap.put(buildSectionKey(grp, yr, sec), count);
            }
        } catch (Exception ignored) {}

        return sections.stream()
                .map(sec -> mapToSectionResponse(sec, activeAssignments, countMap))
                .collect(Collectors.toList());
    }

    @CacheEvict(value = {"sections", "sectionsResponses"}, allEntries = true)
    @Transactional
    public SectionResponse createSection(CreateSectionRequest request) {
        if (sectionRepository.existsByNameIgnoreCaseAndAcademicYear(request.getName(), request.getAcademicYear())) {
            throw new IllegalArgumentException("Section '" + request.getName() + "' for Academic Year '" + request.getAcademicYear() + "' already exists.");
        }

        AcademicSection section = new AcademicSection();
        section.setName(request.getName().trim());
        section.setAcademicYear(request.getAcademicYear().trim());
        section.setBranchGroup(request.getBranchGroup() != null ? request.getBranchGroup() : "MPC");
        section.setIntermediateYear(request.getIntermediateYear() != null ? request.getIntermediateYear() : "1st Year");
        section.setCapacity(request.getCapacity() != null ? request.getCapacity() : 60);
        section.setDescription(request.getDescription());
        section.setActive(request.isActive());

        AcademicSection saved = sectionRepository.save(section);
        return mapToSectionResponse(saved);
    }

    @CacheEvict(value = {"sections", "sectionsResponses"}, allEntries = true)
    @Transactional
    public SectionResponse updateSection(Long id, CreateSectionRequest request) {
        AcademicSection section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section with ID " + id + " not found."));

        section.setName(request.getName().trim());
        section.setAcademicYear(request.getAcademicYear().trim());
        if (request.getBranchGroup() != null) section.setBranchGroup(request.getBranchGroup());
        if (request.getIntermediateYear() != null) section.setIntermediateYear(request.getIntermediateYear());
        if (request.getCapacity() != null) section.setCapacity(request.getCapacity());
        section.setDescription(request.getDescription());
        section.setActive(request.isActive());

        AcademicSection saved = sectionRepository.save(section);
        return mapToSectionResponse(saved);
    }

    @CacheEvict(value = {"sections", "sectionsResponses"}, allEntries = true)
    @Transactional
    public void deleteSection(Long id) {
        AcademicSection section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section with ID " + id + " not found."));

        long count = studentRepository.countStudentsBySectionDetails(section.getBranchGroup(), section.getIntermediateYear(), section.getName());
        if (count > 0) {
            throw new IllegalArgumentException("This section contains students. Move or remove all students before deleting the section.");
        }

        sectionRepository.delete(section);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getSectionMembers(Long sectionId) {
        AcademicSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section with ID " + sectionId + " not found."));

        List<Student> students = studentRepository.findStudentsBySectionDetails(section.getBranchGroup(), section.getIntermediateYear(), section.getName());
        return students.stream().map(StudentResponse::new).collect(Collectors.toList());
    }

    @CacheEvict(value = {"sections", "sectionsResponses", "adminDashboard"}, allEntries = true)
    @Transactional
    public void assignStudentsToSection(Long sectionId, List<String> studentIds) {
        AcademicSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section with ID " + sectionId + " not found."));

        if (studentIds == null || studentIds.isEmpty()) return;

        for (String studentId : studentIds) {
            studentRepository.findByStudentId(studentId).ifPresent(student -> {
                StudentAcademicDetail academic = student.getAcademicDetail();
                if (academic != null) {
                    academic.setSection(section.getName());
                    if (section.getBranchGroup() != null) academic.setBranchGroup(section.getBranchGroup());
                    if (section.getIntermediateYear() != null) academic.setIntermediateYear(section.getIntermediateYear());
                    studentRepository.save(student);
                }
            });
        }
    }

    @CacheEvict(value = {"sections", "sectionsResponses", "adminDashboard"}, allEntries = true)
    @Transactional
    public void removeStudentFromSection(Long sectionId, String studentId) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new IllegalArgumentException("Section with ID " + sectionId + " not found.");
        }

        Student student = studentRepository.findByStudentId(studentId).orElse(null);
        if (student == null) {
            try {
                student = studentRepository.findById(Long.parseLong(studentId)).orElse(null);
            } catch (NumberFormatException ignored) {}
        }

        if (student == null) {
            throw new StudentNotFoundException("Student with ID " + studentId + " not found.");
        }

        StudentAcademicDetail academic = student.getAcademicDetail();
        if (academic != null) {
            academic.setSection("Unassigned");
            studentRepository.save(student);
        }
    }

    @CacheEvict(value = {"sections", "sectionsResponses", "adminDashboard"}, allEntries = true)
    @Transactional
    public void removeStudentsFromSection(Long sectionId, List<String> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) return;
        for (String studentId : studentIds) {
            removeStudentFromSection(sectionId, studentId);
        }
    }

    private SectionResponse mapToSectionResponse(AcademicSection section) {
        List<FacultyAssignment> activeAssignments = assignmentRepository.findAll().stream()
                .filter(FacultyAssignment::isActive)
                .collect(Collectors.toList());
        return mapToSectionResponse(section, activeAssignments, null);
    }

    private SectionResponse mapToSectionResponse(AcademicSection section, List<FacultyAssignment> activeAssignments, Map<String, Long> countMap) {
        SectionResponse dto = new SectionResponse();
        dto.setId(section.getId());
        dto.setName(section.getName());
        dto.setBranchGroup(section.getBranchGroup());
        dto.setIntermediateYear(section.getIntermediateYear());
        dto.setAcademicYear(section.getAcademicYear());
        dto.setCapacity(section.getCapacity());
        dto.setDescription(section.getDescription());
        dto.setActive(section.isActive());
        dto.setCreatedAt(section.getCreatedAt());
        dto.setUpdatedAt(section.getUpdatedAt());

        long count = 0;
        if (section.getName() != null) {
            String secKey = buildSectionKey(section.getBranchGroup(), section.getIntermediateYear(), section.getName());
            if (countMap != null && countMap.containsKey(secKey)) {
                count = countMap.get(secKey);
            } else {
                try {
                    count = studentRepository.countStudentsBySectionDetails(section.getBranchGroup(), section.getIntermediateYear(), section.getName());
                } catch (Exception e) {
                    count = 0;
                }
            }
        }
        dto.setTotalStudents(count);
        dto.setStudentCount(count);

        if (activeAssignments != null) {
            FacultyAssignment matched = activeAssignments.stream()
                    .filter(fa -> matchesSectionAssignment(fa, section))
                    .findFirst()
                    .orElse(null);

            if (matched != null && matched.getFaculty() != null) {
                dto.setAssignedFacultyId(matched.getFaculty().getId());
                String fullName = matched.getFaculty().getFullName();
                if (fullName == null || fullName.isBlank()) {
                    if (matched.getFaculty().getUser() != null) {
                        fullName = matched.getFaculty().getUser().getFullName();
                    }
                }
                dto.setAssignedFacultyName(fullName != null && !fullName.isBlank() ? fullName : "Not Assigned");
            } else {
                dto.setAssignedFacultyName("Not Assigned");
            }
        } else {
            dto.setAssignedFacultyName("Not Assigned");
        }

        return dto;
    }

    private boolean matchesSectionAssignment(FacultyAssignment fa, AcademicSection sec) {
        if (fa == null || !fa.isActive() || sec == null) return false;

        boolean groupMatch = fa.getBranchGroup() != null && sec.getBranchGroup() != null &&
                fa.getBranchGroup().trim().equalsIgnoreCase(sec.getBranchGroup().trim());
        if (!groupMatch) return false;

        boolean yearMatch = fa.getIntermediateYear() != null && sec.getIntermediateYear() != null &&
                fa.getIntermediateYear().trim().equalsIgnoreCase(sec.getIntermediateYear().trim());
        if (!yearMatch) return false;

        if (fa.getSection() == null || sec.getName() == null) return false;

        String faSec = fa.getSection().trim().toLowerCase().replace("section ", "");
        String secName = sec.getName().trim().toLowerCase().replace("section ", "");

        return faSec.equals(secName);
    }

    private String buildSectionKey(String group, String year, String section) {
        String g = group == null ? "" : group.trim().toLowerCase();
        String y = year == null ? "" : year.trim().toLowerCase();
        String s = section == null ? "" : section.trim().toLowerCase().replace("section ", "");
        return g + ":" + y + ":" + s;
    }
}
