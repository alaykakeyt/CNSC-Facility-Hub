package com.example.cnscfacilityhubproject.activities.sacUI;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cnscfacilityhubproject.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class sacNotificationFragment extends Fragment {

    private View layoutEmptyState;
    private LinearLayout layoutNotificationList;
    private TextView tvIncomingCount;

    private FirebaseFirestore db;
    private ListenerRegistration incomingNotificationListener;

    public sacNotificationFragment() {
        super(R.layout.fragment_sac_notification);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        layoutEmptyState =
                view.findViewById(R.id.layoutEmptyState);

        layoutNotificationList =
                view.findViewById(R.id.layoutNotificationList);

        tvIncomingCount =
                view.findViewById(R.id.tvIncomingCount);

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

    private void listenForIncomingNotifications() {

        if (incomingNotificationListener != null) {
            incomingNotificationListener.remove();
        }

        incomingNotificationListener =
                db.collection("requests")
                        .addSnapshotListener((snapshot, error) -> {

                            if (!isAdded()) return;

                            if (error != null || snapshot == null) {

                                Toast.makeText(
                                        requireContext(),
                                        "Failed to load notifications.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                updateNotificationState(
                                        new ArrayList<>()
                                );

                                return;
                            }

                            List<DocumentSnapshot> incomingDocs =
                                    new ArrayList<>();

                            for (DocumentSnapshot doc :
                                    snapshot.getDocuments()) {

                                if (isIncomingSACRequest(doc)) {
                                    incomingDocs.add(doc);
                                }
                            }

                            updateNotificationState(incomingDocs);
                        });
    }

    private void updateNotificationState(
            List<DocumentSnapshot> docs
    ) {

        layoutNotificationList.removeAllViews();

        int count = docs.size();

        tvIncomingCount.setText(
                count
                        + " incoming booking request"
                        + (count == 1 ? "" : "s")
        );

        if (count == 0) {

            layoutEmptyState.setVisibility(View.VISIBLE);
            layoutNotificationList.setVisibility(View.GONE);

            return;
        }

        layoutEmptyState.setVisibility(View.GONE);
        layoutNotificationList.setVisibility(View.VISIBLE);

        for (DocumentSnapshot doc : docs) {

            layoutNotificationList.addView(
                    SACViewFactory.createNotificationCard(
                            requireContext(),
                            doc,
                            v -> openRequestDetails(doc.getId())
                    )
            );
        }
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
                || "Rejected".equalsIgnoreCase(sacStatus)) {

            return false;
        }

        if ("Approved".equalsIgnoreCase(sacStatus)) {
            return false;
        }

        return "Pending".equalsIgnoreCase(status)
                || "Pending".equalsIgnoreCase(sacStatus)
                || status.isEmpty();
    }

    /**
     * FIXED VERSION
     *
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
                getStringValue(doc,
                        "notificationTarget");

        if ("SAC".equalsIgnoreCase(notificationTarget)) {
            return true;
        }

        String workflowStage =
                getStringValue(doc,
                        "workflowStage");

        return "SAC_REVIEW"
                .equalsIgnoreCase(workflowStage);
    }

    private void openRequestDetails(
            String requestId
    ) {

        if (requestId == null
                || requestId.trim().isEmpty()) {

            Toast.makeText(
                    requireContext(),
                    "No request found.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.sac_fragment_container,
                        sacRequestsViewDetailsFragment
                                .newInstance(requestId)
                )
                .addToBackStack(null)
                .commit();
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