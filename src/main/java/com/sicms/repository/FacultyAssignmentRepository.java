package com.sicms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sicms.entity.FacultyAssignment;

public interface FacultyAssignmentRepository extends JpaRepository<FacultyAssignment, Long> {

    List<FacultyAssignment> findByFacultyIdAndActiveTrue(Long facultyId);

    List<FacultyAssignment> findByFacultyId(Long facultyId);

    @Query("SELECT fa FROM FacultyAssignment fa WHERE fa.faculty.id = :facultyId AND fa.active = true")
    List<FacultyAssignment> findActiveByFacultyId(@Param("facultyId") Long facultyId);

    @Query("SELECT fa FROM FacultyAssignment fa WHERE fa.branchGroup = :group AND fa.intermediateYear = :year AND fa.section = :section AND fa.active = true")
    List<FacultyAssignment> findActiveAssignments(@Param("group") String group,
                                                   @Param("year") String year,
                                                   @Param("section") String section);
}
