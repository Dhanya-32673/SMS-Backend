package com.sicms.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_academic_details")
public class StudentAcademicDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    @JsonBackReference
    private Student student;

    @Column(name = "university_id", length = 50)
    private String universityId;

    @Column(length = 50)
    private String department = "General";

    @Column(name = "branch_group", nullable = false, length = 50)
    private String branchGroup; // MPC, BiPC, MEC, CEC, HEC, etc.

    @Column(name = "intermediate_year", nullable = false, length = 20)
    private String intermediateYear; // 1st Year, 2nd Year

    private Integer semester;

    @Column(nullable = false, length = 10)
    private String section;

    @Column(nullable = false, length = 20)
    private String batch;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;

    @Column(length = 30)
    private String regulation;

    @Column(name = "admission_type", length = 30)
    private String admissionType = "REGULAR";

    @Column(name = "hostel_day_scholar", nullable = false, length = 20)
    private String hostelDayScholar = "DAY_SCHOLAR";

    @Column(length = 30)
    private String medium = "English";

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StudentAcademicDetail() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public String getUniversityId() {
        return universityId;
    }

    public void setUniversityId(String universityId) {
        this.universityId = universityId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getBranchGroup() {
        return branchGroup;
    }

    public void setBranchGroup(String branchGroup) {
        this.branchGroup = branchGroup;
    }

    public String getIntermediateYear() {
        return intermediateYear;
    }

    public void setIntermediateYear(String intermediateYear) {
        this.intermediateYear = intermediateYear;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }

    public void setAdmissionDate(LocalDate admissionDate) {
        this.admissionDate = admissionDate;
    }

    public String getRegulation() {
        return regulation;
    }

    public void setRegulation(String regulation) {
        this.regulation = regulation;
    }

    public String getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(String admissionType) {
        this.admissionType = admissionType;
    }

    public String getHostelDayScholar() {
        return hostelDayScholar;
    }

    public void setHostelDayScholar(String hostelDayScholar) {
        this.hostelDayScholar = hostelDayScholar;
    }

    public String getMedium() {
        return medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
