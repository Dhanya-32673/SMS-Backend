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
 * and generating error report workbooks.
 */
public class StudentExcelImportHelper {

    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DD_SLASH_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Canonical headers for template and export compatibility
    public static final String[] TEMPLATE_HEADERS = {
        // 1. Identifiers & Personal Details (0 - 17)
        "Student ID", "Admission Number", "Roll Number *",
        "First Name *", "Middle Name", "Last Name *", "Full Name",
        "Gender *", "Date of Birth *", "Blood Group",
        "Nationality", "Religion", "Category",
        "Aadhaar Number", "PAN Number", "Identification Marks", "Profile Photo URL", "Status",

        // 2. Contact & Address Details (18 - 26)
        "Mobile Number *", "Alternate Mobile", "Email Address *",
        "Address *", "City *", "District *", "State *", "PIN Code *", "Country *",

        // 3. Parent & Guardian Details (27 - 36)
        "Father Name *", "Mother Name *", "Parent Mobile *",
        "Parent Email", "Occupation", "Annual Income",
        "Guardian Name", "Guardian Mobile", "Guardian Relation", "Guardian Address",

        // 4. Academic Details (37 - 49)
        "Academic Year *", "Department", "Branch / Group *",
        "Intermediate Year *", "Semester", "Section *",
        "Batch *", "Admission Date *", "Admission Type",
        "Hostel / Day Scholar *", "Medium", "Regulation", "University / Board ID"
    };

    /**
     * Generates a beautifully formatted student import template with instruction header,
     * column headers, and a realistic sample row.
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
            titleCell.setCellValue("BHASHYAM EDUCATIONAL INSTITUTION — STUDENT REGISTRATION IMPORT TEMPLATE");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, totalCols - 1));

            // Row 1: Note Banner
            Row noteRow = sheet.createRow(1);
            noteRow.setHeightInPoints(22);
            Cell noteCell = noteRow.createCell(0);
            noteCell.setCellValue("INSTRUCTIONS: Columns with '*' are required. Date format: DD-MM-YYYY or YYYY-MM-DD. Roll numbers, mobile numbers & Aadhaar preserve leading zeros. Do not rename column headers.");
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

            // Row 3: Realistic Sample Row
            Row sampleRow = sheet.createRow(3);
            sampleRow.setHeightInPoints(22);
            String[] sampleValues = {
                // 1. Personal Details
                "", "ADM2026001", "26INT101",
                "Rahul", "Kumar", "Sharma", "Rahul Kumar Sharma",
                "MALE", "15-06-2008", "O+",
                "Indian", "Hindu", "General",
                "987654321012", "ABCDE1234F", "Mole on right cheek", "", "ACTIVE",

                // 2. Contact Details
                "9876543210", "9123456780", "rahul.sharma@example.com",
                "Plot 42, Nagarjuna Nagar", "Guntur", "Guntur", "Andhra Pradesh", "522001", "India",

                // 3. Parent Details
                "Suresh Sharma", "Sunita Sharma", "9876543211",
                "suresh.sharma@example.com", "Business", "600000",
                "Ramesh Sharma", "9876543212", "Uncle", "Guntur",

                // 4. Academic Details
                "2026-2027", "General", "MPC",
                "1st Year", "1", "A",
                "2026-2028", "10-06-2026", "REGULAR",
                "DAY_SCHOLAR", "English", "CBSE", "UNIV-2026"
            };

            for (int col = 0; col < sampleValues.length && col < totalCols; col++) {
                Cell cell = sampleRow.createCell(col);
                cell.setCellValue(sampleValues[col]);
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

            // 1. Locate header row (search first 10 rows for "Roll Number" or "First Name")
            int headerRowIdx = -1;
            Row headerRow = null;
            Map<String, Integer> colMap = new HashMap<>();

            for (int r = 0; r <= Math.min(sheet.getLastRowNum(), 10); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, Integer> tempMap = mapHeaders(row, dataFormatter);
                if (tempMap.containsKey("rollnumber") || tempMap.containsKey("firstname")) {
                    headerRowIdx = r;
                    headerRow = row;
                    colMap = tempMap;
                    break;
                }
            }

            if (headerRow == null) {
                headerErrorsOut.add("Could not find a valid student header row. Please use the official Student Import Template.");
                return rows;
            }

            // Validate required columns presence
            validateRequiredHeaders(colMap, headerErrorsOut);
            if (!headerErrorsOut.isEmpty()) {
                return rows;
            }

            // 2. Iterate data rows
            int lastRowNum = sheet.getLastRowNum();
            for (int r = headerRowIdx + 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowBlank(row, dataFormatter)) {
                    continue; // skip blank rows
                }

                StudentImportRowDto dto = new StudentImportRowDto();
                dto.setRowNumber(r + 1); // 1-based row number

                // Read Personal Details
                dto.setStudentId(getString(row, colMap, "studentid", dataFormatter));
                dto.setAdmissionNumber(getString(row, colMap, "admissionnumber", dataFormatter));
                dto.setRollNumber(getString(row, colMap, "rollnumber", dataFormatter));
                dto.setFirstName(getString(row, colMap, "firstname", dataFormatter));
                dto.setMiddleName(getString(row, colMap, "middlename", dataFormatter));
                dto.setLastName(getString(row, colMap, "lastname", dataFormatter));
                dto.setFullName(getString(row, colMap, "fullname", dataFormatter));
                dto.setGender(getString(row, colMap, "gender", dataFormatter));
                dto.setDateOfBirth(getDate(row, colMap, "dateofbirth", dataFormatter));
                dto.setBloodGroup(getString(row, colMap, "bloodgroup", dataFormatter));
                dto.setNationality(getStringOrDefault(row, colMap, "nationality", dataFormatter, "Indian"));
                dto.setReligion(getString(row, colMap, "religion", dataFormatter));
                dto.setCasteCategory(getString(row, colMap, "category", dataFormatter, "castecategory"));
                dto.setAadhaarNumber(getString(row, colMap, "aadhaarnumber", dataFormatter, "aadhaar"));
                dto.setPanNumber(getString(row, colMap, "pannumber", dataFormatter, "pan"));
                dto.setIdentificationMarks(getString(row, colMap, "identificationmarks", dataFormatter));
                dto.setProfilePhotoUrl(getString(row, colMap, "profilephotourl", dataFormatter, "photo"));
                dto.setStudentStatus(getStringOrDefault(row, colMap, "status", dataFormatter, "ACTIVE"));

                // Read Contact Details
                dto.setMobileNumber(getString(row, colMap, "mobilenumber", dataFormatter, "mobile"));
                dto.setAlternateMobile(getString(row, colMap, "alternatemobile", dataFormatter));
                dto.setEmail(getString(row, colMap, "emailaddress", dataFormatter, "email"));
                dto.setAddress(getString(row, colMap, "address", dataFormatter, "residentialaddress"));
                dto.setCity(getStringOrDefault(row, colMap, "city", dataFormatter, "Hyderabad"));
                dto.setDistrict(getStringOrDefault(row, colMap, "district", dataFormatter, "Hyderabad"));
                dto.setState(getStringOrDefault(row, colMap, "state", dataFormatter, "Telangana"));
                dto.setPinCode(getStringOrDefault(row, colMap, "pincode", dataFormatter, "500001"));
                dto.setCountry(getStringOrDefault(row, colMap, "country", dataFormatter, "India"));

                // Read Parent & Guardian Details
                dto.setFatherName(getString(row, colMap, "fathername", dataFormatter));
                dto.setMotherName(getString(row, colMap, "mothername", dataFormatter));
                dto.setParentMobile(getString(row, colMap, "parentmobile", dataFormatter));
                dto.setParentEmail(getString(row, colMap, "parentemail", dataFormatter));
                dto.setOccupation(getString(row, colMap, "occupation", dataFormatter));
                dto.setAnnualIncome(getBigDecimal(row, colMap, "annualincome", dataFormatter));
                dto.setGuardianName(getString(row, colMap, "guardianname", dataFormatter));
                dto.setGuardianMobile(getString(row, colMap, "guardianmobile", dataFormatter));
                dto.setGuardianRelation(getString(row, colMap, "guardianrelation", dataFormatter));
                dto.setGuardianAddress(getString(row, colMap, "guardianaddress", dataFormatter));

                // Read Academic Details
                dto.setAcademicYear(getString(row, colMap, "academicyear", dataFormatter));
                dto.setDepartment(getStringOrDefault(row, colMap, "department", dataFormatter, "General"));
                dto.setBranchGroup(getString(row, colMap, "branchgroup", dataFormatter, "group"));
                dto.setIntermediateYear(getString(row, colMap, "intermediateyear", dataFormatter, "year"));
                dto.setSemester(getInteger(row, colMap, "semester", dataFormatter));
                dto.setSection(getString(row, colMap, "section", dataFormatter));
                dto.setBatch(getString(row, colMap, "batch", dataFormatter));
                dto.setAdmissionDate(getDate(row, colMap, "admissiondate", dataFormatter));
                dto.setAdmissionType(getStringOrDefault(row, colMap, "admissiontype", dataFormatter, "REGULAR"));
                dto.setHostelDayScholar(getStringOrDefault(row, colMap, "hosteldayscholar", dataFormatter, "DAY_SCHOLAR"));
                dto.setMedium(getStringOrDefault(row, colMap, "medium", dataFormatter, "English"));
                dto.setRegulation(getString(row, colMap, "regulation", dataFormatter));
                dto.setUniversityId(getString(row, colMap, "universityboardid", dataFormatter, "universityid"));

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

            String[] errHeaders = {"Row #", "Status", "Roll Number", "Student Name", "Group", "Year", "Section", "Errors / Reason"};

            // Row 0: Title Banner
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BHASHYAM EDUCATIONAL INSTITUTION — STUDENT IMPORT ERROR REPORT");
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
                    createCell(row, col++, dto.getRollNumber(), cellStyle);
                    createCell(row, col++, dto.getFullName(), cellStyle);
                    createCell(row, col++, dto.getBranchGroup(), cellStyle);
                    createCell(row, col++, dto.getIntermediateYear(), cellStyle);
                    createCell(row, col++, dto.getSection(), cellStyle);

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

    private static void validateRequiredHeaders(Map<String, Integer> colMap, List<String> errors) {
        String[][] requiredGroups = {
            {"rollnumber", "Roll Number"},
            {"firstname", "First Name"},
            {"lastname", "Last Name"},
            {"gender", "Gender"},
            {"dateofbirth", "Date of Birth"},
            {"mobilenumber", "Mobile Number"},
            {"emailaddress", "Email Address"},
            {"address", "Address"},
            {"fathername", "Father Name"},
            {"mothername", "Mother Name"},
            {"parentmobile", "Parent Mobile"},
            {"branchgroup", "Branch / Group"},
            {"intermediateyear", "Intermediate Year"},
            {"section", "Section"},
            {"batch", "Batch"},
            {"academicyear", "Academic Year"},
            {"admissiondate", "Admission Date"},
            {"hosteldayscholar", "Hostel / Day Scholar"}
        };

        for (String[] req : requiredGroups) {
            String key = req[0];
            String label = req[1];
            boolean found = colMap.containsKey(key);
            if (!found) {
                // Secondary fallback keys
                if ("mobilenumber".equals(key) && colMap.containsKey("mobile")) found = true;
                else if ("emailaddress".equals(key) && colMap.containsKey("email")) found = true;
                else if ("branchgroup".equals(key) && colMap.containsKey("group")) found = true;
                else if ("intermediateyear".equals(key) && colMap.containsKey("year")) found = true;
                else if ("address".equals(key) && colMap.containsKey("residentialaddress")) found = true;
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

    private static BigDecimal getBigDecimal(Row row, Map<String, Integer> colMap, String key, DataFormatter formatter, String... fallbacks) {
        String str = getString(row, colMap, key, formatter, fallbacks);
        if (str == null || str.isBlank()) return null;
        try {
            // Strip any currency symbols or commas
            String clean = str.replaceAll("[^0-9.]", "");
            if (clean.isEmpty()) return null;
            return new BigDecimal(clean);
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer getInteger(Row row, Map<String, Integer> colMap, String key, DataFormatter formatter, String... fallbacks) {
        String str = getString(row, colMap, key, formatter, fallbacks);
        if (str == null || str.isBlank()) return null;
        try {
            String clean = str.replaceAll("[^0-9]", "");
            if (clean.isEmpty()) return null;
            return Integer.parseInt(clean);
        } catch (Exception e) {
            return null;
        }
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
