package main;

import domain.*;
import persistence.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        String url = "jdbc:postgresql://localhost:5432/appointment_system";
        String dbUser = "postgres";
        String dbPassword = "123456";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword)) {
            UserDAO userDAO = new UserDAO(conn);
            AppointmentDAO appointmentDAO = new AppointmentDAO(conn);
            TimeSlotDAO timeSlotDAO = new TimeSlotDAO(conn);
            ScheduleDAO scheduleDAO = new ScheduleDAO(conn);

            System.out.print("Enter username: ");
            String inputUsername = scanner.nextLine();
            System.out.print("Enter password: ");
            String inputPassword = scanner.nextLine();

            Optional<User> maybeUser = userDAO.login(inputUsername, inputPassword);
            if (!maybeUser.isPresent()) {
                System.out.println("❌ Login failed!");
                return;
            }
            User user = maybeUser.get();

            System.out.println("✅ Welcome " + user.getName() + " (" + user.getRole() + ")");

            boolean running = true;
            while (running) {
                if (user.getRole() == User.Role.ADMIN) {
                    System.out.println("\n--- Admin Menu ---");
                    System.out.println("1. View work days");
                    System.out.println("2. Add work day");
                    System.out.println("3. View available slots by day");
                    System.out.println("4. Add slot to a work day");
                    System.out.println("5. View all appointments");
                    System.out.println("6. Cancel appointment");
                    System.out.println("7. Modify appointment participants");
                    System.out.println("8. Logout");
                } else {
                    System.out.println("\n--- Patient Menu ---");
                    System.out.println("1. View my appointments");
                    System.out.println("2. View available slots by day");
                    System.out.println("3. Book appointment");
                    System.out.println("4. Logout");
                }

                System.out.print("Choose option: ");
                String rawChoice = scanner.nextLine();
                int choice;
                try {
                    choice = Integer.parseInt(rawChoice);
                } catch (NumberFormatException ex) {
                    System.out.println("Invalid choice.");
                    continue;
                }

                if (user.getRole() == User.Role.ADMIN) {
                    Admin admin = new Admin(
                            user.getId(),
                            user.getName(),
                            user.getEmail(),
                            user.getPhoneNumber(),
                            user.getUsername(),
                            appointmentDAO,
                            timeSlotDAO,
                            scheduleDAO
                    );

                    switch (choice) {
                        case 1: {
                            List<Schedule> days = admin.viewWorkDays();
                            if (days.isEmpty()) {
                                System.out.println("No work days found.");
                            } else {
                                days.forEach(d -> System.out.println(d.getId() + " - " + d.getWorkDate()));
                            }
                            break;
                        }
                        case 2: {
                            System.out.print("Enter work day (YYYY-MM-DD): ");
                            String dateStr = scanner.nextLine();
                            try {
                                LocalDate date = LocalDate.parse(dateStr);
                                admin.addWorkDay(date);
                                System.out.println("✅ Work day added: " + date);
                            } catch (Exception ex) {
                                System.out.println("Invalid date format.");
                            }
                            break;
                        }
                        case 3: {
                            System.out.print("Enter schedule ID: ");
                            String raw = scanner.nextLine();
                            try {
                                long scheduleId = Long.parseLong(raw);
                                List<TimeSlot> slots = admin.viewAvailableSlotsByDay(scheduleId);
                                if (slots.isEmpty()) System.out.println("No available slots for that day.");
                                else slots.forEach(System.out::println);
                            } catch (NumberFormatException ex) {
                                System.out.println("Invalid schedule ID.");
                            }
                            break;
                        }
                        case 4: {
                            System.out.print("Enter schedule ID: ");
                            String rawSid = scanner.nextLine();
                            System.out.print("Enter start time (HH:mm): ");
                            String startStr = scanner.nextLine();
                            System.out.print("Enter end time (HH:mm): ");
                            String endStr = scanner.nextLine();
                            try {
                                long scheduleId = Long.parseLong(rawSid);
                                Schedule schedule = scheduleDAO.getScheduleById(scheduleId);
                                if (schedule == null) {
                                    System.out.println("Schedule not found.");
                                    break;
                                }
                                LocalDate day = schedule.getWorkDate();
                                LocalTime startTime = LocalTime.parse(startStr);
                                LocalTime endTime = LocalTime.parse(endStr);
                                OffsetDateTime start = OffsetDateTime.of(day, startTime, ZoneOffset.UTC);
                                OffsetDateTime end = OffsetDateTime.of(day, endTime, ZoneOffset.UTC);
                                if (!end.isAfter(start)) {
                                    System.out.println("End time must be after start time.");
                                    break;
                                }
                                admin.addSlot(scheduleId, start, end);
                                System.out.println("✅ Slot added for " + day + " from " + start + " to " + end);
                            } catch (NumberFormatException ex) {
                                System.out.println("Invalid schedule ID.");
                            } catch (Exception ex) {
                                System.out.println("Invalid time format. Use HH:mm");
                            }
                            break;
                        }
                        case 5: {
                            List<Appointment> appointments = appointmentDAO.getAllAppointments();
                            appointments.forEach(System.out::println);
                            break;
                        }
                        case 6: {
                            System.out.print("Enter appointment ID to cancel: ");
                            String raw = scanner.nextLine();
                            try {
                                long cancelId = Long.parseLong(raw);
                                admin.cancelAppointment(cancelId);
                            } catch (NumberFormatException ex) {
                                System.out.println("Invalid ID.");
                            }
                            break;
                        }
                        case 7: {
                            System.out.print("Enter appointment ID to modify: ");
                            String rawId = scanner.nextLine();
                            System.out.print("Enter new participants count: ");
                            String rawCount = scanner.nextLine();
                            try {
                                long modifyId = Long.parseLong(rawId);
                                int newCount = Integer.parseInt(rawCount);
                                admin.modifyAppointment(modifyId, newCount);
                            } catch (NumberFormatException ex) {
                                System.out.println("Invalid input.");
                            }
                            break;
                        }
                        case 8: {
                            user.logout();
                            running = false;
                            System.out.println("👋 Logged out.");
                            break;
                        }
                        default:
                            System.out.println("Invalid choice.");
                    }
                } else {
                    switch (choice) {
                        case 1: {
                            List<Appointment> myAppointments = appointmentDAO.getAllAppointments();
                            try {
                                long uid = Long.parseLong(user.getId());
                                myAppointments.stream()
                                        .filter(ap -> ap.getCreatedBy() == uid)
                                        .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                                        .forEach(System.out::println);
                            } catch (NumberFormatException ex) {
                                System.out.println("Unable to match your appointments due to ID format.");
                            }
                            break;
                        }
                        case 2: {
                            List<Schedule> schedules = scheduleDAO.getAllSchedules();
                            if (schedules.isEmpty()) {
                                System.out.println("No work days available.");
                                break;
                            }
                            System.out.println("Work days:");
                            schedules.forEach(d -> System.out.println(d.getId() + " - " + d.getWorkDate()));

                            System.out.print("Choose a schedule ID: ");
                            String rawDay = scanner.nextLine();
                            try {
                                long scheduleId = Long.parseLong(rawDay);
                                List<TimeSlot> slots = timeSlotDAO.getAvailableSlotsByScheduleId(scheduleId);
                                if (slots.isEmpty()) System.out.println("No available slots for that day.");
                                else slots.forEach(System.out::println);
                            } catch (NumberFormatException ex) {
                                System.out.println("Invalid schedule ID.");
                            }
                            break;
                        }
                        case 3: {
                            List<Schedule> schedules = scheduleDAO.getAllSchedules();
                            if (schedules.isEmpty()) {
                                System.out.println("No work days available.");
                                break;
                            }
                            System.out.println("Work days:");
                            schedules.forEach(d -> System.out.println(d.getId() + " - " + d.getWorkDate()));

                            System.out.print("Choose a schedule ID: ");
                            String rawDay = scanner.nextLine();
                            long scheduleId;
                            try {
                                scheduleId = Long.parseLong(rawDay);
                            } catch (NumberFormatException ex) {
                                System.out.println("Invalid schedule ID.");
                                break;
                            }

                            List<TimeSlot> availableSlots = timeSlotDAO.getAvailableSlotsByScheduleId(scheduleId);
                            if (availableSlots.isEmpty()) {
                                System.out.println("No available slots for that day.");
                                break;
                            }

                            System.out.println("📅 Available Slots:");
                            availableSlots.forEach(System.out::println);

                            System.out.print("Enter Slot ID to book: ");
                            String rawSlotId = scanner.nextLine();
                            System.out.print("Enter appointment type (e.g., URGENT/FOLLOW_UP/ASSESSMENT/VIRTUAL/IN_PERSON/INDIVIDUAL/GROUP): ");
                            String type = scanner.nextLine();
                            type = type == null ? null : type.trim().toUpperCase();
                            if ("FOLLOW-UP".equals(type) || "FOLLOWUP".equals(type)) type = "FOLLOW_UP";

                            System.out.print("Enter participants count: ");
                            String rawCount = scanner.nextLine();

                            try {
                                long chosenSlotId = Long.parseLong(rawSlotId);
                                int participantsCount = Integer.parseInt(rawCount);

                                TimeSlot chosenSlot = timeSlotDAO.getTimeSlotById(chosenSlotId);
                                if (chosenSlot != null && chosenSlot.isAvailable()) {
                                    Appointment newAppointment = new Appointment(
                                            type,
                                            "CONFIRMED",
                                            chosenSlot.getStartTime(),
                                            chosenSlot.getEndTime(),
                                            participantsCount,
                                            5,
                                            Long.parseLong(user.getId()),
                                            chosenSlotId
                                    );

                                    appointmentDAO.addAppointment(newAppointment);
                                    timeSlotDAO.updateAvailability(chosenSlotId, false);

                                    System.out.println("✅ Appointment booked successfully!");
                                } else {
                                    System.out.println("❌ Slot not available!");
                                }
                            } catch (NumberFormatException ex) {
                                System.out.println("Invalid numeric input.");
                            }
                            break;
                        }
                        case 4: {
                            user.logout();
                            running = false;
                            System.out.println("👋 Logged out.");
                            break;
                        }
                        default:
                            System.out.println("Invalid choice.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        scanner.close();
    }
}