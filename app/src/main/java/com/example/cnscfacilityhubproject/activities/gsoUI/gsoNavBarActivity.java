package com.example.cnscfacilityhubproject.activities.gsoUI;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.activities.LoginActivity;
import com.example.cnscfacilityhubproject.utils.RoleGuardHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class gsoNavBarActivity extends AppCompatActivity {

    private LinearLayout navHome, navRequests, navReports, navUsers, navProfile;
    private TextView textHome, textRequests, textReports, textUsers, textProfile;
    private ImageView iconHome, iconRequests, iconReports, iconUsers, iconProfile;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private static final String ACTIVE_COLOR = "#970705";
    private static final String INACTIVE_COLOR = "#313131";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gso_nav_bar);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (!ensureUserLoggedIn()) {
            return;
        }

        bindNavViews();
        setupNavClickListeners();

        ProgressBar progressBar = findViewById(R.id.roleVerificationProgress);
        RoleGuardHelper roleGuard = new RoleGuardHelper(this, progressBar);

        roleGuard.verifyAndProceed("GSO", new RoleGuardHelper.OnRoleVerified() {
            @Override
            public void onSuccess() {
                if (savedInstanceState == null) {
                    loadFragment(new gsoHomeFragment());
                    setSelectedTab(Tab.HOME);
                }
            }

            @Override
            public void onFailure(String message) {
                // Already handled by RoleGuardHelper
            }
        });
    }

    private void bindNavViews() {
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
    }

    private void setupNavClickListeners() {
        navHome.setOnClickListener(v -> {
            loadFragment(new gsoHomeFragment());
            setSelectedTab(Tab.HOME);
        });

        navReports.setOnClickListener(v -> {
            loadFragment(new gsoReportsFragment());
            setSelectedTab(Tab.REPORTS);
        });

        navUsers.setOnClickListener(v -> {
            loadFragment(new gsoUsersFragment());
            setSelectedTab(Tab.USERS);
        });

        // This is still navRequests because your XML ID is still navRequests,
        // but it now opens the Notification tab.
        navRequests.setOnClickListener(v -> {
            loadFragment(new gsoRequestsFragment());
            setSelectedTab(Tab.NOTIFICATION);
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

    private void loadFragmentWithBackStack(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.gso_fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void navigateToRequests(String filter) {
        Bundle bundle = new Bundle();
        bundle.putString("filter", filter);
        gsoRequestsFragment fragment = new gsoRequestsFragment();
        fragment.setArguments(bundle);
        loadFragmentWithBackStack(fragment);
        setSelectedTab(Tab.NOTIFICATION);
    }

    public void navigateToReports() {
        loadFragmentWithBackStack(new gsoReportsFragment());
        setSelectedTab(Tab.REPORTS);
    }

    public void navigateToUsers() {
        loadFragmentWithBackStack(new gsoUsersFragment());
        setSelectedTab(Tab.USERS);
    }

    public void setSelectedTab(Tab selectedTab) {
        resetTabs();

        int active = Color.parseColor(ACTIVE_COLOR);

        switch (selectedTab) {
            case HOME:
                textHome.setTextColor(active);
                iconHome.setImageTintList(ColorStateList.valueOf(active));
                break;

            case REPORTS:
                textReports.setTextColor(active);
                iconReports.setImageTintList(ColorStateList.valueOf(active));
                break;

            case USERS:
                textUsers.setTextColor(active);
                iconUsers.setImageTintList(ColorStateList.valueOf(active));
                break;

            case NOTIFICATION:
                textRequests.setTextColor(active);
                iconRequests.setImageTintList(ColorStateList.valueOf(active));
                break;

            case PROFILE:
                textProfile.setTextColor(active);
                iconProfile.setImageTintList(ColorStateList.valueOf(active));
                break;
        }
    }

    private void resetTabs() {
        int dark = Color.parseColor(INACTIVE_COLOR);

        textHome.setTextColor(dark);
        textReports.setTextColor(dark);
        textUsers.setTextColor(dark);
        textRequests.setTextColor(dark);
        textProfile.setTextColor(dark);

        iconHome.setImageTintList(ColorStateList.valueOf(dark));
        iconReports.setImageTintList(ColorStateList.valueOf(dark));
        iconUsers.setImageTintList(ColorStateList.valueOf(dark));
        iconRequests.setImageTintList(ColorStateList.valueOf(dark));
        iconProfile.setImageTintList(ColorStateList.valueOf(dark));
    }

    private boolean ensureUserLoggedIn() {
        if (auth.getCurrentUser() != null) {
            return true;
        }

        redirectToLogin("Please log in first.");
        return false;
    }

    private void redirectToLogin(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }

    public enum Tab {
        HOME,
        REPORTS,
        USERS,
        NOTIFICATION,
        PROFILE
    }
}