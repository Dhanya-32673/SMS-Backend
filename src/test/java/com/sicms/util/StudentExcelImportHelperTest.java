package com.sicms.util;

import com.sicms.dto.StudentImportRowDto;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class StudentExcelImportHelperTest {

    @Test
    public void testGenerateTemplateCreatesValidXlsx() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StudentExcelImportHelper.generateTemplate(out);
        byte[] bytes = out.toByteArray();

        Assertions.assertTrue(bytes.length > 0, "Template output must not be empty");

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("Student Import Template");
            Assertions.assertNotNull(sheet, "Sheet 'Student Import Template' must exist");

            // Row 0 is Title, Row 1 is Instructions, Row 2 is Headers, Row 3 is Sample
            Assertions.assertTrue(sheet.getPhysicalNumberOfRows() >= 4);

            // Row 2: Headers - 22 columns
            Row headerRow = sheet.getRow(2);
            Assertions.assertEquals(22, headerRow.getLastCellNum(), "Template must have exactly 22 columns");
            Assertions.assertEquals("Student ID", headerRow.getCell(0).getStringCellValue());
            Assertions.assertEquals("Admission Number", headerRow.getCell(1).getStringCellValue());
            Assertions.assertEquals("Full Name *", headerRow.getCell(2).getStringCellValue());
            Assertions.assertEquals("Gender *", headerRow.getCell(3).getStringCellValue());
            Assertions.assertEquals("Date of Birth *", headerRow.getCell(4).getStringCellValue());
            Assertions.assertEquals("Nationality", headerRow.getCell(5).getStringCellValue());
            Assertions.assertEquals("Religion", headerRow.getCell(6).getStringCellValue());
            Assertions.assertEquals("Category", headerRow.getCell(7).getStringCellValue());
            Assertions.assertEquals("Aadhaar Number", headerRow.getCell(8).getStringCellValue());
            Assertions.assertEquals("Profile Photo URL", headerRow.getCell(9).getStringCellValue());
            Assertions.assertEquals("Mobile Number *", headerRow.getCell(10).getStringCellValue());
            Assertions.assertEquals("Alternate Mobile", headerRow.getCell(11).getStringCellValue());
            Assertions.assertEquals("Email Address - 1 *", headerRow.getCell(12).getStringCellValue());
            Assertions.assertEquals("Email Address - 2 *", headerRow.getCell(13).getStringCellValue());
            Assertions.assertEquals("Father Name *", headerRow.getCell(14).getStringCellValue());
            Assertions.assertEquals("Mother Name *", headerRow.getCell(15).getStringCellValue());
            Assertions.assertEquals("Academic Year *", headerRow.getCell(16).getStringCellValue());
            Assertions.assertEquals("Branch / Group *", headerRow.getCell(17).getStringCellValue());
            Assertions.assertEquals("Intermediate Year *", headerRow.getCell(18).getStringCellValue());
            Assertions.assertEquals("Batch *", headerRow.getCell(19).getStringCellValue());
            Assertions.assertEquals("Admission Type", headerRow.getCell(20).getStringCellValue());
            Assertions.assertEquals("Hostel / Day Scholar *", headerRow.getCell(21).getStringCellValue());

            // Row 3: Sample data
            Row sampleRow = sheet.getRow(3);
            Assertions.assertEquals("ADM2026001", sampleRow.getCell(1).getStringCellValue());
            Assertions.assertEquals("Rahul Kumar Sharma", sampleRow.getCell(2).getStringCellValue());
            Assertions.assertEquals("MALE", sampleRow.getCell(3).getStringCellValue());
            Assertions.assertEquals("15-06-2008", sampleRow.getCell(4).getStringCellValue());
            Assertions.assertEquals("9876543210", sampleRow.getCell(10).getStringCellValue());
            Assertions.assertEquals("MPC", sampleRow.getCell(17).getStringCellValue());
        }
    }

    @Test
    public void testParseGeneratedTemplateSampleRow() throws Exception {
        // 1. Generate template into bytes
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StudentExcelImportHelper.generateTemplate(out);
        byte[] templateBytes = out.toByteArray();

        // 2. Wrap into MockMultipartFile
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Student_Template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                templateBytes
        );

        // 3. Parse file
        List<String> headerErrors = new ArrayList<>();
        List<StudentImportRowDto> rows = StudentExcelImportHelper.parseExcel(file, headerErrors);

        Assertions.assertTrue(headerErrors.isEmpty(), "Header errors must be empty for official template: " + headerErrors);
        Assertions.assertEquals(1, rows.size(), "Template contains 1 sample row");

        StudentImportRowDto sample = rows.get(0);
        Assertions.assertEquals("ADM2026001", sample.getAdmissionNumber());
        Assertions.assertEquals("Rahul Kumar Sharma", sample.getFullName());
        Assertions.assertEquals("MALE", sample.getGender());
        Assertions.assertEquals("9876543210", sample.getMobileNumber());
        Assertions.assertEquals("rahul.sharma@example.com", sample.getEmailAddress1());
        Assertions.assertEquals("rahul.alt@example.com", sample.getEmailAddress2());
        Assertions.assertEquals("Suresh Sharma", sample.getFatherName());
        Assertions.assertEquals("Sunita Sharma", sample.getMotherName());
        Assertions.assertEquals("MPC", sample.getBranchGroup());
        Assertions.assertEquals("1st Year", sample.getIntermediateYear());
        Assertions.assertEquals("2026-2028", sample.getBatch());
        Assertions.assertEquals("DAY_SCHOLAR", sample.getHostelDayScholar());
    }

    @Test
    public void testParseRejectsObsoleteColumn() throws Exception {
        // Create an excel workbook with obsolete "Roll Number" column
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row r0 = sheet.createRow(0);
            r0.createCell(0).setCellValue("Admission Number");
            r0.createCell(1).setCellValue("Full Name");
            r0.createCell(2).setCellValue("Roll Number"); // obsolete!
            r0.createCell(3).setCellValue("Gender");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "obsolete.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray()
            );

            List<String> headerErrors = new ArrayList<>();
            List<StudentImportRowDto> rows = StudentExcelImportHelper.parseExcel(file, headerErrors);

            Assertions.assertTrue(rows.isEmpty(), "Parsed rows must be empty when header validation fails");
            Assertions.assertFalse(headerErrors.isEmpty(), "Must reject file containing obsolete column 'Roll Number'");
            Assertions.assertTrue(headerErrors.stream().anyMatch(e -> e.contains("Unsupported or obsolete column detected: 'Roll Number'")));
        }
    }

    @Test
    public void testGenerateErrorReport() throws Exception {
        StudentImportRowDto failed = new StudentImportRowDto();
        failed.setRowNumber(5);
        failed.setStatus("ERROR");
        failed.setStudentId("STU2026001");
        failed.setAdmissionNumber("ADM00123");
        failed.setFullName("John Doe");
        failed.setBranchGroup("MPC");
        failed.setIntermediateYear("1st Year");
        failed.setMobileNumber("9876543210");
        failed.addError("Invalid Email Address - 1");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StudentExcelImportHelper.generateErrorReport(List.of(failed), out);
        byte[] bytes = out.toByteArray();

        Assertions.assertTrue(bytes.length > 0);
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Import Error Report");
            Assertions.assertNotNull(sheet);
            Assertions.assertEquals("STU2026001", sheet.getRow(2).getCell(2).getStringCellValue());
            Assertions.assertEquals("ADM00123", sheet.getRow(2).getCell(3).getStringCellValue());
            Assertions.assertEquals("John Doe", sheet.getRow(2).getCell(4).getStringCellValue());
            Assertions.assertTrue(sheet.getRow(2).getCell(8).getStringCellValue().contains("Invalid Email Address - 1"));
        }
    }
}
