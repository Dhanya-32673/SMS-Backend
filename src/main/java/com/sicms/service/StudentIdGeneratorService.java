package com.sicms.service;

import com.sicms.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
public class StudentIdGeneratorService {

    private final StudentRepository studentRepository;

    @Autowired
    public StudentIdGeneratorService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * Generates a safe, unique, human-readable Student ID (e.g., STU2026001008)
     * using database student count and duplicate collision checks without throwing
     * SQL sequence exceptions that trigger transaction rollbacks.
     */
    @Transactional(readOnly = true)
    public String generateNextStudentId() {
        int currentYear = Year.now().getValue();
        long studentCount = studentRepository.count();
        long nextNum = studentCount + 1001;

        String candidateId = String.format("STU%d%06d", currentYear, nextNum);

        int attempts = 0;
        while (studentRepository.existsByStudentId(candidateId) && attempts < 500) {
            nextNum++;
            candidateId = String.format("STU%d%06d", currentYear, nextNum);
            attempts++;
        }

        return candidateId;
    }

    @Transactional(readOnly = true)
    public String generateStudentId() {
        return generateNextStudentId();
    }
}
