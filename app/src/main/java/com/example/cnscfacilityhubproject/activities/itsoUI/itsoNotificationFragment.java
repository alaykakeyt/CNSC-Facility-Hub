package com.example.cnscfacilityhubproject.activities.itsoUI;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.adapters.ItsoNotificationAdapter;
import com.example.cnscfacilityhubproject.utils.ItsoReminderHelper;
import com.example.cnscfacilityhubproject.utils.RequestDataHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * ITSO Notification Fragment - displays incoming requests and technical event reminders.
 * Uses RecyclerView for dynamic rendering of all notifications (no 3-card limit).
 */
public class itsoNotificationFragment extends Fragment {

    private View layoutEmptyState, layoutNotificationList;
    private RecyclerView recyclerViewNotifications;

    private TextView tvIncomingCount, badgeNotification;

    private ItsoNotificationAdapter notificationAdapter;

    private FirebaseFirestore db;
    private ListenerRegistration incomingNotificationListener;

    public itsoNotificationFragment() {
        super(R.layout.fragment_itso_notification);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        bindViews(view);
        setupBadgeStyle();
        setupRecyclerView();
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
        recyclerViewNotifications = view.findViewById(R.id.recyclerViewNotifications);

        tvIncomingCount = view.findViewById(R.id.tvIncomingCount);
        badgeNotification = view.findViewById(R.id.badgeNotification);
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

    private void setupRecyclerView() {
        if (recyclerViewNotifications == null) return;

        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        notificationAdapter = new ItsoNotificationAdapter(this::openRequestDetails);
        recyclerViewNotifications.setAdapter(notificationAdapter);
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
                        if (RequestDataHelper.shouldShowInRequestList(doc)
                                && (isIncomingITSORequest(doc) || ItsoReminderHelper.isUpcomingTechnicalEvent(doc))) {
                            incomingDocs.add(doc);
                        }
                    }

                    int count = incomingDocs.size();

                    tvIncomingCount.setText(
                            count + " incoming booking request" + (count == 1 ? "" : "s")
                    );

                    updateNotificationBadge(count);
                    updateNotificationState(count > 0);

                    // Update adapter with all notifications (no 3-card limit)
                    if (notificationAdapter != null) {
                        notificationAdapter.setNotifications(incomingDocs);
                    }
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

    private String getStringValue(DocumentSnapshot doc, String fieldName) {
        String value = doc.getString(fieldName);
        return value != null ? value.trim() : "";
    }
}