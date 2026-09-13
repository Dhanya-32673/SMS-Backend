package com.sicms.util;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.sicms.entity.DocumentType;
import com.sicms.entity.Student;
import com.sicms.entity.StudentDocument;

/**
 * Utility class to export Student Database Information into professional Excel (.xlsx) format.
 * Exports exactly the official 22 canonical columns matching the Import Template 1:1.
 */
public class StudentExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static final String[] HEADERS = {
        "Student ID",
        "Admission Number",
        "Full Name",
        "Gender",
        "Date of Birth",
        "Nationality",
        "Religion",
        "Category",
        "Aadhaar Number",
        "Profile Photo URL",
        "Mobile Number",
        "Alternate Mobile",
        "Email Address - 1",
        "Email Address - 2",
        "Father Name",
        "Mother Name",
        "Academic Year",
        "Branch / Group",
        "Intermediate Year",
        "Batch",
        "Admission Type",
        "Hostel / Day Scholar"
    };

    /**
     * Exports students database records into Excel (.xlsx) format matching the official 22 columns.
     */
    public static void exportToStream(
            List<Student> students,
            Map<String, List<StudentDocument>> studentDocumentsMap,
            List<DocumentType> requiredDocumentTypes,
            OutputStream outputStream
    ) throws IOException {

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(500)) {
            workbook.setCompressTempFiles(true);
            SXSSFSheet sheet = workbook.createSheet("Students Directory");
            sheet.trackAllColumnsForAutoSizing();

            // Date format
            short dateFormat = workbook.getCreationHelper().createDataFormat().getFormat("dd-MM-yyyy");
            short textFormat = workbook.getCreationHelper().createDataFormat().getFormat("@");

            // Fonts
            Font titleFont = workbook.createFont();
            titleFont.setFontName("Calibri");
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);
            titleFont.setColor(IndexedColors.WHITE.getIndex());

            Font headerFont = workbook.createFont();
            headerFont.setFontName("Calibri");
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 10);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            Font dataFont = workbook.createFont();
            dataFont.setFontName("Calibri");
            dataFont.setFontHeightInPoints((short) 10);

            // Title Style (Navy Blue)
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(headerStyle);

            // Data Style - Default Left
            CellStyle defaultDataStyle = workbook.createCellStyle();
            defaultDataStyle.setFont(dataFont);
            defaultDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            defaultDataStyle.setAlignment(HorizontalAlignment.LEFT);
            defaultDataStyle.setDataFormat(textFormat);
            setBorders(defaultDataStyle);

            // Data Style - Centered Text
            CellStyle centeredDataStyle = workbook.createCellStyle();
            centeredDataStyle.setFont(dataFont);
            centeredDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            centeredDataStyle.setAlignment(HorizontalAlignment.CENTER);
            centeredDataStyle.setDataFormat(textFormat);
            setBorders(centeredDataStyle);

            // Data Style - Date
            CellStyle dateDataStyle = workbook.createCellStyle();
            dateDataStyle.setFont(dataFont);
            dateDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dateDataStyle.setAlignment(HorizontalAlignment.CENTER);
            dateDataStyle.setDataFormat(dateFormat);
            setBorders(dateDataStyle);

            // Row 0: Title Banner
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(30);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BHASHYAM IIT JEE ACADEMY — OFFICIAL STUDENTS DIRECTORY");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

            // Row 1: Header Row
            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Populate Student Data Rows
            int rowIdx = 2;
            if (students != null) {
                for (Student student : students) {
                    Row row = sheet.createRow(rowIdx++);
                    row.setHeightInPoints(20);

                    int col = 0;
                    // 1. Student ID
                    createCell(row, col++, student.getStudentId(), centeredDataStyle);
                    // 2. Admission Number
                    createCell(row, col++, student.getAdmissionNumber(), centeredDataStyle);
                    // 3. Full Name
                    createCell(row, col++, student.getFullName(), defaultDataStyle);
                    // 4. Gender
                    createCell(row, col++, student.getGender(), centeredDataStyle);
                    // 5. Date of Birth
                    createDateCell(row, col++, student.getDateOfBirth(), dateDataStyle);
                    // 6. Nationality
                    createCell(row, col++, student.getNationality() != null ? student.getNationality() : "Indian", centeredDataStyle);
                    // 7. Religion
                    createCell(row, col++, student.getReligion(), centeredDataStyle);
                    // 8. Category
                    createCell(row, col++, student.getCategory(), centeredDataStyle);
                    // 9. Aadhaar Number (Text format to prevent scientific notation)
                    createCell(row, col++, student.getAadhaarNumber(), centeredDataStyle);
                    // 10. Profile Photo URL
                    createCell(row, col++, student.getProfilePhotoUrl(), defaultDataStyle);
                    // 11. Mobile Number
                    createCell(row, col++, student.getMobileNumber(), centeredDataStyle);
                    // 12. Alternate Mobile
                    createCell(row, col++, student.getAlternateMobile(), centeredDataStyle);
                    // 13. Email Address - 1
                    createCell(row, col++, student.getEmailAddress1(), defaultDataStyle);
                    // 14. Email Address - 2
                    createCell(row, col++, student.getEmailAddress2(), defaultDataStyle);
                    // 15. Father Name
                    createCell(row, col++, student.getFatherName(), defaultDataStyle);
                    // 16. Mother Name
                    createCell(row, col++, student.getMotherName(), defaultDataStyle);
                    // 17. Academic Year
                    createCell(row, col++, student.getAcademicYear(), centeredDataStyle);
                    // 18. Branch / Group
                    createCell(row, col++, student.getBranchGroup(), centeredDataStyle);
                    // 19. Intermediate Year
                    createCell(row, col++, student.getIntermediateYear(), centeredDataStyle);
                    // 20. Batch
                    createCell(row, col++, student.getBatch(), centeredDataStyle);
                    // 21. Admission Type
                    createCell(row, col++, student.getAdmissionType(), centeredDataStyle);
                    // 22. Hostel / Day Scholar
                    createCell(row, col++, student.getHostelDayScholar(), centeredDataStyle);
                }
            }

            sheet.createFreezePane(0, 2);

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
                int width = sheet.getColumnWidth(i);
                if (width < 14 * 256) {
                    sheet.setColumnWidth(i, 14 * 256);
                } else if (width > 50 * 256) {
                    sheet.setColumnWidth(i, 50 * 256);
                }
            }

            workbook.write(outputStream);
        }
    }

    private static void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private static void createDateCell(Row row, int col, LocalDate date, CellStyle style) {
        Cell cell = row.createCell(col);
        if (date != null) {
            cell.setCellValue(date.format(DATE_FORMATTER));
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    private static void setBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
    }
}
