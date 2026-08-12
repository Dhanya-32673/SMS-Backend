package com.sicms.repository;

import com.sicms.entity.Faculty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    Optional<Faculty> findByFacultyId(String facultyId);

    Optional<Faculty> findByUserId(Long userId);

    Optional<Faculty> findByEmail(String email);

    boolean existsByFacultyId(String facultyId);

    boolean existsByEmployeeId(String employeeId);

    boolean existsByEmail(String email);

    @Query("SELECT f FROM Faculty f WHERE " +
           "(:query IS NULL OR :query = '' OR LOWER(f.facultyId) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(f.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(f.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(f.mobileNumber) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:group IS NULL OR :group = '' OR f.primaryGroup = :group) AND " +
           "(:status IS NULL OR :status = '' OR f.status = :status)")
    Page<Faculty> searchFaculty(@Param("query") String query,
                               @Param("group") String group,
                               @Param("status") String status,
                               Pageable pageable);

    long countByStatus(String status);
}
