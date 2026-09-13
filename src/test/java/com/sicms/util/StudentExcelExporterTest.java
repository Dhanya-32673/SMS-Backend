package com.sicms.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

import com.sicms.entity.DocumentType;
import com.sicms.entity.Student;
import com.sicms.entity.StudentDocument;
import com.sicms.entity.StudentStatus;
import com.sicms.entity.User;

public class StudentExcelExporterTest {

    @Test
    public void testExcelExportGeneratesValidXlsx() throws Exception {
        // 1. Arrange Mock Data with 22 canonical fields
        Student student = new Student();
        student.setId(1L);
        student.setStudentId("STU2026001");
        student.setAdmissionNumber("ADM2026001");
        student.setFullName("Rahul Kumar Sharma");
        student.setGender("MALE");
        student.setDateOfBirth(LocalDate.of(2008, 5, 15));
        student.setNationality("Indian");
        student.setReligion("Hindu");
        student.setCategory("OC");
        student.setAadhaarNumber("123456789012");
        student.setProfilePhotoUrl("https://example.com/photo.jpg");
        student.setMobileNumber("9876543210");
        student.setAlternateMobile("9123456780");
        student.setEmailAddress1("rahul.sharma@example.com");
        student.setEmailAddress2("rahul.alt@example.com");
        student.setFatherName("Suresh Sharma");
        student.setMotherName("Sunita Sharma");
        student.setAcademicYear("2026-2027");
        student.setBranchGroup("MPC");
        student.setIntermediateYear("1st Year");
        student.setBatch("2026-2028");
        student.setAdmissionType("REGULAR");
        student.setHostelDayScholar("DAY_SCHOLAR");

        student.setStatus(StudentStatus.ACTIVE);
        student.setSection("A");
        student.setCreatedAt(LocalDateTime.of(2026, 1, 10, 10, 30));
        student.setUpdatedAt(LocalDateTime.of(2026, 2, 1, 14, 0));

        User creator = new User();
        creator.setFullName("Principal Admin");
        creator.setEmail("admin@bhashyam.edu");
        student.setCreatedBy(creator);

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
            Assertions.assertEquals("BHASHYAM IIT JEE ACADEMY — OFFICIAL STUDENTS DIRECTORY", titleCell.getStringCellValue());

            // Check Row 1: Header Row - Exactly 22 columns
            Row headerRow = sheet.getRow(1);
            Assertions.assertNotNull(headerRow);
            Assertions.assertEquals(22, headerRow.getLastCellNum(), "Total columns must be exactly 22");
            Assertions.assertEquals("Student ID", headerRow.getCell(0).getStringCellValue());
            Assertions.assertEquals("Admission Number", headerRow.getCell(1).getStringCellValue());
            Assertions.assertEquals("Full Name", headerRow.getCell(2).getStringCellValue());
            Assertions.assertEquals("Gender", headerRow.getCell(3).getStringCellValue());
            Assertions.assertEquals("Date of Birth", headerRow.getCell(4).getStringCellValue());
            Assertions.assertEquals("Nationality", headerRow.getCell(5).getStringCellValue());
            Assertions.assertEquals("Religion", headerRow.getCell(6).getStringCellValue());
            Assertions.assertEquals("Category", headerRow.getCell(7).getStringCellValue());
            Assertions.assertEquals("Aadhaar Number", headerRow.getCell(8).getStringCellValue());
            Assertions.assertEquals("Profile Photo URL", headerRow.getCell(9).getStringCellValue());
            Assertions.assertEquals("Mobile Number", headerRow.getCell(10).getStringCellValue());
            Assertions.assertEquals("Alternate Mobile", headerRow.getCell(11).getStringCellValue());
            Assertions.assertEquals("Email Address - 1", headerRow.getCell(12).getStringCellValue());
            Assertions.assertEquals("Email Address - 2", headerRow.getCell(13).getStringCellValue());
            Assertions.assertEquals("Father Name", headerRow.getCell(14).getStringCellValue());
            Assertions.assertEquals("Mother Name", headerRow.getCell(15).getStringCellValue());
            Assertions.assertEquals("Academic Year", headerRow.getCell(16).getStringCellValue());
            Assertions.assertEquals("Branch / Group", headerRow.getCell(17).getStringCellValue());
            Assertions.assertEquals("Intermediate Year", headerRow.getCell(18).getStringCellValue());
            Assertions.assertEquals("Batch", headerRow.getCell(19).getStringCellValue());
            Assertions.assertEquals("Admission Type", headerRow.getCell(20).getStringCellValue());
            Assertions.assertEquals("Hostel / Day Scholar", headerRow.getCell(21).getStringCellValue());

            // Check Row 2: Data Row
            Row dataRow = sheet.getRow(2);
            Assertions.assertNotNull(dataRow);
            Assertions.assertEquals("STU2026001", dataRow.getCell(0).getStringCellValue());
            Assertions.assertEquals("ADM2026001", dataRow.getCell(1).getStringCellValue());
            Assertions.assertEquals("Rahul Kumar Sharma", dataRow.getCell(2).getStringCellValue());
            Assertions.assertEquals("MALE", dataRow.getCell(3).getStringCellValue());
            Assertions.assertEquals("15-05-2008", dataRow.getCell(4).getStringCellValue());
            Assertions.assertEquals("Indian", dataRow.getCell(5).getStringCellValue());
            Assertions.assertEquals("Hindu", dataRow.getCell(6).getStringCellValue());
            Assertions.assertEquals("OC", dataRow.getCell(7).getStringCellValue());
            Assertions.assertEquals("123456789012", dataRow.getCell(8).getStringCellValue());
            Assertions.assertEquals("https://example.com/photo.jpg", dataRow.getCell(9).getStringCellValue());
            Assertions.assertEquals("9876543210", dataRow.getCell(10).getStringCellValue());
            Assertions.assertEquals("9123456780", dataRow.getCell(11).getStringCellValue());
            Assertions.assertEquals("rahul.sharma@example.com", dataRow.getCell(12).getStringCellValue());
            Assertions.assertEquals("rahul.alt@example.com", dataRow.getCell(13).getStringCellValue());
            Assertions.assertEquals("Suresh Sharma", dataRow.getCell(14).getStringCellValue());
            Assertions.assertEquals("Sunita Sharma", dataRow.getCell(15).getStringCellValue());
            Assertions.assertEquals("2026-2027", dataRow.getCell(16).getStringCellValue());
            Assertions.assertEquals("MPC", dataRow.getCell(17).getStringCellValue());
            Assertions.assertEquals("1st Year", dataRow.getCell(18).getStringCellValue());
            Assertions.assertEquals("2026-2028", dataRow.getCell(19).getStringCellValue());
            Assertions.assertEquals("REGULAR", dataRow.getCell(20).getStringCellValue());
            Assertions.assertEquals("DAY_SCHOLAR", dataRow.getCell(21).getStringCellValue());
        }
    }
}
