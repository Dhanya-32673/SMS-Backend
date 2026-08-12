package com.sicms.repository;

import com.sicms.entity.DocumentRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRequirementRepository extends JpaRepository<DocumentRequirement, Long> {

    List<DocumentRequirement> findByActiveTrue();

    List<DocumentRequirement> findByAcademicYearAndIntermediateYearAndBranchGroupAndActiveTrue(
            String academicYear, String intermediateYear, String branchGroup
    );
}
