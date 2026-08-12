package com.sicms.repository;

import com.sicms.entity.StudentGuardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, Long> {
    Optional<StudentGuardian> findFirstByStudentIdOrderByIdAsc(Long studentId);

    default Optional<StudentGuardian> findByStudentId(Long studentId) {
        return findFirstByStudentIdOrderByIdAsc(studentId);
    }
}
