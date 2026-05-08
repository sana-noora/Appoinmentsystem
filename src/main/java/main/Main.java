package main;

import connectDB.connectDB;
import domain.*;
import persistence.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import service.ReminderService;

public class Main {

    private static final String OWNER_EMAIL = "nooraqaradeh3@gmail.com";
    private static final int MAX_GROUP = 5;

    private static final DateTimeFormatter DISPLAY_DT =
            DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy  HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter INPUT_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

    public static Scanner sc = new Scanner(System.in);

    // ====================================================
    // HELPERS (✅ هذه اللي نرفع فيها الكوفرج)
    // ====================================================

    private static boolean isWithin24h(OffsetDateTime start) {
        return OffsetDateTime.now(ZoneOffset.UTC).plusHours(24).isAfter(start);
    }

    private static String fmtTime(OffsetDateTime dt) {
        if (dt == null) return "N/A";
        return dt.atZoneSameInstant(ZoneId.systemDefault()).format(DISPLAY_DT);
    }

    private static String friendlyType(String t) {
        if (t == null) return "Unknown";
        switch (t.toUpperCase()) {
            case "FIRST_VISIT":       return "Individual – First Visit";
            case "FOLLOW_UP":         return "Individual – Follow-up";
            case "VIRTUAL":           return "Individual – Virtual";
            case "GROUP_FIRST_VISIT": return "Group – First Visit";
            case "GROUP_FOLLOW_UP":   return "Group – Follow-up";
            case "GROUP_VIRTUAL":     return "Group – Virtual";
            default:                  return t;
        }
    }

    private static int sortGroup(Appointment a) {
        if (Appointment.STATUS_DONE.equals(a.getStatus())) return 0;
        if (Appointment.STATUS_CANCELED.equals(a.getStatus())) return 1;
        return 2;
    }

    private static int readInt(int min, int max) {
        try {
            int v = Integer.parseInt(sc.nextLine().trim());
            if (v >= min && v <= max) return v;
        } catch (NumberFormatException ignored) {}
        System.out.println("  Invalid input.\n");
        return -1;
    }

    private static long readLong() {
        try { return Long.parseLong(sc.nextLine().trim()); }
        catch (NumberFormatException e) {
            System.out.println("  Invalid ID.\n");
            return -1;
        }
    }

    private static void printBanner() {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║   Apartment Visit Appointment Scheduling System      ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();
    }
}