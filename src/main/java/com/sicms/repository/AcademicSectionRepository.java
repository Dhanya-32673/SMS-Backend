package com.sicms.repository;

import com.sicms.entity.AcademicSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicSectionRepository extends JpaRepository<AcademicSection, Long> {

    List<AcademicSection> findByBranchGroupAndIntermediateYearAndActiveTrue(String branchGroup, String intermediateYear);

    List<AcademicSection> findByActiveTrue();

    boolean existsByNameIgnoreCaseAndAcademicYear(String name, String academicYear);

    boolean existsByNameIgnoreCaseAndBranchGroupAndIntermediateYearAndAcademicYear(String name, String branchGroup, String intermediateYear, String academicYear);

    java.util.Optional<AcademicSection> findByNameIgnoreCase(String name);
}
