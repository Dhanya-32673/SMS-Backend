package com.sicms.repository;

import com.sicms.entity.StudentAcademicDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentAcademicRepository extends JpaRepository<StudentAcademicDetail, Long> {
    Optional<StudentAcademicDetail> findFirstByStudentIdOrderByIdAsc(Long studentId);

    default Optional<StudentAcademicDetail> findByStudentId(Long studentId) {
        return findFirstByStudentIdOrderByIdAsc(studentId);
    }
}
