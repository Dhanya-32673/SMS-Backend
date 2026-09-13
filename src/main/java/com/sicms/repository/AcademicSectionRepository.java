package com.sicms.repository;

import com.sicms.entity.AcademicSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicSectionRepository extends JpaRepository<AcademicSection, Long> {

    List<AcademicSection> findByBranchGroupAndIntermediateYearAndActiveTrue(String branchGroup, String intermediateYear);

    List<AcademicSection> findByActiveTrue();

    boolean existsByNameIgnoreCaseAndAcademicYear(String name, String academicYear);

    boolean existsByNameIgnoreCaseAndBranchGroupAndIntermediateYearAndAcademicYear(String name, String branchGroup, String intermediateYear, String academicYear);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT TRIM(s.intermediateYear) FROM AcademicSection s WHERE " +
           "s.active = true AND " +
           "LOWER(TRIM(s.branchGroup)) = LOWER(TRIM(:group)) AND " +
           "s.intermediateYear IS NOT NULL AND TRIM(s.intermediateYear) <> ''")
    List<String> findDistinctIntermediateYearsByGroup(@org.springframework.data.repository.query.Param("group") String group);

    java.util.Optional<AcademicSection> findByNameIgnoreCase(String name);
}
