package main;

import connectDB.connectDB;
import domain.*;
import persistence.*;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;
import service.ReminderService;


public class Main {

	private static final String PROMPT_CHOICE = "  Choice: ";
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static final String OWNER_EMAIL = "nooraqaradeh3@gmail.com";
    private static final int    MAX_GROUP   = 5;

    // ── Scanner is non-final so tests can replace it ──────────────────
    private static Scanner sc = new Scanner(System.in);

    /**
     * Returns the current Scanner instance (used internally and by tests).
     */
    public static Scanner getScanner() {
        return sc;
    }

    /**
     * Replaces the Scanner instance. Call this in tests BEFORE invoking any
     * Main method so that all sc.nextLine() calls read from the test's input.
     */
    public static void setScanner(Scanner scanner) {
        sc = scanner;
    }

    private static final DateTimeFormatter DISPLAY_DT =
            DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy  HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter INPUT_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

    // ================================================================
    //  MAIN
    // ================================================================
    public static void main(String[] args) {
        while (true) {
            printBanner();
            try (Connection conn = connectDB.getConnection()) {
                logger.info("[OK] Connected to database.");

                AppointmentDAO apptDAO  = new AppointmentDAO(conn);
                TimeSlotDAO    slotDAO  = new TimeSlotDAO(conn);
                ScheduleDAO    schedDAO = new ScheduleDAO(conn);
                UserDAO        userDAO  = new UserDAO(conn);

                apptDAO.markPastAppointmentsDone();
                new ReminderService(apptDAO, userDAO).sendReminders();

                User user = loginLoop(userDAO);
                if (user == null) {
                    logger.warning("Too many failed login attempts. Exiting.");
                    return;
                }

                System.out.println("\nWelcome, " + user.getName() + "  [" +
                        (user.getRole() == User.Role.ADMIN ? "Admin" : "Visitor") + "]\n");
                logger.info("User logged in: " + user.getUsername() + " [" + user.getRole() + "]");

                if (user.getRole() == User.Role.ADMIN) {
                    Admin admin = new Admin.Builder()
                        .setId(user.getId())
                        .setName(user.getName())
                        .setEmail(user.getEmail())
                        .setPhoneNumber(user.getPhoneNumber())
                        .setUsername(user.getUsername())
                        .setAppointmentDAO(apptDAO)
                        .setTimeSlotDAO(slotDAO)
                        .setScheduleDAO(schedDAO)
                        .build();
                    admin.markLoggedIn();
                    adminMenu(admin, apptDAO, slotDAO, schedDAO, userDAO);
                } else {
                    user.markLoggedIn();
                    visitorMenu(user, apptDAO, slotDAO, schedDAO, userDAO);
                }

            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Database error during connection", e);
            }
        }
    }

    // ================================================================
    //  LOGIN
    // ================================================================
    private static User loginLoop(UserDAO userDAO) throws SQLException {
        for (int i = 1; i <= 3; i++) {
            logger.info("Prompting for username (Attempt " + i + "/3)");
            String u = sc.nextLine().trim();
            logger.info("Prompting for password for user: " + u + " (Attempt " + i + "/3)");
            String p = sc.nextLine().trim();

            Optional<User> opt = userDAO.login(u, p);
            if (opt.isPresent()) {
                logger.info("Login successful for user: " + u);
                return opt.get();
            }
            logger.warning("Invalid login attempt " + i + "/3 for user: " + u);
            System.out.println("  ✗ Invalid credentials. Attempt " + i + "/3.\n");
        }
        return null;
    }

    // ================================================================
    //  ADMIN MENU
    // ================================================================
    private static void adminMenu(Admin admin, AppointmentDAO apptDAO,
                                  TimeSlotDAO slotDAO, ScheduleDAO schedDAO,
                                  UserDAO userDAO) throws SQLException {
        while (admin.isLoggedIn()) {
            System.out.println("\n╔══════════════════════════════════════╗");
            System.out.println("║            ADMIN MENU                ║");
            System.out.println("╠══════════════════════════════════════╣");
            System.out.println("║  1. View appointments (by day)       ║");
            System.out.println("║  2. Cancel appointment               ║");
            System.out.println("║  3. Edit appointment                 ║");
            System.out.println("║  4. Add work day                     ║");
            System.out.println("║  5. View day slots                   ║");
            System.out.println("║  6. Add slot                         ║");
            System.out.println("║  7. Add new user                     ║");
            System.out.println("║  8. View all users                   ║");
            System.out.println("║  9. Logout                           ║");
            System.out.println("╚══════════════════════════════════════╝");
            System.out.print("Choice: ");

            switch (sc.nextLine().trim()) {
                case "1": adminViewAppointments(apptDAO, schedDAO, userDAO);           break;
                case "2": adminCancelAppointment(apptDAO, slotDAO, schedDAO, userDAO); break;
                case "3": adminEditAppointment(apptDAO, schedDAO, userDAO);            break;
                case "4": adminAddWorkDay(schedDAO);                                   break;
                case "5": adminViewDaySlots(slotDAO, schedDAO);                        break;
                case "6": adminAddSlot(slotDAO, schedDAO);                             break;
                case "7": adminAddUser(userDAO);                                       break;
                case "8": adminViewAllUsers(userDAO);                                  break;
                case "9": admin.logout(); System.out.println("Logged out.\n");
                    logger.info("Admin logged out: " + admin.getUsername());
                    break;
                default:  System.out.println("  Invalid option.\n");
                    logger.warning("Invalid menu option selected in admin menu");
            }
        }
    }

    // ── ADMIN 1: View appointments ───────────────────────────────────
    private static void adminViewAppointments(AppointmentDAO apptDAO,
                                              ScheduleDAO schedDAO,
                                              UserDAO userDAO) throws SQLException {
        List<Schedule> all = schedDAO.getFutureSchedules();
        if (all.isEmpty()) {
            System.out.println("  No work days in system.\n");
            logger.info("Admin: No work days available");
            return;
        }

        Schedule chosen = pickSchedule(all, "Work Days", "Choose day (number): ");
        if (chosen == null) return;

        List<Appointment> appts = apptDAO.getActiveAppointmentsByDate(chosen.getWorkDate());
        if (appts.isEmpty()) {
            System.out.println("  No active appointments on " + chosen.getWorkDate() + ".\n");
            logger.info("Admin: No appointments on " + chosen.getWorkDate());
            return;
        }

        System.out.println("\n  ── Appointments on " + chosen.getWorkDate().format(DISPLAY_DATE)
                + " ──────────────────────────────────────");
        logger.info("Admin viewing appointments for: " + chosen.getWorkDate());

        for (Appointment a : appts) {
            long dur = Duration.between(a.getStartTime(), a.getEndTime()).toMinutes();
            Optional<User> u = userDAO.getUserById(String.valueOf(a.getCreatedBy()));
            String username = u.map(User::getUsername).orElse("?");
            System.out.printf("  [%d] %s  |  %d min  |  %s  |  by: %s%n",
                    a.getId(), fmtTime(a.getStartTime()), dur,
                    friendlyType(a.getType()), username);
        }
        System.out.println();
    }

    // ── ADMIN 2: Cancel appointment ──────────────────────────────────
    private static void adminCancelAppointment(AppointmentDAO apptDAO,
                                               TimeSlotDAO slotDAO,
                                               ScheduleDAO schedDAO,
                                               UserDAO userDAO) throws SQLException {
        List<Schedule> days = schedDAO.getFutureSchedules();
        if (days.isEmpty()) {
            System.out.println("  No future work days.\n");
            logger.info("Admin: No future work days for cancellation");
            return;
        }

        Schedule chosen = pickSchedule(days, "Future Work Days", "Choose day (number): ");
        if (chosen == null) return;

        List<Appointment> appts = apptDAO.getFutureAppointmentsByDate(chosen.getWorkDate());
        if (appts.isEmpty()) {
            System.out.println("  No future appointments on that day.\n");
            logger.info("Admin: No future appointments on " + chosen.getWorkDate());
            return;
        }

        Appointment appt = pickAdminAppt(appts, apptDAO, userDAO, "Appointment ID to cancel: ");
        if (appt == null) return;

        System.out.print("  Reason / note for user: ");
        String note = sc.nextLine().trim();

        apptDAO.cancelByAdmin(appt.getId(), note.isEmpty() ? null : note);
        if (appt.getSlotId() != null)
            slotDAO.updateAvailability(appt.getSlotId(), true);

        System.out.println("  ✓ Appointment canceled. Slot is now free.\n");
        logger.info("Admin cancelled appointment ID: " + appt.getId()
                + " - Note: " + (note.isEmpty() ? "none" : note));
    }

 // ── ADMIN 3: Edit appointment ────────────────────────────────────
    private static void adminEditAppointment(AppointmentDAO apptDAO,
                                             ScheduleDAO schedDAO,
                                             UserDAO userDAO) throws SQLException {
        List<Schedule> days = schedDAO.getFutureSchedules();
        if (days.isEmpty()) {
            System.out.println("  No future work days.\n");
            logger.info("Admin: No future work days for editing");
            return;
        }

        Schedule chosen = pickSchedule(days, "Future Work Days", "Choose day (number): ");
        if (chosen == null) return;

        List<Appointment> appts = apptDAO.getFutureAppointmentsByDate(chosen.getWorkDate());
        if (appts.isEmpty()) {
            System.out.println("  No future appointments on that day.\n");
            logger.info("Admin: No future appointments on " + chosen.getWorkDate() + " for editing");
            return;
        }

        Appointment appt = pickAdminAppt(appts, apptDAO, userDAO, "Appointment ID to edit: ");
        if (appt == null) return;

        System.out.println("\n  What to edit?");
        System.out.println("    1. Change appointment type (Individual/Group)");
        System.out.println("    2. Change participant count (Group only)");
        System.out.print(PROMPT_CHOICE);
        String ch = sc.nextLine().trim();

        if ("1".equals(ch)) {
            applyEditType(appt, apptDAO);
        } else if ("2".equals(ch)) {
            applyEditCount(appt, apptDAO);
        } else {
            System.out.println("  Invalid choice.\n");
            logger.warning("Invalid edit choice in admin edit appointment");
        }
    }

    /** Handles changing the appointment type (Individual/Group). */
    private static void applyEditType(Appointment appt,
                                       AppointmentDAO apptDAO) throws SQLException {
        String newType = askAppointmentType("  New category: 1=Individual  2=Group");
        if (newType == null) return;

        System.out.print("  Note / reason for user: ");
        String note = sc.nextLine().trim();
        if (note.isEmpty()) note = null;

        apptDAO.updateTypeAndNote(appt.getId(), newType, note);
        System.out.println("  ✓ Appointment updated.\n");
        logger.info("Admin updated appointment ID: " + appt.getId() + " - Type: " + newType);
    }

    /** Handles changing the participant count (Group appointments only). */
    private static void applyEditCount(Appointment appt,
                                        AppointmentDAO apptDAO) throws SQLException {
        if (!appt.isGroup()) {
            System.out.println("  Participant count is only for Group appointments.\n");
            logger.warning("Admin attempted to change participant count on non-group appointment");
            return;
        }

        System.out.print("  New participant count (1-" + MAX_GROUP + "): ");
        int newCount = readInt(1, MAX_GROUP);
        if (newCount == -1) return;

        System.out.print("  Note / reason for user: ");
        String note = sc.nextLine().trim();
        if (note.isEmpty()) note = null;

        apptDAO.updateParticipantsAndNote(appt.getId(), newCount, note);
        System.out.println("  ✓ Appointment updated.\n");
        logger.info("Admin updated appointment ID: " + appt.getId() + " - Count: " + newCount);
    }

    // ── ADMIN 4: Add work day ────────────────────────────────────────
    private static void adminAddWorkDay(ScheduleDAO schedDAO) throws SQLException {
        List<Schedule> future = schedDAO.getFutureSchedules();
        if (!future.isEmpty()) {
            System.out.println("\n  ── Current & Future Work Days ──────────────────────");
            for (Schedule s : future)
                System.out.println("    " + s.getWorkDate().format(DISPLAY_DATE));
        }

        System.out.print("\n  New work date (yyyy-MM-dd): ");
        String input = sc.nextLine().trim();
        LocalDate date;
        try {
            date = LocalDate.parse(input, INPUT_DATE);
        } catch (DateTimeParseException e) {
            System.out.println("  Invalid format.\n");
            logger.warning("Invalid date format provided: " + input);
            return;
        }

        if (date.isBefore(LocalDate.now())) {
            System.out.println("  Date must be today or in the future.\n");
            logger.warning("Admin attempted to add past work date: " + date);
            return;
        }
        if (schedDAO.existsByDate(date)) {
            System.out.println("  This day already exists. Please choose a different date.\n");
            logger.warning("Admin attempted to add duplicate work date: " + date);
            return;
        }
        schedDAO.addSchedule(new Schedule(date));
        System.out.println("  ✓ Work day added successfully.\n");
        logger.info("Admin added new work day: " + date);
    }

    // ── ADMIN 5: View day slots ──────────────────────────────────────
    private static void adminViewDaySlots(TimeSlotDAO slotDAO,
                                          ScheduleDAO schedDAO) throws SQLException {
        List<Schedule> future = schedDAO.getFutureSchedules();
        if (future.isEmpty()) {
            System.out.println("  No work days.\n");
            logger.info("Admin: No work days available for viewing slots");
            return;
        }

        Schedule chosen = pickSchedule(future, "Work Days", "Choose day: ");
        if (chosen == null) return;

        List<TimeSlot> slots = slotDAO.getAllSlotsBySchedule(chosen.getId());
        if (slots.isEmpty()) {
            System.out.println("  No slots for that day.\n");
            logger.info("Admin: No slots on " + chosen.getWorkDate());
            return;
        }

        System.out.println("\n  ── Slots for " + chosen.getWorkDate().format(DISPLAY_DATE)
                + " ─────────────────────────────────────");
        logger.info("Admin viewing slots for: " + chosen.getWorkDate());

        for (TimeSlot s : slots) {
            String status;
            if (s.isAvailable()) {
                status = "✅ Available";
            } else {
                String who = slotDAO.getBookedByUsername(s.getId());
                status = "❌ Booked" + (who != null ? " by: " + who : "");
            }
            System.out.printf("  %s  |  %s%n", fmtTime(s.getStartTime()), status);
        }
        System.out.println();
    }

    // ── ADMIN 6: Add slot ────────────────────────────────────────────
    private static void adminAddSlot(TimeSlotDAO slotDAO,
                                     ScheduleDAO schedDAO) throws SQLException {
        List<Schedule> future = schedDAO.getFutureSchedules();
        if (future.isEmpty()) {
            System.out.println("  No work days.\n");
            logger.info("Admin: No work days available for adding slot");
            return;
        }

        Schedule chosen = pickSchedule(future, "Work Days", "Choose day: ");
        if (chosen == null) return;

        List<TimeSlot> existing = slotDAO.getAllSlotsBySchedule(chosen.getId());
        if (!existing.isEmpty()) {
            System.out.println("  Existing slots:");
            for (TimeSlot s : existing)
                System.out.println("    " + fmtTime(s.getStartTime()));
        }

        System.out.print("  New slot start hour (0-23, whole hours only e.g. 9, 10, 14): ");
        int hour = readInt(0, 23);
        if (hour == -1) return;

        OffsetDateTime start = chosen.getWorkDate().atTime(hour, 0)
                .atZone(ZoneId.systemDefault()).toOffsetDateTime()
                .withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(1);

        if (slotDAO.existsByScheduleAndStart(chosen.getId(), start)) {
            System.out.println("  This slot already exists. Please choose a different time.\n");
            logger.warning("Admin attempted to add duplicate slot at " + fmtTime(start));
            return;
        }

        slotDAO.addTimeSlot(new TimeSlot(chosen.getId(), start, end, true));
        System.out.println("  ✓ Slot added: " + fmtTime(start) + "\n");
        logger.info("Admin added new time slot: " + fmtTime(start));
    }

    // ── ADMIN 7: Add user ────────────────────────────────────────────
    private static void adminAddUser(UserDAO userDAO) throws SQLException {
        System.out.println("\n  ── Add New User ──────────────────────────────────────");
        System.out.print("  Name       : ");
        String name = sc.nextLine().trim();
        System.out.print("  Email      : ");
        String mail = sc.nextLine().trim();
        System.out.print("  Phone      : ");
        String ph   = sc.nextLine().trim();
        System.out.print("  Username   : ");
        String un   = sc.nextLine().trim();
        System.out.print("  Password   : ");
        String pw   = sc.nextLine().trim();
        System.out.print("  Role (1=Admin, 2=Visitor): ");
        String rc = sc.nextLine().trim();
        User.Role role = "1".equals(rc) ? User.Role.ADMIN : User.Role.VISITOR;
        userDAO.addUser(new User("", name, mail, ph, un, role), pw);
        System.out.println("  ✓ User registered successfully.\n");
        logger.info("Admin created new user: " + un + " with role: " + role);
    }

    // ── ADMIN 8: View all users ──────────────────────────────────────
    private static void adminViewAllUsers(UserDAO userDAO) throws SQLException {
        List<User> users = userDAO.getAllUsers();
        System.out.println("\n  ── All Users ─────────────────────────────────────────");
        System.out.printf("  %-4s  %-20s  %-30s  %-13s  %-10s%n",
                "ID", "Name", "Email", "Phone", "Role");
        System.out.println("  " + new String(new char[85]).replace('\0', '-'));
        for (User u : users)
            System.out.printf("  %-4s  %-20s  %-30s  %-13s  %-10s%n",
                    u.getId(), u.getName(), u.getEmail(), u.getPhoneNumber(), u.getRole());
        System.out.println();
        logger.info("Admin viewed all users. Total count: " + users.size());
    }

    // ================================================================
    //  VISITOR MENU
    // ================================================================
    private static void visitorMenu(User visitor, AppointmentDAO apptDAO,
                                    TimeSlotDAO slotDAO, ScheduleDAO schedDAO,
                                    UserDAO userDAO) throws SQLException {
        while (visitor.isLoggedIn()) {
            System.out.println("\n╔══════════════════════════════════════╗");
            System.out.println("║           VISITOR MENU               ║");
            System.out.println("╠══════════════════════════════════════╣");
            System.out.println("║  1. Book appointment                 ║");
            System.out.println("║  2. My appointments                  ║");
            System.out.println("║  3. Edit my appointment              ║");
            System.out.println("║  4. Cancel my appointment            ║");
            System.out.println("║  5. Logout                           ║");
            System.out.println("╚══════════════════════════════════════╝");
            System.out.print("Choice: ");

            switch (sc.nextLine().trim()) {
                case "1": visitorBook(visitor, apptDAO, slotDAO, schedDAO);       break;
                case "2": visitorMyAppointments(visitor, apptDAO);                break;
                case "3": visitorEdit(visitor, apptDAO);                          break;
                case "4": visitorCancel(visitor, apptDAO, slotDAO);               break;
                case "5": visitor.logout(); System.out.println("Logged out.\n");
                    logger.info("Visitor logged out: " + visitor.getUsername());
                    break;
                default:  System.out.println("  Invalid option.\n");
                    logger.warning("Invalid menu option selected in visitor menu");
            }
        }
    }

 // ── VISITOR 1: Book ─────────────────────────────────────────────
    private static void visitorBook(User visitor, AppointmentDAO apptDAO,
                                    TimeSlotDAO slotDAO, ScheduleDAO schedDAO)
            throws SQLException {
        List<Schedule> days = schedDAO.getFutureSchedules();
        if (days.isEmpty()) {
            System.out.println("  No available work days.\n");
            logger.info("Visitor " + visitor.getUsername() + ": No available work days");
            return;
        }

        Schedule chosen = pickSchedule(days, "Future Work Days", "Choose day: ");
        if (chosen == null) return;

        List<TimeSlot> avail = slotDAO.getAvailableSlotsByScheduleId(chosen.getId());
        if (avail.isEmpty()) {
            System.out.println("  No available slots on that day.\n");
            logger.info("Visitor " + visitor.getUsername()
                    + ": No available slots on " + chosen.getWorkDate());
            return;
        }

        System.out.println("\n  ── Available Slots ───────────────────────────────────");
        for (int i = 0; i < avail.size(); i++)
            System.out.printf("  %d. %s%n", i + 1, fmtTime(avail.get(i).getStartTime()));

        System.out.print("  Choose slot: ");
        int si = readInt(1, avail.size());
        if (si == -1) return;
        TimeSlot slot = avail.get(si - 1);

        System.out.print("  Duration in minutes (max 60): ");
        int dur = readInt(1, 60);
        if (dur == -1) {
            System.out.println("  ✗ Duration cannot exceed 60 minutes.\n");
            logger.warning("Visitor " + visitor.getUsername() + ": Invalid duration");
            return;
        }

        BookingTypeResult result = resolveBookingType(visitor);
        if (result == null) return;

        OffsetDateTime start = slot.getStartTime();
        OffsetDateTime end   = start.plusMinutes(dur);

        Appointment a = new Appointment(result.apptType, Appointment.STATUS_CONFIRMED,
                start, end, result.participants, result.maxP,
                Long.parseLong(visitor.getId()), slot.getId());
        apptDAO.addAppointment(a);
        slotDAO.updateAvailability(slot.getId(), false);

        System.out.println("\n  ✅ Appointment booked successfully!");
        System.out.println("     Date     : " + chosen.getWorkDate().format(DISPLAY_DATE));
        System.out.println("     Start    : " + fmtTime(start));
        System.out.println("     Duration : " + dur + " minutes");
        System.out.println("     Type     : " + friendlyType(result.apptType));
        if (result.participants > 1)
            System.out.println("     Visitors : " + result.participants);
        System.out.println();
        System.out.println("  ℹ  You can freely modify or cancel this appointment");
        System.out.println("     if more than 24 hours remain.");
        System.out.println("     • Cancellation fee if < 24h: 200 NIS");
        System.out.println("     • Modification fee if < 24h: 100 NIS");
        System.out.println();
        logger.info("Visitor " + visitor.getUsername() + " booked appointment: "
                + result.apptType + " for " + dur + " minutes");
    }

    /** Simple container for booking type resolution result. */
    private static class BookingTypeResult {
        final String apptType;
        final int    participants;
        final int    maxP;

        BookingTypeResult(String apptType, int participants, int maxP) {
            this.apptType     = apptType;
            this.participants = participants;
            this.maxP         = maxP;
        }
    }

    /**
     * Asks the visitor to choose Individual or Group, then the visit sub-type.
     * Returns null if any input is invalid.
     */
   private static BookingTypeResult resolveBookingType(User visitor) {
    System.out.println("  Appointment type: 1=Individual  2=Group");
    System.out.print(PROMPT_CHOICE);
    String cat = sc.nextLine().trim();

    if ("1".equals(cat)) {
        System.out.println("  Visit type: 1=First Visit  2=Follow-up  3=Virtual");
        System.out.print(PROMPT_CHOICE);
        String apptType = resolveVisitType("1", sc.nextLine().trim());
        if (apptType == null) {
            System.out.println("  Invalid type.\n");
            logger.warning("Visitor " + visitor.getUsername()
                    + ": Invalid individual visit type");
            return null;
        }
        return new BookingTypeResult(apptType, 1, 1);
    }

    if ("2".equals(cat)) {
        System.out.print("  Number of participants (1-" + MAX_GROUP + "): ");
        int participants = readInt(1, MAX_GROUP);
        if (participants == -1) {
            System.out.println("  ✗ Group cannot exceed 5 participants.\n");
            logger.warning("Visitor " + visitor.getUsername()
                    + ": Invalid group participant count");
            return null;
        }
        System.out.println("  Visit type: 1=First Visit  2=Follow-up  3=Virtual");
        System.out.print(PROMPT_CHOICE);
        String apptType = resolveVisitType("2", sc.nextLine().trim());
        if (apptType == null) {
            System.out.println("  Invalid type.\n");
            logger.warning("Visitor " + visitor.getUsername()
                    + ": Invalid group visit type");
            return null;
        }
        return new BookingTypeResult(apptType, participants, MAX_GROUP);
    }

    System.out.println("  Invalid category.\n");
    logger.warning("Visitor " + visitor.getUsername() + ": Invalid appointment category");
    return null;
}

    // ── VISITOR 2: My appointments ───────────────────────────────────
    private static void visitorMyAppointments(User visitor,
                                              AppointmentDAO apptDAO) throws SQLException {
        long uid = Long.parseLong(visitor.getId());
        List<Appointment> all = apptDAO.getAppointmentsByUser(uid);
        if (all.isEmpty()) {
            System.out.println("  You have no appointments.\n");
            logger.info("Visitor " + visitor.getUsername() + ": No appointments in system");
            return;
        }

        List<Appointment> visible = all.stream()
                .filter(a -> !(Appointment.STATUS_CANCELED.equals(a.getStatus())
                               && !a.isCanceledByAdmin()))
                .collect(Collectors.toList());

        if (visible.isEmpty()) {
            System.out.println("  You have no appointments.\n");
            logger.info("Visitor " + visitor.getUsername() + ": No visible appointments");
            return;
        }

        visible.sort((a, b) -> {
            int groupA = sortGroup(a), groupB = sortGroup(b);
            if (groupA != groupB) return Integer.compare(groupA, groupB);
            return a.getStartTime().compareTo(b.getStartTime());
        });

        System.out.println("\n  ── My Appointments ───────────────────────────────────────────");
        for (Appointment a : visible) {
            long dur = Duration.between(a.getStartTime(), a.getEndTime()).toMinutes();
            String statusLabel;
            if (Appointment.STATUS_DONE.equals(a.getStatus())) {
                statusLabel = "[DONE]";
            } else if (Appointment.STATUS_CANCELED.equals(a.getStatus())) {
                statusLabel = "[CANCELED by Admin]";
            } else {
                statusLabel = isWithin24h(a.getStartTime())
                        ? "[⚠ Less than 24h remaining]" : "[Upcoming]";
            }
            ZonedDateTime zdt = a.getStartTime().atZoneSameInstant(ZoneId.systemDefault());
            System.out.printf("  [%d] %s  %s  |  %d min  |  %s%s%n",
                    a.getId(), zdt.toLocalDate().format(DISPLAY_DATE),
                    fmtTime(a.getStartTime()), dur, friendlyType(a.getType()),
                    a.isGroup() ? "  (visitors: " + a.getParticipantsCount() + ")" : "");
            System.out.println("       Status : " + statusLabel);
            if (a.getAdminNote() != null && !a.getAdminNote().trim().isEmpty())
                System.out.println("       📝 Note from Admin: " + a.getAdminNote());
        }
        System.out.println();
        logger.info("Visitor " + visitor.getUsername() + " viewed appointments. Count: " + visible.size());
    }

    private static int sortGroup(Appointment a) {
        if (Appointment.STATUS_DONE.equals(a.getStatus()))     return 0;
        if (Appointment.STATUS_CANCELED.equals(a.getStatus())) return 1;
        return 2;
    }

    // ── VISITOR 3: Edit ──────────────────────────────────────────────
    private static void visitorEdit(User visitor,
                                    AppointmentDAO apptDAO) throws SQLException {
        List<Appointment> future = getVisitorFutureAppts(visitor, apptDAO);
        if (future.isEmpty()) {
            System.out.println("  No future appointments to edit.\n");
            logger.info("Visitor " + visitor.getUsername() + ": No future appointments to edit");
            return;
        }

        printVisitorApptList(future);

        System.out.print("  Appointment ID to edit: ");
        long apptId = readLong();
        if (apptId == -1) return;

        Appointment appt = future.stream()
                .filter(a -> a.getId() == apptId).findFirst().orElse(null);
        if (appt == null) {
            System.out.println("  Appointment not found or not yours.\n");
            logger.warning("Visitor " + visitor.getUsername()
                    + ": Attempted to edit non-existent appointment");
            return;
        }

        if (isWithin24h(appt.getStartTime())) {
            printWithin24hWarning("edit");
            logger.warning("Visitor " + visitor.getUsername()
                    + ": Attempted to edit appointment within 24h");
            return;
        }

        System.out.println("  What to change? 1=Appointment type  2=Visitor count (Group only)");
        System.out.print(PROMPT_CHOICE);
        String ch = sc.nextLine().trim();

        if ("1".equals(ch)) {
            String newType = askAppointmentType("  Category: 1=Individual  2=Group");
            if (newType == null) return;
            apptDAO.updateType(apptId, newType);
            System.out.println("  ✓ Type updated.\n");
            logger.info("Visitor " + visitor.getUsername() + " edited appointment type to: " + newType);

        } else if ("2".equals(ch)) {
            if (!appt.isGroup()) {
                System.out.println("  Only Group appointments can change visitor count.\n");
                logger.warning("Visitor " + visitor.getUsername()
                        + ": Attempted to change count on non-group appointment");
                return;
            }
            System.out.print("  New visitor count (1-" + MAX_GROUP + "): ");
            int cnt = readInt(1, MAX_GROUP);
            if (cnt == -1) return;
            apptDAO.updateParticipants(apptId, cnt);
            System.out.println("  ✓ Visitor count updated.\n");
            logger.info("Visitor " + visitor.getUsername() + " updated participant count to: " + cnt);
        } else {
            System.out.println("  Invalid.\n");
            logger.warning("Visitor " + visitor.getUsername() + ": Invalid edit choice");
        }
    }

    // ── VISITOR 4: Cancel ────────────────────────────────────────────
    private static void visitorCancel(User visitor, AppointmentDAO apptDAO,
                                      TimeSlotDAO slotDAO) throws SQLException {
        List<Appointment> future = getVisitorFutureAppts(visitor, apptDAO);
        if (future.isEmpty()) {
            System.out.println("  No future appointments to cancel.\n");
            logger.info("Visitor " + visitor.getUsername() + ": No future appointments to cancel");
            return;
        }

        printVisitorApptList(future);

        System.out.print("  Appointment ID to cancel: ");
        long apptId = readLong();
        if (apptId == -1) return;

        Appointment appt = future.stream()
                .filter(a -> a.getId() == apptId).findFirst().orElse(null);
        if (appt == null) {
            System.out.println("  Appointment not found or not yours.\n");
            logger.warning("Visitor " + visitor.getUsername()
                    + ": Attempted to cancel non-existent appointment");
            return;
        }

        if (isWithin24h(appt.getStartTime())) {
            printWithin24hWarning("cancel");
            logger.warning("Visitor " + visitor.getUsername()
                    + ": Attempted to cancel appointment within 24h");
            return;
        }

        System.out.print("  Confirm cancellation? (yes/no): ");
        if (!"yes".equalsIgnoreCase(sc.nextLine().trim())) {
            System.out.println("  Aborted.\n");
            logger.info("Visitor " + visitor.getUsername() + ": Cancellation aborted");
            return;
        }

        apptDAO.updateStatus(apptId, Appointment.STATUS_CANCELED);
        if (appt.getSlotId() != null)
            slotDAO.updateAvailability(appt.getSlotId(), true);

        System.out.println("  ✓ Appointment canceled. Slot is now free. No penalty applied.\n");
        logger.info("Visitor " + visitor.getUsername() + " cancelled appointment ID: " + apptId);
    } 

    // ================================================================
    //  SHARED PRIVATE HELPERS
    // ================================================================

    private static Schedule pickSchedule(List<Schedule> days, String header, String prompt) {
        System.out.println("\n  ── " + header + " ─────────────────────────────────────");
        for (int i = 0; i < days.size(); i++)
            System.out.printf("  %d. %s%n", i + 1, days.get(i).getWorkDate().format(DISPLAY_DATE));
        System.out.print("  " + prompt);
        int idx = readInt(1, days.size());
        return idx == -1 ? null : days.get(idx - 1);
    }

    private static Appointment pickAdminAppt(List<Appointment> appts,
                                              AppointmentDAO apptDAO,
                                              UserDAO userDAO,
                                              String prompt) throws SQLException {
        printAppointmentList(appts, userDAO);
        System.out.print("  " + prompt);
        long apptId = readLong();
        if (apptId == -1) return null;
        Appointment appt = apptDAO.getAppointmentById(apptId);
        if (appt == null || !appt.getStartTime().isAfter(OffsetDateTime.now())) {
            System.out.println("  Appointment not found or already past.\n");
            logger.warning("Invalid appointment ID or already past: " + apptId);
            return null;
        }
        return appt;
    }

    private static String askAppointmentType(String categoryPrompt) {
        System.out.println(categoryPrompt);
        System.out.print(PROMPT_CHOICE);
        String cat = sc.nextLine().trim();
        System.out.println("  Visit type: 1=First Visit  2=Follow-up  3=Virtual");
        System.out.print(PROMPT_CHOICE);
        String vt = sc.nextLine().trim();
        String type = resolveVisitType(cat, vt);
        if (type == null) {
            System.out.println("  Invalid.\n");
            logger.warning("Invalid appointment type: category=" + cat + ", visitType=" + vt);
        }
        return type;
    }

    private static String resolveVisitType(String cat, String vt) {
        if ("1".equals(cat)) {
            switch (vt) {
                case "1": return Appointment.TYPE_FIRST_VISIT;
                case "2": return Appointment.TYPE_FOLLOW_UP;
                case "3": return Appointment.TYPE_VIRTUAL;
                default:  return null;
            }
        } else if ("2".equals(cat)) {
            switch (vt) {
                case "1": return Appointment.TYPE_GROUP_FIRST_VISIT;
                case "2": return Appointment.TYPE_GROUP_FOLLOW_UP;
                case "3": return Appointment.TYPE_GROUP_VIRTUAL;
                default:  return null;
            }
        }
        return null;
    }

    private static List<Appointment> getVisitorFutureAppts(User visitor,
                                                            AppointmentDAO apptDAO)
            throws SQLException {
        long uid = Long.parseLong(visitor.getId());
        return apptDAO.getAppointmentsByUser(uid).stream()
                .filter(a -> Appointment.STATUS_CONFIRMED.equals(a.getStatus())
                          && a.getStartTime().isAfter(OffsetDateTime.now()))
                .collect(Collectors.toList());
    }

    private static void printVisitorApptList(List<Appointment> appts) {
        System.out.println("\n  ── Future Appointments ───────────────────────────────");
        for (Appointment a : appts) {
            long dur = Duration.between(a.getStartTime(), a.getEndTime()).toMinutes();
            System.out.printf("  [%d] %s  |  %d min  |  %s%s%n",
                    a.getId(), fmtTime(a.getStartTime()), dur, friendlyType(a.getType()),
                    isWithin24h(a.getStartTime()) ? "  ⚠" : "");
        }
        System.out.println();
    }

    private static void printWithin24hWarning(String verb) {
        System.out.println();
        System.out.println("  ✗ You cannot " + verb + " this appointment because it is less than");
        System.out.println("    24 hours away.");
        System.out.println("    Contact: " + OWNER_EMAIL);
        System.out.println("    • Modification fee: 100 NIS");
        System.out.println("    • Cancellation fee: 200 NIS");
        System.out.println();
    }

    // ================================================================
    //  UTILITIES
    // ================================================================

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

    private static void printAppointmentList(List<Appointment> appts,
                                             UserDAO userDAO) throws SQLException {
        System.out.println("\n  ── Appointments ──────────────────────────────────────");
        for (Appointment a : appts) {
            long dur = Duration.between(a.getStartTime(), a.getEndTime()).toMinutes();
            Optional<User> u = userDAO.getUserById(String.valueOf(a.getCreatedBy()));
            String username = u.map(User::getUsername).orElse("?");
            System.out.printf("  [%d] %s  |  %d min  |  %s  |  by: %s%n",
                    a.getId(), fmtTime(a.getStartTime()), dur,
                    friendlyType(a.getType()), username);
        }
        System.out.println();
    }

    private static int readInt(int min, int max) {
        try {
            int v = Integer.parseInt(sc.nextLine().trim());
            if (v >= min && v <= max) return v;
        } catch (NumberFormatException e) {
            logger.fine("Invalid integer input provided");
        }
        System.out.println("  Invalid input.\n");
        return -1;
    }

    private static long readLong() {
        try {
            return Long.parseLong(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            logger.fine("Invalid long input provided");
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