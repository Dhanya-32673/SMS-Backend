package com.sicms.util;

import com.sicms.dto.StudentImportRowDto;
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

            // Row 2: Headers
            Assertions.assertEquals("Roll Number *", sheet.getRow(2).getCell(2).getStringCellValue());
            Assertions.assertEquals("First Name *", sheet.getRow(2).getCell(3).getStringCellValue());

            // Row 3: Sample data
            Assertions.assertEquals("26INT101", sheet.getRow(3).getCell(2).getStringCellValue());
            Assertions.assertEquals("Rahul", sheet.getRow(3).getCell(3).getStringCellValue());
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

        Assertions.assertTrue(headerErrors.isEmpty(), "Header errors must be empty for official template");
        Assertions.assertEquals(1, rows.size(), "Template contains 1 sample row");

        StudentImportRowDto sample = rows.get(0);
        Assertions.assertEquals("26INT101", sample.getRollNumber());
        Assertions.assertEquals("Rahul", sample.getFirstName());
        Assertions.assertEquals("Sharma", sample.getLastName());
        Assertions.assertEquals("MALE", sample.getGender());
        Assertions.assertEquals("9876543210", sample.getMobileNumber());
        Assertions.assertEquals("rahul.sharma@example.com", sample.getEmail());
        Assertions.assertEquals("MPC", sample.getBranchGroup());
        Assertions.assertEquals("1st Year", sample.getIntermediateYear());
        Assertions.assertEquals("A", sample.getSection());
    }

    @Test
    public void testGenerateErrorReport() throws Exception {
        StudentImportRowDto failed = new StudentImportRowDto();
        failed.setRowNumber(5);
        failed.setStatus("ERROR");
        failed.setRollNumber("00123");
        failed.setFullName("John Doe");
        failed.setBranchGroup("MPC");
        failed.setIntermediateYear("1st Year");
        failed.setSection("A");
        failed.addError("Invalid email format");
        failed.addError("Section B does not exist");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StudentExcelImportHelper.generateErrorReport(List.of(failed), out);
        byte[] bytes = out.toByteArray();

        Assertions.assertTrue(bytes.length > 0);
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Import Error Report");
            Assertions.assertNotNull(sheet);
            Assertions.assertEquals("00123", sheet.getRow(2).getCell(2).getStringCellValue());
            Assertions.assertTrue(sheet.getRow(2).getCell(7).getStringCellValue().contains("Invalid email format"));
        }
    }
}
