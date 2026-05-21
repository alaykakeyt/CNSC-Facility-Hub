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
import com.example.cnscfacilityhubproject.utils.RequestDataHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Source;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;



public class sacHomeFragment extends Fragment {

    private TextView tvGreeting;
    private TextView tvSACName;
    private TextView tvPendingCount;
    private TextView tvApprovedCount;
    private TextView tvRecentEmpty;

    private LinearLayout layoutRecentRequests;
    private LinearLayout sacPendingReq;
    private LinearLayout sacApprovedReq;

    private MaterialButton btnReviewRequests;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private ListenerRegistration profileListener;
    private ListenerRegistration dashboardListener;

    public sacHomeFragment() {
        super(R.layout.fragment_sac_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();

        bindViews(view);
        clearFirebaseDrivenTexts();
        setupGreeting();
        setupActions();
        loadSACProfileHeader();
        listenDashboardData();
    }

    @Override
    public void onDestroyView() {
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }

        if (dashboardListener != null) {
            dashboardListener.remove();
            dashboardListener = null;
        }

        super.onDestroyView();
    }

    private void bindViews(View view) {
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvSACName = view.findViewById(R.id.tvSACName);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        tvApprovedCount = view.findViewById(R.id.tvApprovedCount);
        tvRecentEmpty = view.findViewById(R.id.tvRecentEmpty);

        layoutRecentRequests = view.findViewById(R.id.layoutRecentRequests);
        btnReviewRequests = view.findViewById(R.id.btnReviewRequests);

        sacPendingReq = view.findViewById(R.id.sacPendingReq);
        sacApprovedReq = view.findViewById(R.id.sacApprovedReq);
    }

    private void clearFirebaseDrivenTexts() {
        setTextOrHide(tvSACName, "");
        tvPendingCount.setText("");
        tvApprovedCount.setText("");
    }

    private void setupGreeting() {
        int hour = Integer.parseInt(
                new SimpleDateFormat("HH", Locale.getDefault()).format(new Date())
        );

        if (hour < 12) {
            tvGreeting.setText("Good Morning");
        } else if (hour < 18) {
            tvGreeting.setText("Good Afternoon");
        } else {
            tvGreeting.setText("Good Evening");
        }
    }

    private void loadSACProfileHeader() {
        if (currentUser == null) {
            setTextOrHide(tvSACName, "");
            return;
        }

        DocumentReference userRef = db.collection("users").document(currentUser.getUid());

        userRef.get(Source.CACHE)
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;

                    if (documentSnapshot.exists()) {
                        bindSACProfileHeader(documentSnapshot);
                    }
                });

        profileListener = userRef.addSnapshotListener((documentSnapshot, error) -> {
            if (!isAdded()) return;

            if (error != null) {
                Toast.makeText(requireContext(), "Failed to load SAC profile.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                bindSACProfileHeader(documentSnapshot);
            } else {
                setTextOrHide(tvSACName, "");
            }
        });
    }

    private void bindSACProfileHeader(DocumentSnapshot userDoc) {
        String fullName = getStringValue(userDoc, "fullName");

        if (fullName.isEmpty()) {
            setTextOrHide(tvSACName, "");
            return;
        }

        setTextOrHide(tvSACName, "Hello, " + fullName);
    }

    private void setupActions() {
        btnReviewRequests.setOnClickListener(v -> openNotifications("All"));

        sacPendingReq.setClickable(true);
        sacPendingReq.setFocusable(true);
        sacPendingReq.setOnClickListener(v -> openNotifications("Pending"));

        sacApprovedReq.setClickable(true);
        sacApprovedReq.setFocusable(true);
        sacApprovedReq.setOnClickListener(v -> openNotifications("Approved"));
    }

    private void listenDashboardData() {
        if (dashboardListener != null) {
            dashboardListener.remove();
            dashboardListener = null;
        }

        dashboardListener = db.collection("requests")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null || snapshot == null) {
                        Toast.makeText(requireContext(), "Failed to load SAC dashboard.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int pending = 0;
                    int approved = 0;
                    int recentCount = 0;

                    layoutRecentRequests.removeAllViews();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (!RequestDataHelper.shouldShowInRequestList(doc) || !isSACRelatedRequest(doc)) {
                            continue;
                        }

                        String status = getDisplayStatus(doc);

                        if ("Pending".equalsIgnoreCase(status)) {
                            pending++;
                        }

                        if ("Approved".equalsIgnoreCase(status)) {
                            approved++;
                        }

                        if (recentCount < 3) {
                            layoutRecentRequests.addView(
                                    SACViewFactory.createCompactRequestCard(
                                            requireContext(),
                                            doc,
                                            status,
                                            v -> openDetails(doc.getId())
                                    )
                            );
                            recentCount++;
                        }
                    }

                    tvPendingCount.setText(String.format(Locale.getDefault(), "%02d", pending));
                    tvApprovedCount.setText(String.format(Locale.getDefault(), "%02d", approved));

                    tvRecentEmpty.setVisibility(recentCount == 0 ? View.VISIBLE : View.GONE);
                    layoutRecentRequests.setVisibility(recentCount == 0 ? View.GONE : View.VISIBLE);
                });
    }

    private void openNotifications(String filter) {
        if (requireActivity() instanceof sacNavBarActivity) {
            ((sacNavBarActivity) requireActivity()).openNotificationsWithFilter(filter);
        }
    }



    private void openDetails(String requestId) {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.sac_fragment_container, sacRequestsViewDetailsFragment.newInstance(requestId))
                .addToBackStack(null)
                .commit();
    }

    private boolean isSACRelatedRequest(DocumentSnapshot doc) {
        String facility = getFinalFacility(doc);

        if (!"Student Center".equalsIgnoreCase(facility)) {
            return false;
        }

        if (Boolean.TRUE.equals(doc.getBoolean("needsSAC"))) {
            return true;
        }

        if (Boolean.TRUE.equals(doc.getBoolean("sendToSAC"))) {
            return true;
        }

        String notificationTarget = getStringValue(doc, "notificationTarget");
        if ("SAC".equalsIgnoreCase(notificationTarget)) {
            return true;
        }

        String workflowStage = getStringValue(doc, "workflowStage");
        if ("SAC_REVIEW".equalsIgnoreCase(workflowStage)) {
            return true;
        }

        String sacStatus = getStringValue(doc, "sacStatus");
        return !sacStatus.isEmpty();
    }

    private String getFinalFacility(DocumentSnapshot doc) {
        String finalFacilityName = getStringValue(doc, "finalFacilityName");
        if (!finalFacilityName.isEmpty()) {
            return finalFacilityName;
        }

        String facility = getStringValue(doc, "facility");
        String otherFacility = getStringValue(doc, "otherFacility");

        if ("Others".equalsIgnoreCase(facility) && !otherFacility.isEmpty()) {
            return otherFacility;
        }

        return facility;
    }

    private String getDisplayStatus(DocumentSnapshot doc) {
        String sacStatus = getStringValue(doc, "sacStatus");
        String status = getStringValue(doc, "status");

        if ("Rejected".equalsIgnoreCase(sacStatus) || "Rejected".equalsIgnoreCase(status)) {
            return "Rejected";
        }

        if (!sacStatus.isEmpty()) {
            return sacStatus;
        }

        return status;
    }

    private void setTextOrHide(TextView textView, String value) {
        if (textView == null) return;

        String cleanValue = value == null ? "" : value.trim();
        textView.setText(cleanValue);
        textView.setVisibility(cleanValue.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String getStringValue(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value == null ? "" : String.valueOf(value).trim();
    }
}
