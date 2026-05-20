package com.example.cnscfacilityhubproject.utils;

import android.content.Intent;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.cnscfacilityhubproject.activities.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

/**
 * Helper to safely verify user role before loading fragments in NavBar activities.
 * 
 * Fixes the race condition where fragments load before async role verification completes.
 * Shows a loading state while verification is in progress.
 */
public class RoleGuardHelper {

    public interface OnRoleVerified {
        void onSuccess();
        void onFailure(String message);
    }

    private final AppCompatActivity activity;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final ProgressBar loadingIndicator;

    public RoleGuardHelper(AppCompatActivity activity, ProgressBar loadingIndicator) {
        this.activity = activity;
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.loadingIndicator = loadingIndicator;
    }

    /**
     * Verify that current user has the expected role before proceeding.
     * Shows loading indicator while verification is in progress.
     * 
     * @param expectedRole The expected role (e.g., "Requestor", "GSO", "ITSO", "SAC")
     * @param callback Callback to execute after verification completes
     */
    public void verifyAndProceed(String expectedRole, OnRoleVerified callback) {
        // Show loading state
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(android.view.View.VISIBLE);
        }

        // Step 1: Check if user is logged in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            handleVerificationFailure("Please log in first.", callback);
            return;
        }

        // Step 2: Verify user role in Firestore
        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Hide loading state
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisibility(android.view.View.GONE);
                    }

                    // Check if profile exists
                    if (!documentSnapshot.exists()) {
                        auth.signOut();
                        handleVerificationFailure(
                                "User profile not found. Please contact the administrator.",
                                callback
                        );
                        return;
                    }

                    // Check if role matches expected role
                    String userType = documentSnapshot.getString("userType");
                    if (userType == null || !expectedRole.equalsIgnoreCase(userType.trim())) {
                        auth.signOut();
                        handleVerificationFailure(
                                "Access denied. Please log in with a " + expectedRole + " account.",
                                callback
                        );
                        return;
                    }

                    // Role verification successful!
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    // Hide loading state
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisibility(android.view.View.GONE);
                    }

                    // Handle error (network, permission denied, etc.)
                    String message = "Unable to verify user role: " + e.getMessage();
                    handleVerificationFailure(message, callback);
                });
    }

    /**
     * Handle verification failure by signing out and redirecting to login
     */
    private void handleVerificationFailure(String message, OnRoleVerified callback) {
        auth.signOut();
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        callback.onFailure(message);

        // Redirect to LoginActivity
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
