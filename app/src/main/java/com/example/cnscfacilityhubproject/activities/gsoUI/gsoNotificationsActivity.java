package com.example.cnscfacilityhubproject.activities.gsoUI;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.utils.RequestDataHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class gsoNotificationsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvIncomingCount;
    private LinearLayout layoutNotificationList;
    private View layoutEmptyState;

    private FirebaseFirestore db;
    private ListenerRegistration notificationListener;

    private final List<DocumentSnapshot> gsoNotifications = new ArrayList<>();

    private static final int COLOR_PRIMARY = Color.rgb(151, 7, 5);
    private static final int COLOR_DARK = Color.rgb(49, 49, 49);
    private static final int COLOR_GREEN = Color.rgb(46, 125, 50);
    private static final int COLOR_WHITE = Color.WHITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gso_notifications);

        db = FirebaseFirestore.getInstance();

        bindViews();
        setupActions();
        listenForGSONotifications();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvIncomingCount = findViewById(R.id.tvIncomingCount);
        layoutNotificationList = findViewById(R.id.layoutNotificationList);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
    }

    private void setupActions() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void listenForGSONotifications() {
        if (notificationListener != null) {
            notificationListener.remove();
        }

        notificationListener = db.collection("requests")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        Toast.makeText(
                                this,
                                "Failed to load GSO notifications.",
                                Toast.LENGTH_SHORT
                        ).show();
                        showEmptyState();
                        return;
                    }

                    gsoNotifications.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (RequestDataHelper.shouldShowInRequestList(doc) && isIncomingGSORequest(doc)) {
                            gsoNotifications.add(doc);
                        }
                    }

                    renderNotifications();
                });
    }

    private boolean isIncomingGSORequest(DocumentSnapshot doc) {
        String status = getStringValue(doc, "status");

        if ("Approved".equalsIgnoreCase(status)
                || "Rejected".equalsIgnoreCase(status)
                || "Returned".equalsIgnoreCase(status)) {
            return false;
        }

        return isGSORequest(doc);
    }

    private boolean isGSORequest(DocumentSnapshot doc) {
        Boolean sendToGSO = doc.getBoolean("sendToGSO");
        if (Boolean.TRUE.equals(sendToGSO)) {
            return true;
        }

        String notificationTarget = getStringValue(doc, "notificationTarget");
        if ("GSO".equalsIgnoreCase(notificationTarget)) {
            return true;
        }

        String workflowStage = getStringValue(doc, "workflowStage");
        return "GSO_REVIEW".equalsIgnoreCase(workflowStage);
    }

    private void renderNotifications() {
        layoutNotificationList.removeAllViews();

        int count = gsoNotifications.size();
        tvIncomingCount.setText(
                count + " incoming booking request" + (count == 1 ? "" : "s")
        );

        if (count == 0) {
            showEmptyState();
            return;
        }

        layoutEmptyState.setVisibility(View.GONE);
        layoutNotificationList.setVisibility(View.VISIBLE);

        for (DocumentSnapshot doc : gsoNotifications) {
            layoutNotificationList.addView(createNotificationCard(doc));
        }
    }

    private View createNotificationCard(DocumentSnapshot doc) {
        String requestId = doc.getId();
        String status = getDisplayStatus(doc);
        String purpose = getStringValue(doc, "purpose");
        String facility = getFinalFacility(doc);
        String startDate = getStringValue(doc, "startDateText");
        String endDate = getStringValue(doc, "endDateText");
        String startTime = getStringValue(doc, "timeStartText");
        String endTime = getStringValue(doc, "timeEndText");
        String requestorName = firstNonEmpty(
                getStringValue(doc, "requestorName"),
                getStringValue(doc, "fullName")
        );

        MaterialCardView card = new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(12));

        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(COLOR_WHITE);
        card.setRadius(dp(24));
        card.setCardElevation(dp(6));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(COLOR_DARK);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(18));

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        MaterialCardView iconCard = new MaterialCardView(this);
        LinearLayout.LayoutParams iconCardParams = new LinearLayout.LayoutParams(dp(46), dp(46));
        iconCard.setLayoutParams(iconCardParams);
        iconCard.setRadius(dp(15));
        iconCard.setCardElevation(0);
        iconCard.setCardBackgroundColor(COLOR_PRIMARY);

        ImageView icon = new ImageView(this);
        icon.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        icon.setPadding(dp(10), dp(10), dp(10), dp(10));
        icon.setImageResource(android.R.drawable.ic_menu_my_calendar);
        icon.setColorFilter(COLOR_WHITE);

        iconCard.addView(icon);

        LinearLayout titleLayout = new LinearLayout(this);
        titleLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        titleParams.setMargins(dp(12), 0, dp(8), 0);
        titleLayout.setLayoutParams(titleParams);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(!purpose.isEmpty() ? purpose : "GSO Booking Request");
        tvTitle.setTextColor(COLOR_DARK);
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(null, Typeface.BOLD);

        TextView tvMeta = new TextView(this);
        tvMeta.setText(buildMetaText(facility, startDate, endDate, startTime, endTime));
        tvMeta.setTextColor(COLOR_DARK);
        tvMeta.setTextSize(12f);
        tvMeta.setAlpha(0.65f);

        titleLayout.addView(tvTitle);
        titleLayout.addView(tvMeta);

        Chip chipStatus = new Chip(this);
        chipStatus.setText(status);
        chipStatus.setTextColor(getStatusMainColor(status));
        chipStatus.setChipBackgroundColor(ColorStateList.valueOf(getStatusLightColor(status)));
        chipStatus.setChipStrokeWidth(0);
        chipStatus.setCheckable(false);
        chipStatus.setClickable(false);

        headerRow.addView(iconCard);
        headerRow.addView(titleLayout);
        headerRow.addView(chipStatus);

        TextView tvDescription = new TextView(this);
        tvDescription.setText(buildDescriptionText(facility, requestorName));
        tvDescription.setTextColor(COLOR_DARK);
        tvDescription.setTextSize(14f);
        tvDescription.setLineSpacing(2f, 1f);

        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        descParams.setMargins(0, dp(12), 0, 0);
        tvDescription.setLayoutParams(descParams);

        MaterialButton btnViewRequest = new MaterialButton(this);
        btnViewRequest.setText("View Request");
        btnViewRequest.setAllCaps(false);
        btnViewRequest.setTextColor(COLOR_WHITE);
        btnViewRequest.setTypeface(null, Typeface.BOLD);
        btnViewRequest.setBackgroundTintList(ColorStateList.valueOf(COLOR_DARK));
        btnViewRequest.setCornerRadius(dp(16));
        btnViewRequest.setElevation(0);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
        );
        buttonParams.setMargins(0, dp(14), 0, 0);
        btnViewRequest.setLayoutParams(buttonParams);

        btnViewRequest.setOnClickListener(v -> openRequestDetails(requestId));

        container.addView(headerRow);
        container.addView(tvDescription);
        container.addView(btnViewRequest);

        card.addView(container);

        return card;
    }

    private void openRequestDetails(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            Toast.makeText(this, "Request ID not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        // If your existing GSO details screen has a different name, replace this class name.
        gsoRequestsViewDetailsFragment fragment = gsoRequestsViewDetailsFragment.newInstance(requestId);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack(null)
                .commit();
    }

    private String getDisplayStatus(DocumentSnapshot doc) {
        String status = getStringValue(doc, "status");
        String gsoStatus = getStringValue(doc, "gsoStatus");

        if ("Approved".equalsIgnoreCase(status)
                || "Approved".equalsIgnoreCase(gsoStatus)) {
            return "Approved";
        }

        if ("Returned".equalsIgnoreCase(status)
                || "Returned".equalsIgnoreCase(gsoStatus)) {
            return "Returned";
        }

        return "Pending";
    }

    private String buildDescriptionText(String facility, String requestorName) {
        StringBuilder builder = new StringBuilder();

        builder.append("A booking request is ready for GSO review.");

        if (!facility.isEmpty()) {
            builder.append("\nFacility: ").append(facility);
        }

        if (!requestorName.isEmpty()) {
            builder.append("\nRequestor: ").append(requestorName);
        }

        return builder.toString();
    }

    private String buildMetaText(
            String facility,
            String startDate,
            String endDate,
            String startTime,
            String endTime
    ) {
        StringBuilder builder = new StringBuilder();

        if (!facility.isEmpty()) {
            builder.append(facility);
        }

        if (!startDate.isEmpty()) {
            if (builder.length() > 0) builder.append(" • ");

            if (!endDate.isEmpty() && !startDate.equalsIgnoreCase(endDate)) {
                builder.append(startDate).append(" - ").append(endDate);
            } else {
                builder.append(startDate);
            }
        }

        if (!startTime.isEmpty()) {
            if (builder.length() > 0) builder.append(" • ");

            if (!endTime.isEmpty()) {
                builder.append(startTime).append(" - ").append(endTime);
            } else {
                builder.append(startTime);
            }
        }

        return builder.length() == 0 ? "No schedule details" : builder.toString();
    }

    private String getFinalFacility(DocumentSnapshot doc) {
        String finalFacilityName = getStringValue(doc, "finalFacilityName");
        if (!finalFacilityName.isEmpty()) return finalFacilityName;

        String facility = getStringValue(doc, "facility");
        String otherFacility = getStringValue(doc, "otherFacility");

        if ("Others".equalsIgnoreCase(facility) && !otherFacility.isEmpty()) {
            return otherFacility;
        }

        return facility;
    }

    private int getStatusMainColor(String status) {
        if ("Approved".equalsIgnoreCase(status)) return COLOR_GREEN;
        if ("Returned".equalsIgnoreCase(status)) return COLOR_PRIMARY;
        return COLOR_DARK;
    }

    private int getStatusLightColor(String status) {
        if ("Approved".equalsIgnoreCase(status)) return Color.parseColor("#E7F4E8");
        if ("Returned".equalsIgnoreCase(status)) return Color.parseColor("#F3D9D9");
        return Color.parseColor("#EEEEEE");
    }

    private void showEmptyState() {
        if (tvIncomingCount != null) {
            tvIncomingCount.setText("0 incoming booking requests");
        }

        if (layoutNotificationList != null) {
            layoutNotificationList.setVisibility(View.GONE);
        }

        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(View.VISIBLE);
        }
    }

    private String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) return first.trim();
        if (second != null && !second.trim().isEmpty()) return second.trim();
        return "";
    }

    private String getStringValue(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
