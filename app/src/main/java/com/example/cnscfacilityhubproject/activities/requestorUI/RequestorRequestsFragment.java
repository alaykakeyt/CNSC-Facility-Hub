package com.example.cnscfacilityhubproject.activities.requestorUI;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.utils.RequestDataHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class RequestorRequestsFragment extends Fragment {

    private AutoCompleteTextView actvRequestFilter;
    private LinearLayout layoutRequestList;
    private View layoutEmptyState;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private TextView tvRequestCount;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration requestsListener;

    private String selectedFilter = "All";
    private final List<DocumentSnapshot> requestList = new ArrayList<>();

    private final String[] filterOptions = {
            "All",
            "Pending",
            "Returned",
            "Approved"
    };

    public RequestorRequestsFragment() {
        super(R.layout.fragment_requestor_requests);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews(view);
        setupFilter();
        listenForRequests();
    }

    @Override
    public void onResume() {
        super.onResume();

        setupFilter();

        if (actvRequestFilter != null) {
            actvRequestFilter.setText(selectedFilter, false);
        }

        renderRequests();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }
    }

    private void bindViews(View view) {
        actvRequestFilter = view.findViewById(R.id.actvRequestFilter);
        layoutRequestList = view.findViewById(R.id.layoutRequestList);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = view.findViewById(R.id.tvEmptySubtitle);
        tvRequestCount = view.findViewById(R.id.tvRequestCount);
    }

    private void setupFilter() {
        if (actvRequestFilter == null || !isAdded()) return;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                filterOptions
        );

        actvRequestFilter.setAdapter(adapter);
        actvRequestFilter.setThreshold(0);
        actvRequestFilter.setText(selectedFilter, false);

        actvRequestFilter.setOnClickListener(v -> actvRequestFilter.showDropDown());
        actvRequestFilter.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) actvRequestFilter.showDropDown();
        });

        actvRequestFilter.setOnItemClickListener((parent, view, position, id) -> {
            selectedFilter = filterOptions[position];
            actvRequestFilter.setText(selectedFilter, false);
            renderRequests();
        });
    }

    private void listenForRequests() {
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }

        if (auth.getCurrentUser() == null) {
            showEmptyState("No logged in user", "Please log in to view your requests.");
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        requestsListener = db.collection("requests")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null || snapshot == null) {
                        Toast.makeText(requireContext(), "Failed to load requests.", Toast.LENGTH_LONG).show();

                        showEmptyState(
                                "Unable to load requests",
                                "Please check your connection and try again."
                        );
                        return;
                    }

                    requestList.clear();
                    requestList.addAll(snapshot.getDocuments());

                    Collections.sort(requestList, new Comparator<DocumentSnapshot>() {
                        @Override
                        public int compare(DocumentSnapshot a, DocumentSnapshot b) {
                            Timestamp timeA = a.getTimestamp("updatedAt");
                            Timestamp timeB = b.getTimestamp("updatedAt");

                            if (timeA == null && timeB == null) return 0;
                            if (timeA == null) return 1;
                            if (timeB == null) return -1;

                            return timeB.compareTo(timeA);
                        }
                    });

                    if (actvRequestFilter != null) {
                        actvRequestFilter.setText(selectedFilter, false);
                    }

                    renderRequests();
                });
    }

    private void renderRequests() {
        if (layoutRequestList == null || layoutEmptyState == null) return;

        layoutRequestList.removeAllViews();

        List<DocumentSnapshot> filtered = new ArrayList<>();

        for (DocumentSnapshot doc : requestList) {
            if (!RequestDataHelper.shouldShowInRequestList(doc)) {
                continue;
            }

            String displayStatus = getDisplayStatus(doc);

            if ("All".equalsIgnoreCase(selectedFilter)) {
                filtered.add(doc);
            } else if (selectedFilter.equalsIgnoreCase(displayStatus)) {
                filtered.add(doc);
            }
        }

        if (tvRequestCount != null) {
            tvRequestCount.setText(filtered.size() + " request" + (filtered.size() == 1 ? "" : "s"));
        }

        if (filtered.isEmpty()) {
            showEmptyState(
                    "No " + selectedFilter.toLowerCase(Locale.getDefault()) + " requests",
                    "You have no requests under this filter."
            );
            return;
        }

        layoutEmptyState.setVisibility(View.GONE);
        layoutRequestList.setVisibility(View.VISIBLE);

        for (DocumentSnapshot doc : filtered) {
            layoutRequestList.addView(createRequestCard(doc));
        }
    }

    private View createRequestCard(DocumentSnapshot doc) {
        String requestId = doc.getId();
        String status = getDisplayStatus(doc);
        String purpose = getStringValue(doc, "purpose");
        String facility = getFinalFacility(doc);
        String startDate = getStringValue(doc, "startDateText");
        String endDate = getStringValue(doc, "endDateText");
        String startTime = getStringValue(doc, "timeStartText");
        String endTime = getStringValue(doc, "timeEndText");

        MaterialCardView card = new MaterialCardView(requireContext());

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(12));

        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(24));
        card.setCardElevation(dp(6));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(Color.parseColor("#313131"));

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(18));

        LinearLayout headerRow = new LinearLayout(requireContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        MaterialCardView iconCard = new MaterialCardView(requireContext());
        iconCard.setLayoutParams(new LinearLayout.LayoutParams(dp(46), dp(46)));
        iconCard.setRadius(dp(15));
        iconCard.setCardElevation(0);
        iconCard.setCardBackgroundColor(getStatusMainColor(status));

        ImageView icon = new ImageView(requireContext());
        icon.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        icon.setPadding(dp(10), dp(10), dp(10), dp(10));
        icon.setImageResource(getStatusIcon(status));
        icon.setColorFilter(Color.WHITE);

        iconCard.addView(icon);

        LinearLayout titleLayout = new LinearLayout(requireContext());
        titleLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        titleParams.setMargins(dp(12), 0, dp(8), 0);
        titleLayout.setLayoutParams(titleParams);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(!purpose.isEmpty() ? purpose : "Untitled Request");
        tvTitle.setTextColor(Color.parseColor("#313131"));
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvMeta = new TextView(requireContext());
        tvMeta.setText(buildMetaText(facility, startDate, endDate, startTime, endTime));
        tvMeta.setTextColor(Color.parseColor("#313131"));
        tvMeta.setTextSize(12f);
        tvMeta.setAlpha(0.65f);

        titleLayout.addView(tvTitle);
        titleLayout.addView(tvMeta);

        Chip chipStatus = new Chip(requireContext());
        chipStatus.setText(status);
        chipStatus.setTextColor(getStatusMainColor(status));
        chipStatus.setChipBackgroundColor(ColorStateList.valueOf(getStatusLightColor(status)));
        chipStatus.setChipStrokeWidth(0);
        chipStatus.setCheckable(false);
        chipStatus.setClickable(false);

        headerRow.addView(iconCard);
        headerRow.addView(titleLayout);
        headerRow.addView(chipStatus);

        TextView tvDescription = new TextView(requireContext());
        tvDescription.setText(buildDescriptionText(doc, status));
        tvDescription.setTextColor(Color.parseColor("#313131"));
        tvDescription.setTextSize(14f);
        tvDescription.setLineSpacing(2f, 1f);

        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        descParams.setMargins(0, dp(12), 0, 0);
        tvDescription.setLayoutParams(descParams);

        MaterialButton btnViewDetails = new MaterialButton(requireContext());
        btnViewDetails.setText("View Details");
        btnViewDetails.setAllCaps(false);
        btnViewDetails.setTextColor(Color.WHITE);
        btnViewDetails.setTypeface(null, android.graphics.Typeface.BOLD);
        btnViewDetails.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#313131")));
        btnViewDetails.setCornerRadius(dp(16));
        btnViewDetails.setElevation(0);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
        );
        btnParams.setMargins(0, dp(14), 0, 0);
        btnViewDetails.setLayoutParams(btnParams);

        btnViewDetails.setOnClickListener(v -> openRequestDetails(requestId));

        container.addView(headerRow);
        container.addView(tvDescription);
        container.addView(btnViewDetails);

        card.addView(container);

        return card;
    }

    private void openRequestDetails(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Request ID not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestorRequestDetailsFragment fragment =
                RequestorRequestDetailsFragment.newInstance(requestId);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private String getDisplayStatus(DocumentSnapshot doc) {
        String status = getStringValue(doc, "status");
        String gsoStatus = getStringValue(doc, "gsoStatus");
        String sacStatus = getStringValue(doc, "sacStatus");
        String itsoStatus = getStringValue(doc, "itsoStatus");
        String workflowStage = getStringValue(doc, "workflowStage");

        if ("Returned".equalsIgnoreCase(status)
                || "Returned".equalsIgnoreCase(gsoStatus)
                || "RETURNED".equalsIgnoreCase(workflowStage)) {
            return "Returned";
        }

        if ("Rejected".equalsIgnoreCase(status)
                || "Rejected".equalsIgnoreCase(gsoStatus)
                || "Rejected".equalsIgnoreCase(sacStatus)
                || "Rejected".equalsIgnoreCase(itsoStatus)
                || "REJECTED_BY_SAC".equalsIgnoreCase(workflowStage)
                || "REJECTED_BY_ITSO".equalsIgnoreCase(workflowStage)) {
            return "Returned";
        }

        if ("Approved".equalsIgnoreCase(status)
                || "Approved".equalsIgnoreCase(gsoStatus)
                || "APPROVED".equalsIgnoreCase(workflowStage)) {
            return "Approved";
        }

        return "Pending";
    }

    private String buildDescriptionText(DocumentSnapshot doc, String status) {
        String notificationTarget = getStringValue(doc, "notificationTarget");
        String workflowStage = getStringValue(doc, "workflowStage");
        String itsoAvailability = getStringValue(doc, "itsoAvailability");
        String returnReason = getStringValue(doc, "returnReason");
        String gsoReturnReason = getStringValue(doc, "gsoReturnReason");

        if ("Approved".equalsIgnoreCase(status)) {
            return "Your booking request has been approved.";
        }

        if ("Returned".equalsIgnoreCase(status)) {
            String reason = !gsoReturnReason.isEmpty() ? gsoReturnReason : returnReason;

            if (!reason.isEmpty()) {
                return "Your booking request has been returned.\nReason: " + reason;
            }

            return "Your booking request has been returned or rejected. Please review the request details.";
        }

        if ("GSO".equalsIgnoreCase(notificationTarget)
                || "GSO_REVIEW".equalsIgnoreCase(workflowStage)) {

            if ("Not Available".equalsIgnoreCase(itsoAvailability)) {
                return "ITSO marked the technical requirements as not available. Your request is now under GSO review.";
            }

            if ("Available".equalsIgnoreCase(itsoAvailability)) {
                return "ITSO marked the technical requirements as available. Your request is now under GSO review.";
            }

            return "Your request is pending review by GSO.";
        }

        if ("SAC".equalsIgnoreCase(notificationTarget)
                || "SAC_REVIEW".equalsIgnoreCase(workflowStage)
                || "WAITING_SAC_APPROVAL".equalsIgnoreCase(workflowStage)) {
            return "Your request is pending review by SAC.";
        }

        if ("ITSO".equalsIgnoreCase(notificationTarget)
                || "ITSO_REVIEW".equalsIgnoreCase(workflowStage)
                || "WAITING_ITSO_APPROVAL".equalsIgnoreCase(workflowStage)) {
            return "Your request is pending review by ITSO.";
        }

        return "Your booking request is pending review.";
    }

    private String buildMetaText(
            String facility,
            String startDate,
            String endDate,
            String startTime,
            String endTime
    ) {
        StringBuilder builder = new StringBuilder();

        if (!facility.isEmpty()) builder.append(facility);

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

    private int getStatusIcon(String status) {
        if ("Approved".equalsIgnoreCase(status)) return android.R.drawable.checkbox_on_background;
        if ("Returned".equalsIgnoreCase(status)) return android.R.drawable.ic_menu_revert;
        return android.R.drawable.ic_menu_recent_history;
    }

    private int getStatusMainColor(String status) {
        if ("Approved".equalsIgnoreCase(status)) return Color.parseColor("#2E7D32");
        if ("Returned".equalsIgnoreCase(status)) return Color.parseColor("#970705");
        return Color.parseColor("#313131");
    }

    private int getStatusLightColor(String status) {
        if ("Approved".equalsIgnoreCase(status)) return Color.parseColor("#E7F4E8");
        if ("Returned".equalsIgnoreCase(status)) return Color.parseColor("#F3D9D9");
        return Color.parseColor("#EEEEEE");
    }

    private void showEmptyState(String title, String subtitle) {
        if (layoutRequestList != null) layoutRequestList.setVisibility(View.GONE);
        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
        if (tvEmptyTitle != null) tvEmptyTitle.setText(title);
        if (tvEmptySubtitle != null) tvEmptySubtitle.setText(subtitle);
        if (tvRequestCount != null) tvRequestCount.setText("0 requests");
    }

    private String getStringValue(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}