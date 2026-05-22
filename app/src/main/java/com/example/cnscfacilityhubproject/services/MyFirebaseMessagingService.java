package com.example.cnscfacilityhubproject.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.cnscfacilityhubproject.utils.AppNotificationHelper;
import com.example.cnscfacilityhubproject.utils.FcmTokenHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Firebase Cloud Messaging service.
 * Handles incoming push notifications and FCM token refresh.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "CNSC_FCM";

    /**
     * Called when a new FCM token is generated.
     * This happens when:
     * - App is first installed
     * - User uninstalls/reinstalls the app
     * - User clears app data
     * - Firebase detects a security issue
     *
     * @param token The new FCM token.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token generated");

        // Save the new token to Firestore if user is logged in
        FcmTokenHelper.saveCurrentUserToken();
    }

    /**
     * Called when a push notification is received.
     * This method handles both foreground and background scenarios.
     *
     * For foreground (app in use): We display the notification locally.
     * For background (app not in use): System handles the notification.
     *
     * @param remoteMessage The incoming message.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Message received from Firebase");

        // Extract notification title and body
        String title = null;
        String body = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        // Extract custom data from the notification
        String requestId = remoteMessage.getData().get("requestId");
        String targetRole = remoteMessage.getData().get("targetRole");
        String targetScreen = remoteMessage.getData().get("targetScreen");
        String notificationType = remoteMessage.getData().get("notificationType");
        String channelId = remoteMessage.getData().get("channelId");

        // Use fallback if notification data is missing
        if (title == null || title.isEmpty()) {
            title = "CNSC Facility Hub";
        }
        if (body == null || body.isEmpty()) {
            body = "You have a new notification";
        }
        if (channelId == null || channelId.isEmpty()) {
            channelId = AppNotificationHelper.REQUEST_UPDATES_CHANNEL;
        }

        // Log notification data (do not log sensitive user info in production)
        Log.d(TAG, "Notification - Title: " + title);
        Log.d(TAG, "Notification - Type: " + notificationType);

        // Display notification if app is in foreground
        AppNotificationHelper.showNotification(
                this,
                title,
                body,
                channelId,
                requestId,
                targetScreen,
                notificationType
        );
    }

    /**
     * Called when message sending fails.
     * This can happen if there are issues with the message payload or delivery.
     *
     * @param errorCode The error code.
     */
    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
        Log.w(TAG, "Messages were deleted on the server");
    }
}
