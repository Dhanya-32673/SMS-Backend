package com.sicms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sicms.entity.DocumentCategory;
import com.sicms.entity.DocumentStatus;
import com.sicms.entity.StudentDocument;

@Repository
public interface StudentDocumentRepository extends JpaRepository<StudentDocument, Long> {

        @EntityGraph(attributePaths = {"student", "documentType"})
        List<StudentDocument> findByStudentId(Long studentId);

        @EntityGraph(attributePaths = {"student", "documentType"})
        List<StudentDocument> findByStudent_StudentId(String studentId);

        @EntityGraph(attributePaths = {"student", "documentType"})
        Optional<StudentDocument> findFirstByStudent_StudentIdAndDocumentType_IdOrderByIdDesc(String studentId, Long documentTypeId);

        long countByStudent_StudentId(String studentId);

        Optional<StudentDocument> findFirstByStudentIdAndDocumentTypeIdOrderByIdDesc(Long studentId, Long documentTypeId);

        default Optional<StudentDocument> findByStudentIdAndDocumentTypeId(Long studentId, Long documentTypeId) {
            return findFirstByStudentIdAndDocumentTypeIdOrderByIdDesc(studentId, documentTypeId);
        }

        Optional<StudentDocument> findFirstByStudent_StudentIdAndDocumentType_IdAndStatusNotOrderByIdDesc(String studentId,
                        Long documentTypeId, DocumentStatus status);

        default Optional<StudentDocument> findByStudent_StudentIdAndDocumentType_IdAndStatusNot(String studentId,
                        Long documentTypeId, DocumentStatus status) {
            return findFirstByStudent_StudentIdAndDocumentType_IdAndStatusNotOrderByIdDesc(studentId, documentTypeId, status);
        }

        Optional<StudentDocument> findFirstByStudent_StudentIdAndDocumentType_CodeIgnoreCaseOrderByIdDesc(String studentId, String code);

        default Optional<StudentDocument> findByStudent_StudentIdAndDocumentType_CodeIgnoreCase(String studentId, String code) {
            return findFirstByStudent_StudentIdAndDocumentType_CodeIgnoreCaseOrderByIdDesc(studentId, code);
        }

        List<StudentDocument> findByStatusIn(java.util.Collection<DocumentStatus> statuses);

        long countByStatus(DocumentStatus status);

        @Query("SELECT d.status AS status, COUNT(d) AS count FROM StudentDocument d GROUP BY d.status")
        List<Object[]> countDocumentsByStatus();

        @Query(value = "SELECT GREATEST(0, (" +
                        "(SELECT COUNT(*) FROM students s WHERE s.status = 'ACTIVE') * " +
                        "(SELECT COUNT(*) FROM document_types dt WHERE dt.active = true AND dt.required_by_default = true)"
                        +
                        ") - (" +
                        "SELECT COUNT(DISTINCT CONCAT(d.student_id, '-', d.document_type_id)) " +
                        "FROM student_documents d " +
                        "JOIN students s ON d.student_id = s.id " +
                        "JOIN document_types dt ON d.document_type_id = dt.id " +
                        "WHERE s.status = 'ACTIVE' AND dt.active = true AND dt.required_by_default = true " +
                        "AND d.status NOT IN ('REJECTED', 'ARCHIVED')" +
                        "))", nativeQuery = true)
        long countTotalMissingDocumentsFast();

        @Query("SELECT d FROM StudentDocument d JOIN FETCH d.student s JOIN FETCH d.documentType t WHERE " +
                        "(:studentId IS NULL OR :studentId = '' OR LOWER(s.studentId) = LOWER(CAST(:studentId AS string))) AND "
                        +
                        "(:documentTypeId IS NULL OR t.id = :documentTypeId) AND " +
                        "(:category IS NULL OR t.category = :category) AND " +
                        "(:status IS NULL OR d.status = :status) AND " +
                        "(:search IS NULL OR :search = '' OR (" +
                        "   LOWER(s.studentId) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
                        "   LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
                        "   LOWER(s.fullName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
                        "   LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
                        "))")
        Page<StudentDocument> filterAndSearchDocuments(
                        @Param("studentId") String studentId,
                        @Param("documentTypeId") Long documentTypeId,
                        @Param("category") DocumentCategory category,
                        @Param("status") DocumentStatus status,
                        @Param("search") String search,
                        Pageable pageable);

        @Query("SELECT d FROM StudentDocument d JOIN FETCH d.student s JOIN FETCH s.academicDetail a JOIN FETCH d.documentType t WHERE " +
                        "EXISTS (SELECT fa.id FROM FacultyAssignment fa WHERE fa.faculty.id = :facultyId AND fa.active = true AND "
                        +
                        "LOWER(fa.branchGroup) = LOWER(a.branchGroup) AND LOWER(fa.intermediateYear) = LOWER(a.intermediateYear) AND "
                        +
                        "LOWER(fa.section) = LOWER(a.section) AND LOWER(fa.academicYear) = LOWER(a.academicYear)) AND "
                        +
                        "(:studentId IS NULL OR :studentId = '' OR LOWER(s.studentId) = LOWER(CAST(:studentId AS string))) AND "
                        +
                        "(:documentTypeId IS NULL OR t.id = :documentTypeId) AND " +
                        "(:category IS NULL OR t.category = :category) AND " +
                        "(:status IS NULL OR d.status = :status) AND " +
                        "(:search IS NULL OR :search = '' OR (" +
                        "   LOWER(s.studentId) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
                        "   LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
                        "   LOWER(s.fullName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
                        "   LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
                        "))")
        Page<StudentDocument> filterAndSearchDocumentsForFaculty(
                        @Param("facultyId") Long facultyId,
                        @Param("studentId") String studentId,
                        @Param("documentTypeId") Long documentTypeId,
                        @Param("category") DocumentCategory category,
                        @Param("status") DocumentStatus status,
                        @Param("search") String search,
                        Pageable pageable);

        @Query("SELECT d FROM StudentDocument d JOIN FETCH d.student s JOIN FETCH s.academicDetail a JOIN FETCH d.documentType t WHERE d.id = :id AND EXISTS (SELECT fa.id FROM FacultyAssignment fa WHERE fa.faculty.id = :facultyId AND fa.active = true AND "
                        +
                        "LOWER(fa.branchGroup) = LOWER(a.branchGroup) AND LOWER(fa.intermediateYear) = LOWER(a.intermediateYear) AND "
                        +
                        "LOWER(fa.section) = LOWER(a.section) AND LOWER(fa.academicYear) = LOWER(a.academicYear))")
        Optional<StudentDocument> findAccessibleDocumentByFaculty(@Param("id") Long id,
                        @Param("facultyId") Long facultyId);

        @Query("SELECT d FROM StudentDocument d JOIN FETCH d.student s JOIN FETCH d.documentType t JOIN s.academicDetail a WHERE EXISTS (SELECT fa.id FROM FacultyAssignment fa WHERE fa.faculty.id = :facultyId AND fa.active = true AND "
                        +
                        "LOWER(fa.branchGroup) = LOWER(a.branchGroup) AND LOWER(fa.intermediateYear) = LOWER(a.intermediateYear) AND "
                        +
                        "LOWER(fa.section) = LOWER(a.section) AND (fa.academicYear IS NULL OR fa.academicYear = '' OR a.academicYear IS NULL OR a.academicYear = '' OR LOWER(fa.academicYear) = LOWER(a.academicYear)))")
        List<StudentDocument> findDocumentsForFacultyScope(@Param("facultyId") Long facultyId);

        Page<StudentDocument> findByStatus(DocumentStatus status, Pageable pageable);
}
