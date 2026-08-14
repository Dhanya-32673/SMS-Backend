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

import com.sicms.entity.DocumentStatus;
import com.sicms.entity.DocumentType;
import com.sicms.entity.Student;
import com.sicms.entity.StudentAcademicDetail;
import com.sicms.entity.StudentContactDetail;
import com.sicms.entity.StudentDocument;
import com.sicms.entity.StudentGuardian;
import com.sicms.entity.StudentParentDetail;
import com.sicms.entity.StudentStatus;

public class StudentExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private static final String[] HEADERS = {
        // 1. Personal Information (0-25)
        "Student ID", "Admission Number", "Roll Number", "Registration Number",
        "First Name", "Middle Name", "Last Name", "Full Name",
        "Gender", "Date of Birth", "Blood Group", "Aadhaar Number",
        "Nationality", "Category", "Religion", "Caste",
        "Mobile Number", "Alternate Mobile Number", "Email Address",
        "Address Line 1", "Address Line 2", "City", "District",
        "State", "Country", "PIN Code",

        // 2. Academic Information (26-39)
        "Academic Year", "Course", "Department", "Program",
        "Group", "Year", "Semester", "Section",
        "Batch", "Admission Date", "Admission Type", "Previous School/College",
        "Current Status", "Active/Inactive Status",

        // 3. Parent / Guardian Information (40-48)
        "Father Name", "Father Mobile", "Father Occupation",
        "Mother Name", "Mother Mobile", "Mother Occupation",
        "Guardian Name", "Guardian Relation", "Guardian Mobile",

        // 4. Hostel / Transport (49-54)
        "Hostel Required", "Hostel Name", "Room Number",
        "Transport Required", "Bus Route", "Pickup Point",

        // 5. Certificate / Document Information (55-65)
        "SSC Certificate Uploaded", "Intermediate Certificate Uploaded", "Transfer Certificate Uploaded",
        "Caste Certificate Uploaded", "Income Certificate Uploaded", "Aadhaar Uploaded",
        "Photo Uploaded", "Signature Uploaded", "Other Documents Uploaded",
        "Pending Documents Count", "Missing Documents Count",

        // 6. System Information (66-69)
        "Created By", "Created Date", "Last Updated By", "Last Updated Date"
    };

    /**
     * Exports students data into Excel (.xlsx) format using Apache POI streaming.
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

            // CreationHelper for Date formats
            short dateFormat = workbook.getCreationHelper().createDataFormat().getFormat("dd-MM-yyyy");
            short dateTimeFormat = workbook.getCreationHelper().createDataFormat().getFormat("dd-MM-yyyy HH:mm");

            // Fonts
            Font titleFont = workbook.createFont();
            titleFont.setFontName("Calibri");
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.WHITE.getIndex());

            Font headerFont = workbook.createFont();
            headerFont.setFontName("Calibri");
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            Font dataFont = workbook.createFont();
            dataFont.setFontName("Calibri");
            dataFont.setFontHeightInPoints((short) 10);

            // Title Style (Dark Blue Background, White Bold Text)
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            titleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header Style (Light Blue Background, Navy Bold Text, Borders)
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setThinBorders(headerStyle);

            // Regular Data Style
            CellStyle defaultDataStyle = workbook.createCellStyle();
            defaultDataStyle.setFont(dataFont);
            defaultDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorders(defaultDataStyle);

            // Date Data Style
            CellStyle dateDataStyle = workbook.createCellStyle();
            dateDataStyle.setFont(dataFont);
            dateDataStyle.setDataFormat(dateFormat);
            dateDataStyle.setAlignment(HorizontalAlignment.CENTER);
            dateDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorders(dateDataStyle);

            // DateTime Data Style
            CellStyle dateTimeDataStyle = workbook.createCellStyle();
            dateTimeDataStyle.setFont(dataFont);
            dateTimeDataStyle.setDataFormat(dateTimeFormat);
            dateTimeDataStyle.setAlignment(HorizontalAlignment.CENTER);
            dateTimeDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorders(dateTimeDataStyle);

            // Status Styles: Active (Green), Inactive (Red), Pending (Orange)
            Font greenFont = workbook.createFont();
            greenFont.setFontName("Calibri");
            greenFont.setBold(true);
            greenFont.setColor(IndexedColors.DARK_GREEN.getIndex());

            CellStyle activeStatusStyle = workbook.createCellStyle();
            activeStatusStyle.setFont(greenFont);
            activeStatusStyle.setAlignment(HorizontalAlignment.CENTER);
            activeStatusStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            activeStatusStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            activeStatusStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setThinBorders(activeStatusStyle);

            Font redFont = workbook.createFont();
            redFont.setFontName("Calibri");
            redFont.setBold(true);
            redFont.setColor(IndexedColors.DARK_RED.getIndex());

            CellStyle inactiveStatusStyle = workbook.createCellStyle();
            inactiveStatusStyle.setFont(redFont);
            inactiveStatusStyle.setAlignment(HorizontalAlignment.CENTER);
            inactiveStatusStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            inactiveStatusStyle.setFillForegroundColor(IndexedColors.CORAL.getIndex());
            inactiveStatusStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setThinBorders(inactiveStatusStyle);

            Font orangeFont = workbook.createFont();
            orangeFont.setFontName("Calibri");
            orangeFont.setBold(true);
            orangeFont.setColor(IndexedColors.ORANGE.getIndex());

            CellStyle pendingStatusStyle = workbook.createCellStyle();
            pendingStatusStyle.setFont(orangeFont);
            pendingStatusStyle.setAlignment(HorizontalAlignment.CENTER);
            pendingStatusStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            pendingStatusStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
            pendingStatusStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setThinBorders(pendingStatusStyle);

            int totalColumns = HEADERS.length;

            // 1. Create Title Row (Row 0)
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(40);
            for (int col = 0; col < totalColumns; col++) {
                Cell cell = titleRow.createCell(col);
                cell.setCellStyle(titleStyle);
            }
            Cell mainTitleCell = titleRow.getCell(0);
            mainTitleCell.setCellValue("BHASHYAM EDUCATIONAL INSTITUTION - STUDENTS DIRECTORY");
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
                    StudentGuardian guardian = student.getGuardianDetail();

                    List<StudentDocument> docs = studentDocumentsMap != null
                            ? studentDocumentsMap.getOrDefault(student.getStudentId(), List.of())
                            : List.of();

                    // Map all 70 columns
                    int col = 0;

                    // --- Personal Information ---
                    createCell(row, col++, student.getStudentId(), defaultDataStyle);
                    createCell(row, col++, student.getAdmissionNumber(), defaultDataStyle);
                    createCell(row, col++, student.getRollNumber(), defaultDataStyle);
                    createCell(row, col++, academic != null ? academic.getUniversityId() : "", defaultDataStyle);
                    createCell(row, col++, student.getFirstName(), defaultDataStyle);
                    createCell(row, col++, student.getMiddleName(), defaultDataStyle);
                    createCell(row, col++, student.getLastName(), defaultDataStyle);
                    createCell(row, col++, student.getFullName(), defaultDataStyle);
                    createCell(row, col++, student.getGender(), defaultDataStyle);
                    createDateCell(row, col++, student.getDateOfBirth(), dateDataStyle);
                    createCell(row, col++, student.getBloodGroup(), defaultDataStyle);
                    createCell(row, col++, student.getAadhaarNumber(), defaultDataStyle);
                    createCell(row, col++, student.getNationality() != null ? student.getNationality() : "Indian", defaultDataStyle);
                    createCell(row, col++, student.getCasteCategory(), defaultDataStyle);
                    createCell(row, col++, student.getReligion(), defaultDataStyle);
                    createCell(row, col++, student.getCasteCategory(), defaultDataStyle);

                    createCell(row, col++, contact != null ? contact.getMobileNumber() : "", defaultDataStyle);
                    createCell(row, col++, contact != null ? contact.getAlternateMobile() : "", defaultDataStyle);
                    createCell(row, col++, contact != null ? contact.getEmail() : "", defaultDataStyle);
                    createCell(row, col++, contact != null ? contact.getAddress() : "", defaultDataStyle);
                    createCell(row, col++, "", defaultDataStyle); // Address Line 2
                    createCell(row, col++, contact != null ? contact.getCity() : "", defaultDataStyle);
                    createCell(row, col++, contact != null ? contact.getDistrict() : "", defaultDataStyle);
                    createCell(row, col++, contact != null ? contact.getState() : "", defaultDataStyle);
                    createCell(row, col++, contact != null ? contact.getCountry() : "India", defaultDataStyle);
                    createCell(row, col++, contact != null ? contact.getPinCode() : "", defaultDataStyle);

                    // --- Academic Information ---
                    createCell(row, col++, academic != null ? academic.getAcademicYear() : "", defaultDataStyle);
                    createCell(row, col++, "Intermediate", defaultDataStyle); // Course
                    createCell(row, col++, academic != null ? academic.getDepartment() : "General", defaultDataStyle);
                    createCell(row, col++, "Intermediate", defaultDataStyle); // Program
                    createCell(row, col++, academic != null ? academic.getBranchGroup() : "", defaultDataStyle);
                    createCell(row, col++, academic != null ? academic.getIntermediateYear() : "", defaultDataStyle);
                    createCell(row, col++, (academic != null && academic.getSemester() != null) ? String.valueOf(academic.getSemester()) : "", defaultDataStyle);
                    createCell(row, col++, academic != null ? academic.getSection() : "", defaultDataStyle);
                    createCell(row, col++, academic != null ? academic.getBatch() : "", defaultDataStyle);
                    createDateCell(row, col++, academic != null ? academic.getAdmissionDate() : null, dateDataStyle);
                    createCell(row, col++, academic != null ? academic.getAdmissionType() : "REGULAR", defaultDataStyle);
                    createCell(row, col++, academic != null ? academic.getRegulation() : "", defaultDataStyle);

                    // Current Status & Active/Inactive
                    StudentStatus status = student.getStatus();
                    String statusStr = status != null ? status.name() : "ACTIVE";
                    CellStyle statusStyle = "ACTIVE".equalsIgnoreCase(statusStr) ? activeStatusStyle
                            : "INACTIVE".equalsIgnoreCase(statusStr) ? inactiveStatusStyle
                            : pendingStatusStyle;
                    createCell(row, col++, statusStr, statusStyle);

                    String activeStr = (status == StudentStatus.ACTIVE) ? "Active" : "Inactive";
                    createCell(row, col++, activeStr, statusStyle);

                    // --- Parent / Guardian Information ---
                    String fName = parent != null && parent.getFatherName() != null ? parent.getFatherName()
                            : (guardian != null ? guardian.getFatherName() : "");
                    createCell(row, col++, fName, defaultDataStyle);
                    createCell(row, col++, parent != null ? parent.getParentMobile() : "", defaultDataStyle);
                    createCell(row, col++, parent != null ? parent.getOccupation() : "", defaultDataStyle);

                    String mName = parent != null && parent.getMotherName() != null ? parent.getMotherName()
                            : (guardian != null ? guardian.getMotherName() : "");
                    createCell(row, col++, mName, defaultDataStyle);
                    createCell(row, col++, parent != null ? parent.getParentMobile() : "", defaultDataStyle);
                    createCell(row, col++, "", defaultDataStyle); // Mother Occupation

                    createCell(row, col++, guardian != null ? guardian.getGuardianName() : "", defaultDataStyle);
                    createCell(row, col++, guardian != null ? guardian.getRelationship() : "", defaultDataStyle);
                    createCell(row, col++, guardian != null ? guardian.getGuardianMobile() : "", defaultDataStyle);

                    // --- Hostel / Transport ---
                    String hostelType = academic != null && academic.getHostelDayScholar() != null
                            ? academic.getHostelDayScholar() : "DAY_SCHOLAR";
                    createCell(row, col++, "HOSTELLER".equalsIgnoreCase(hostelType) ? "Yes" : "No", defaultDataStyle);
                    createCell(row, col++, "HOSTELLER".equalsIgnoreCase(hostelType) ? "Campus Hostel" : "", defaultDataStyle);
                    createCell(row, col++, "", defaultDataStyle); // Room Number
                    createCell(row, col++, "No", defaultDataStyle); // Transport Required
                    createCell(row, col++, "", defaultDataStyle); // Bus Route
                    createCell(row, col++, "", defaultDataStyle); // Pickup Point

                    // --- Certificate / Document Information ---
                    createCell(row, col++, hasDoc(docs, "SSC_MEMO", "SSC") ? "Yes" : "No", defaultDataStyle);
                    createCell(row, col++, hasDoc(docs, "INTER_1ST_MEMO", "INTER") ? "Yes" : "No", defaultDataStyle);
                    createCell(row, col++, hasDoc(docs, "TRANSFER_CERT", "TRANSFER") ? "Yes" : "No", defaultDataStyle);
                    createCell(row, col++, hasDoc(docs, "CASTE_CERT", "CASTE") ? "Yes" : "No", defaultDataStyle);
                    createCell(row, col++, hasDoc(docs, "INCOME_CERT", "INCOME") ? "Yes" : "No", defaultDataStyle);
                    createCell(row, col++, hasDoc(docs, "AADHAAR_DOC", "AADHAAR") ? "Yes" : "No", defaultDataStyle);

                    boolean hasPhoto = student.getProfilePhotoUrl() != null && !student.getProfilePhotoUrl().isBlank();
                    createCell(row, col++, hasPhoto ? "Yes" : "No", defaultDataStyle);
                    createCell(row, col++, hasDoc(docs, "SIGNATURE_DOC", "SIGNATURE") ? "Yes" : "No", defaultDataStyle);
                    createCell(row, col++, hasDoc(docs, "OTHER", "OTHER_DOC") ? "Yes" : "No", defaultDataStyle);

                    long pendingCount = docs.stream()
                            .filter(d -> d.getStatus() == DocumentStatus.UPLOADED || d.getStatus() == DocumentStatus.PENDING)
                            .count();
                    createCell(row, col++, String.valueOf(pendingCount), defaultDataStyle);

                    long missingCount = calculateMissingCount(docs, requiredDocumentTypes);
                    createCell(row, col++, String.valueOf(missingCount), defaultDataStyle);

                    // --- System Information ---
                    String createdBy = student.getCreatedBy() != null
                            ? (student.getCreatedBy().getFullName() != null ? student.getCreatedBy().getFullName() : student.getCreatedBy().getEmail())
                            : "System";
                    createCell(row, col++, createdBy, defaultDataStyle);
                    createDateTimeCell(row, col++, student.getCreatedAt(), dateTimeDataStyle);
                    createCell(row, col++, createdBy, defaultDataStyle);
                    createDateTimeCell(row, col++, student.getUpdatedAt(), dateTimeDataStyle);
                }
            }

            // 5. Auto-size all columns with minimum and maximum bounds
            for (int col = 0; col < totalColumns; col++) {
                sheet.autoSizeColumn(col);
                int currentWidth = sheet.getColumnWidth(col);
                int minWidth = 14 * 256; // 14 characters
                int maxWidth = 45 * 256; // 45 characters
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

    private static void createDateTimeCell(Row row, int colIndex, LocalDateTime dateTime, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        if (dateTime != null) {
            cell.setCellValue(dateTime.format(DATETIME_FORMATTER));
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    private static boolean hasDoc(List<StudentDocument> docs, String... codes) {
        if (docs == null || docs.isEmpty()) return false;
        for (StudentDocument doc : docs) {
            if (doc.getStatus() == DocumentStatus.REJECTED || doc.getStatus() == DocumentStatus.ARCHIVED) {
                continue;
            }
            if (doc.getDocumentType() != null) {
                String docCode = doc.getDocumentType().getCode();
                for (String c : codes) {
                    if (docCode != null && docCode.toUpperCase().contains(c.toUpperCase())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static long calculateMissingCount(List<StudentDocument> docs, List<DocumentType> requiredTypes) {
        if (requiredTypes == null || requiredTypes.isEmpty()) return 0;
        long presentCount = 0;
        for (DocumentType req : requiredTypes) {
            boolean present = docs.stream().anyMatch(d ->
                    d.getDocumentType() != null &&
                    d.getDocumentType().getId().equals(req.getId()) &&
                    d.getStatus() != DocumentStatus.REJECTED &&
                    d.getStatus() != DocumentStatus.ARCHIVED
            );
            if (present) {
                presentCount++;
            }
        }
        return Math.max(0, requiredTypes.size() - presentCount);
    }
}
