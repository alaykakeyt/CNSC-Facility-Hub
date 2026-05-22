package com.example.cnscfacilityhubproject.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Helper class to handle Android 13+ notification permission requests.
 * Requests POST_NOTIFICATIONS permission at runtime for API 33+.
 */
public class NotificationPermissionHelper {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    /**
     * Request notification permission on Android 13 and above.
     * On lower API levels, this method does nothing.
     *
     * @param activity The activity context from which to request the permission.
     */
    public static void requestNotificationPermission(Activity activity) {
        if (activity == null) {
            return;
        }

        // Only request on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if permission is already granted
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted, request it
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            }
        }
    }

    /**
     * Check if notification permission is granted.
     * On API levels below 33, this method returns true.
     *
     * @param activity The activity context.
     * @return true if permission is granted or API < 33, false otherwise.
     */
    public static boolean isNotificationPermissionGranted(Activity activity) {
        if (activity == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        // Permission not required on API < 33
        return true;
    }
}
