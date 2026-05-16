package com.example.cnscfacilityhubproject.activities.gsoUI;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.activities.LoginActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class gsoNavBarActivity extends AppCompatActivity {

    private LinearLayout navHome, navRequests, navReports, navUsers, navProfile;
    private TextView textHome, textRequests, textReports, textUsers, textProfile;
    private ImageView iconHome, iconRequests, iconReports, iconUsers, iconProfile;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gso_nav_bar);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (!ensureUserLoggedIn()) {
            return;
        }

        verifyUserRole("GSO");

        navHome = findViewById(R.id.navHome);
        navRequests = findViewById(R.id.navRequests);
        navReports = findViewById(R.id.navReports);
        navUsers = findViewById(R.id.navUsers);
        navProfile = findViewById(R.id.navProfile);

        textHome = findViewById(R.id.textHome);
        textRequests = findViewById(R.id.textRequests);
        textReports = findViewById(R.id.textReports);
        textUsers = findViewById(R.id.textUsers);
        textProfile = findViewById(R.id.textProfile);

        iconHome = findViewById(R.id.iconHome);
        iconRequests = findViewById(R.id.iconRequests);
        iconReports = findViewById(R.id.iconReports);
        iconUsers = findViewById(R.id.iconUsers);
        iconProfile = findViewById(R.id.iconProfile);

        if (savedInstanceState == null) {
            loadFragment(new gsoHomeFragment());
            setSelectedTab(Tab.HOME);
        }

        navHome.setOnClickListener(v -> {
            loadFragment(new gsoHomeFragment());
            setSelectedTab(Tab.HOME);
        });

        navRequests.setOnClickListener(v -> {
            loadFragment(new gsoRequestsFragment());
            setSelectedTab(Tab.REQUESTS);
        });

        navReports.setOnClickListener(v -> {
            loadFragment(new gsoReportsFragment());
            setSelectedTab(Tab.REPORTS);
        });

        navUsers.setOnClickListener(v -> {
            loadFragment(new gsoUsersFragment());
            setSelectedTab(Tab.USERS);
        });

        navProfile.setOnClickListener(v -> {
            loadFragment(new gsoProfileFragment());
            setSelectedTab(Tab.PROFILE);
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.gso_fragment_container, fragment)
                .commit();
    }

    private void setSelectedTab(Tab selectedTab) {
        resetTabs();

        switch (selectedTab) {
            case HOME:
                textHome.setTextColor(Color.parseColor("#970705"));
                iconHome.setImageTintList(ColorStateList.valueOf(Color.parseColor("#970705")));
                break;

            case REQUESTS:
                textRequests.setTextColor(Color.parseColor("#970705"));
                iconRequests.setImageTintList(ColorStateList.valueOf(Color.parseColor("#970705")));
                break;

            case REPORTS:
                textReports.setTextColor(Color.parseColor("#970705"));
                iconReports.setImageTintList(ColorStateList.valueOf(Color.parseColor("#970705")));
                break;

            case USERS:
                textUsers.setTextColor(Color.parseColor("#970705"));
                iconUsers.setImageTintList(ColorStateList.valueOf(Color.parseColor("#970705")));
                break;

            case PROFILE:
                textProfile.setTextColor(Color.parseColor("#970705"));
                iconProfile.setImageTintList(ColorStateList.valueOf(Color.parseColor("#970705")));
                break;
        }
    }

    private void resetTabs() {
        int dark = Color.parseColor("#313131");

        textHome.setTextColor(dark);
        textRequests.setTextColor(dark);
        textReports.setTextColor(dark);
        textUsers.setTextColor(dark);
        textProfile.setTextColor(dark);

        iconHome.setImageTintList(ColorStateList.valueOf(dark));
        iconRequests.setImageTintList(ColorStateList.valueOf(dark));
        iconReports.setImageTintList(ColorStateList.valueOf(dark));
        iconUsers.setImageTintList(ColorStateList.valueOf(dark));
        iconProfile.setImageTintList(ColorStateList.valueOf(dark));
    }



    private boolean ensureUserLoggedIn() {
        if (auth.getCurrentUser() != null) {
            return true;
        }

        redirectToLogin("Please log in first.");
        return false;
    }

    private void verifyUserRole(String expectedRole) {
        if (auth.getCurrentUser() == null) {
            redirectToLogin("Please log in first.");
            return;
        }

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {
                        auth.signOut();
                        redirectToLogin("User profile not found. Please contact the administrator.");
                        return;
                    }

                    String userType = documentSnapshot.getString("userType");

                    if (userType == null || !expectedRole.equalsIgnoreCase(userType.trim())) {
                        auth.signOut();
                        redirectToLogin("Access denied. Please log in with a " + expectedRole + " account.");
                    }
                })
                .addOnFailureListener(e -> {
                    auth.signOut();
                    redirectToLogin("Unable to verify user role.");
                });
    }

    private void redirectToLogin(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }


    private enum Tab {
        HOME, REQUESTS, REPORTS, USERS, PROFILE
    }


    }
