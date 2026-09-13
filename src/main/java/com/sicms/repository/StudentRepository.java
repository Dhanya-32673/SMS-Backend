package com.sicms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sicms.entity.Student;
import com.sicms.entity.StudentStatus;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findFirstByStudentIdOrderByIdAsc(String studentId);

    default Optional<Student> findByStudentId(String studentId) {
        return findFirstByStudentIdOrderByIdAsc(studentId);
    }

    Optional<Student> findFirstByEmailAddress1IgnoreCaseOrEmailAddress2IgnoreCaseOrStudentIdIgnoreCaseOrAdmissionNumberIgnoreCase(String email1, String email2, String studentId, String admissionNumber);

    default Optional<Student> findByEmailOrStudentId(String identifier) {
        if (identifier == null || identifier.isBlank()) return Optional.empty();
        String clean = identifier.trim();
        return findFirstByEmailAddress1IgnoreCaseOrEmailAddress2IgnoreCaseOrStudentIdIgnoreCaseOrAdmissionNumberIgnoreCase(clean, clean, clean, clean);
    }

    Optional<Student> findByStudentIdIgnoreCase(String studentId);

    Optional<Student> findByAdmissionNumberIgnoreCase(String admissionNumber);

    boolean existsByStudentId(String studentId);

    boolean existsByAdmissionNumberIgnoreCase(String admissionNumber);

    List<Student> findByStatus(StudentStatus status);

    default List<Student> findAllWithAcademicDetail() {
        return findAll();
    }

    // Native query to fetch next val from sequence for safe thread-safe Student ID generation
    @Query(value = "SELECT nextval('student_id_seq')", nativeQuery = true)
    Long getNextStudentIdSequenceValue();

    // Group count by Branch Group for Donut Charts
    @Query("SELECT s.branchGroup AS branchGroup, COUNT(s) AS count FROM Student s GROUP BY s.branchGroup")
    List<Object[]> countStudentsByBranchGroup();

    // Group count by Department / Branch Group for compatibility
    @Query("SELECT s.branchGroup AS department, COUNT(s) AS count FROM Student s GROUP BY s.branchGroup")
    List<Object[]> countStudentsByDepartment();

    // Group count by Intermediate Year for Faculty Charts
    @Query("SELECT s.intermediateYear AS intermediateYear, COUNT(s) AS count FROM Student s GROUP BY s.intermediateYear")
    List<Object[]> countStudentsByCurrentYear();

    @Query(value = "SELECT s FROM Student s WHERE " +
           "(:campus IS NULL OR :campus = '' OR LOWER(TRIM(s.campus)) = LOWER(TRIM(:campus))) AND " +
           "(:department IS NULL OR :department = '' OR LOWER(TRIM(s.branchGroup)) = LOWER(TRIM(:department))) AND " +
           "(:academicYear IS NULL OR :academicYear = '' OR s.academicYear = :academicYear) AND " +
           "(:currentYear IS NULL OR (:currentYear = 1 AND LOWER(s.intermediateYear) LIKE '1%') OR (:currentYear = 2 AND LOWER(s.intermediateYear) LIKE '2%')) AND " +
           "(:section IS NULL OR :section = '' OR LOWER(s.section) = LOWER(:section)) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR (" +
           "   LOWER(s.studentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.emailAddress1) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%'))" +
           "))",
           countQuery = "SELECT COUNT(s) FROM Student s WHERE " +
           "(:campus IS NULL OR :campus = '' OR LOWER(TRIM(s.campus)) = LOWER(TRIM(:campus))) AND " +
           "(:department IS NULL OR :department = '' OR LOWER(TRIM(s.branchGroup)) = LOWER(TRIM(:department))) AND " +
           "(:academicYear IS NULL OR :academicYear = '' OR s.academicYear = :academicYear) AND " +
           "(:currentYear IS NULL OR (:currentYear = 1 AND LOWER(s.intermediateYear) LIKE '1%') OR (:currentYear = 2 AND LOWER(s.intermediateYear) LIKE '2%')) AND " +
           "(:section IS NULL OR :section = '' OR LOWER(s.section) = LOWER(:section)) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR (" +
           "   LOWER(s.studentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.emailAddress1) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%'))" +
           "))")
    Page<Student> filterAndSearchStudents(
            @Param("campus") String campus,
            @Param("department") String department,
            @Param("academicYear") String academicYear,
            @Param("currentYear") Integer currentYear,
            @Param("section") String section,
            @Param("status") StudentStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(value = "SELECT s FROM Student s WHERE " +
           "(EXISTS (SELECT fa.id FROM FacultyAssignment fa WHERE fa.faculty.id = :facultyId AND fa.active = true AND " +
           "LOWER(fa.branchGroup) = LOWER(s.branchGroup) AND LOWER(fa.intermediateYear) = LOWER(s.intermediateYear) AND " +
           "LOWER(fa.section) = LOWER(s.section) AND (fa.academicYear IS NULL OR fa.academicYear = '' OR s.academicYear IS NULL OR s.academicYear = '' OR LOWER(fa.academicYear) = LOWER(s.academicYear))) " +
           "OR (s.createdBy.id = :userId)) AND " +
           "(:department IS NULL OR :department = '' OR LOWER(TRIM(s.branchGroup)) = LOWER(TRIM(:department))) AND " +
           "(:academicYear IS NULL OR :academicYear = '' OR s.academicYear = :academicYear) AND " +
           "(:currentYear IS NULL OR (:currentYear = 1 AND LOWER(s.intermediateYear) LIKE '1%') OR (:currentYear = 2 AND LOWER(s.intermediateYear) LIKE '2%')) AND " +
           "(:section IS NULL OR :section = '' OR LOWER(s.section) = LOWER(:section)) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR (" +
           "   LOWER(s.studentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.emailAddress1) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%'))" +
           "))",
           countQuery = "SELECT COUNT(s) FROM Student s WHERE " +
           "(EXISTS (SELECT fa.id FROM FacultyAssignment fa WHERE fa.faculty.id = :facultyId AND fa.active = true AND " +
           "LOWER(fa.branchGroup) = LOWER(s.branchGroup) AND LOWER(fa.intermediateYear) = LOWER(s.intermediateYear) AND " +
           "LOWER(fa.section) = LOWER(s.section) AND (fa.academicYear IS NULL OR fa.academicYear = '' OR s.academicYear IS NULL OR s.academicYear = '' OR LOWER(fa.academicYear) = LOWER(s.academicYear))) " +
           "OR (s.createdBy.id = :userId)) AND " +
           "(:department IS NULL OR :department = '' OR LOWER(TRIM(s.branchGroup)) = LOWER(TRIM(:department))) AND " +
           "(:academicYear IS NULL OR :academicYear = '' OR s.academicYear = :academicYear) AND " +
           "(:currentYear IS NULL OR (:currentYear = 1 AND LOWER(s.intermediateYear) LIKE '1%') OR (:currentYear = 2 AND LOWER(s.intermediateYear) LIKE '2%')) AND " +
           "(:section IS NULL OR :section = '' OR LOWER(s.section) = LOWER(:section)) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR (" +
           "   LOWER(s.studentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.emailAddress1) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%'))" +
           "))")
    Page<Student> filterAndSearchStudentsForFaculty(
            @Param("facultyId") Long facultyId,
            @Param("userId") Long userId,
            @Param("department") String department,
            @Param("academicYear") String academicYear,
            @Param("currentYear") Integer currentYear,
            @Param("section") String section,
            @Param("status") StudentStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT s FROM Student s WHERE " +
           "LOWER(s.studentId) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.emailAddress1) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.branchGroup) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Student> searchByQuery(@Param("query") String query);

    @Query("SELECT s FROM Student s WHERE " +
           "(EXISTS (SELECT fa.id FROM FacultyAssignment fa WHERE fa.faculty.id = :facultyId AND fa.active = true AND " +
           "LOWER(fa.branchGroup) = LOWER(s.branchGroup) AND LOWER(fa.intermediateYear) = LOWER(s.intermediateYear) AND " +
           "LOWER(fa.section) = LOWER(s.section) AND (fa.academicYear IS NULL OR fa.academicYear = '' OR s.academicYear IS NULL OR s.academicYear = '' OR LOWER(fa.academicYear) = LOWER(s.academicYear))) " +
           "OR (s.createdBy.id = :userId)) AND (" +
           "LOWER(s.studentId) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.emailAddress1) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.branchGroup) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Student> searchByQueryForFaculty(@Param("query") String query, @Param("facultyId") Long facultyId, @Param("userId") Long userId);

    @Query("SELECT s FROM Student s WHERE " +
           "(EXISTS (SELECT fa.id FROM FacultyAssignment fa WHERE fa.faculty.id = :facultyId AND fa.active = true AND " +
           "LOWER(fa.branchGroup) = LOWER(s.branchGroup) AND LOWER(fa.intermediateYear) = LOWER(s.intermediateYear) AND " +
           "LOWER(fa.section) = LOWER(s.section) AND (fa.academicYear IS NULL OR fa.academicYear = '' OR s.academicYear IS NULL OR s.academicYear = '' OR LOWER(fa.academicYear) = LOWER(s.academicYear))) " +
           "OR (s.createdBy.id = :userId))")
    List<Student> findAccessibleStudentsByFaculty(@Param("facultyId") Long facultyId, @Param("userId") Long userId);

    @Query("SELECT s FROM Student s WHERE EXISTS (SELECT fa.id FROM FacultyAssignment fa WHERE fa.faculty.id = :facultyId AND fa.active = true AND " +
           "LOWER(fa.branchGroup) = LOWER(s.branchGroup) AND LOWER(fa.intermediateYear) = LOWER(s.intermediateYear) AND " +
           "LOWER(fa.section) = LOWER(s.section) AND (fa.academicYear IS NULL OR fa.academicYear = '' OR s.academicYear IS NULL OR s.academicYear = '' OR LOWER(fa.academicYear) = LOWER(s.academicYear)))")
    List<Student> findAccessibleStudentsByFaculty(@Param("facultyId") Long facultyId);

    List<Student> findTop5ByOrderByIdDesc();

    @Query("SELECT s FROM Student s WHERE LOWER(s.studentId) = LOWER(:studentId) AND (" +
           "EXISTS (SELECT fa.id FROM FacultyAssignment fa WHERE fa.faculty.id = :facultyId AND fa.active = true AND " +
           "LOWER(fa.branchGroup) = LOWER(s.branchGroup) AND LOWER(fa.intermediateYear) = LOWER(s.intermediateYear) AND " +
           "LOWER(fa.section) = LOWER(s.section) AND (fa.academicYear IS NULL OR fa.academicYear = '' OR s.academicYear IS NULL OR s.academicYear = '' OR LOWER(fa.academicYear) = LOWER(s.academicYear))) " +
           "OR (s.createdBy.id = :userId))")
    Optional<Student> findAccessibleStudentByFaculty(@Param("studentId") String studentId, @Param("facultyId") Long facultyId, @Param("userId") Long userId);

    long countBySectionIgnoreCase(String section);

    long countByBranchGroupIgnoreCase(String branchGroup);

    List<Student> findBySectionIgnoreCase(String section);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.status = com.sicms.entity.StudentStatus.ACTIVE AND " +
           "(:branchGroup IS NULL OR :branchGroup = '' OR LOWER(s.branchGroup) = LOWER(:branchGroup)) AND " +
           "(:intermediateYear IS NULL OR :intermediateYear = '' OR LOWER(s.intermediateYear) = LOWER(:intermediateYear)) AND " +
           "(LOWER(s.section) = LOWER(:sectionName) OR " +
           " LOWER(s.section) = LOWER(CONCAT('Section ', :sectionName)) OR " +
           " LOWER(:sectionName) = LOWER(CONCAT('Section ', s.section)))")
    long countStudentsBySectionDetails(@Param("branchGroup") String branchGroup,
                                        @Param("intermediateYear") String intermediateYear,
                                        @Param("sectionName") String sectionName);

    @Query("SELECT s FROM Student s WHERE s.status = com.sicms.entity.StudentStatus.ACTIVE AND " +
           "(:branchGroup IS NULL OR :branchGroup = '' OR LOWER(s.branchGroup) = LOWER(:branchGroup)) AND " +
           "(:intermediateYear IS NULL OR :intermediateYear = '' OR LOWER(s.intermediateYear) = LOWER(:intermediateYear)) AND " +
           "(LOWER(s.section) = LOWER(:sectionName) OR " +
           " LOWER(s.section) = LOWER(CONCAT('Section ', :sectionName)) OR " +
           " LOWER(:sectionName) = LOWER(CONCAT('Section ', s.section)))")
    List<Student> findStudentsBySectionDetails(@Param("branchGroup") String branchGroup,
                                               @Param("intermediateYear") String intermediateYear,
                                               @Param("sectionName") String sectionName);

    @Query("SELECT LOWER(COALESCE(s.branchGroup, '')), LOWER(COALESCE(s.intermediateYear, '')), LOWER(s.section), COUNT(s) " +
           "FROM Student s " +
           "WHERE s.status = com.sicms.entity.StudentStatus.ACTIVE AND s.section IS NOT NULL " +
           "GROUP BY LOWER(COALESCE(s.branchGroup, '')), LOWER(COALESCE(s.intermediateYear, '')), LOWER(s.section)")
    List<Object[]> countStudentsGroupedBySection();

    @Query("SELECT DISTINCT s FROM Student s " +
           "LEFT JOIN FETCH s.createdBy u " +
           "ORDER BY s.id ASC")
    List<Student> findAllForExcelExport();

    @Query("SELECT DISTINCT s FROM Student s " +
           "LEFT JOIN FETCH s.createdBy u " +
           "WHERE (EXISTS (SELECT fa.id FROM FacultyAssignment fa WHERE fa.faculty.id = :facultyId AND fa.active = true AND " +
           "LOWER(fa.branchGroup) = LOWER(s.branchGroup) AND LOWER(fa.intermediateYear) = LOWER(s.intermediateYear) AND " +
           "LOWER(fa.section) = LOWER(s.section) AND (fa.academicYear IS NULL OR fa.academicYear = '' OR s.academicYear IS NULL OR s.academicYear = '' OR LOWER(fa.academicYear) = LOWER(s.academicYear))) " +
           "OR (s.createdBy.id = :userId)) " +
           "ORDER BY s.id ASC")
    List<Student> findAccessibleStudentsForFacultyExport(@Param("facultyId") Long facultyId, @Param("userId") Long userId);

    @Modifying
    @Query(value = "UPDATE students SET created_by = NULL WHERE created_by = :userId", nativeQuery = true)
    void clearCreatedByForUser(@Param("userId") Long userId);

    @Query("SELECT DISTINCT TRIM(s.branchGroup) FROM Student s WHERE s.branchGroup IS NOT NULL AND TRIM(s.branchGroup) <> '' ORDER BY TRIM(s.branchGroup) ASC")
    List<String> findDistinctBranchGroups();

    List<Student> findByCampusIdOrderByIdAsc(Long campusId);

    List<Student> findByCampusIgnoreCaseOrderByIdAsc(String campus);

    long countByCampusId(Long campusId);

    long countByCampusIgnoreCase(String campus);

    @Query("SELECT s FROM Student s WHERE " +
           "(s.campusId = :campusId OR (:campusName IS NOT NULL AND :campusName <> '' AND LOWER(TRIM(s.campus)) = LOWER(TRIM(:campusName)))) " +
           "ORDER BY s.id ASC")
    List<Student> findStudentsByCampusIdOrName(@Param("campusId") Long campusId, @Param("campusName") String campusName);

    @Query("SELECT COUNT(s) FROM Student s WHERE " +
           "(s.campusId = :campusId OR (:campusName IS NOT NULL AND :campusName <> '' AND LOWER(TRIM(s.campus)) = LOWER(TRIM(:campusName)))) AND " +
           "s.status = com.sicms.entity.StudentStatus.ACTIVE")
    long countStudentsByCampusIdOrName(@Param("campusId") Long campusId, @Param("campusName") String campusName);

    @Query("SELECT LOWER(TRIM(s.campus)), COUNT(s) FROM Student s " +
           "WHERE s.status = com.sicms.entity.StudentStatus.ACTIVE AND s.campus IS NOT NULL AND TRIM(s.campus) <> '' " +
           "GROUP BY LOWER(TRIM(s.campus))")
    List<Object[]> countStudentsGroupedByCampus();
}
