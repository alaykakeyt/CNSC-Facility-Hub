package com.example.cnscfacilityhubproject.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to manage FCM (Firebase Cloud Messaging) tokens.
 * Saves and removes FCM tokens in Firestore under users/{uid}/fcmTokens/{tokenHash}.
 */
public class FcmTokenHelper {

    private static final String TAG = "CNSC_FCM";
    private static final String TOKENS_COLLECTION = "fcmTokens";

    /**
     * Save the current user's FCM token to Firestore.
     * Must be called when user is logged in.
     */
    public static void saveCurrentUserToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "Failed to get FCM token", task.getException());
                return;
            }

            String token = task.getResult();
            // TODO: REMOVE TOKEN LOGGING BEFORE PRODUCTION
            Log.d(TAG, "Current FCM Token: " + token);

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Log.w(TAG, "Cannot save FCM token to Firestore: User not logged in");
                return;
            }
            saveTokenForUser(currentUser.getUid(), token);
        });
    }

    /**
     * Save FCM token for a specific user.
     *
     * @param uid   The user's UID
     * @param token The FCM token
     */
    public static void saveTokenForUser(String uid, String token) {
        if (uid == null || uid.isEmpty() || token == null || token.isEmpty()) {
            Log.w(TAG, "Cannot save FCM token: Invalid UID or token");
            return;
        }

        try {
            // Create a hash of the token to use as document ID
            String tokenHash = hashToken(token);

            // Create token document
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("token", token);
            tokenData.put("platform", "android");
            tokenData.put("active", true);
            tokenData.put("createdAt", FieldValue.serverTimestamp());
            tokenData.put("updatedAt", FieldValue.serverTimestamp());

            // Save to Firestore: users/{uid}/fcmTokens/{tokenHash}
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .collection(TOKENS_COLLECTION)
                    .document(tokenHash)
                    .set(tokenData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "FCM token saved for user: " + uid);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save FCM token", e);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error saving FCM token", e);
        }
    }

    /**
     * Remove (mark as inactive) the current user's FCM token.
     * Called on logout.
     *
     * @param onComplete Callback to run when removal is finished (successfully or not)
     */
    public static void removeCurrentUserToken(Runnable onComplete) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "Cannot remove FCM token: User not logged in");
            if (onComplete != null) onComplete.run();
            return;
        }

        String uid = currentUser.getUid();

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "Failed to get FCM token for removal", task.getException());
                if (onComplete != null) onComplete.run();
                return;
            }

            String token = task.getResult();
            removeTokenForUser(uid, token, onComplete);
        });
    }

    /**
     * Remove (mark as inactive) FCM token for a specific user.
     *
     * @param uid        The user's UID
     * @param token      The FCM token
     * @param onComplete Callback to run when removal is finished
     */
    public static void removeTokenForUser(String uid, String token, Runnable onComplete) {
        if (uid == null || uid.isEmpty() || token == null || token.isEmpty()) {
            Log.w(TAG, "Cannot remove FCM token: Invalid UID or token");
            if (onComplete != null) onComplete.run();
            return;
        }

        try {
            String tokenHash = hashToken(token);

            // Mark token as inactive instead of deleting
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("active", false);
            tokenData.put("updatedAt", FieldValue.serverTimestamp());

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .collection(TOKENS_COLLECTION)
                    .document(tokenHash)
                    .set(tokenData, SetOptions.merge())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "FCM token marked inactive for user: " + uid);
                        } else {
                            Log.e(TAG, "Failed to remove FCM token", task.getException());
                        }
                        if (onComplete != null) onComplete.run();
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error removing FCM token", e);
            if (onComplete != null) onComplete.run();
        }
    }

    /**
     * Generate SHA-256 hash of the FCM token.
     * Used as document ID to avoid special characters in Firestore paths.
     *
     * @param token The FCM token
     * @return The hex string of the SHA-256 hash
     */
    private static String hashToken(String token) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
