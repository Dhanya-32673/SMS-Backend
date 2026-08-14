package com.sicms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    Optional<Student> findByRollNumberIgnoreCase(String rollNumber);

    boolean existsByStudentId(String studentId);

    boolean existsByRollNumberIgnoreCase(String rollNumber);

    boolean existsByAdmissionNumberIgnoreCase(String admissionNumber);

    // Native query to fetch next val from sequence for safe thread-safe Student ID generation
    @Query(value = "SELECT nextval('student_id_seq')", nativeQuery = true)
    Long getNextStudentIdSequenceValue();

    // Group count by Branch Group for Donut Charts
    @Query("SELECT a.branchGroup AS branchGroup, COUNT(s) AS count FROM Student s JOIN s.academicDetail a GROUP BY a.branchGroup")
    List<Object[]> countStudentsByBranchGroup();

    // Group count by Department for Donut Charts
    @Query("SELECT a.department AS department, COUNT(s) AS count FROM Student s JOIN s.academicDetail a GROUP BY a.department")
    List<Object[]> countStudentsByDepartment();

    // Group count by Intermediate Year for Faculty Charts
    @Query("SELECT a.intermediateYear AS intermediateYear, COUNT(s) AS count FROM Student s JOIN s.academicDetail a GROUP BY a.intermediateYear")
    List<Object[]> countStudentsByCurrentYear();

    @Query(value = "SELECT s FROM Student s JOIN FETCH s.academicDetail a LEFT JOIN FETCH s.contactDetail c WHERE " +
           "(:department IS NULL OR :department = '' OR LOWER(a.department) = LOWER(:department) OR LOWER(a.branchGroup) = LOWER(:department)) AND " +
           "(:academicYear IS NULL OR :academicYear = '' OR a.academicYear = :academicYear) AND " +
           "(:currentYear IS NULL OR a.semester = :currentYear) AND " +
           "(:section IS NULL OR :section = '' OR LOWER(a.section) = LOWER(:section)) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR (" +
           "   LOWER(s.studentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))" +
           "))",
           countQuery = "SELECT COUNT(s) FROM Student s JOIN s.academicDetail a LEFT JOIN s.contactDetail c WHERE " +
           "(:department IS NULL OR :department = '' OR LOWER(a.department) = LOWER(:department) OR LOWER(a.branchGroup) = LOWER(:department)) AND " +
           "(:academicYear IS NULL OR :academicYear = '' OR a.academicYear = :academicYear) AND " +
           "(:currentYear IS NULL OR a.semester = :currentYear) AND " +
           "(:section IS NULL OR :section = '' OR LOWER(a.section) = LOWER(:section)) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR (" +
           "   LOWER(s.studentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))" +
           "))")
    Page<Student> filterAndSearchStudents(
            @Param("department") String department,
            @Param("academicYear") String academicYear,
            @Param("currentYear") Integer currentYear,
            @Param("section") String section,
            @Param("status") StudentStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(value = "SELECT s FROM Student s JOIN FETCH s.academicDetail a LEFT JOIN FETCH s.contactDetail c WHERE " +
           "(EXISTS (SELECT fa.id FROM FacultyAssignment fa WHERE fa.faculty.id = :facultyId AND fa.active = true AND " +
           "LOWER(fa.branchGroup) = LOWER(a.branchGroup) AND LOWER(fa.intermediateYear) = LOWER(a.intermediateYear) AND " +
           "LOWER(fa.section) = LOWER(a.section) AND (fa.academicYear IS NULL OR fa.academicYear = '' OR a.academicYear IS NULL OR a.academicYear = '' OR LOWER(fa.academicYear) = LOWER(a.academicYear))) " +
           "OR (s.createdBy.id = :userId)) AND " +
           "(:department IS NULL OR :department = '' OR LOWER(a.department) = LOWER(:department) OR LOWER(a.branchGroup) = LOWER(:department)) AND " +
           "(:academicYear IS NULL OR :academicYear = '' OR a.academicYear = :academicYear) AND " +
           "(:currentYear IS NULL OR a.semester = :currentYear) AND " +
           "(:section IS NULL OR :section = '' OR LOWER(a.section) = LOWER(:section)) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR (" +
           "   LOWER(s.studentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))" +
           "))",
           countQuery = "SELECT COUNT(s) FROM Student s JOIN s.academicDetail a LEFT JOIN s.contactDetail c WHERE " +
           "(EXISTS (SELECT fa.id FROM FacultyAssignment fa WHERE fa.faculty.id = :facultyId AND fa.active = true AND " +
           "LOWER(fa.branchGroup) = LOWER(a.branchGroup) AND LOWER(fa.intermediateYear) = LOWER(a.intermediateYear) AND " +
           "LOWER(fa.section) = LOWER(a.section) AND (fa.academicYear IS NULL OR fa.academicYear = '' OR a.academicYear IS NULL OR a.academicYear = '' OR LOWER(fa.academicYear) = LOWER(a.academicYear))) " +
           "OR (s.createdBy.id = :userId)) AND " +
           "(:department IS NULL OR :department = '' OR LOWER(a.department) = LOWER(:department) OR LOWER(a.branchGroup) = LOWER(:department)) AND " +
           "(:academicYear IS NULL OR :academicYear = '' OR a.academicYear = :academicYear) AND " +
           "(:currentYear IS NULL OR a.semester = :currentYear) AND " +
           "(:section IS NULL OR :section = '' OR LOWER(a.section) = LOWER(:section)) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR (" +
           "   LOWER(s.studentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(s.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))" +
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

    @Query("SELECT s FROM Student s LEFT JOIN s.academicDetail a LEFT JOIN s.contactDetail c WHERE " +
           "LOWER(s.studentId) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.branchGroup) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Student> searchByQuery(@Param("query") String query);

    @Query("SELECT s FROM Student s LEFT JOIN s.academicDetail a LEFT JOIN s.contactDetail c WHERE " +
           "(EXISTS (SELECT fa.id FROM FacultyAssignment fa WHERE fa.faculty.id = :facultyId AND fa.active = true AND " +
           "LOWER(fa.branchGroup) = LOWER(a.branchGroup) AND LOWER(fa.intermediateYear) = LOWER(a.intermediateYear) AND " +
           "LOWER(fa.section) = LOWER(a.section) AND (fa.academicYear IS NULL OR fa.academicYear = '' OR a.academicYear IS NULL OR a.academicYear = '' OR LOWER(fa.academicYear) = LOWER(a.academicYear))) " +
           "OR (s.createdBy.id = :userId)) AND (" +
           "LOWER(s.studentId) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.branchGroup) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Student> searchByQueryForFaculty(@Param("query") String query, @Param("facultyId") Long facultyId, @Param("userId") Long userId);

    @Query("SELECT s FROM Student s JOIN FETCH s.academicDetail a WHERE " +
           "(EXISTS (SELECT fa.id FROM FacultyAssignment fa WHERE fa.faculty.id = :facultyId AND fa.active = true AND " +
           "LOWER(fa.branchGroup) = LOWER(a.branchGroup) AND LOWER(fa.intermediateYear) = LOWER(a.intermediateYear) AND " +
           "LOWER(fa.section) = LOWER(a.section) AND (fa.academicYear IS NULL OR fa.academicYear = '' OR a.academicYear IS NULL OR a.academicYear = '' OR LOWER(fa.academicYear) = LOWER(a.academicYear))) " +
           "OR (s.createdBy.id = :userId))")
    List<Student> findAccessibleStudentsByFaculty(@Param("facultyId") Long facultyId, @Param("userId") Long userId);

    @Query("SELECT s FROM Student s JOIN FETCH s.academicDetail a WHERE EXISTS (SELECT fa.id FROM FacultyAssignment fa WHERE fa.faculty.id = :facultyId AND fa.active = true AND " +
           "LOWER(fa.branchGroup) = LOWER(a.branchGroup) AND LOWER(fa.intermediateYear) = LOWER(a.intermediateYear) AND " +
           "LOWER(fa.section) = LOWER(a.section) AND (fa.academicYear IS NULL OR fa.academicYear = '' OR a.academicYear IS NULL OR a.academicYear = '' OR LOWER(fa.academicYear) = LOWER(a.academicYear)))")
    List<Student> findAccessibleStudentsByFaculty(@Param("facultyId") Long facultyId);

    List<Student> findTop5ByOrderByIdDesc();

    @Query("SELECT s FROM Student s JOIN s.academicDetail a WHERE LOWER(s.studentId) = LOWER(:studentId) AND (" +
           "EXISTS (SELECT fa.id FROM FacultyAssignment fa WHERE fa.faculty.id = :facultyId AND fa.active = true AND " +
           "LOWER(fa.branchGroup) = LOWER(a.branchGroup) AND LOWER(fa.intermediateYear) = LOWER(a.intermediateYear) AND " +
           "LOWER(fa.section) = LOWER(a.section) AND (fa.academicYear IS NULL OR fa.academicYear = '' OR a.academicYear IS NULL OR a.academicYear = '' OR LOWER(fa.academicYear) = LOWER(a.academicYear))) " +
           "OR (s.createdBy.id = :userId))")
    Optional<Student> findAccessibleStudentByFaculty(@Param("studentId") String studentId, @Param("facultyId") Long facultyId, @Param("userId") Long userId);

    long countByAcademicDetail_SectionIgnoreCase(String section);

    long countByAcademicDetail_BranchGroupIgnoreCase(String branchGroup);

    List<Student> findByAcademicDetail_SectionIgnoreCase(String section);

    @Query("SELECT COUNT(s) FROM Student s JOIN s.academicDetail a WHERE s.status = com.sicms.entity.StudentStatus.ACTIVE AND " +
           "(:branchGroup IS NULL OR :branchGroup = '' OR LOWER(a.branchGroup) = LOWER(:branchGroup)) AND " +
           "(:intermediateYear IS NULL OR :intermediateYear = '' OR LOWER(a.intermediateYear) = LOWER(:intermediateYear)) AND " +
           "(LOWER(a.section) = LOWER(:sectionName) OR " +
           " LOWER(a.section) = LOWER(CONCAT('Section ', :sectionName)) OR " +
           " LOWER(:sectionName) = LOWER(CONCAT('Section ', a.section)))")
    long countStudentsBySectionDetails(@Param("branchGroup") String branchGroup,
                                       @Param("intermediateYear") String intermediateYear,
                                       @Param("sectionName") String sectionName);

    @Query("SELECT s FROM Student s JOIN s.academicDetail a WHERE s.status = com.sicms.entity.StudentStatus.ACTIVE AND " +
           "(:branchGroup IS NULL OR :branchGroup = '' OR LOWER(a.branchGroup) = LOWER(:branchGroup)) AND " +
           "(:intermediateYear IS NULL OR :intermediateYear = '' OR LOWER(a.intermediateYear) = LOWER(:intermediateYear)) AND " +
           "(LOWER(a.section) = LOWER(:sectionName) OR " +
           " LOWER(a.section) = LOWER(CONCAT('Section ', :sectionName)) OR " +
           " LOWER(:sectionName) = LOWER(CONCAT('Section ', a.section)))")
    List<Student> findStudentsBySectionDetails(@Param("branchGroup") String branchGroup,
                                               @Param("intermediateYear") String intermediateYear,
                                               @Param("sectionName") String sectionName);

    @Query("SELECT DISTINCT s FROM Student s LEFT JOIN FETCH s.academicDetail a")
    List<Student> findAllWithAcademicDetail();

    @Query("SELECT LOWER(COALESCE(a.branchGroup, '')), LOWER(COALESCE(a.intermediateYear, '')), LOWER(a.section), COUNT(s) " +
           "FROM Student s JOIN s.academicDetail a " +
           "WHERE s.status = com.sicms.entity.StudentStatus.ACTIVE AND a.section IS NOT NULL " +
           "GROUP BY LOWER(COALESCE(a.branchGroup, '')), LOWER(COALESCE(a.intermediateYear, '')), LOWER(a.section)")
    List<Object[]> countStudentsGroupedBySection();

    @Query("SELECT DISTINCT s FROM Student s " +
           "LEFT JOIN FETCH s.academicDetail a " +
           "LEFT JOIN FETCH s.contactDetail c " +
           "LEFT JOIN FETCH s.parentDetail p " +
           "LEFT JOIN FETCH s.guardianDetail g " +
           "LEFT JOIN FETCH s.createdBy u " +
           "ORDER BY s.id ASC")
    List<Student> findAllForExcelExport();

    @Query("SELECT DISTINCT s FROM Student s " +
           "LEFT JOIN FETCH s.academicDetail a " +
           "LEFT JOIN FETCH s.contactDetail c " +
           "LEFT JOIN FETCH s.parentDetail p " +
           "LEFT JOIN FETCH s.guardianDetail g " +
           "LEFT JOIN FETCH s.createdBy u " +
           "WHERE (EXISTS (SELECT fa.id FROM FacultyAssignment fa WHERE fa.faculty.id = :facultyId AND fa.active = true AND " +
           "LOWER(fa.branchGroup) = LOWER(a.branchGroup) AND LOWER(fa.intermediateYear) = LOWER(a.intermediateYear) AND " +
           "LOWER(fa.section) = LOWER(a.section) AND (fa.academicYear IS NULL OR fa.academicYear = '' OR a.academicYear IS NULL OR a.academicYear = '' OR LOWER(fa.academicYear) = LOWER(a.academicYear))) " +
           "OR (s.createdBy.id = :userId)) " +
           "ORDER BY s.id ASC")
    List<Student> findAccessibleStudentsForFacultyExport(@Param("facultyId") Long facultyId, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Student s SET s.createdBy = null WHERE s.createdBy.id = :userId")
    void clearCreatedByForUser(@Param("userId") Long userId);
}
