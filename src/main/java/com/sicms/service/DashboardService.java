package com.sicms.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sicms.dto.AdminDashboardSummaryResponse;
import com.sicms.dto.FacultyDashboardSummaryResponse;
import com.sicms.dto.StudentSummaryResponse;
import com.sicms.entity.DocumentStatus;
import com.sicms.entity.Faculty;
import com.sicms.entity.Student;
import com.sicms.entity.StudentDocument;
import com.sicms.repository.FacultyRepository;
import com.sicms.repository.StudentDocumentRepository;
import com.sicms.repository.StudentRepository;
import com.sicms.repository.UserRepository;

@Service
public class DashboardService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final FacultyRepository facultyRepository;
    private final StudentDocumentRepository documentRepository;
    private final DocumentService documentService;

    @Autowired
    public DashboardService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            FacultyRepository facultyRepository,
            StudentDocumentRepository documentRepository,
            DocumentService documentService) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.facultyRepository = facultyRepository;
        this.documentRepository = documentRepository;
        this.documentService = documentService;
    }

        @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse getAdminSummary() {
        AdminDashboardSummaryResponse summary = new AdminDashboardSummaryResponse();

        long totalStudents = studentRepository.count();
        long totalFaculty = facultyRepository.count();

        summary.setTotalStudents(totalStudents);
        summary.setTotalFaculty(totalFaculty);
        summary.setNewAdmissions(totalStudents);

        // Real Document Stats
        long totalCertificates = documentRepository.count();
        long uploadedCertificates = documentRepository.countByStatus(DocumentStatus.UPLOADED);
        long pendingVerification = documentRepository.countByStatus(DocumentStatus.PENDING);
        long verifiedDocuments = documentRepository.countByStatus(DocumentStatus.VERIFIED);
        long rejectedDocuments = documentRepository.countByStatus(DocumentStatus.REJECTED);
        long missingDocsCount = documentService.countMissingDocuments(null, false);

        summary.setTotalCertificates(totalCertificates);
        summary.setUploadedCertificates(uploadedCertificates);
        summary.setPendingVerification(pendingVerification);
        summary.setVerifiedDocuments(verifiedDocuments);
        summary.setRejectedDocuments(rejectedDocuments);
        summary.setMissingDocuments(missingDocsCount);

        double rate = totalCertificates > 0 ? (double) verifiedDocuments / totalCertificates * 100.0 : 0.0;
        summary.setCertificateCompletionRate(Math.round(rate * 10.0) / 10.0);

        Map<String, Long> statusDist = new LinkedHashMap<>();
        statusDist.put("Uploaded", uploadedCertificates);
        statusDist.put("Pending", pendingVerification);
        statusDist.put("Verified", verifiedDocuments);
        statusDist.put("Rejected", rejectedDocuments);
        statusDist.put("Missing", missingDocsCount);
        summary.setCertificateStatusDistribution(statusDist);

        // Intermediate Branch Group Distribution
        Map<String, Long> groupMap = new LinkedHashMap<>();
        groupMap.put("MPC", 0L);
        groupMap.put("BiPC", 0L);
        groupMap.put("MEC", 0L);
        groupMap.put("CEC", 0L);
        groupMap.put("HEC", 0L);

        List<Object[]> groupCounts = studentRepository.countStudentsByBranchGroup();
        for (Object[] row : groupCounts) {
            String grp = (String) row[0];
            Long count = (Long) row[1];
            if (grp != null) {
                groupMap.put(grp.toUpperCase(), count);
            }
        }
        summary.setStudentsByDepartment(groupMap);

        // Monthly registration trend
        List<Map<String, Object>> monthlyTrend = new ArrayList<>();
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (int i = 0; i < months.length; i++) {
            Map<String, Object> m = new HashMap<>();
            m.put("month", months[i]);
            m.put("registrations", (i == 6 ? totalStudents : 0));
            monthlyTrend.add(m);
        }
        summary.setMonthlyRegistrations(monthlyTrend);

        // 5 Most recent students
        List<Student> recentStudents = studentRepository.findTop5ByOrderByIdDesc();
        List<StudentSummaryResponse> recentList = recentStudents.stream()
                .map(StudentSummaryResponse::new)
                .toList();
        summary.setRecentStudents(recentList);

        return summary;
    }

        @Transactional(readOnly = true)
    public FacultyDashboardSummaryResponse getFacultySummaryForUser(String currentUserEmail) {
        FacultyDashboardSummaryResponse summary = new FacultyDashboardSummaryResponse();

        Faculty faculty = facultyRepository.findByUserId(
                userRepository.findByEmailIgnoreCase(currentUserEmail)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Faculty record not found for current user"))
                        .getId()
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Faculty record not found for current user"));

        List<Student> accessibleStudents = studentRepository.findAccessibleStudentsByFaculty(faculty.getId());
        long totalStudents = accessibleStudents.size();
        summary.setAssignedStudentsCount(totalStudents);

        List<StudentDocument> facultyDocs = documentRepository.findDocumentsForFacultyScope(faculty.getId());
        long totalDocuments = 0;
        long pendingVerification = 0;
        long verifiedCertificates = 0;

        for (StudentDocument doc : facultyDocs) {
            if (doc.getStatus() != DocumentStatus.ARCHIVED) {
                totalDocuments++;
                if (doc.getStatus() == DocumentStatus.PENDING || doc.getStatus() == DocumentStatus.UPLOADED) {
                    pendingVerification++;
                } else if (doc.getStatus() == DocumentStatus.VERIFIED) {
                    verifiedCertificates++;
                }
            }
        }

        long missingDocsCount = documentService.countMissingDocuments(currentUserEmail, true);

        summary.setTotalDocumentsCount(totalDocuments);
        summary.setPendingDocumentsCount(pendingVerification);
        summary.setMissingDocuments(missingDocsCount);

        double rate = totalDocuments > 0 ? (double) verifiedCertificates / totalDocuments * 100.0 : 0.0;
        summary.setCertificateCompletionRate(Math.round(rate * 10.0) / 10.0);

        // Year distribution count
        Map<String, Long> yearMap = new LinkedHashMap<>();
        yearMap.put("1st Year", 0L);
        yearMap.put("2nd Year", 0L);

        Map<String, Long> filteredYearCounts = new LinkedHashMap<>();
        for (Student student : accessibleStudents) {
            String year = student.getAcademicDetail() != null ? student.getAcademicDetail().getIntermediateYear() : null;
            if (year != null) {
                filteredYearCounts.put(year, filteredYearCounts.getOrDefault(year, 0L) + 1);
            }
        }
        for (Map.Entry<String, Long> entry : filteredYearCounts.entrySet()) {
            yearMap.put(entry.getKey(), entry.getValue());
        }
        summary.setStudentsByYear(yearMap);

        // Student lists
        List<StudentSummaryResponse> studentList = accessibleStudents.stream()
                .sorted(Comparator.comparing(Student::getId, Comparator.nullsLast(Long::compareTo)).reversed())
                .map(StudentSummaryResponse::new)
                .toList();

        summary.setAssignedStudents(studentList);
        summary.setRecentStudents(studentList.stream().limit(5).toList());

        return summary;
    }
}
