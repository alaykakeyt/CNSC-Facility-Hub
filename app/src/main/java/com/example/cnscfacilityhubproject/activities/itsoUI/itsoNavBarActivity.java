package com.example.cnscfacilityhubproject.activities.itsoUI;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.activities.LoginActivity;
import com.example.cnscfacilityhubproject.utils.ItsoReminderHelper;
import com.example.cnscfacilityhubproject.utils.RequestDataHelper;
import com.example.cnscfacilityhubproject.utils.RoleGuardHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class itsoNavBarActivity extends AppCompatActivity {

    private LinearLayout navHome, navNotification, navProfile;

    private TextView textHome, textNotification, textProfile;
    private ImageView iconHome, iconNotification, iconProfile;

    private TextView badgeNotifications;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration notificationBadgeListener;

    private final List<String> unseenNotificationIds = new ArrayList<>();

    private static final int COLOR_PRIMARY = Color.rgb(151, 7, 5);
    private static final int COLOR_DARK = Color.rgb(49, 49, 49);
    private static final int COLOR_WHITE = Color.WHITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itso_nav_bar);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (!ensureUserLoggedIn()) {
            return;
        }

        bindViews();
        setupBadgeStyle();
        setupNavigation();
        listenForIncomingITSONotifications();

        ProgressBar progressBar = findViewById(R.id.roleVerificationProgress);
        RoleGuardHelper roleGuard = new RoleGuardHelper(this, progressBar);

        roleGuard.verifyAndProceed("ITSO", new RoleGuardHelper.OnRoleVerified() {
            @Override
            public void onSuccess() {
                if (savedInstanceState == null) {
                    loadFragment(new itsoHomeFragment());
                    setSelectedTab(Tab.HOME);
                }
            }

            @Override
            public void onFailure(String message) {
                // Already handled by RoleGuardHelper.
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (notificationBadgeListener != null) {
            notificationBadgeListener.remove();
            notificationBadgeListener = null;
        }
    }

    private void bindViews() {
        navHome = findViewById(R.id.itsonavHome);
        navNotification = findViewById(R.id.itsonavNotifications);
        navProfile = findViewById(R.id.itsonavProfile);

        textHome = findViewById(R.id.itsotextHome);
        textNotification = findViewById(R.id.itsotextNotifications);
        textProfile = findViewById(R.id.itsotextProfile);

        iconHome = findViewById(R.id.iconHome);
        iconNotification = findViewById(R.id.iconNotifications);
        iconProfile = findViewById(R.id.iconProfile);

        badgeNotifications = findViewById(R.id.badgeNotifications);
    }

    private void setupBadgeStyle() {
        if (badgeNotifications == null) return;

        GradientDrawable badgeBackground = new GradientDrawable();
        badgeBackground.setShape(GradientDrawable.OVAL);
        badgeBackground.setColor(COLOR_PRIMARY);

        badgeNotifications.setBackground(badgeBackground);
        badgeNotifications.setGravity(Gravity.CENTER);
        badgeNotifications.setTextColor(COLOR_WHITE);
        badgeNotifications.setTextSize(8f);
        badgeNotifications.setTypeface(null, Typeface.BOLD);
        badgeNotifications.setVisibility(View.GONE);
        badgeNotifications.setText("0");
    }

    private void setupNavigation() {
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                loadFragment(new itsoHomeFragment());
                setSelectedTab(Tab.HOME);
            });
        }

        if (navNotification != null) {
            navNotification.setOnClickListener(v -> {
                clearNotificationBadgeAndMarkSeen();
                loadFragment(new itsoNotificationFragment());
                setSelectedTab(Tab.NOTIFICATION);
            });
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                loadFragment(new itsoProfileFragment());
                setSelectedTab(Tab.PROFILE);
            });
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.itso_fragment_container, fragment)
                .commit();
    }

    private void setSelectedTab(Tab selectedTab) {
        resetTabs();

        switch (selectedTab) {
            case HOME:
                selectTab(textHome, iconHome);
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

    private void listenForIncomingITSONotifications() {
        if (notificationBadgeListener != null) {
            notificationBadgeListener.remove();
        }

        notificationBadgeListener = db.collection("requests")
                .addSnapshotListener((snapshot, error) -> {
                    unseenNotificationIds.clear();

                    if (error != null || snapshot == null) {
                        updateNotificationBadge(0);
                        return;
                    }

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (RequestDataHelper.shouldShowInRequestList(doc)
                                && (isIncomingITSORequest(doc)
                                || (ItsoReminderHelper.isUpcomingTechnicalEvent(doc)
                                && ItsoReminderHelper.isReminderUnseen(doc)))) {
                            unseenNotificationIds.add(doc.getId());
                        }
                    }

                    updateNotificationBadge(unseenNotificationIds.size());
                });
    }

    private boolean isIncomingITSORequest(DocumentSnapshot doc) {
        String status = getStringValue(doc, "status");

        if (!"Pending".equalsIgnoreCase(status)) {
            return false;
        }

        Boolean itsoNotificationSeen = doc.getBoolean("itsoNotificationSeen");
        Boolean itsoSeen = doc.getBoolean("itsoSeen");

        if (Boolean.TRUE.equals(itsoNotificationSeen)
                || Boolean.TRUE.equals(itsoSeen)) {
            return false;
        }

        return isITSORequest(doc);
    }

    private boolean isITSORequest(DocumentSnapshot doc) {
        Boolean sendToITSO = doc.getBoolean("sendToITSO");

        if (Boolean.TRUE.equals(sendToITSO)) {
            return true;
        }

        String notificationTarget = getStringValue(doc, "notificationTarget");

        if ("ITSO".equalsIgnoreCase(notificationTarget)) {
            return true;
        }

        String workflowStage = getStringValue(doc, "workflowStage");

        return "ITSO_REVIEW".equalsIgnoreCase(workflowStage);
    }

    private void updateNotificationBadge(int count) {
        if (badgeNotifications == null) return;

        if (count <= 0) {
            badgeNotifications.setVisibility(View.GONE);
            badgeNotifications.setText("0");
            return;
        }

        badgeNotifications.setVisibility(View.VISIBLE);
        badgeNotifications.setText(count > 99 ? "99+" : String.valueOf(count));
    }

    private void clearNotificationBadgeAndMarkSeen() {
        updateNotificationBadge(0);

        if (unseenNotificationIds.isEmpty()) {
            return;
        }

        List<String> idsToUpdate = new ArrayList<>(unseenNotificationIds);
        unseenNotificationIds.clear();

        for (String requestId : idsToUpdate) {
            db.collection("requests")
                    .document(requestId)
                    .update(
                            "itsoNotificationSeen", true,
                            "itsoSeen", true,
                            "itsoNotificationOpenedAt", FieldValue.serverTimestamp(),
                            "updatedAt", FieldValue.serverTimestamp()
                    );
        }
    }

    private String getStringValue(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value == null ? "" : String.valueOf(value).trim();
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
        NOTIFICATION,
        PROFILE
    }
}