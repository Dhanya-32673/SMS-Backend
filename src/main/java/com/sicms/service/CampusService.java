package com.sicms.service;

import com.sicms.dto.CampusResponse;
import com.sicms.dto.CreateCampusRequest;
import com.sicms.dto.StudentResponse;
import com.sicms.entity.Campus;
import com.sicms.entity.Student;
import com.sicms.exception.ResourceNotFoundException;
import com.sicms.repository.CampusRepository;
import com.sicms.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CampusService {

    public static final List<String> OFFICIAL_CAMPUS_LIST = List.of(
        "TITANIC",
        "SUSRUTHA",
        "DHANVANTARI",
        "GIRLS",
        "VAIDEHI",
        "MEDEX",
        "AIIMS CCO",
        "CCO",
        "ABDUL KALAM",
        "DCO",
        "INDRA BHAVAN",
        "APARNA",
        "VISWAKARMA",
        "VASISTA",
        "GARUDA",
        "GCO",
        "ADITHYA CO",
        "VAARAHI"
    );

    private final CampusRepository campusRepository;
    private final StudentRepository studentRepository;

    public CampusService(CampusRepository campusRepository, StudentRepository studentRepository) {
        this.campusRepository = campusRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public void initOfficialCampuses() {
        for (int i = 0; i < OFFICIAL_CAMPUS_LIST.size(); i++) {
            String campusName = OFFICIAL_CAMPUS_LIST.get(i);
            int displayOrder = i + 1;

            Optional<Campus> existing = campusRepository.findByNameIgnoreCase(campusName);
            if (existing.isPresent()) {
                Campus campus = existing.get();
                if (!campus.getDisplayOrder().equals(displayOrder) || !campus.isActive()) {
                    campus.setDisplayOrder(displayOrder);
                    campus.setActive(true);
                    campusRepository.save(campus);
                }
            } else {
                Campus newCampus = new Campus(campusName, displayOrder);
                campusRepository.save(newCampus);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Campus> getAllOfficialCampuses() {
        initOfficialCampuses();
        List<Campus> activeList = campusRepository.findByActiveTrueOrderByDisplayOrderAsc();
        List<Campus> result = new ArrayList<>();
        for (String officialName : OFFICIAL_CAMPUS_LIST) {
            for (Campus c : activeList) {
                if (c.getName().equalsIgnoreCase(officialName)) {
                    result.add(c);
                    break;
                }
            }
        }
        return result.isEmpty() ? activeList : result;
    }

    @Transactional(readOnly = true)
    public List<CampusResponse> getAllCampusResponses() {
        initOfficialCampuses();
        List<Campus> campuses = campusRepository.findAllByOrderByDisplayOrderAsc();
        
        // Build map of counts grouped by campus name
        Map<String, Long> countMap = new HashMap<>();
        try {
            List<Object[]> grouped = studentRepository.countStudentsGroupedByCampus();
            if (grouped != null) {
                for (Object[] row : grouped) {
                    if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                        String cName = row[0].toString().trim().toLowerCase();
                        long count = ((Number) row[1]).longValue();
                        countMap.put(cName, count);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        List<CampusResponse> responses = new ArrayList<>();
        for (Campus c : campuses) {
            long count = countMap.getOrDefault(c.getName().trim().toLowerCase(), 0L);
            if (count == 0L) {
                // Fallback to explicit query by id or name if grouped query did not pick it up
                count = studentRepository.countStudentsByCampusIdOrName(c.getId(), c.getName());
            }
            responses.add(new CampusResponse(c, count));
        }

        // Maintain display order of OFFICIAL_CAMPUS_LIST if applicable
        responses.sort(Comparator.comparingInt(r -> r.getDisplayOrder() != null ? r.getDisplayOrder() : 999));
        return responses;
    }

    @Transactional(readOnly = true)
    public CampusResponse getCampusResponseById(Long id) {
        Campus campus = campusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campus with ID " + id + " not found."));
        long count = studentRepository.countStudentsByCampusIdOrName(campus.getId(), campus.getName());
        return new CampusResponse(campus, count);
    }

    @Transactional
    public CampusResponse createCampus(CreateCampusRequest request) {
        String cleanName = request.getName().trim();
        if (campusRepository.existsByNameIgnoreCase(cleanName)) {
            throw new IllegalArgumentException("Campus with name '" + cleanName + "' already exists.");
        }

        Campus campus = new Campus();
        campus.setName(cleanName);
        campus.setCode(request.getCode() != null && !request.getCode().isBlank() 
                ? request.getCode().trim().toUpperCase() 
                : cleanName.replaceAll("\\s+", "_").toUpperCase());
        campus.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 99);
        campus.setActive(request.isActive());
        campus.setDescription(request.getDescription());

        Campus saved = campusRepository.save(campus);
        return new CampusResponse(saved, 0);
    }

    @Transactional
    public CampusResponse updateCampus(Long id, CreateCampusRequest request) {
        Campus campus = campusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campus with ID " + id + " not found."));

        String cleanName = request.getName().trim();
        if (campusRepository.existsByNameIgnoreCaseAndIdNot(cleanName, id)) {
            throw new IllegalArgumentException("Another campus with name '" + cleanName + "' already exists.");
        }

        String oldName = campus.getName();
        campus.setName(cleanName);
        if (request.getCode() != null && !request.getCode().isBlank()) {
            campus.setCode(request.getCode().trim().toUpperCase());
        }
        if (request.getDisplayOrder() != null) {
            campus.setDisplayOrder(request.getDisplayOrder());
        }
        campus.setActive(request.isActive());
        campus.setDescription(request.getDescription());

        Campus saved = campusRepository.save(campus);

        // If campus name changed, synchronize student campus string
        if (!oldName.equalsIgnoreCase(cleanName)) {
            List<Student> students = studentRepository.findByCampusIdOrderByIdAsc(id);
            for (Student s : students) {
                s.setCampus(cleanName);
            }
            studentRepository.saveAll(students);
        }

        long count = studentRepository.countStudentsByCampusIdOrName(saved.getId(), saved.getName());
        return new CampusResponse(saved, count);
    }

    @Transactional
    public void deleteCampus(Long id) {
        Campus campus = campusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campus with ID " + id + " not found."));

        long count = studentRepository.countStudentsByCampusIdOrName(campus.getId(), campus.getName());
        if (count > 0) {
            throw new IllegalStateException("Cannot delete Campus '" + campus.getName() + "' because " + count 
                    + " student(s) are currently enrolled. Please reassign or unassign students first.");
        }

        // Safely soft-deactivate to preserve audit trail
        campus.setActive(false);
        campusRepository.save(campus);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getCampusStudents(Long campusId) {
        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() -> new ResourceNotFoundException("Campus with ID " + campusId + " not found."));

        List<Student> students = studentRepository.findStudentsByCampusIdOrName(campus.getId(), campus.getName());
        return students.stream().map(StudentResponse::new).toList();
    }

    @Transactional
    public int assignStudentsToCampus(Long campusId, List<String> studentIdentifiers) {
        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() -> new ResourceNotFoundException("Campus with ID " + campusId + " not found."));

        if (studentIdentifiers == null || studentIdentifiers.isEmpty()) {
            return 0;
        }

        int assignedCount = 0;
        List<Student> studentsToSave = new ArrayList<>();

        for (String identifier : studentIdentifiers) {
            if (identifier == null || identifier.isBlank()) continue;
            String cleanId = identifier.trim();

            Student student = null;
            // 1. Try resolving by numeric database ID
            try {
                Long numericId = Long.parseLong(cleanId);
                student = studentRepository.findById(numericId).orElse(null);
            } catch (NumberFormatException ignored) {
            }

            // 2. Try resolving by business studentId
            if (student == null) {
                student = studentRepository.findByStudentId(cleanId).orElse(null);
            }

            // 3. Fallback to admission number or email
            if (student == null) {
                student = studentRepository.findByEmailOrStudentId(cleanId).orElse(null);
            }

            if (student == null) {
                throw new ResourceNotFoundException("Student with ID '" + cleanId + "' not found.");
            }

            // Check if already assigned to this campus
            boolean isSameCampusId = campus.getId().equals(student.getCampusId());
            boolean isSameCampusName = campus.getName().equalsIgnoreCase(student.getCampus());
            if (isSameCampusId && isSameCampusName) {
                // Already assigned, no change needed
                continue;
            }

            student.setCampus(campus.getName());
            student.setCampusId(campus.getId());
            studentsToSave.add(student);
            assignedCount++;
        }

        if (!studentsToSave.isEmpty()) {
            studentRepository.saveAll(studentsToSave);
        }

        return assignedCount;
    }

    @Transactional
    public void removeStudentFromCampus(Long campusId, String studentIdentifier) {
        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() -> new ResourceNotFoundException("Campus with ID " + campusId + " not found."));

        if (studentIdentifier == null || studentIdentifier.isBlank()) return;
        String cleanId = studentIdentifier.trim();

        Student student = null;
        try {
            Long numericId = Long.parseLong(cleanId);
            student = studentRepository.findById(numericId).orElse(null);
        } catch (NumberFormatException ignored) {
        }

        if (student == null) {
            student = studentRepository.findByStudentId(cleanId).orElse(null);
        }

        if (student == null) {
            student = studentRepository.findByEmailOrStudentId(cleanId).orElse(null);
        }

        if (student == null) {
            throw new ResourceNotFoundException("Student with ID '" + cleanId + "' not found.");
        }

        // Unassign from campus (clearing campus details, keeping student record intact)
        student.setCampus(null);
        student.setCampusId(null);
        studentRepository.save(student);
    }

    @Transactional
    public void removeStudentsFromCampus(Long campusId, List<String> studentIdentifiers) {
        if (studentIdentifiers == null || studentIdentifiers.isEmpty()) return;
        for (String id : studentIdentifiers) {
            removeStudentFromCampus(campusId, id);
        }
    }

    @Transactional(readOnly = true)
    public List<String> getOfficialCampusNames() {
        return OFFICIAL_CAMPUS_LIST;
    }

    public boolean isValidCampus(String campusName) {
        if (campusName == null || campusName.isBlank()) return false;
        String clean = campusName.trim().toUpperCase();
        return OFFICIAL_CAMPUS_LIST.contains(clean);
    }
}
