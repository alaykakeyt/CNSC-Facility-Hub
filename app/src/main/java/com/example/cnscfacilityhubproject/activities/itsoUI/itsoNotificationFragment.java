package com.example.cnscfacilityhubproject.activities.itsoUI;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cnscfacilityhubproject.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class itsoNotificationFragment extends Fragment {

    private View layoutEmptyState, layoutNotificationList;
    private View cardNotification1, cardNotification2, cardNotification3;

    private MaterialButton btnViewRequest1, btnViewRequest2, btnViewRequest3;

    private TextView tvIncomingCount, badgeNotification;

    private TextView tvNotificationTitle1, tvNotificationMeta1, tvNotificationDesc1;
    private TextView tvNotificationTitle2, tvNotificationMeta2, tvNotificationDesc2;
    private TextView tvNotificationTitle3, tvNotificationMeta3, tvNotificationDesc3;

    private Chip chipNotificationStatus1, chipNotificationStatus2, chipNotificationStatus3;

    private FirebaseFirestore db;
    private ListenerRegistration incomingNotificationListener;

    private String requestId1, requestId2, requestId3;

    public itsoNotificationFragment() {
        super(R.layout.fragment_itso_notification);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        bindViews(view);
        setupBadgeStyle();
        setupActions();
        listenForIncomingNotifications();
    }

    @Override
    public void onResume() {
        super.onResume();
        listenForIncomingNotifications();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (incomingNotificationListener != null) {
            incomingNotificationListener.remove();
            incomingNotificationListener = null;
        }
    }

    private void bindViews(View view) {
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        layoutNotificationList = view.findViewById(R.id.layoutNotificationList);

        cardNotification1 = view.findViewById(R.id.cardNotification1);
        cardNotification2 = view.findViewById(R.id.cardNotification2);
        cardNotification3 = view.findViewById(R.id.cardNotification3);

        tvIncomingCount = view.findViewById(R.id.tvIncomingCount);
        badgeNotification = view.findViewById(R.id.badgeNotification);

        btnViewRequest1 = view.findViewById(R.id.btnViewRequest1);
        btnViewRequest2 = view.findViewById(R.id.btnViewRequest2);
        btnViewRequest3 = view.findViewById(R.id.btnViewRequest3);

        tvNotificationTitle1 = view.findViewById(R.id.tvNotificationTitle1);
        tvNotificationMeta1 = view.findViewById(R.id.tvNotificationMeta1);
        tvNotificationDesc1 = view.findViewById(R.id.tvNotificationDesc1);

        tvNotificationTitle2 = view.findViewById(R.id.tvNotificationTitle2);
        tvNotificationMeta2 = view.findViewById(R.id.tvNotificationMeta2);
        tvNotificationDesc2 = view.findViewById(R.id.tvNotificationDesc2);

        tvNotificationTitle3 = view.findViewById(R.id.tvNotificationTitle3);
        tvNotificationMeta3 = view.findViewById(R.id.tvNotificationMeta3);
        tvNotificationDesc3 = view.findViewById(R.id.tvNotificationDesc3);

        chipNotificationStatus1 = view.findViewById(R.id.chipNotificationStatus1);
        chipNotificationStatus2 = view.findViewById(R.id.chipNotificationStatus2);
        chipNotificationStatus3 = view.findViewById(R.id.chipNotificationStatus3);
    }

    private void setupBadgeStyle() {
        if (badgeNotification == null) return;

        GradientDrawable badgeBackground = new GradientDrawable();
        badgeBackground.setShape(GradientDrawable.OVAL);
        badgeBackground.setColor(Color.parseColor("#970705"));

        badgeNotification.setBackground(badgeBackground);
        badgeNotification.setVisibility(View.GONE);
        badgeNotification.setGravity(Gravity.CENTER);
        badgeNotification.setTextColor(Color.WHITE);
        badgeNotification.setTextSize(10f);
        badgeNotification.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void setupActions() {
        btnViewRequest1.setOnClickListener(v -> openRequestDetails(requestId1));
        btnViewRequest2.setOnClickListener(v -> openRequestDetails(requestId2));
        btnViewRequest3.setOnClickListener(v -> openRequestDetails(requestId3));
    }

    private void listenForIncomingNotifications() {
        if (incomingNotificationListener != null) {
            incomingNotificationListener.remove();
        }

        incomingNotificationListener = db.collection("requests")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (!isAdded()) return;

                    if (error != null || queryDocumentSnapshots == null) {
                        Toast.makeText(requireContext(), "Failed to load notifications.", Toast.LENGTH_SHORT).show();
                        updateNotificationBadge(0);
                        updateNotificationState(false);
                        tvIncomingCount.setText("0 incoming booking requests");
                        return;
                    }

                    List<DocumentSnapshot> incomingDocs = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        if (isIncomingITSORequest(doc)) {
                            incomingDocs.add(doc);
                        }
                    }

                    int count = incomingDocs.size();

                    tvIncomingCount.setText(
                            count + " incoming booking request" + (count == 1 ? "" : "s")
                    );

                    updateNotificationBadge(count);
                    updateNotificationState(count > 0);

                    requestId1 = null;
                    requestId2 = null;
                    requestId3 = null;

                    bindNotificationCard(0, incomingDocs);
                    bindNotificationCard(1, incomingDocs);
                    bindNotificationCard(2, incomingDocs);
                });
    }

    private boolean isIncomingITSORequest(DocumentSnapshot doc) {
        String workflowStage = getStringValue(doc, "workflowStage");
        String notificationTarget = getStringValue(doc, "notificationTarget");
        String status = getStringValue(doc, "status");
        String itsoStatus = getStringValue(doc, "itsoStatus");

        Boolean sendToITSO = doc.getBoolean("sendToITSO");

        boolean forITSO =
                Boolean.TRUE.equals(sendToITSO)
                        || "ITSO".equalsIgnoreCase(notificationTarget)
                        || "ITSO_REVIEW".equalsIgnoreCase(workflowStage);

        if (!forITSO) return false;

        if ("Approved".equalsIgnoreCase(itsoStatus)
                || "Rejected".equalsIgnoreCase(itsoStatus)
                || "Available".equalsIgnoreCase(itsoStatus)
                || "Not Available".equalsIgnoreCase(itsoStatus)) {
            return false;
        }

        if ("Cancelled".equalsIgnoreCase(status)
                || "Rejected".equalsIgnoreCase(status)
                || "Returned".equalsIgnoreCase(status)) {
            return false;
        }

        return true;
    }

    private void updateNotificationBadge(int count) {
        if (badgeNotification == null) return;

        if (count <= 0) {
            badgeNotification.setVisibility(View.GONE);
            badgeNotification.setText("0");
            return;
        }

        badgeNotification.setVisibility(View.VISIBLE);
        badgeNotification.setText(count > 99 ? "99+" : String.valueOf(count));
    }

    private void bindNotificationCard(int index, List<DocumentSnapshot> docs) {
        View card;
        TextView tvTitle;
        TextView tvMeta;
        TextView tvDesc;
        Chip chipStatus;

        if (index == 0) {
            card = cardNotification1;
            tvTitle = tvNotificationTitle1;
            tvMeta = tvNotificationMeta1;
            tvDesc = tvNotificationDesc1;
            chipStatus = chipNotificationStatus1;
        } else if (index == 1) {
            card = cardNotification2;
            tvTitle = tvNotificationTitle2;
            tvMeta = tvNotificationMeta2;
            tvDesc = tvNotificationDesc2;
            chipStatus = chipNotificationStatus2;
        } else {
            card = cardNotification3;
            tvTitle = tvNotificationTitle3;
            tvMeta = tvNotificationMeta3;
            tvDesc = tvNotificationDesc3;
            chipStatus = chipNotificationStatus3;
        }

        if (index >= docs.size()) {
            card.setVisibility(View.GONE);
            return;
        }

        DocumentSnapshot doc = docs.get(index);

        if (index == 0) requestId1 = doc.getId();
        if (index == 1) requestId2 = doc.getId();
        if (index == 2) requestId3 = doc.getId();

        String purpose = getStringValue(doc, "purpose");
        String facility = getFinalFacility(doc);
        String startDateText = getStringValue(doc, "startDateText");
        String endDateText = getStringValue(doc, "endDateText");
        String timeStart = getStringValue(doc, "timeStartText");
        String timeEnd = getStringValue(doc, "timeEndText");

        tvTitle.setText(!purpose.isEmpty() ? purpose : "Untitled Request");
        tvMeta.setText(buildMetaText(facility, startDateText, endDateText, timeStart, timeEnd));
        tvDesc.setText(buildTechnicalSummary(doc));

        chipStatus.setText("Incoming");
        chipStatus.setTextColor(Color.parseColor("#970705"));
        chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F5E5E5")));

        card.setVisibility(View.VISIBLE);
    }

    private void openRequestDetails(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "No request found.", Toast.LENGTH_SHORT).show();
            return;
        }

        itsoHomeViewDetailsFragment fragment = itsoHomeViewDetailsFragment.newInstance(requestId);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.itso_fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void updateNotificationState(boolean hasIncomingBookings) {
        layoutNotificationList.setVisibility(hasIncomingBookings ? View.VISIBLE : View.GONE);
        layoutEmptyState.setVisibility(hasIncomingBookings ? View.GONE : View.VISIBLE);
    }

    private String buildMetaText(String facility, String startDate, String endDate, String startTime, String endTime) {
        StringBuilder metaBuilder = new StringBuilder();

        if (!facility.isEmpty()) metaBuilder.append(facility);

        if (!startDate.isEmpty()) {
            if (metaBuilder.length() > 0) metaBuilder.append(" • ");

            if (!endDate.isEmpty() && !startDate.equalsIgnoreCase(endDate)) {
                metaBuilder.append(startDate).append(" - ").append(endDate);
            } else {
                metaBuilder.append(startDate);
            }
        }

        if (!startTime.isEmpty()) {
            if (metaBuilder.length() > 0) metaBuilder.append(" • ");

            if (!endTime.isEmpty()) {
                metaBuilder.append(startTime).append(" - ").append(endTime);
            } else {
                metaBuilder.append(startTime);
            }
        }

        return metaBuilder.length() == 0 ? "No schedule details" : metaBuilder.toString();
    }

    private String buildTechnicalSummary(DocumentSnapshot doc) {
        StringBuilder sb = new StringBuilder();

        appendIfTrue(sb, doc.getBoolean("soundSystemSetup"), "Sound system");
        appendIfTrue(sb, doc.getBoolean("microphones"), "Microphones");
        appendIfTrue(sb, doc.getBoolean("portableSpeaker"), "Portable speaker");
        appendIfTrue(sb, doc.getBoolean("lights"), "Lights");
        appendIfTrue(sb, doc.getBoolean("livestreamingServices"), "Livestreaming");
        appendIfTrue(sb, doc.getBoolean("zoomHosting"), "Zoom hosting");
        appendIfTrue(sb, doc.getBoolean("gmeetHosting"), "GMeet hosting");
        appendIfTrue(sb, doc.getBoolean("webCamera"), "Web camera");
        appendIfTrue(sb, doc.getBoolean("tripod"), "Tripod");
        appendIfTrue(sb, doc.getBoolean("multimediaProjector"), "Projector");

        String connectors = getStringValue(doc, "connectors");

        if (!connectors.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Connectors: ").append(connectors);
        }

        return sb.length() == 0 ? "Technical support request submitted." : sb.toString();
    }

    private void appendIfTrue(StringBuilder sb, Boolean value, String label) {
        if (Boolean.TRUE.equals(value)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(label);
        }
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

    private String getStringValue(DocumentSnapshot doc, String field) {
        String value = doc.getString(field);
        return value != null ? value.trim() : "";
    }
}