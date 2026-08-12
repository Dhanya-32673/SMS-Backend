package com.sicms.service;

import com.sicms.entity.DocumentType;
import com.sicms.repository.DocumentTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DocumentTypeService {

    private final DocumentTypeRepository documentTypeRepository;

    @Autowired
    public DocumentTypeService(DocumentTypeRepository documentTypeRepository) {
        this.documentTypeRepository = documentTypeRepository;
    }

    public List<DocumentType> getAllDocumentTypes() {
        return documentTypeRepository.findAll();
    }

    public List<DocumentType> getActiveDocumentTypes() {
        return documentTypeRepository.findByActiveTrue();
    }

    public DocumentType getDocumentTypeById(Long id) {
        return documentTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document type not found with ID: " + id));
    }

    @Transactional
    public DocumentType createDocumentType(DocumentType documentType) {
        if (documentTypeRepository.existsByCodeIgnoreCase(documentType.getCode())) {
            throw new IllegalArgumentException("Document type with code '" + documentType.getCode() + "' already exists.");
        }
        return documentTypeRepository.save(documentType);
    }

    @Transactional
    public DocumentType updateDocumentType(Long id, DocumentType updated) {
        DocumentType existing = getDocumentTypeById(id);
        existing.setName(updated.getName());
        if (updated.getCode() != null && !updated.getCode().isBlank()) {
            existing.setCode(updated.getCode());
        }
        existing.setCategory(updated.getCategory());
        existing.setDescription(updated.getDescription());
        existing.setRequiredByDefault(updated.isRequiredByDefault());
        existing.setHasExpiry(updated.isHasExpiry());
        existing.setActive(updated.isActive());
        return documentTypeRepository.save(existing);
    }

    @Transactional
    public void deleteDocumentType(Long id) {
        DocumentType existing = getDocumentTypeById(id);
        documentTypeRepository.delete(existing);
    }
}
