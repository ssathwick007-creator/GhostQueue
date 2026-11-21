package com.ghostqueue.service;

import com.ghostqueue.model.InAppNotification;
import com.ghostqueue.supabase.SessionManager;
import com.ghostqueue.supabase.SupabaseClient;
import com.ghostqueue.supabase.SupabaseConfig;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationService {

    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());
    private static NotificationService instance;

    private final SupabaseClient client = SupabaseClient.getInstance();

    private NotificationService() {}

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    public List<InAppNotification> getNotificationsForCurrentUser() {
        String userId = SessionManager.getInstance().getCurrentSession() != null ?
                SessionManager.getInstance().getCurrentSession().getUserId() : null;
        if (userId == null) return Collections.emptyList();

        try {
            Map<String, String> params = new HashMap<>();
            params.put("user_id", "eq." + userId);
            params.put("select", "*");
            params.put("order", "created_at.desc");
            params.put("limit", "20");
            SupabaseClient.Response resp = client.get(SupabaseConfig.getRestEndpoint("notifications"), params);
            return mapNotifications(resp.asList());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load notifications for " + userId, e);
            return Collections.emptyList();
        }
    }

    public void markAsRead(long notificationId) {
        try {
            Map<String, Object> data = Map.of("is_read", true);
            Map<String, String> filter = Map.of("notification_id", "eq." + notificationId);
            client.patch(SupabaseConfig.getRestEndpoint("notifications"), filter, data);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to mark notification as read", e);
        }
    }

    public void markAllAsRead() {
        String userId = SessionManager.getInstance().getCurrentSession() != null ?
                SessionManager.getInstance().getCurrentSession().getUserId() : null;
        if (userId == null) return;
        try {
            Map<String, Object> data = Map.of("is_read", true);
            Map<String, String> filter = Map.of("user_id", "eq." + userId);
            client.patch(SupabaseConfig.getRestEndpoint("notifications"), filter, data);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to mark all notifications as read", e);
        }
    }

    public void notifyAppointmentBooked(com.ghostqueue.model.Appointment appt) {
        String userId = SessionManager.getInstance().getCurrentSession() != null ?
                SessionManager.getInstance().getCurrentSession().getUserId() : null;
        if (userId != null) {
            createNotification(userId, "Appointment Booked",
                    "Your consultation with " + appt.getDoctorName() + " on " + appt.getAppointmentDate() + " at " + appt.getAppointmentTime() + " is confirmed.",
                    "APPOINTMENT");
        }
    }

    public void notifyAppointmentCancelled(com.ghostqueue.model.Appointment appt, String reason) {
        String userId = SessionManager.getInstance().getCurrentSession() != null ?
                SessionManager.getInstance().getCurrentSession().getUserId() : null;
        if (userId != null) {
            createNotification(userId, "Appointment Cancelled",
                    "Appointment " + appt.getId() + " has been cancelled. Reason: " + reason,
                    "CANCELLATION");
        }
    }

    public void createNotification(String userId, String title, String message, String type) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("user_id", userId);
            data.put("title", title);
            data.put("message", message);
            data.put("type", type != null ? type : "INFO");
            data.put("is_read", false);

            client.post(SupabaseConfig.getRestEndpoint("notifications"), data);
            LOGGER.info("Notification dispatched to user " + userId + ": " + title);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create notification", e);
        }
    }

    public int getUnreadCount() {
        List<InAppNotification> list = getNotificationsForCurrentUser();
        int count = 0;
        for (InAppNotification n : list) {
            if (!n.isRead()) count++;
        }
        return count;
    }

    private List<InAppNotification> mapNotifications(List<Map<String, Object>> rows) {
        List<InAppNotification> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            long id = ((Number) r.getOrDefault("notification_id", 0)).longValue();
            String userId = (String) r.get("user_id");
            String title = (String) r.getOrDefault("title", "Hospital Notification");
            String msg = (String) r.getOrDefault("message", "");
            String type = (String) r.getOrDefault("type", "INFO");
            boolean read = Boolean.TRUE.equals(r.get("is_read"));

            LocalDateTime createdAt = LocalDateTime.now();
            if (r.get("created_at") instanceof String cs) {
                try { createdAt = LocalDateTime.parse(cs.substring(0, 19)); } catch (Exception ignored) {}
            }

            list.add(new InAppNotification(id, userId, title, msg, type, read, createdAt));
        }
        return list;
    }
}
