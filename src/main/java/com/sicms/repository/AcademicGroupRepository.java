package com.sicms.repository;

import com.sicms.entity.AcademicGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicGroupRepository extends JpaRepository<AcademicGroup, Long> {

    Optional<AcademicGroup> findByCode(String code);

    List<AcademicGroup> findByActiveTrue();

    boolean existsByCode(String code);
}
