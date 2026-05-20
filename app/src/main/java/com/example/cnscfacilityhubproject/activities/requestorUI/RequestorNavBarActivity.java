package com.example.cnscfacilityhubproject.activities.requestorUI;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.utils.RequestDataHelper;
import com.example.cnscfacilityhubproject.utils.RoleGuardHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import android.content.Intent;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.cnscfacilityhubproject.activities.LoginActivity;

public class RequestorNavBarActivity extends AppCompatActivity {

    private LinearLayout navHome, navRequest, navRequests, navNotification, navProfile;

    private TextView textHome, textRequest, textRequests, textNotification, textProfile;
    private TextView badgeNotification;

    private ImageView iconHome, iconRequest, iconRequests, iconNotification, iconProfile;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration incomingNotificationListener;

    private static final int COLOR_PRIMARY = Color.rgb(151, 7, 5);
    private static final int COLOR_DARK = Color.rgb(49, 49, 49);
    private static final int COLOR_WHITE = Color.WHITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requestor_nav_bar);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (!ensureUserLoggedIn()) {
            return;
        }

        bindViews();
        setupBadgeStyle();
        setupNavigationClicks();
        listenForIncomingNotificationBadge();

        // Use RoleGuardHelper to verify role before loading fragments
        ProgressBar progressBar = findViewById(R.id.roleVerificationProgress);
        RoleGuardHelper roleGuard = new RoleGuardHelper(this, progressBar);
        
        roleGuard.verifyAndProceed("Requestor", new RoleGuardHelper.OnRoleVerified() {
            @Override
            public void onSuccess() {
                // Role verified! Now safe to load fragments
                if (savedInstanceState == null) {
                    loadFragment(new RequestorHomeFragment());
                    setSelectedTab(Tab.HOME);
                }
            }

            @Override
            public void onFailure(String message) {
                // Already handled by RoleGuardHelper (redirected to login)
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (incomingNotificationListener != null) {
            incomingNotificationListener.remove();
            incomingNotificationListener = null;
        }
    }

    private void bindViews() {
        navHome = findViewById(R.id.navHome);
        navRequest = findViewById(R.id.navRequest);
        navRequests = findViewById(R.id.navRequests);
        navNotification = findViewById(R.id.navNotification);
        navProfile = findViewById(R.id.navProfile);

        textHome = findViewById(R.id.textHome);
        textRequest = findViewById(R.id.textRequest);
        textRequests = findViewById(R.id.textRequests);
        textNotification = findViewById(R.id.textNotification);
        textProfile = findViewById(R.id.textProfile);

        iconHome = findViewById(R.id.iconHome);
        iconRequest = findViewById(R.id.iconRequest);
        iconRequests = findViewById(R.id.iconRequests);
        iconNotification = findViewById(R.id.iconNotification);
        iconProfile = findViewById(R.id.iconProfile);

        badgeNotification = findViewById(R.id.badgeNotification);
    }

    private void setupBadgeStyle() {
        if (badgeNotification == null) return;

        GradientDrawable badgeBackground = new GradientDrawable();
        badgeBackground.setShape(GradientDrawable.OVAL);
        badgeBackground.setColor(COLOR_PRIMARY);

        badgeNotification.setBackground(badgeBackground);
        badgeNotification.setGravity(Gravity.CENTER);
        badgeNotification.setTextColor(COLOR_WHITE);
        badgeNotification.setTextSize(8f);
        badgeNotification.setTypeface(null, Typeface.BOLD);
        badgeNotification.setVisibility(View.GONE);
        badgeNotification.setText("0");
    }

    private void setupNavigationClicks() {
        navHome.setOnClickListener(v -> {
            loadFragment(new RequestorHomeFragment());
            setSelectedTab(Tab.HOME);
        });

        navRequest.setOnClickListener(v -> {
            loadFragment(new RequestorRequestFragment());
            setSelectedTab(Tab.REQUEST);
        });

        navRequests.setOnClickListener(v -> {
            loadFragment(new RequestorRequestsFragment());
            setSelectedTab(Tab.REQUESTS);
        });

        navNotification.setOnClickListener(v -> {
            loadFragment(new RequestorNotificationFragment());
            setSelectedTab(Tab.NOTIFICATION);
            markAllNotificationsAsSeen();
        });

        navProfile.setOnClickListener(v -> {
            loadFragment(new RequestorProfileFragment());
            setSelectedTab(Tab.PROFILE);
        });
    }

    private void markAllNotificationsAsSeen() {
        if (auth.getCurrentUser() == null) return;

        String userId = auth.getCurrentUser().getUid();

        db.collection("requests")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) return;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (RequestDataHelper.isRequestorNotificationUnseen(doc)) {
                            doc.getReference().update(
                                    "requestorSeen", true,
                                    "requestorNotificationSeen", true,
                                    "requestorApprovedSeen", true,
                                    "notificationForRequestor", false,
                                    "requestorNotificationOpenedAt", FieldValue.serverTimestamp(),
                                    "updatedAt", FieldValue.serverTimestamp()
                            );
                        }
                    }
                    updateIncomingBadge(0);
                });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void listenForIncomingNotificationBadge() {
        updateIncomingBadge(0);

        if (auth.getCurrentUser() == null) return;

        if (incomingNotificationListener != null) {
            incomingNotificationListener.remove();
        }

        String userId = auth.getCurrentUser().getUid();

        incomingNotificationListener = db.collection("requests")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        updateIncomingBadge(0);
                        return;
                    }

                    int unseenCount = 0;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (RequestDataHelper.shouldShowInRequestList(doc) && RequestDataHelper.isRequestorNotificationUnseen(doc)) {
                            unseenCount++;
                        }
                    }

                    updateIncomingBadge(unseenCount);
                });
    }

    private void updateIncomingBadge(int count) {
        if (badgeNotification == null) return;

        if (count <= 0) {
            badgeNotification.setVisibility(View.GONE);
            badgeNotification.setText("0");
            return;
        }

        badgeNotification.setVisibility(View.VISIBLE);
        badgeNotification.setText(count > 99 ? "99+" : String.valueOf(count));
    }

    private void setSelectedTab(Tab selectedTab) {
        resetTabs();

        switch (selectedTab) {
            case HOME:
                selectTab(textHome, iconHome);
                break;

            case REQUEST:
                selectTab(textRequest, iconRequest);
                break;

            case REQUESTS:
                selectTab(textRequests, iconRequests);
                break;

            case NOTIFICATION:
                selectTab(textNotification, iconNotification);
                break;

            case PROFILE:
                selectTab(textProfile, iconProfile);
                break;
        }
    }

    private void resetTabs() {
        resetTab(textHome, iconHome);
        resetTab(textRequest, iconRequest);
        resetTab(textRequests, iconRequests);
        resetTab(textNotification, iconNotification);
        resetTab(textProfile, iconProfile);
    }

    private void selectTab(TextView text, ImageView icon) {
        if (text != null) {
            text.setTextColor(COLOR_PRIMARY);
        }

        if (icon != null) {
            icon.setImageTintList(ColorStateList.valueOf(COLOR_PRIMARY));
        }
    }

    private void resetTab(TextView text, ImageView icon) {
        if (text != null) {
            text.setTextColor(COLOR_DARK);
        }

        if (icon != null) {
            icon.setImageTintList(ColorStateList.valueOf(COLOR_DARK));
        }
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

    private enum Tab {
        HOME,
        REQUEST,
        REQUESTS,
        NOTIFICATION,
        PROFILE
    }
}