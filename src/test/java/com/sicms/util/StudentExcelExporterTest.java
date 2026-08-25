package com.sicms.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sicms.entity.DocumentCategory;
import com.sicms.entity.DocumentStatus;
import com.sicms.entity.DocumentType;
import com.sicms.entity.Student;
import com.sicms.entity.StudentAcademicDetail;
import com.sicms.entity.StudentContactDetail;
import com.sicms.entity.StudentDocument;
import com.sicms.entity.StudentParentDetail;
import com.sicms.entity.StudentStatus;
import com.sicms.entity.User;

public class StudentExcelExporterTest {

    @Test
    public void testExcelExportGeneratesValidXlsx() throws Exception {
        // 1. Arrange Mock Data
        Student student = new Student();
        student.setId(1L);
        student.setStudentId("STU2026001");
        student.setRollNumber("26MPC001");
        student.setAdmissionNumber("ADM2026001");
        student.setFirstName("Rahul");
        student.setMiddleName("Kumar");
        student.setLastName("Sharma");
        student.setFullName("Rahul Kumar Sharma");
        student.setGender("MALE");
        student.setDateOfBirth(LocalDate.of(2008, 5, 15));
        student.setBloodGroup("O+");
        student.setNationality("Indian");
        student.setReligion("Hindu");
        student.setCasteCategory("General");
        student.setAadhaarNumber("123456789012");
        student.setPanNumber("ABCDE1234F");
        student.setIdentificationMarks("Mole on neck");
        student.setStatus(StudentStatus.ACTIVE);
        student.setCreatedAt(LocalDateTime.of(2026, 1, 10, 10, 30));
        student.setUpdatedAt(LocalDateTime.of(2026, 2, 1, 14, 0));

        User creator = new User();
        creator.setFullName("Principal Admin");
        creator.setEmail("admin@bhashyam.edu");
        student.setCreatedBy(creator);

        StudentContactDetail contact = new StudentContactDetail();
        contact.setMobileNumber("9876543210");
        contact.setAlternateMobile("9876543211");
        contact.setEmail("rahul@example.com");
        contact.setAddress("Flat 101, Bhashyam Enclave");
        contact.setCity("Hyderabad");
        contact.setDistrict("Hyderabad");
        contact.setState("Telangana");
        contact.setCountry("India");
        contact.setPinCode("500001");
        student.setContactDetail(contact);

        StudentAcademicDetail academic = new StudentAcademicDetail();
        academic.setUniversityId("BIE202699");
        academic.setDepartment("Science");
        academic.setBranchGroup("MPC");
        academic.setIntermediateYear("1st Year");
        academic.setSemester(1);
        academic.setSection("A");
        academic.setBatch("2026-2028");
        academic.setAcademicYear("2026-2027");
        academic.setAdmissionDate(LocalDate.of(2026, 6, 1));
        academic.setAdmissionType("REGULAR");
        academic.setRegulation("State Board");
        academic.setHostelDayScholar("DAY_SCHOLAR");
        academic.setMedium("English");
        student.setAcademicDetail(academic);

        StudentParentDetail parent = new StudentParentDetail();
        parent.setFatherName("Suresh Sharma");
        parent.setMotherName("Sunita Sharma");
        parent.setParentMobile("9876543210");
        parent.setParentEmail("parent@example.com");
        parent.setOccupation("Software Engineer");
        parent.setAnnualIncome(new BigDecimal("600000.00"));
        student.setParentDetail(parent);

        List<Student> students = List.of(student);
        Map<String, List<StudentDocument>> docsMap = Map.of();
        List<DocumentType> requiredTypes = List.of();

        // 2. Act - Export to byte stream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StudentExcelExporter.exportToStream(students, docsMap, requiredTypes, out);
        byte[] bytes = out.toByteArray();

        // 3. Assert - Parse generated Excel workbook
        Assertions.assertTrue(bytes.length > 0, "Excel output byte array must not be empty");

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("Students Directory");
            Assertions.assertNotNull(sheet, "Sheet 'Students Directory' must exist");

            // Check Row 0: Title Banner
            Row titleRow = sheet.getRow(0);
            Assertions.assertNotNull(titleRow);
            Cell titleCell = titleRow.getCell(0);
            Assertions.assertEquals("BHASHYAM EDUCATIONAL INSTITUTION - STUDENTS DIRECTORY", titleCell.getStringCellValue());

            // Check Row 1: Header Row
            Row headerRow = sheet.getRow(1);
            Assertions.assertNotNull(headerRow);
            Assertions.assertEquals(45, headerRow.getLastCellNum(), "Total columns must be 45");
            Assertions.assertEquals("Student ID", headerRow.getCell(0).getStringCellValue());
            Assertions.assertEquals("Full Name", headerRow.getCell(6).getStringCellValue());
            Assertions.assertEquals("Mobile Number", headerRow.getCell(17).getStringCellValue());
            Assertions.assertEquals("Father Name", headerRow.getCell(26).getStringCellValue());
            Assertions.assertEquals("Academic Year", headerRow.getCell(32).getStringCellValue());
            Assertions.assertEquals("University / Board ID", headerRow.getCell(44).getStringCellValue());

            // Check Row 2: Data Row
            Row dataRow = sheet.getRow(2);
            Assertions.assertNotNull(dataRow);
            Assertions.assertEquals("STU2026001", dataRow.getCell(0).getStringCellValue());
            Assertions.assertEquals("ADM2026001", dataRow.getCell(1).getStringCellValue());
            Assertions.assertEquals("26MPC001", dataRow.getCell(2).getStringCellValue());
            Assertions.assertEquals("Rahul Kumar Sharma", dataRow.getCell(6).getStringCellValue());
            Assertions.assertEquals("15-05-2008", dataRow.getCell(8).getStringCellValue());
            Assertions.assertEquals("ACTIVE", dataRow.getCell(16).getStringCellValue());
            Assertions.assertEquals("9876543210", dataRow.getCell(17).getStringCellValue());
            Assertions.assertEquals("Suresh Sharma", dataRow.getCell(26).getStringCellValue());
            Assertions.assertEquals("2026-2027", dataRow.getCell(32).getStringCellValue());
            Assertions.assertEquals("MPC", dataRow.getCell(34).getStringCellValue());
            Assertions.assertEquals("BIE202699", dataRow.getCell(44).getStringCellValue());
        }
    }
}
