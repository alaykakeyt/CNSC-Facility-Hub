package com.example.cnscfacilityhubproject.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cnscfacilityhubproject.R;

import com.example.cnscfacilityhubproject.activities.requestorUI.RequestorNavBarActivity;
import com.example.cnscfacilityhubproject.activities.gsoUI.gsoNavBarActivity;
import com.example.cnscfacilityhubproject.activities.itsoUI.itsoNavBarActivity;
import com.example.cnscfacilityhubproject.activities.sacUI.sacNavBarActivity;
import com.example.cnscfacilityhubproject.models.Booking;
import com.example.cnscfacilityhubproject.utils.AppNotificationHelper;
import com.example.cnscfacilityhubproject.utils.FcmTokenHelper;
import com.example.cnscfacilityhubproject.utils.NotificationPermissionHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;


public class LoginActivity extends AppCompatActivity {



    private void loadBookingData(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        List<Booking> mockBookings = new ArrayList<>();

        mockBookings.add(new Booking(
                "Leadership Seminar",
                "Audio Visual Room",
                "2026-04-15",
                "08:00",
                "12:00",
                "approved"
        ));

        mockBookings.add(new Booking(
                "Faculty Meeting",
                "Conference Hall",
                "2026-04-15",
                "13:00",
                "15:00",
                "approved"
        ));

        mockBookings.add(new Booking(
                "Workshop",
                "Lab 1",
                "2026-04-16",
                "09:00",
                "11:00",
                "approved"
        ));

        for (Booking booking : mockBookings) {
            db.collection("bookings").add(booking);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize notification channels
        AppNotificationHelper.createNotificationChannels(this);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();





        EditText email = findViewById(R.id.etCampusId);
        EditText password = findViewById(R.id.etPassword);
        Button loginBtn = findViewById(R.id.btnLogin);
        TextView signUpTxt = findViewById(R.id.tvSignUp);


        loginBtn.setOnClickListener(v -> {

            String userEmail = email.getText().toString().trim();
            String userPass = password.getText().toString();

            if (userEmail.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter your email.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userPass.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter your password.", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(userEmail, userPass)
                    .addOnCompleteListener(task -> {

                        if (!task.isSuccessful()) {
                            String errorMessage = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Login failed.";

                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            return;
                        }

                        FirebaseUser currentUser = auth.getCurrentUser();

                        if (currentUser == null) {
                            Toast.makeText(LoginActivity.this, "Login failed. Please try again.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        db.collection("users")
                                .document(currentUser.getUid())
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {

                                    if (!documentSnapshot.exists()) {
                                        auth.signOut();
                                        Toast.makeText(
                                                LoginActivity.this,
                                                "User profile not found. Please contact the administrator.",
                                                Toast.LENGTH_LONG
                                        ).show();
                                        return;
                                    }

                                    String userType = documentSnapshot.getString("userType");

                                    if (userType == null || userType.trim().isEmpty()) {
                                        auth.signOut();
                                        Toast.makeText(
                                                LoginActivity.this,
                                                "User role is missing. Please contact the administrator.",
                                                Toast.LENGTH_LONG
                                        ).show();
                                        return;
                                    }

                                    routeUserByRole(userType.trim().toLowerCase(Locale.ROOT), auth);
                                })
                                .addOnFailureListener(e -> {
                                    auth.signOut();
                                    Toast.makeText(
                                            LoginActivity.this,
                                            "Failed to load user profile: " + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                });
                    });
        });




        signUpTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                email.setText("");
                password.setText("");

                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));

            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activityLogin), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void routeUserByRole(String userType, FirebaseAuth auth) {
        Intent intent;

        // Save FCM token for this user
        FcmTokenHelper.saveCurrentUserToken();

        // Request notification permission on Android 13+
        NotificationPermissionHelper.requestNotificationPermission(this);

        switch (userType) {
            case "requestor":
                intent = new Intent(LoginActivity.this, RequestorNavBarActivity.class);
                break;

            case "itso":
                intent = new Intent(LoginActivity.this, itsoNavBarActivity.class);
                break;

            case "sac":
                intent = new Intent(LoginActivity.this, sacNavBarActivity.class);
                break;

            case "gso":
                intent = new Intent(LoginActivity.this, gsoNavBarActivity.class);
                break;

            default:
                auth.signOut();
                Toast.makeText(
                        LoginActivity.this,
                        "Unknown user role: " + userType,
                        Toast.LENGTH_LONG
                ).show();
                return;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

}