package com.example.cnscfacilityhubproject.activities.sacUI;

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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class sacNavBarActivity extends AppCompatActivity {

    private LinearLayout navHome, navRequest, navNotification, navProfile;

    private TextView textHome,
            textRequest,
            textNotification,
            textProfile;

    private ImageView iconHome,
            iconRequest,
            iconNotification,
            iconProfile;

    private TextView badgeNotifications;

    private FirebaseFirestore db;

    private ListenerRegistration notificationBadgeListener;

    private final List<String> unseenNotificationIds =
            new ArrayList<>();

    private static final int COLOR_PRIMARY =
            Color.rgb(151, 7, 5);

    private static final int COLOR_DARK =
            Color.rgb(49, 49, 49);

    private static final int COLOR_WHITE =
            Color.WHITE;

    private enum Tab {
        HOME,
        REQUEST,
        NOTIFICATION,
        PROFILE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sac_nav_bar);

        db = FirebaseFirestore.getInstance();

        bindViews();
        setupBadgeStyle();
        setupNavigation();
        listenForIncomingSACNotifications();

        if (savedInstanceState == null) {
            loadFragment(new sacHomeFragment());
            setSelectedTab(Tab.HOME);
        }
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

        navHome =
                findViewById(R.id.sacnavHome);

        navRequest =
                findViewById(R.id.sacnavRequests);

        navNotification =
                findViewById(R.id.sacnavNotifications);

        navProfile =
                findViewById(R.id.sacnavProfile);

        textHome =
                findViewById(R.id.sactextHome);

        textRequest =
                findViewById(R.id.sactextRequests);

        textNotification =
                findViewById(R.id.sactextNotifications);

        textProfile =
                findViewById(R.id.sactextProfile);

        iconHome =
                findViewById(R.id.iconHome);

        iconRequest =
                findViewById(R.id.iconRequests);

        iconNotification =
                findViewById(R.id.iconNotifications);

        iconProfile =
                findViewById(R.id.iconProfile);

        badgeNotifications =
                findViewById(R.id.badgeNotifications);
    }

    private void setupBadgeStyle() {

        if (badgeNotifications == null) return;

        GradientDrawable badgeBackground =
                new GradientDrawable();

        badgeBackground.setShape(
                GradientDrawable.OVAL
        );

        badgeBackground.setColor(
                COLOR_PRIMARY
        );

        badgeNotifications.setBackground(
                badgeBackground
        );

        badgeNotifications.setGravity(
                Gravity.CENTER
        );

        badgeNotifications.setTextColor(
                COLOR_WHITE
        );

        badgeNotifications.setTextSize(8f);

        badgeNotifications.setTypeface(
                null,
                Typeface.BOLD
        );

        badgeNotifications.setVisibility(
                View.GONE
        );

        badgeNotifications.setText("0");
    }

    private void setupNavigation() {

        navHome.setOnClickListener(v -> {
            loadFragment(new sacHomeFragment());
            setSelectedTab(Tab.HOME);
        });

        navRequest.setOnClickListener(v -> {
            loadFragment(new sacRequestsFragment());
            setSelectedTab(Tab.REQUEST);
        });

        navNotification.setOnClickListener(v -> {

            clearNotificationBadgeAndMarkSeen();

            loadFragment(new sacNotificationFragment());

            setSelectedTab(Tab.NOTIFICATION);
        });

        navProfile.setOnClickListener(v -> {
            loadFragment(new sacProfileFragment());
            setSelectedTab(Tab.PROFILE);
        });
    }

    private void loadFragment(Fragment fragment) {

        getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.sac_fragment_container,
                        fragment
                )
                .commit();
    }

    public void openRequestsWithFilter(String filter) {
        Bundle bundle = new Bundle();
        bundle.putString("filter", filter);

        sacRequestsFragment fragment = new sacRequestsFragment();
        fragment.setArguments(bundle);

        loadFragment(fragment);
        setSelectedTab(Tab.REQUEST);
    }

    private void setSelectedTab(Tab selectedTab) {

        resetTabs();

        switch (selectedTab) {

            case HOME:

                textHome.setTextColor(
                        COLOR_PRIMARY
                );

                iconHome.setImageTintList(
                        ColorStateList.valueOf(
                                COLOR_PRIMARY
                        )
                );

                break;

            case REQUEST:

                textRequest.setTextColor(
                        COLOR_PRIMARY
                );

                iconRequest.setImageTintList(
                        ColorStateList.valueOf(
                                COLOR_PRIMARY
                        )
                );

                break;

            case NOTIFICATION:

                textNotification.setTextColor(
                        COLOR_PRIMARY
                );

                iconNotification.setImageTintList(
                        ColorStateList.valueOf(
                                COLOR_PRIMARY
                        )
                );

                break;

            case PROFILE:

                textProfile.setTextColor(
                        COLOR_PRIMARY
                );

                iconProfile.setImageTintList(
                        ColorStateList.valueOf(
                                COLOR_PRIMARY
                        )
                );

                break;
        }
    }

    private void resetTabs() {

        textHome.setTextColor(COLOR_DARK);
        textRequest.setTextColor(COLOR_DARK);
        textNotification.setTextColor(COLOR_DARK);
        textProfile.setTextColor(COLOR_DARK);

        iconHome.setImageTintList(
                ColorStateList.valueOf(COLOR_DARK)
        );

        iconRequest.setImageTintList(
                ColorStateList.valueOf(COLOR_DARK)
        );

        iconNotification.setImageTintList(
                ColorStateList.valueOf(COLOR_DARK)
        );

        iconProfile.setImageTintList(
                ColorStateList.valueOf(COLOR_DARK)
        );
    }

    private void listenForIncomingSACNotifications() {

        if (notificationBadgeListener != null) {
            notificationBadgeListener.remove();
        }

        notificationBadgeListener =
                db.collection("requests")
                        .addSnapshotListener((snapshot, error) -> {

                            unseenNotificationIds.clear();

                            if (error != null
                                    || snapshot == null) {

                                updateNotificationBadge(0);
                                return;
                            }

                            for (DocumentSnapshot doc :
                                    snapshot.getDocuments()) {

                                if (isIncomingSACRequest(doc)) {

                                    unseenNotificationIds.add(
                                            doc.getId()
                                    );
                                }
                            }

                            updateNotificationBadge(
                                    unseenNotificationIds.size()
                            );
                        });
    }

    private boolean isIncomingSACRequest(
            DocumentSnapshot doc
    ) {

        if (!isSACRequest(doc)) {
            return false;
        }

        String status =
                getStringValue(doc, "status");

        String sacStatus =
                getStringValue(doc, "sacStatus");

        if ("Rejected".equalsIgnoreCase(status)
                || "Approved".equalsIgnoreCase(sacStatus)
                || "Rejected".equalsIgnoreCase(sacStatus)) {

            return false;
        }

        Boolean sacNotificationSeen =
                doc.getBoolean("sacNotificationSeen");

        Boolean sacSeen =
                doc.getBoolean("sacSeen");

        return !Boolean.TRUE.equals(sacNotificationSeen)
                && !Boolean.TRUE.equals(sacSeen);
    }

    /**
     * ONLY SHOW REQUESTS THAT ARE
     * CURRENTLY ASSIGNED TO SAC.
     */
    private boolean isSACRequest(
            DocumentSnapshot doc
    ) {

        Boolean sendToSAC =
                doc.getBoolean("sendToSAC");

        if (Boolean.TRUE.equals(sendToSAC)) {
            return true;
        }

        String notificationTarget =
                getStringValue(
                        doc,
                        "notificationTarget"
                );

        if ("SAC".equalsIgnoreCase(notificationTarget)) {
            return true;
        }

        String workflowStage =
                getStringValue(
                        doc,
                        "workflowStage"
                );

        return "SAC_REVIEW"
                .equalsIgnoreCase(workflowStage);
    }

    private void updateNotificationBadge(
            int count
    ) {

        if (badgeNotifications == null) return;

        if (count <= 0) {

            badgeNotifications.setVisibility(
                    View.GONE
            );

            badgeNotifications.setText("0");

            return;
        }

        badgeNotifications.setVisibility(
                View.VISIBLE
        );

        badgeNotifications.setText(
                count > 99
                        ? "99+"
                        : String.valueOf(count)
        );
    }

    private void clearNotificationBadgeAndMarkSeen() {

        updateNotificationBadge(0);

        if (unseenNotificationIds.isEmpty()) {
            return;
        }

        List<String> idsToUpdate =
                new ArrayList<>(unseenNotificationIds);

        unseenNotificationIds.clear();

        for (String requestId : idsToUpdate) {

            db.collection("requests")
                    .document(requestId)
                    .update(
                            "sacNotificationSeen",
                            true,

                            "sacSeen",
                            true,

                            "updatedAt",
                            FieldValue.serverTimestamp()
                    );
        }
    }

    private String getStringValue(
            DocumentSnapshot doc,
            String field
    ) {

        Object value = doc.get(field);

        return value == null
                ? ""
                : String.valueOf(value).trim();
    }
}
