package com.sicms.util;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import com.sicms.entity.StudentAcademicDetail;
import com.sicms.entity.StudentContactDetail;
import com.sicms.entity.StudentDocument;
import com.sicms.entity.StudentParentDetail;
import com.sicms.entity.StudentStatus;

/**
 * Utility class to export Student Database Information into professional Excel (.xlsx) format.
 * Exports only actual database fields captured during student registration.
 */
public class StudentExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final String[] HEADERS = {
        // 1. Identifiers & Personal Details (0 - 16)
        "Student ID", "Admission Number", "Roll Number",
        "First Name", "Middle Name", "Last Name", "Full Name",
        "Gender", "Date of Birth", "Blood Group",
        "Nationality", "Religion", "Category",
        "Aadhaar Number", "PAN Number", "Identification Marks", "Status",

        // 2. Contact & Address Details (17 - 25)
        "Mobile Number", "Alternate Mobile", "Email Address",
        "Address", "City", "District", "State", "PIN Code", "Country",

        // 3. Parent Details (26 - 31)
        "Father Name", "Mother Name", "Parent Mobile",
        "Parent Email", "Occupation", "Annual Income",

        // 4. Academic Details (32 - 44)
        "Academic Year", "Department", "Branch / Group",
        "Intermediate Year", "Semester", "Section",
        "Batch", "Admission Date", "Admission Type",
        "Hostel / Day Scholar", "Medium", "Regulation", "University / Board ID"
    };

    /**
     * Exports students database records into Excel (.xlsx) format using Apache POI streaming.
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

            // Header Style (Royal Blue)
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorders(headerStyle);

            // Data Cell Styles
            CellStyle defaultDataStyle = workbook.createCellStyle();
            defaultDataStyle.setFont(dataFont);
            defaultDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorders(defaultDataStyle);

            CellStyle dateDataStyle = workbook.createCellStyle();
            dateDataStyle.setFont(dataFont);
            dateDataStyle.setDataFormat(dateFormat);
            dateDataStyle.setAlignment(HorizontalAlignment.CENTER);
            dateDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorders(dateDataStyle);

            // Status Styles
            Font activeFont = workbook.createFont();
            activeFont.setFontName("Calibri");
            activeFont.setBold(true);
            activeFont.setFontHeightInPoints((short) 9);
            activeFont.setColor(IndexedColors.DARK_GREEN.getIndex());

            CellStyle activeStatusStyle = workbook.createCellStyle();
            activeStatusStyle.setFont(activeFont);
            activeStatusStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            activeStatusStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            activeStatusStyle.setAlignment(HorizontalAlignment.CENTER);
            activeStatusStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorders(activeStatusStyle);

            Font inactiveFont = workbook.createFont();
            inactiveFont.setFontName("Calibri");
            inactiveFont.setBold(true);
            inactiveFont.setFontHeightInPoints((short) 9);
            inactiveFont.setColor(IndexedColors.DARK_RED.getIndex());

            CellStyle inactiveStatusStyle = workbook.createCellStyle();
            inactiveStatusStyle.setFont(inactiveFont);
            inactiveStatusStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            inactiveStatusStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            inactiveStatusStyle.setAlignment(HorizontalAlignment.CENTER);
            inactiveStatusStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorders(inactiveStatusStyle);

            int totalColumns = HEADERS.length;

            // 1. Create Title Banner (Row 0)
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(32);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BHASHYAM EDUCATIONAL INSTITUTION - STUDENTS DIRECTORY");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, totalColumns - 1));

            // 2. Create Header Row (Row 1)
            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(26);
            for (int col = 0; col < totalColumns; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(HEADERS[col]);
                cell.setCellStyle(headerStyle);
            }

            // 3. Freeze top 2 rows (Title + Header)
            sheet.createFreezePane(0, 2);

            // 4. Populate Student Data Rows (Row 2 onwards)
            int rowIdx = 2;
            if (students != null) {
                for (Student student : students) {
                    Row row = sheet.createRow(rowIdx++);
                    row.setHeightInPoints(20);

                    StudentContactDetail contact = student.getContactDetail();
                    StudentAcademicDetail academic = student.getAcademicDetail();
                    StudentParentDetail parent = student.getParentDetail();

                    int col = 0;

                    // --- 1. Identifiers & Personal Details ---
                    createCell(row, col++, student.getStudentId(), defaultDataStyle);
                    createCell(row, col++, student.getAdmissionNumber(), defaultDataStyle);
                    createCell(row, col++, student.getRollNumber(), defaultDataStyle);
                    createCell(row, col++, student.getFirstName(), defaultDataStyle);
                    createCell(row, col++, student.getMiddleName(), defaultDataStyle);
                    createCell(row, col++, student.getLastName(), defaultDataStyle);
                    createCell(row, col++, student.getFullName(), defaultDataStyle);
                    createCell(row, col++, student.getGender(), defaultDataStyle);
                    createDateCell(row, col++, student.getDateOfBirth(), dateDataStyle);
                    createCell(row, col++, student.getBloodGroup(), defaultDataStyle);
                    createCell(row, col++, student.getNationality() != null ? student.getNationality() : "Indian", defaultDataStyle);
                    createCell(row, col++, student.getReligion(), defaultDataStyle);
                    createCell(row, col++, student.getCasteCategory(), defaultDataStyle);
                    createCell(row, col++, student.getAadhaarNumber(), defaultDataStyle);
                    createCell(row, col++, student.getPanNumber(), defaultDataStyle);
                    createCell(row, col++, student.getIdentificationMarks(), defaultDataStyle);

                    // Status
                    StudentStatus status = student.getStatus();
                    String statusStr = status != null ? status.name() : "ACTIVE";
                    CellStyle statusStyle = "ACTIVE".equalsIgnoreCase(statusStr) ? activeStatusStyle : inactiveStatusStyle;
                    createCell(row, col++, statusStr, statusStyle);

                    // --- 2. Contact & Address Details ---
                    createCell(row, col++, contact != null ? contact.getMobileNumber() : "", defaultDataStyle);
                    createCell(row, col++, contact != null ? contact.getAlternateMobile() : "", defaultDataStyle);
                    createCell(row, col++, contact != null ? contact.getEmail() : "", defaultDataStyle);
                    createCell(row, col++, contact != null ? contact.getAddress() : "", defaultDataStyle);
                    createCell(row, col++, contact != null ? contact.getCity() : "", defaultDataStyle);
                    createCell(row, col++, contact != null ? contact.getDistrict() : "", defaultDataStyle);
                    createCell(row, col++, contact != null ? contact.getState() : "", defaultDataStyle);
                    createCell(row, col++, contact != null ? contact.getPinCode() : "", defaultDataStyle);
                    createCell(row, col++, contact != null ? contact.getCountry() : "India", defaultDataStyle);

                    // --- 3. Parent Details ---
                    createCell(row, col++, parent != null ? parent.getFatherName() : "", defaultDataStyle);
                    createCell(row, col++, parent != null ? parent.getMotherName() : "", defaultDataStyle);
                    createCell(row, col++, parent != null ? parent.getParentMobile() : "", defaultDataStyle);
                    createCell(row, col++, parent != null ? parent.getParentEmail() : "", defaultDataStyle);
                    createCell(row, col++, parent != null ? parent.getOccupation() : "", defaultDataStyle);
                    createCell(row, col++, parent != null && parent.getAnnualIncome() != null ? parent.getAnnualIncome().toPlainString() : "", defaultDataStyle);

                    // --- 4. Academic Details ---
                    createCell(row, col++, academic != null ? academic.getAcademicYear() : "", defaultDataStyle);
                    createCell(row, col++, academic != null ? academic.getDepartment() : "General", defaultDataStyle);
                    createCell(row, col++, academic != null ? academic.getBranchGroup() : "", defaultDataStyle);
                    createCell(row, col++, academic != null ? academic.getIntermediateYear() : "", defaultDataStyle);
                    createCell(row, col++, (academic != null && academic.getSemester() != null) ? String.valueOf(academic.getSemester()) : "", defaultDataStyle);
                    createCell(row, col++, academic != null ? academic.getSection() : "", defaultDataStyle);
                    createCell(row, col++, academic != null ? academic.getBatch() : "", defaultDataStyle);
                    createDateCell(row, col++, academic != null ? academic.getAdmissionDate() : null, dateDataStyle);
                    createCell(row, col++, academic != null ? academic.getAdmissionType() : "REGULAR", defaultDataStyle);
                    createCell(row, col++, academic != null ? academic.getHostelDayScholar() : "DAY_SCHOLAR", defaultDataStyle);
                    createCell(row, col++, academic != null ? academic.getMedium() : "English", defaultDataStyle);
                    createCell(row, col++, academic != null ? academic.getRegulation() : "", defaultDataStyle);
                    createCell(row, col++, academic != null ? academic.getUniversityId() : "", defaultDataStyle);
                }
            }

            // 5. Auto-size all columns with minimum and maximum bounds
            for (int col = 0; col < totalColumns; col++) {
                sheet.autoSizeColumn(col);
                int currentWidth = sheet.getColumnWidth(col);
                int minWidth = 14 * 256; // 14 characters
                int maxWidth = 40 * 256; // 40 characters
                if (currentWidth < minWidth) {
                    sheet.setColumnWidth(col, minWidth);
                } else if (currentWidth > maxWidth) {
                    sheet.setColumnWidth(col, maxWidth);
                }
            }

            // Write workbook to output stream
            workbook.write(outputStream);
            workbook.dispose(); // clean up temporary streaming disk files
        }
    }

    private static void setThinBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
    }

    private static void createCell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private static void createDateCell(Row row, int colIndex, LocalDate date, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        if (date != null) {
            cell.setCellValue(date.format(DATE_FORMATTER));
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }
}
