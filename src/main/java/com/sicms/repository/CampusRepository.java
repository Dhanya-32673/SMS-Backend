package com.sicms.repository;

import com.sicms.entity.Campus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampusRepository extends JpaRepository<Campus, Long> {
    List<Campus> findAllByOrderByDisplayOrderAsc();
    List<Campus> findByActiveTrueOrderByDisplayOrderAsc();
    Optional<Campus> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
