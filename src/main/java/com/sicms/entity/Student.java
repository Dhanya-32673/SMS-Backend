package com.sicms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "students", indexes = {
    @Index(name = "idx_student_student_id", columnList = "student_id"),
    @Index(name = "idx_student_admission_number", columnList = "admission_number"),
    @Index(name = "idx_student_status", columnList = "status"),
    @Index(name = "idx_student_full_name", columnList = "full_name"),
    @Index(name = "idx_student_branch_group", columnList = "branch_group"),
    @Index(name = "idx_student_academic_year", columnList = "academic_year"),
    @Index(name = "idx_student_section", columnList = "section"),
    @Index(name = "idx_student_campus", columnList = "campus"),
    @Index(name = "idx_student_campus_id", columnList = "campus_id"),
    @Index(name = "idx_student_mobile", columnList = "mobile_number"),
    @Index(name = "idx_student_email_1", columnList = "email_address_1")
})
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1. Student ID
    @Column(name = "student_id", nullable = false, unique = true, length = 30)
    private String studentId;

    // 2. Admission Number
    @Column(name = "admission_number", length = 50)
    private String admissionNumber;

    // 3. Full Name
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    // 4. Gender
    @Column(nullable = false, length = 20)
    private String gender;

    // 5. Date of Birth
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    // 6. Nationality
    @Column(length = 50)
    private String nationality = "Indian";

    // 7. Religion
    @Column(length = 50)
    private String religion;

    // 8. Category
    @Column(length = 50)
    private String category;

    // 9. Aadhaar Number
    @Column(name = "aadhaar_number", length = 20)
    private String aadhaarNumber;

    // 10. Profile Photo URL
    @Column(name = "profile_photo_url", columnDefinition = "TEXT")
    private String profilePhotoUrl;

    // 11. Mobile Number
    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    // 12. Alternate Mobile
    @Column(name = "alternate_mobile", length = 20)
    private String alternateMobile;

    // 13. Email Address - 1
    @Column(name = "email_address_1", nullable = false, length = 150)
    private String emailAddress1;

    // 14. Email Address - 2
    @Column(name = "email_address_2", length = 150)
    private String emailAddress2;

    // 15. Father Name
    @Column(name = "father_name", nullable = false, length = 100)
    private String fatherName;

    // 16. Mother Name
    @Column(name = "mother_name", nullable = false, length = 100)
    private String motherName;

    // 17. Academic Year
    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    // 18. Branch / Group
    @Column(name = "branch_group", nullable = false, length = 50)
    private String branchGroup;

    // 19. Intermediate Year
    @Column(name = "intermediate_year", nullable = false, length = 20)
    private String intermediateYear;

    // 20. Batch
    @Column(nullable = false, length = 20)
    private String batch;

    // 21. Admission Type
    @Column(name = "admission_type", length = 30)
    private String admissionType = "REGULAR";

    // 22. Hostel / Day Scholar
    @Column(name = "hostel_day_scholar", nullable = false, length = 30)
    private String hostelDayScholar = "DAY_SCHOLAR";

    @Column(name = "campus", length = 100)
    private String campus;

    @Column(name = "campus_id")
    private Long campusId;

    // Technical Fields
    @Column(nullable = false, length = 10)
    private String section = "Unassigned";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudentStatus status = StudentStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private java.util.List<User> users = new java.util.ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Student() {
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

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getAdmissionNumber() {
        return admissionNumber;
    }

    public void setAdmissionNumber(String admissionNumber) {
        this.admissionNumber = admissionNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getReligion() {
        return religion;
    }

    public void setReligion(String religion) {
        this.religion = religion;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAadhaarNumber() {
        return aadhaarNumber;
    }

    public void setAadhaarNumber(String aadhaarNumber) {
        this.aadhaarNumber = aadhaarNumber;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getAlternateMobile() {
        return alternateMobile;
    }

    public void setAlternateMobile(String alternateMobile) {
        this.alternateMobile = alternateMobile;
    }

    public String getEmailAddress1() {
        return emailAddress1;
    }

    public void setEmailAddress1(String emailAddress1) {
        this.emailAddress1 = emailAddress1;
    }

    public String getEmailAddress2() {
        return emailAddress2;
    }

    public void setEmailAddress2(String emailAddress2) {
        this.emailAddress2 = emailAddress2;
    }

    public String getFatherName() {
        return fatherName;
    }

    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }

    public String getMotherName() {
        return motherName;
    }

    public void setMotherName(String motherName) {
        this.motherName = motherName;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
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

    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
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

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public void setStatus(StudentStatus status) {
        this.status = status;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }

    public Long getCampusId() {
        return campusId;
    }

    public void setCampusId(Long campusId) {
        this.campusId = campusId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public java.util.List<User> getUsers() {
        return users;
    }

    public void setUsers(java.util.List<User> users) {
        this.users = users;
    }

    public User getUser() {
        return (users != null && !users.isEmpty()) ? users.get(0) : null;
    }
}
