package main;

import domain.*;
import persistence.*;
import java.sql.Connection;
import java.sql.DriverManager;
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
                    System.out.println("1. View available slots");
                    System.out.println("2. View all appointments");
                    System.out.println("3. Cancel appointment");
                    System.out.println("4. Modify appointment participants");
                    System.out.println("5. Logout");
                } else {
                    System.out.println("\n--- Patient Menu ---");
                    System.out.println("1. View my appointments");
                    System.out.println("2. View available slots");
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
                            null,
                            appointmentDAO,
                            timeSlotDAO
                    );

                    switch (choice) {
                        case 1: {
                            List<TimeSlot> availSlots = admin.viewAvailableSlots();
                            availSlots.forEach(System.out::println);
                            break;
                        }
                        case 2: {
                            List<Appointment> appointments = appointmentDAO.getAllAppointments();
                            appointments.forEach(System.out::println);
                            break;
                        }
                        case 3: {
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
                        case 4: {
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
                        case 5: {
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
                                        .forEach(System.out::println);
                            } catch (NumberFormatException ex) {
                                System.out.println("Unable to match your appointments due to ID format.");
                            }
                            break;
                        }
                        case 2: {
                            List<TimeSlot> availSlots = timeSlotDAO.getAvailableSlots();
                            availSlots.forEach(System.out::println);
                            break;
                        }
                        case 3: {
                            List<TimeSlot> availableSlots = timeSlotDAO.getAvailableSlots();
                            System.out.println("📅 Available Slots:");
                            availableSlots.forEach(System.out::println);

                            System.out.print("Enter Slot ID to book: ");
                            String rawSlotId = scanner.nextLine();
                            System.out.print("Enter appointment type (urgent/follow-up/etc): ");
                            String type = scanner.nextLine();
                            type = type == null ? null : type.trim().toUpperCase();
                            if (type.equals("FOLLOW-UP") || type.equals("FOLLOWUP")) type = "FOLLOW_UP";

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