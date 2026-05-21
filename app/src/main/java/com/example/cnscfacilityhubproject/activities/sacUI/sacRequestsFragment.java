package com.example.cnscfacilityhubproject.activities.sacUI;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class sacRequestsFragment extends Fragment {

    private AutoCompleteTextView actvRequestFilter;
    private LinearLayout layoutRequestList;
    private View layoutEmptyState;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;

    private FirebaseFirestore db;
    private ListenerRegistration requestsListener;

    private final List<DocumentSnapshot> sacRequestList = new ArrayList<>();
    private String selectedFilter = "All";

    public sacRequestsFragment() {
        super(R.layout.fragment_sac_requests);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            selectedFilter = getArguments().getString("filter", "All");
        }

        bindViews(view);
        setupFilter();
        listenForSACRequests();
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
    }

    private void setupFilter() {
        String[] filterOptions = {
                "All",
                "Pending",
                "Approved",
                "Rejected"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                filterOptions
        );

        actvRequestFilter.setAdapter(adapter);
        actvRequestFilter.setText(selectedFilter, false);

        actvRequestFilter.setOnItemClickListener((parent, view, position, id) -> {
            selectedFilter = filterOptions[position];
            renderRequestList();
        });
    }

    private void listenForSACRequests() {
        if (requestsListener != null) {
            requestsListener.remove();
        }

        requestsListener = db.collection("requests")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null || snapshot == null) {
                        Toast.makeText(
                                requireContext(),
                                "Failed to load SAC requests.",
                                Toast.LENGTH_LONG
                        ).show();

                        showEmptyState(
                                "Unable to load requests",
                                "Please check your connection and try again."
                        );

                        return;
                    }

                    sacRequestList.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (isSACRequest(doc) && RequestDataHelper.shouldShowInRequestList(doc)) {
                            sacRequestList.add(doc);
                        }
                    }

                    renderRequestList();
                });
    }

    private boolean isSACRequest(DocumentSnapshot doc) {
        if (!isStudentCenterFacility(doc)) {
            return false;
        }

        Boolean sendToSAC = doc.getBoolean("sendToSAC");

        if (Boolean.TRUE.equals(sendToSAC)) {
            return true;
        }

        Boolean notificationForSAC = doc.getBoolean("notificationForSAC");

        if (Boolean.TRUE.equals(notificationForSAC)) {
            return true;
        }

        String notificationTarget = getStringValue(doc, "notificationTarget");

        if ("SAC".equalsIgnoreCase(notificationTarget)) {
            return true;
        }

        String workflowStage = getStringValue(doc, "workflowStage");

        if ("SAC_REVIEW".equalsIgnoreCase(workflowStage)
                || "REJECTED_BY_SAC".equalsIgnoreCase(workflowStage)) {
            return true;
        }

        String sacStatus = getStringValue(doc, "sacStatus");

        return "Pending".equalsIgnoreCase(sacStatus)
                || "Approved".equalsIgnoreCase(sacStatus)
                || "Rejected".equalsIgnoreCase(sacStatus);
    }

    private boolean isStudentCenterFacility(DocumentSnapshot doc) {
        String finalFacilityName = getStringValue(doc, "finalFacilityName");
        String facility = getStringValue(doc, "facility");
        String selectedFacility = getStringValue(doc, "selectedFacility");
        String facilityName = getStringValue(doc, "facilityName");

        return isStudentCenterText(finalFacilityName)
                || isStudentCenterText(facility)
                || isStudentCenterText(selectedFacility)
                || isStudentCenterText(facilityName);
    }

    private boolean isStudentCenterText(String value) {
        if (value == null) {
            return false;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        return "student center".equals(normalized)
                || normalized.contains("student center");
    }

    private void renderRequestList() {
        if (layoutRequestList == null || layoutEmptyState == null) return;

        layoutRequestList.removeAllViews();

        List<DocumentSnapshot> filtered = new ArrayList<>();

        for (DocumentSnapshot doc : sacRequestList) {
            String status = getDisplayStatus(doc);

            if ("All".equalsIgnoreCase(selectedFilter)
                    || selectedFilter.equalsIgnoreCase(status)) {
                filtered.add(doc);
            }
        }

        if (filtered.isEmpty()) {
            showEmptyState(
                    "No " + selectedFilter.toLowerCase(Locale.getDefault()) + " requests",
                    "There are no Student Center requests under this selected filter."
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

        String purpose = getStringValue(doc, "purpose");
        String status = getDisplayStatus(doc);
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

        cardParams.setMargins(0, 0, 0, dp(14));

        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(26));
        card.setCardElevation(dp(7));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(Color.parseColor("#313131"));

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(18));

        LinearLayout headerRow = new LinearLayout(requireContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        MaterialCardView iconCard = new MaterialCardView(requireContext());

        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                dp(48),
                dp(48)
        );

        iconCard.setLayoutParams(iconParams);
        iconCard.setRadius(dp(16));
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

        tvTitle.setText(!purpose.isEmpty() ? purpose : "Student Center Request");
        tvTitle.setTextColor(Color.parseColor("#313131"));
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvMeta = new TextView(requireContext());

        tvMeta.setText(buildMetaText(
                facility,
                startDate,
                endDate,
                startTime,
                endTime
        ));

        tvMeta.setTextColor(Color.parseColor("#313131"));
        tvMeta.setTextSize(12f);
        tvMeta.setAlpha(0.68f);

        titleLayout.addView(tvTitle);
        titleLayout.addView(tvMeta);

        Chip chipStatus = new Chip(requireContext());

        chipStatus.setText(status);
        chipStatus.setTextColor(getStatusMainColor(status));

        chipStatus.setChipBackgroundColor(
                ColorStateList.valueOf(getStatusLightColor(status))
        );

        chipStatus.setChipStrokeWidth(0);
        chipStatus.setCheckable(false);
        chipStatus.setClickable(false);

        headerRow.addView(iconCard);
        headerRow.addView(titleLayout);
        headerRow.addView(chipStatus);

        TextView tvDescription = new TextView(requireContext());

        tvDescription.setText(buildRequestSummary(doc));
        tvDescription.setTextColor(Color.parseColor("#313131"));
        tvDescription.setTextSize(14f);
        tvDescription.setLineSpacing(3f, 1f);

        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        descParams.setMargins(0, dp(14), 0, 0);

        tvDescription.setLayoutParams(descParams);

        MaterialButton btnViewRequest = new MaterialButton(requireContext());

        btnViewRequest.setText("View Request");
        btnViewRequest.setAllCaps(false);
        btnViewRequest.setTextColor(Color.WHITE);

        btnViewRequest.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        btnViewRequest.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#313131"))
        );

        btnViewRequest.setCornerRadius(dp(16));
        btnViewRequest.setElevation(0);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        );

        buttonParams.setMargins(0, dp(16), 0, 0);

        btnViewRequest.setLayoutParams(buttonParams);

        btnViewRequest.setOnClickListener(v -> openViewDetails(requestId));

        container.addView(headerRow);
        container.addView(tvDescription);
        container.addView(btnViewRequest);

        card.addView(container);

        return card;
    }

    private void openViewDetails(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    "Request ID not found.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.sac_fragment_container,
                        sacRequestsViewDetailsFragment.newInstance(requestId)
                )
                .addToBackStack(null)
                .commit();
    }

    private String getDisplayStatus(DocumentSnapshot doc) {
        String sacStatus = getStringValue(doc, "sacStatus");
        String status = getStringValue(doc, "status");

        if ("Rejected".equalsIgnoreCase(sacStatus)
                || "Rejected".equalsIgnoreCase(status)) {
            return "Rejected";
        }

        if ("Approved".equalsIgnoreCase(sacStatus)
                || "Approved".equalsIgnoreCase(status)) {
            return "Approved";
        }

        return "Pending";
    }

    private String buildRequestSummary(DocumentSnapshot doc) {
        StringBuilder builder = new StringBuilder();

        String facility = getFinalFacility(doc);
        String participants = getStringValue(doc, "participants");
        String numberOfParticipants = getStringValue(doc, "numberOfParticipants");
        String purpose = getStringValue(doc, "purpose");

        builder.append("Facility: ")
                .append(!facility.isEmpty() ? facility : "—");

        if (!participants.isEmpty()) {
            builder.append("\nParticipants: ").append(participants);
        }

        if (!numberOfParticipants.isEmpty()) {
            builder.append("\nNumber of Participants: ")
                    .append(numberOfParticipants);
        }

        if (!purpose.isEmpty()) {
            builder.append("\nPurpose: ").append(purpose);
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
            if (builder.length() > 0) {
                builder.append(" • ");
            }

            if (!endDate.isEmpty()
                    && !startDate.equalsIgnoreCase(endDate)) {
                builder.append(startDate)
                        .append(" - ")
                        .append(endDate);
            } else {
                builder.append(startDate);
            }
        }

        if (!startTime.isEmpty()) {
            if (builder.length() > 0) {
                builder.append(" • ");
            }

            if (!endTime.isEmpty()) {
                builder.append(startTime)
                        .append(" - ")
                        .append(endTime);
            } else {
                builder.append(startTime);
            }
        }

        return builder.length() == 0
                ? "No schedule details"
                : builder.toString();
    }

    private String getFinalFacility(DocumentSnapshot doc) {
        String finalFacilityName = getStringValue(
                doc,
                "finalFacilityName"
        );

        if (!finalFacilityName.isEmpty()) {
            return finalFacilityName;
        }

        String facility = getStringValue(doc, "facility");

        String otherFacility = getStringValue(
                doc,
                "otherFacility"
        );

        if ("Others".equalsIgnoreCase(facility)
                && !otherFacility.isEmpty()) {
            return otherFacility;
        }

        return facility;
    }

    private int getStatusIcon(String status) {
        if ("Approved".equalsIgnoreCase(status)) {
            return android.R.drawable.checkbox_on_background;
        }

        if ("Rejected".equalsIgnoreCase(status)) {
            return android.R.drawable.ic_delete;
        }

        return android.R.drawable.ic_menu_recent_history;
    }

    private int getStatusMainColor(String status) {
        if ("Approved".equalsIgnoreCase(status)) {
            return Color.parseColor("#2E7D32");
        }

        if ("Rejected".equalsIgnoreCase(status)) {
            return Color.parseColor("#970705");
        }

        return Color.parseColor("#313131");
    }

    private int getStatusLightColor(String status) {
        if ("Approved".equalsIgnoreCase(status)) {
            return Color.parseColor("#E7F4E8");
        }

        if ("Rejected".equalsIgnoreCase(status)) {
            return Color.parseColor("#F3D9D9");
        }

        return Color.parseColor("#EEEEEE");
    }

    private void showEmptyState(String title, String subtitle) {
        layoutRequestList.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);

        if (tvEmptyTitle != null) {
            tvEmptyTitle.setText(title);
        }

        if (tvEmptySubtitle != null) {
            tvEmptySubtitle.setText(subtitle);
        }
    }

    private String getStringValue(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);

        return value == null
                ? ""
                : String.valueOf(value).trim();
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }
}