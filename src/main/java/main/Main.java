package main;

import domain.*;
import persistence.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
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

            // تسجيل الدخول
            System.out.print("Enter username: ");
            String inputUsername = scanner.nextLine();
            System.out.print("Enter password: ");
            String inputPassword = scanner.nextLine();

            User user = userDAO.login(inputUsername, inputPassword);

            if (user == null) {
                System.out.println("❌ Login failed!");
                return;
            }

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
                int choice = Integer.parseInt(scanner.nextLine());

                if (user.getRole() == User.Role.ADMIN) {
                    Admin admin = new Admin(
                            user.getId(),
                            user.getName(),
                            user.getEmail(),
                            user.getPhoneNumber(),
                            user.getUsername(),
                            inputPassword,
                            appointmentDAO,
                            timeSlotDAO
                    );

                    switch (choice) {
                        case 1:
                            List<TimeSlot> slots = admin.viewAvailableSlots();
                            slots.forEach(System.out::println);
                            break;
                        case 2:
                            List<Appointment> appointments = appointmentDAO.getAllAppointments();
                            appointments.forEach(System.out::println);
                            break;
                        case 3:
                            System.out.print("Enter appointment ID to cancel: ");
                            long cancelId = Long.parseLong(scanner.nextLine());
                            admin.cancelAppointment(cancelId);
                            break;
                        case 4:
                            System.out.print("Enter appointment ID to modify: ");
                            long modifyId = Long.parseLong(scanner.nextLine());
                            System.out.print("Enter new participants count: ");
                            int newCount = Integer.parseInt(scanner.nextLine());
                            admin.modifyAppointment(modifyId, newCount);
                            break;
                        case 5:
                            user.logout();
                            running = false;
                            System.out.println("👋 Logged out.");
                            break;
                    }
                } else {
                    switch (choice) {
                        case 1:
                            // استرجاع مواعيد المستخدم من جدول appointments
                            List<Appointment> myAppointments = appointmentDAO.getAllAppointments();
                            myAppointments.stream()
                                    .filter(ap -> ap.getCreatedBy() == Long.parseLong(user.getId()))
                                    .forEach(System.out::println);
                            break;
                        case 2:
                            List<TimeSlot> slots = timeSlotDAO.getAvailableSlots();
                            slots.forEach(System.out::println);
                            break;
                        case 3:
                            // حجز موعد جديد
                            List<TimeSlot> availableSlots = timeSlotDAO.getAvailableSlots();
                            System.out.println("📅 Available Slots:");
                            availableSlots.forEach(System.out::println);

                            System.out.print("Enter Slot ID to book: ");
                            long chosenSlotId = Long.parseLong(scanner.nextLine());

                            System.out.print("Enter appointment type (urgent/follow-up/etc): ");
                            String type = scanner.nextLine();

                            System.out.print("Enter participants count: ");
                            int participantsCount = Integer.parseInt(scanner.nextLine());

                            TimeSlot chosenSlot = timeSlotDAO.getTimeSlotById(chosenSlotId);
                            if (chosenSlot != null && chosenSlot.isAvailable()) {
                                Appointment newAppointment = new Appointment(
                                        type,
                                        "CONFIRMED",
                                        chosenSlot.getStartTime(),
                                        chosenSlot.getEndTime(),
                                        participantsCount,
                                        5, // مثال: الحد الأقصى للمشاركين
                                        Long.parseLong(user.getId()),
                                        chosenSlotId
                                );

                                appointmentDAO.addAppointment(newAppointment);
                                timeSlotDAO.updateAvailability(chosenSlotId, false);

                                System.out.println("✅ Appointment booked successfully!");
                            } else {
                                System.out.println("❌ Slot not available!");
                            }
                            break;
                        case 4:
                            user.logout();
                            running = false;
                            System.out.println("👋 Logged out.");
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        scanner.close();
    }
}
