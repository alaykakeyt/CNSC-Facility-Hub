package com.example.cnscfacilityhubproject.activities.sacUI;

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
import com.example.cnscfacilityhubproject.utils.RequestDataHelper;
import com.example.cnscfacilityhubproject.utils.RoleGuardHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class sacNavBarActivity extends AppCompatActivity {

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

    private enum Tab {
        HOME,
        NOTIFICATION,
        PROFILE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sac_nav_bar);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (!ensureUserLoggedIn()) {
            return;
        }

        bindViews();
        setupBadgeStyle();
        setupNavigation();
        listenForIncomingSACNotifications();

        ProgressBar progressBar = findViewById(R.id.roleVerificationProgress);
        RoleGuardHelper roleGuard = new RoleGuardHelper(this, progressBar);

        roleGuard.verifyAndProceed("SAC", new RoleGuardHelper.OnRoleVerified() {
            @Override
            public void onSuccess() {
                if (savedInstanceState == null) {
                    loadFragment(new sacHomeFragment());
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
        navHome = findViewById(R.id.sacnavHome);
        navNotification = findViewById(R.id.sacnavNotifications);
        navProfile = findViewById(R.id.sacnavProfile);

        textHome = findViewById(R.id.sactextHome);
        textNotification = findViewById(R.id.sactextNotifications);
        textProfile = findViewById(R.id.sactextProfile);

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
        navHome.setOnClickListener(v -> {
            loadFragment(new sacHomeFragment());
            setSelectedTab(Tab.HOME);
        });

        navNotification.setOnClickListener(v -> {
            clearNotificationBadgeAndMarkSeen();
            openNotificationsWithFilter("All");
        });

        navProfile.setOnClickListener(v -> {
            loadFragment(new sacProfileFragment());
            setSelectedTab(Tab.PROFILE);
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.sac_fragment_container, fragment)
                .commit();
    }

    public void openNotificationsWithFilter(String filter) {
        Bundle bundle = new Bundle();
        bundle.putString("filter", filter);

        sacNotificationFragment fragment = new sacNotificationFragment();
        fragment.setArguments(bundle);

        loadFragment(fragment);
        setSelectedTab(Tab.NOTIFICATION);
    }

    private void setSelectedTab(Tab selectedTab) {
        resetTabs();

        switch (selectedTab) {
            case HOME:
                textHome.setTextColor(COLOR_PRIMARY);
                iconHome.setImageTintList(ColorStateList.valueOf(COLOR_PRIMARY));
                break;

            case NOTIFICATION:
                textNotification.setTextColor(COLOR_PRIMARY);
                iconNotification.setImageTintList(ColorStateList.valueOf(COLOR_PRIMARY));
                break;

            case PROFILE:
                textProfile.setTextColor(COLOR_PRIMARY);
                iconProfile.setImageTintList(ColorStateList.valueOf(COLOR_PRIMARY));
                break;
        }
    }

    private void resetTabs() {
        textHome.setTextColor(COLOR_DARK);
        textNotification.setTextColor(COLOR_DARK);
        textProfile.setTextColor(COLOR_DARK);

        iconHome.setImageTintList(ColorStateList.valueOf(COLOR_DARK));
        iconNotification.setImageTintList(ColorStateList.valueOf(COLOR_DARK));
        iconProfile.setImageTintList(ColorStateList.valueOf(COLOR_DARK));
    }

    private void listenForIncomingSACNotifications() {
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
                                && RequestDataHelper.isSACUnseenNotification(doc)) {
                            unseenNotificationIds.add(doc.getId());
                        }
                    }

                    updateNotificationBadge(unseenNotificationIds.size());
                });
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
                            "sacNotificationSeen", true,
                            "sacSeen", true,
                            "sacNotificationOpenedAt", FieldValue.serverTimestamp(),
                            "updatedAt", FieldValue.serverTimestamp()
                    );
        }
    }

    private boolean ensureUserLoggedIn() {
        if (auth != null && auth.getCurrentUser() != null) {
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
}