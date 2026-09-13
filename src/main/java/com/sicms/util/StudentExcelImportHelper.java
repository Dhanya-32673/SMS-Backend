package com.sicms.util;

import com.sicms.dto.StudentImportRowDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Utility class for parsing student import Excel files, generating import templates,
 * and generating error report workbooks based on the official 22 canonical columns.
 */
public class StudentExcelImportHelper {

    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DD_SLASH_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Official 22 canonical headers matching the approved Excel template
    public static final String[] TEMPLATE_HEADERS = {
        "Student ID",
        "Admission Number",
        "Full Name *",
        "Gender *",
        "Date of Birth *",
        "Nationality",
        "Religion",
        "Category",
        "Aadhaar Number",
        "Profile Photo URL",
        "Mobile Number *",
        "Alternate Mobile",
        "Email Address - 1 *",
        "Email Address - 2 *",
        "Father Name *",
        "Mother Name *",
        "Academic Year *",
        "Branch / Group *",
        "Intermediate Year *",
        "Batch *",
        "Admission Type",
        "Hostel / Day Scholar *"
    };

    public static final String[] SAMPLE_VALUES = {
        "",                             // 1. Student ID (blank for auto-generation)
        "ADM2026001",                   // 2. Admission Number
        "Rahul Kumar Sharma",           // 3. Full Name
        "MALE",                         // 4. Gender
        "15-06-2008",                   // 5. Date of Birth
        "Indian",                       // 6. Nationality
        "Hindu",                        // 7. Religion
        "OC",                           // 8. Category
        "987654321012",                 // 9. Aadhaar Number
        "",                             // 10. Profile Photo URL
        "9876543210",                   // 11. Mobile Number
        "9123456780",                   // 12. Alternate Mobile
        "rahul.sharma@example.com",     // 13. Email Address - 1
        "rahul.alt@example.com",        // 14. Email Address - 2
        "Suresh Sharma",                // 15. Father Name
        "Sunita Sharma",                // 16. Mother Name
        "2026-2027",                    // 17. Academic Year
        "MPC",                          // 18. Branch / Group
        "1st Year",                     // 19. Intermediate Year
        "2026-2028",                    // 20. Batch
        "REGULAR",                      // 21. Admission Type
        "DAY_SCHOLAR"                   // 22. Hostel / Day Scholar
    };

    private static final Set<String> SUPPORTED_NORMALIZED_HEADERS = Set.of(
        "studentid", "admissionnumber", "fullname", "gender", "dateofbirth",
        "nationality", "religion", "category", "aadhaarnumber", "profilephotourl",
        "mobilenumber", "alternatemobile", "emailaddress1", "emailaddress2",
        "fathername", "mothername", "academicyear", "branchgroup",
        "intermediateyear", "batch", "admissiontype", "hosteldayscholar"
    );

    private static final Set<String> SUPPORTED_ALIASES = Set.of(
        "mobile", "altmobile", "email1", "email2", "emailaddress", "email",
        "dob", "caste", "photo", "photourl", "group", "branch", "year", "hostel"
    );

    /**
     * Generates a formatted student import template with instruction header,
     * column headers, and a sample row.
     */
    public static void generateTemplate(OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Student Import Template");

            // Fonts
            Font titleFont = workbook.createFont();
            titleFont.setFontName("Calibri");
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);
            titleFont.setColor(IndexedColors.WHITE.getIndex());

            Font noteFont = workbook.createFont();
            noteFont.setFontName("Calibri");
            noteFont.setItalic(true);
            noteFont.setFontHeightInPoints((short) 9);
            noteFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            Font reqHeaderFont = workbook.createFont();
            reqHeaderFont.setFontName("Calibri");
            reqHeaderFont.setBold(true);
            reqHeaderFont.setFontHeightInPoints((short) 10);
            reqHeaderFont.setColor(IndexedColors.WHITE.getIndex());

            Font optHeaderFont = workbook.createFont();
            optHeaderFont.setFontName("Calibri");
            optHeaderFont.setBold(true);
            optHeaderFont.setFontHeightInPoints((short) 10);
            optHeaderFont.setColor(IndexedColors.WHITE.getIndex());

            Font sampleFont = workbook.createFont();
            sampleFont.setFontName("Calibri");
            sampleFont.setFontHeightInPoints((short) 10);
            sampleFont.setColor(IndexedColors.GREY_80_PERCENT.getIndex());

            // Styles
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle noteStyle = workbook.createCellStyle();
            noteStyle.setFont(noteFont);
            noteStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            noteStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            noteStyle.setAlignment(HorizontalAlignment.LEFT);
            noteStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Required headers (Royal Blue)
            CellStyle reqHeaderStyle = workbook.createCellStyle();
            reqHeaderStyle.setFont(reqHeaderFont);
            reqHeaderStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            reqHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            reqHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            reqHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorders(reqHeaderStyle);

            // Optional headers (Steel Blue / Slate)
            CellStyle optHeaderStyle = workbook.createCellStyle();
            optHeaderStyle.setFont(optHeaderFont);
            optHeaderStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            optHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            optHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            optHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorders(optHeaderStyle);

            CellStyle sampleStyle = workbook.createCellStyle();
            sampleStyle.setFont(sampleFont);
            sampleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorders(sampleStyle);

            int totalCols = TEMPLATE_HEADERS.length;

            // Row 0: Title Banner
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(30);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BHASHYAM IIT JEE ACADEMY — STUDENT REGISTRATION IMPORT TEMPLATE");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, totalCols - 1));

            // Row 1: Note Banner
            Row noteRow = sheet.createRow(1);
            noteRow.setHeightInPoints(22);
            Cell noteCell = noteRow.createCell(0);
            noteCell.setCellValue("INSTRUCTIONS: Columns with '*' are required. Date format: DD-MM-YYYY or YYYY-MM-DD. Aadhaar must be exactly 12 digits. Do not add, remove, or rename column headers.");
            noteCell.setCellStyle(noteStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, totalCols - 1));

            // Row 2: Headers
            Row headerRow = sheet.createRow(2);
            headerRow.setHeightInPoints(26);
            for (int col = 0; col < totalCols; col++) {
                Cell cell = headerRow.createCell(col);
                String headerName = TEMPLATE_HEADERS[col];
                cell.setCellValue(headerName);
                cell.setCellStyle(headerName.contains("*") ? reqHeaderStyle : optHeaderStyle);
            }

            // Row 3: Sample Row
            Row sampleRow = sheet.createRow(3);
            sampleRow.setHeightInPoints(22);
            for (int col = 0; col < SAMPLE_VALUES.length && col < totalCols; col++) {
                Cell cell = sampleRow.createCell(col);
                cell.setCellValue(SAMPLE_VALUES[col]);
                cell.setCellStyle(sampleStyle);
            }

            // Freeze panes at row 3 (after title, note, and headers)
            sheet.createFreezePane(0, 3);

            // Auto-size columns with bounds
            for (int col = 0; col < totalCols; col++) {
                sheet.autoSizeColumn(col);
                int width = sheet.getColumnWidth(col);
                if (width < 14 * 256) sheet.setColumnWidth(col, 14 * 256);
                else if (width > 35 * 256) sheet.setColumnWidth(col, 35 * 256);
            }

            workbook.write(outputStream);
        }
    }

    /**
     * Parses an uploaded Excel file (.xlsx) into a list of StudentImportRowDto objects.
     */
    public static List<StudentImportRowDto> parseExcel(MultipartFile file, List<String> headerErrorsOut) throws IOException {
        List<StudentImportRowDto> rows = new ArrayList<>();
        DataFormatter dataFormatter = new DataFormatter();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            if (workbook.getNumberOfSheets() == 0) {
                headerErrorsOut.add("Excel file contains no sheets.");
                return rows;
            }

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() == 0) {
                headerErrorsOut.add("Excel file is empty.");
                return rows;
            }

            // 1. Locate header row (search first 10 rows for "Full Name" or "Admission Number")
            int headerRowIdx = -1;
            Row headerRow = null;
            Map<String, Integer> colMap = new HashMap<>();

            for (int r = 0; r <= Math.min(sheet.getLastRowNum(), 10); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, Integer> tempMap = mapHeaders(row, dataFormatter);
                if (tempMap.containsKey("fullname") || (tempMap.containsKey("admissionnumber") && tempMap.containsKey("gender"))) {
                    headerRowIdx = r;
                    headerRow = row;
                    colMap = tempMap;
                    break;
                }
            }

            if (headerRow == null) {
                headerErrorsOut.add("Could not find a valid student header row. Please use the official 22-column Student Registration Template.");
                return rows;
            }

            // 2. Validate that no unsupported or obsolete columns are present
            validateSupportedHeaders(headerRow, dataFormatter, headerErrorsOut);

            // 3. Validate required columns presence
            validateRequiredHeaders(colMap, headerErrorsOut);
            if (!headerErrorsOut.isEmpty()) {
                return rows;
            }

            // 4. Iterate data rows
            int lastRowNum = sheet.getLastRowNum();
            for (int r = headerRowIdx + 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowBlank(row, dataFormatter)) {
                    continue; // skip blank rows
                }

                StudentImportRowDto dto = new StudentImportRowDto();
                dto.setRowNumber(r + 1); // 1-based row number

                // Read official 22 fields
                dto.setStudentId(getString(row, colMap, "studentid", dataFormatter));
                dto.setAdmissionNumber(getString(row, colMap, "admissionnumber", dataFormatter, "admissionno", "admno"));
                dto.setFullName(getString(row, colMap, "fullname", dataFormatter, "name", "studentname"));
                dto.setGender(getString(row, colMap, "gender", dataFormatter));
                dto.setDateOfBirth(getDate(row, colMap, "dateofbirth", dataFormatter, "dob"));
                dto.setNationality(getStringOrDefault(row, colMap, "nationality", dataFormatter, "Indian"));
                dto.setReligion(getString(row, colMap, "religion", dataFormatter));
                dto.setCategory(getString(row, colMap, "category", dataFormatter, "caste"));
                dto.setAadhaarNumber(getString(row, colMap, "aadhaarnumber", dataFormatter, "aadhaar"));
                dto.setProfilePhotoUrl(getString(row, colMap, "profilephotourl", dataFormatter, "photo", "photourl"));
                dto.setMobileNumber(getString(row, colMap, "mobilenumber", dataFormatter, "mobile"));
                dto.setAlternateMobile(getString(row, colMap, "alternatemobile", dataFormatter, "altmobile"));
                dto.setEmailAddress1(getString(row, colMap, "emailaddress1", dataFormatter, "email1", "emailaddress", "email"));
                dto.setEmailAddress2(getString(row, colMap, "emailaddress2", dataFormatter, "email2"));
                dto.setFatherName(getString(row, colMap, "fathername", dataFormatter, "father"));
                dto.setMotherName(getString(row, colMap, "mothername", dataFormatter, "mother"));
                dto.setAcademicYear(getString(row, colMap, "academicyear", dataFormatter));
                dto.setBranchGroup(getString(row, colMap, "branchgroup", dataFormatter, "group", "branch"));
                dto.setIntermediateYear(getString(row, colMap, "intermediateyear", dataFormatter, "year"));
                dto.setBatch(getString(row, colMap, "batch", dataFormatter));
                dto.setAdmissionType(getStringOrDefault(row, colMap, "admissiontype", dataFormatter, "REGULAR"));
                dto.setHostelDayScholar(getStringOrDefault(row, colMap, "hosteldayscholar", dataFormatter, "DAY_SCHOLAR", "hostel"));
                String campusVal = getString(row, colMap, "campus", dataFormatter, "campusname", "location");
                dto.setCampus(campusVal);

                if (campusVal != null && !campusVal.isBlank()) {
                    boolean isValidCampus = com.sicms.service.CampusService.OFFICIAL_CAMPUS_LIST.stream()
                            .anyMatch(c -> c.equalsIgnoreCase(campusVal.trim()));
                    if (!isValidCampus) {
                        dto.getErrors().add("Invalid campus '" + campusVal.trim() + "'. Allowed campuses are the configured campus master values.");
                        dto.setStatus("ERROR");
                    }
                }

                rows.add(dto);
            }
        }

        return rows;
    }

    /**
     * Generates an Error Report Excel workbook for failed/skipped rows.
     */
    public static void generateErrorReport(List<StudentImportRowDto> failedRows, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Import Error Report");

            Font titleFont = workbook.createFont();
            titleFont.setFontName("Calibri");
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 12);
            titleFont.setColor(IndexedColors.WHITE.getIndex());

            Font headerFont = workbook.createFont();
            headerFont.setFontName("Calibri");
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 10);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            Font cellFont = workbook.createFont();
            cellFont.setFontName("Calibri");
            cellFont.setFontHeightInPoints((short) 10);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.MAROON.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorders(headerStyle);

            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setFont(cellFont);
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorders(cellStyle);

            CellStyle errorStyle = workbook.createCellStyle();
            errorStyle.setFont(cellFont);
            errorStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            errorStyle.setWrapText(true);
            setThinBorders(errorStyle);

            String[] errHeaders = {"Row #", "Status", "Student ID", "Admission Number", "Student Name", "Group", "Year", "Contact", "Errors / Reason"};

            // Row 0: Title Banner
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BHASHYAM IIT JEE ACADEMY — STUDENT IMPORT ERROR REPORT");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, errHeaders.length - 1));

            // Row 1: Headers
            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(24);
            for (int col = 0; col < errHeaders.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(errHeaders[col]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 2;
            if (failedRows != null) {
                for (StudentImportRowDto dto : failedRows) {
                    Row row = sheet.createRow(rowIdx++);
                    row.setHeightInPoints(22);

                    int col = 0;
                    createCell(row, col++, String.valueOf(dto.getRowNumber()), cellStyle);
                    createCell(row, col++, dto.getStatus(), cellStyle);
                    createCell(row, col++, dto.getStudentId(), cellStyle);
                    createCell(row, col++, dto.getAdmissionNumber(), cellStyle);
                    createCell(row, col++, dto.getFullName(), cellStyle);
                    createCell(row, col++, dto.getBranchGroup(), cellStyle);
                    createCell(row, col++, dto.getIntermediateYear(), cellStyle);
                    createCell(row, col++, dto.getMobileNumber(), cellStyle);

                    String allErrors = String.join("; ", dto.getErrors());
                    createCell(row, col++, allErrors, errorStyle);
                }
            }

            sheet.createFreezePane(0, 2);

            for (int col = 0; col < errHeaders.length; col++) {
                sheet.autoSizeColumn(col);
                int width = sheet.getColumnWidth(col);
                if (width < 12 * 256) sheet.setColumnWidth(col, 12 * 256);
                else if (width > 60 * 256) sheet.setColumnWidth(col, 60 * 256);
            }

            workbook.write(outputStream);
        }
    }

    private static Map<String, Integer> mapHeaders(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> map = new HashMap<>();
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            if (cell == null) continue;
            String text = formatter.formatCellValue(cell).trim();
            if (!text.isEmpty()) {
                String normalized = text.toLowerCase()
                        .replaceAll("[^a-z0-9]", ""); // strip spaces, asterisks, slashes, punctuation
                map.put(normalized, c);
            }
        }
        return map;
    }

    private static void validateSupportedHeaders(Row headerRow, DataFormatter formatter, List<String> errors) {
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            if (cell == null) continue;
            String rawText = formatter.formatCellValue(cell).trim();
            if (rawText.isEmpty()) continue;
            String normalized = rawText.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (!SUPPORTED_NORMALIZED_HEADERS.contains(normalized) && !SUPPORTED_ALIASES.contains(normalized)) {
                errors.add("Unsupported or obsolete column detected: '" + rawText + "'. Please use the official 22-column Student Registration Template.");
            }
        }
    }

    private static void validateRequiredHeaders(Map<String, Integer> colMap, List<String> errors) {
        String[][] requiredGroups = {
            {"fullname", "Full Name"},
            {"gender", "Gender"},
            {"dateofbirth", "Date of Birth"},
            {"mobilenumber", "Mobile Number"},
            {"emailaddress1", "Email Address - 1"},
            {"emailaddress2", "Email Address - 2"},
            {"fathername", "Father Name"},
            {"mothername", "Mother Name"},
            {"academicyear", "Academic Year"},
            {"branchgroup", "Branch / Group"},
            {"intermediateyear", "Intermediate Year"},
            {"batch", "Batch"},
            {"hosteldayscholar", "Hostel / Day Scholar"}
        };

        for (String[] req : requiredGroups) {
            String key = req[0];
            String label = req[1];
            boolean found = colMap.containsKey(key);
            if (!found) {
                if ("mobilenumber".equals(key) && colMap.containsKey("mobile")) found = true;
                else if ("emailaddress1".equals(key) && (colMap.containsKey("email1") || colMap.containsKey("emailaddress") || colMap.containsKey("email"))) found = true;
                else if ("emailaddress2".equals(key) && colMap.containsKey("email2")) found = true;
                else if ("dateofbirth".equals(key) && colMap.containsKey("dob")) found = true;
                else if ("branchgroup".equals(key) && (colMap.containsKey("group") || colMap.containsKey("branch"))) found = true;
                else if ("intermediateyear".equals(key) && colMap.containsKey("year")) found = true;
                else if ("hosteldayscholar".equals(key) && colMap.containsKey("hostel")) found = true;
            }
            if (!found) {
                errors.add("Missing required column: '" + label + "'");
            }
        }
    }

    private static String getString(Row row, Map<String, Integer> colMap, String key, DataFormatter formatter, String... fallbacks) {
        Integer colIdx = colMap.get(key);
        if (colIdx == null && fallbacks != null) {
            for (String fb : fallbacks) {
                colIdx = colMap.get(fb);
                if (colIdx != null) break;
            }
        }
        if (colIdx == null) return null;
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;

        // Prevent scientific notation on numeric cells (e.g. Aadhaar Number, Admission Number)
        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            double numericVal = cell.getNumericCellValue();
            if (numericVal == Math.floor(numericVal) && !Double.isInfinite(numericVal)) {
                return BigDecimal.valueOf(numericVal).toPlainString();
            }
        }

        String val = formatter.formatCellValue(cell).trim();
        return val.isEmpty() ? null : val;
    }

    private static String getStringOrDefault(Row row, Map<String, Integer> colMap, String key, DataFormatter formatter, String defVal, String... fallbacks) {
        String val = getString(row, colMap, key, formatter, fallbacks);
        return (val == null || val.isBlank()) ? defVal : val;
    }

    private static LocalDate getDate(Row row, Map<String, Integer> colMap, String key, DataFormatter formatter, String... fallbacks) {
        Integer colIdx = colMap.get(key);
        if (colIdx == null && fallbacks != null) {
            for (String fb : fallbacks) {
                colIdx = colMap.get(fb);
                if (colIdx != null) break;
            }
        }
        if (colIdx == null) return null;
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;

        // 1. POI Native Date cell
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date d = cell.getDateCellValue();
            if (d != null) {
                return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
        }

        // 2. String Date Parsing
        String str = formatter.formatCellValue(cell).trim();
        if (str.isEmpty()) return null;

        List<DateTimeFormatter> formatters = List.of(
            DD_MM_YYYY,
            YYYY_MM_DD,
            DD_SLASH_MM_YYYY,
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy")
        );

        for (DateTimeFormatter dtf : formatters) {
            try {
                return LocalDate.parse(str, dtf);
            } catch (DateTimeParseException ignored) {}
        }

        return null;
    }

    private static boolean isRowBlank(Row row, DataFormatter formatter) {
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !formatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void createCell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
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
}
