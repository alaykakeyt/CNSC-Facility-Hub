package com.example.cnscfacilityhubproject.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.activities.LoginActivity;

/**
 * Helper class to manage notification channels and display local notifications.
 * Creates notification channels for Android O+ and handles notification display.
 */
public class AppNotificationHelper {

    public static final String REQUEST_UPDATES_CHANNEL = "request_updates";
    public static final String STAFF_ASSIGNMENTS_CHANNEL = "staff_assignments";
    public static final String EVENT_REMINDERS_CHANNEL = "event_reminders";
    public static final String CLEARANCE_UPDATES_CHANNEL = "clearance_updates";

    private static final int NOTIFICATION_ID = 1001;

    /**
     * Create notification channels for Android O and above.
     * Must be called once during app initialization.
     *
     * @param context The application context.
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);

            if (notificationManager == null) {
                return;
            }

            // Request Updates Channel
            NotificationChannel requestUpdatesChannel = new NotificationChannel(
                    REQUEST_UPDATES_CHANNEL,
                    "Request Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            requestUpdatesChannel.setDescription(
                    "Notifications about request status and workflow updates"
            );
            notificationManager.createNotificationChannel(requestUpdatesChannel);

            // Staff Assignments Channel
            NotificationChannel staffAssignmentsChannel = new NotificationChannel(
                    STAFF_ASSIGNMENTS_CHANNEL,
                    "Staff Assignments",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            staffAssignmentsChannel.setDescription(
                    "Notifications for assigned GSO, ITSO, and SAC requests"
            );
            notificationManager.createNotificationChannel(staffAssignmentsChannel);

            // Event Reminders Channel
            NotificationChannel eventRemindersChannel = new NotificationChannel(
                    EVENT_REMINDERS_CHANNEL,
                    "Event Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            eventRemindersChannel.setDescription(
                    "Notifications for upcoming approved events and technical reminders"
            );
            notificationManager.createNotificationChannel(eventRemindersChannel);

            // Clearance Updates Channel
            NotificationChannel clearanceUpdatesChannel = new NotificationChannel(
                    CLEARANCE_UPDATES_CHANNEL,
                    "Clearance Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            clearanceUpdatesChannel.setDescription(
                    "Notifications for post-event clearance updates"
            );
            notificationManager.createNotificationChannel(clearanceUpdatesChannel);
        }
    }

    /**
     * Display a local notification.
     * If app is in foreground, use this to show the notification.
     *
     * @param context           The application context.
     * @param title             The notification title.
     * @param body              The notification body text.
     * @param channelId         The notification channel ID.
     * @param requestId         Optional request ID for deep linking.
     * @param targetScreen      Optional target screen for deep linking.
     * @param notificationType  Optional notification type for routing.
     */
    public static void showNotification(
            Context context,
            String title,
            String body,
            String channelId,
            String requestId,
            String targetScreen,
            String notificationType
    ) {
        if (context == null) {
            return;
        }

        // Use defaults if title/body are empty
        if (title == null || title.isEmpty()) {
            title = "CNSC Facility Hub";
        }
        if (body == null || body.isEmpty()) {
            body = "You have a new notification";
        }

        // Use default channel if not specified
        if (channelId == null || channelId.isEmpty()) {
            channelId = REQUEST_UPDATES_CHANNEL;
        }

        // Create intent for notification tap
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Pass data if available
        if (requestId != null && !requestId.isEmpty()) {
            intent.putExtra("requestId", requestId);
        }
        if (targetScreen != null && !targetScreen.isEmpty()) {
            intent.putExtra("targetScreen", targetScreen);
        }
        if (notificationType != null && !notificationType.isEmpty()) {
            intent.putExtra("notificationType", notificationType);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Build notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.mipmap.ic_launcher)  // Use app launcher icon as notification icon
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Show notification
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    /**
     * Show a simple notification with minimal parameters.
     *
     * @param context   The application context.
     * @param title     The notification title.
     * @param body      The notification body text.
     */
    public static void showSimpleNotification(Context context, String title, String body) {
        showNotification(context, title, body, REQUEST_UPDATES_CHANNEL, null, null, null);
    }
}
