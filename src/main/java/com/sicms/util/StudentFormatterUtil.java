package com.sicms.util;

public class StudentFormatterUtil {

    public static String formatSection(String rawSec) {
        if (rawSec == null || rawSec.trim().isEmpty() || rawSec.equalsIgnoreCase("unassigned") || rawSec.equalsIgnoreCase("section unassigned")) {
            return "Unassigned";
        }
        String clean = rawSec.trim();
        if (clean.toLowerCase().startsWith("section ")) {
            clean = clean.substring(8).trim();
        }
        if (clean.equalsIgnoreCase("unassigned")) {
            return "Unassigned";
        }
        return "Section " + clean;
    }

    public static String formatBranchGroup(String group) {
        if (group == null || group.trim().isEmpty() || group.equalsIgnoreCase("n/a")) {
            return "General";
        }
        return group.trim();
    }

    public static String formatIntermediateYear(String year) {
        if (year == null || year.trim().isEmpty() || year.equalsIgnoreCase("n/a")) {
            return "1st Year";
        }
        return year.trim();
    }
}
