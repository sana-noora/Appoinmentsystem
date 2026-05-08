package service;

import domain.Appointment;
import domain.User;
import io.github.cdimascio.dotenv.Dotenv;
import persistence.AppointmentDAO;
import persistence.UserDAO;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

public class ReminderService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int    SMTP_PORT = 587;

    private static final DateTimeFormatter DISPLAY_DT =
            DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy  HH:mm", Locale.ENGLISH);

    private final String senderEmail;
    private final String senderPassword;

    private final AppointmentDAO appointmentDAO;
    private final UserDAO        userDAO;

    public ReminderService(AppointmentDAO appointmentDAO, UserDAO userDAO) {
        this.appointmentDAO = appointmentDAO;
        this.userDAO        = userDAO;

        // Load credentials from .env file (placed in project root)
        Dotenv dotenv = Dotenv.load();
        this.senderEmail    = dotenv.get("EMAIL_USERNAME");
        this.senderPassword = dotenv.get("EMAIL_PASSWORD");
    }

    public void sendReminders() {
        try {
            List<Appointment> all = appointmentDAO.getAllAppointments();
            OffsetDateTime now  = OffsetDateTime.now(ZoneOffset.UTC);
            OffsetDateTime in24 = now.plusHours(24);

            int sent = 0;
            for (Appointment a : all) {
                if (!Appointment.STATUS_CONFIRMED.equals(a.getStatus())) continue;
                if (a.getStartTime() == null) continue;
                if (a.getStartTime().isAfter(now) && a.getStartTime().isBefore(in24)) {
                    Optional<User> optUser = userDAO.getUserById(
                            String.valueOf(a.getCreatedBy()));
                    if (!optUser.isPresent()) continue;

                    User u = optUser.get();
                    if (u.getEmail() == null || u.getEmail().trim().isEmpty()) continue;

                    sendEmail(u, a);
                    sent++;
                }
            }

            if (sent == 0)
                System.out.println("[REMINDER] No upcoming appointments within 24h.");
            else
                System.out.println("[REMINDER] " + sent + " reminder(s) sent.");

        } catch (SQLException e) {
            System.err.println("[REMINDER ERROR] DB error: " + e.getMessage());
        }
    }

    private void sendEmail(User user, Appointment appointment) {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(senderEmail));
            msg.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(user.getEmail()));
            msg.setSubject("Reminder: Your apartment visit is coming up soon!");
            msg.setText(buildBody(user, appointment));
            Transport.send(msg);
            System.out.println("[REMINDER] Email sent to: " + user.getEmail());
        } catch (MessagingException e) {
            System.err.println("[REMINDER ERROR] Failed to send to " +
                    user.getEmail() + ": " + e.getMessage());
        }
    }

    private String buildBody(User user, Appointment appointment) {
        long durationMin = Duration.between(
                appointment.getStartTime(), appointment.getEndTime()).toMinutes();

        String startStr = appointment.getStartTime()
                .atZoneSameInstant(ZoneId.systemDefault())
                .format(DISPLAY_DT);

        StringBuilder sb = new StringBuilder();
        sb.append("Dear ").append(user.getName()).append(",\n\n");
        sb.append("This is a reminder that you have an upcoming apartment visit ")
          .append("within the next 24 hours.\n\n");
        sb.append("── Appointment Details ──────────────────────\n");
        sb.append("  Date & Time  : ").append(startStr).append("\n");
        sb.append("  Duration     : ").append(durationMin).append(" minutes\n");
        sb.append("  Type         : ").append(friendlyType(appointment.getType())).append("\n");
        if (appointment.isGroup())
            sb.append("  Visitors     : ").append(appointment.getParticipantsCount()).append("\n");
        sb.append("─────────────────────────────────────────────\n\n");
        sb.append("Policy reminders:\n");
        sb.append("  • Cancellation fee (< 24h): 200 NIS\n");
        sb.append("  • Modification fee (< 24h): 100 NIS\n\n");
        sb.append("Contact: nooraqaradeh3@gmail.com\n\n");
        sb.append("Best regards,\n");
        sb.append("Apartment Visit Scheduling System\n");
        return sb.toString();
    }

    private String friendlyType(String t) {
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
}