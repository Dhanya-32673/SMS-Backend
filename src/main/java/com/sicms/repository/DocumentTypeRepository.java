package com.sicms.repository;

import com.sicms.entity.DocumentCategory;
import com.sicms.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {

    Optional<DocumentType> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<DocumentType> findByActiveTrue();

    List<DocumentType> findByCategoryAndActiveTrue(DocumentCategory category);
}
