package com.example.cnscfacilityhubproject.activities.gsoUI;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class gsoRequestsFragment extends Fragment {

    private static final String KEY_SELECTED_FILTER = "selected_filter";

    private AutoCompleteTextView actvRequestFilter;
    private LinearLayout layoutRequestList;
    private View layoutEmptyState;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;

    private FirebaseFirestore db;
    private ListenerRegistration requestsListener;

    private String selectedFilter = "All";

    private final List<DocumentSnapshot> gsoRequestList = new ArrayList<>();

    private final String[] filterOptions = {
            "All",
            "Pending",
            "Approved",
            "Returned"
    };

    public gsoRequestsFragment() {
        super(R.layout.fragment_gso_requests);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        if (savedInstanceState != null) {
            selectedFilter = savedInstanceState.getString(KEY_SELECTED_FILTER, "All");
        } else if (getArguments() != null) {
            selectedFilter = getArguments().getString("filter", "All");
        }

        bindViews(view);
        setupFilter();
        listenForGSORequests();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (actvRequestFilter != null) {
            actvRequestFilter.setText(selectedFilter, false);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SELECTED_FILTER, selectedFilter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }

        actvRequestFilter = null;
        layoutRequestList = null;
        layoutEmptyState = null;
        tvEmptyTitle = null;
        tvEmptySubtitle = null;
    }

    private void bindViews(View view) {
        actvRequestFilter = view.findViewById(R.id.actvRequestFilter);
        layoutRequestList = view.findViewById(R.id.layoutRequestList);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = view.findViewById(R.id.tvEmptySubtitle);
    }

    private void setupFilter() {
        NonFilteringArrayAdapter adapter = new NonFilteringArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                filterOptions
        );

        actvRequestFilter.setAdapter(adapter);
        actvRequestFilter.setThreshold(0);
        actvRequestFilter.setText(selectedFilter, false);

        actvRequestFilter.setOnClickListener(v -> actvRequestFilter.showDropDown());

        actvRequestFilter.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                actvRequestFilter.showDropDown();
            }
        });

        actvRequestFilter.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);

            if (item != null) {
                selectedFilter = item.toString();
                actvRequestFilter.setText(selectedFilter, false);
                renderRequestList();
            }
        });
    }

    private void listenForGSORequests() {
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }

        requestsListener = db.collection("requests")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (!isAdded()) return;

                    if (error != null || queryDocumentSnapshots == null) {
                        Toast.makeText(
                                requireContext(),
                                "Failed to load GSO requests.",
                                Toast.LENGTH_LONG
                        ).show();

                        showEmptyState(
                                "Unable to load requests",
                                "Please check your connection and try again."
                        );

                        return;
                    }

                    List<DocumentSnapshot> docs =
                            new ArrayList<>(queryDocumentSnapshots.getDocuments());

                    Collections.sort(docs, new Comparator<DocumentSnapshot>() {
                        @Override
                        public int compare(DocumentSnapshot a,
                                           DocumentSnapshot b) {
                            Timestamp timeA = getBestNotificationTimestamp(a);
                            Timestamp timeB = getBestNotificationTimestamp(b);

                            if (timeA == null && timeB == null) return 0;
                            if (timeA == null) return 1;
                            if (timeB == null) return -1;

                            return timeB.compareTo(timeA);
                        }
                    });

                    gsoRequestList.clear();

                    for (DocumentSnapshot doc : docs) {
                        if (isGSORequest(doc)
                                && RequestDataHelper.shouldShowInRequestList(doc)) {
                            gsoRequestList.add(doc);
                        }
                    }

                    renderRequestList();
                });
    }

    private void renderRequestList() {
        if (layoutRequestList == null || layoutEmptyState == null) {
            return;
        }

        layoutRequestList.removeAllViews();

        List<DocumentSnapshot> filteredList = new ArrayList<>();

        for (DocumentSnapshot doc : gsoRequestList) {
            String displayStatus = getDisplayStatus(doc);

            if ("All".equalsIgnoreCase(selectedFilter)
                    || selectedFilter.equalsIgnoreCase(displayStatus)) {
                filteredList.add(doc);
            }
        }

        if (filteredList.isEmpty()) {
            showEmptyState(
                    "No " + selectedFilter.toLowerCase(Locale.getDefault()) + " requests",
                    "There are no GSO requests under this selected filter."
            );

            return;
        }

        layoutEmptyState.setVisibility(View.GONE);
        layoutRequestList.setVisibility(View.VISIBLE);

        for (DocumentSnapshot doc : filteredList) {
            layoutRequestList.addView(createRequestCard(doc));
        }
    }

    private View createRequestCard(DocumentSnapshot doc) {
        String requestId = doc.getId();

        String purpose = getStringValue(doc, "purpose");
        String displayStatus = getDisplayStatus(doc);
        String facility = getFinalFacility(doc);

        String startDate = getStringValue(doc, "startDateText");
        String endDate = getStringValue(doc, "endDateText");
        String startTime = getStringValue(doc, "timeStartText");
        String endTime = getStringValue(doc, "timeEndText");

        String notifiedDateText = buildNotifiedDateText(doc);

        LinearLayout outerLayout = new LinearLayout(requireContext());
        outerLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams outerParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        outerParams.setMargins(0, 0, 0, dp(12));
        outerLayout.setLayoutParams(outerParams);

        TextView tvNotifiedDate = new TextView(requireContext());
        tvNotifiedDate.setText(notifiedDateText);
        tvNotifiedDate.setTextColor(Color.parseColor("#970705"));
        tvNotifiedDate.setTextSize(12f);
        tvNotifiedDate.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams notifiedDateParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        notifiedDateParams.setMargins(dp(4), 0, 0, dp(6));
        tvNotifiedDate.setLayoutParams(notifiedDateParams);

        MaterialCardView card = new MaterialCardView(requireContext());

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(24));
        card.setCardElevation(dp(6));
        card.setStrokeWidth(dp(2));
        card.setStrokeColor(getStatusMainColor(displayStatus));

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(18));

        LinearLayout headerRow = new LinearLayout(requireContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        MaterialCardView iconCard = new MaterialCardView(requireContext());

        LinearLayout.LayoutParams iconCardParams =
                new LinearLayout.LayoutParams(dp(46), dp(46));

        iconCard.setLayoutParams(iconCardParams);
        iconCard.setRadius(dp(15));
        iconCard.setCardElevation(0);
        iconCard.setCardBackgroundColor(getStatusMainColor(displayStatus));

        ImageView icon = new ImageView(requireContext());
        icon.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        icon.setPadding(dp(10), dp(10), dp(10), dp(10));
        icon.setImageResource(getStatusIcon(displayStatus));
        icon.setColorFilter(Color.WHITE);

        iconCard.addView(icon);

        LinearLayout titleLayout = new LinearLayout(requireContext());
        titleLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                );

        titleParams.setMargins(dp(12), 0, dp(8), 0);
        titleLayout.setLayoutParams(titleParams);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(!purpose.isEmpty() ? purpose : "GSO Request");
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
        tvMeta.setAlpha(0.65f);

        titleLayout.addView(tvTitle);
        titleLayout.addView(tvMeta);

        Chip chipStatus = new Chip(requireContext());
        chipStatus.setText(displayStatus);
        chipStatus.setTextColor(getStatusMainColor(displayStatus));
        chipStatus.setChipBackgroundColor(
                ColorStateList.valueOf(getStatusLightColor(displayStatus))
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
        tvDescription.setLineSpacing(2f, 1f);

        LinearLayout.LayoutParams descParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        descParams.setMargins(0, dp(12), 0, 0);
        tvDescription.setLayoutParams(descParams);

        MaterialButton btnViewRequest = new MaterialButton(requireContext());
        btnViewRequest.setText("View Request");
        btnViewRequest.setAllCaps(false);
        btnViewRequest.setTextColor(Color.WHITE);
        btnViewRequest.setTypeface(null, android.graphics.Typeface.BOLD);
        btnViewRequest.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#313131"))
        );
        btnViewRequest.setCornerRadius(dp(16));
        btnViewRequest.setElevation(0);

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(46)
                );

        buttonParams.setMargins(0, dp(14), 0, 0);
        btnViewRequest.setLayoutParams(buttonParams);

        btnViewRequest.setOnClickListener(v -> openViewDetails(requestId));

        container.addView(headerRow);
        container.addView(tvDescription);
        container.addView(btnViewRequest);

        card.addView(container);

        outerLayout.addView(tvNotifiedDate);
        outerLayout.addView(card);

        return outerLayout;
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

        gsoRequestsViewDetailsFragment fragment =
                gsoRequestsViewDetailsFragment.newInstance(requestId);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(getAvailableContainerId(), fragment)
                .addToBackStack(null)
                .commit();
    }

    private int getAvailableContainerId() {
        if (requireActivity().findViewById(R.id.gso_fragment_container) != null) {
            return R.id.gso_fragment_container;
        }

        return R.id.itso_fragment_container;
    }

    private boolean isGSORequest(DocumentSnapshot doc) {
        String workflowStage = getStringValue(doc, "workflowStage");

        if ("GSO_REVIEW".equalsIgnoreCase(workflowStage)
                || "WAITING_GSO_APPROVAL".equalsIgnoreCase(workflowStage)
                || "REJECTED_BY_GSO".equalsIgnoreCase(workflowStage)
                || "COMPLETED".equalsIgnoreCase(workflowStage)) {
            return true;
        }

        String notificationTarget = getStringValue(doc, "notificationTarget");

        if ("GSO".equalsIgnoreCase(notificationTarget)) {
            return true;
        }

        Boolean sendToGSO = doc.getBoolean("sendToGSO");
        Boolean notificationForGSO = doc.getBoolean("notificationForGSO");
        Boolean notificationForGso = doc.getBoolean("notificationForGso");

        if (Boolean.TRUE.equals(sendToGSO)
                || Boolean.TRUE.equals(notificationForGSO)
                || Boolean.TRUE.equals(notificationForGso)) {
            return true;
        }

        /*
         * Do not show requests in GSO while they are only waiting for SAC or ITSO.
         * Examples: "Waiting for SAC Approval", "Waiting for ITSO Approval".
         */
        String gsoStatus = getStringValue(doc, "gsoStatus");

        return "Pending".equalsIgnoreCase(gsoStatus)
                || "Approved".equalsIgnoreCase(gsoStatus)
                || "Returned".equalsIgnoreCase(gsoStatus)
                || "Rejected".equalsIgnoreCase(gsoStatus);
    }

    private String getDisplayStatus(DocumentSnapshot doc) {
        String gsoStatus = getStringValue(doc, "gsoStatus");
        String status = getStringValue(doc, "status");

        if ("Approved".equalsIgnoreCase(gsoStatus)
                || "Approved".equalsIgnoreCase(status)) {
            return "Approved";
        }

        if ("Returned".equalsIgnoreCase(gsoStatus)
                || "Returned".equalsIgnoreCase(status)
                || "Rejected".equalsIgnoreCase(gsoStatus)
                || "Rejected".equalsIgnoreCase(status)) {
            return "Returned";
        }

        return "Pending";
    }

    private String buildRequestSummary(DocumentSnapshot doc) {
        StringBuilder builder = new StringBuilder();

        String itsoAvailability = getStringValue(doc, "itsoAvailability");

        if (!itsoAvailability.isEmpty()) {
            builder.append("ITSO Availability: ").append(itsoAvailability);
        }

        boolean tablesRequested = Boolean.TRUE.equals(doc.getBoolean("tablesRequested"));
        boolean chairsRequested = Boolean.TRUE.equals(doc.getBoolean("chairsRequested"));

        Long tablesCount = doc.getLong("tablesCount");
        Long chairsCount = doc.getLong("chairsCount");

        if (tablesRequested) {
            if (builder.length() > 0) builder.append("\n");
            builder.append("Tables: ").append(tablesCount != null ? tablesCount : 0);
        }

        if (chairsRequested) {
            if (builder.length() > 0) builder.append("\n");
            builder.append("Chairs: ").append(chairsCount != null ? chairsCount : 0);
        }

        String otherAmenities = getStringValue(doc, "otherAmenities");

        if (!otherAmenities.isEmpty()) {
            if (builder.length() > 0) builder.append("\n");
            builder.append("Other amenities: ").append(otherAmenities);
        }

        if (builder.length() == 0) {
            return "Facility booking request submitted.";
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

        return builder.length() == 0
                ? "No schedule details"
                : builder.toString();
    }

    private String buildNotifiedDateText(DocumentSnapshot doc) {
        Timestamp timestamp = getBestNotificationTimestamp(doc);

        if (timestamp == null) {
            return "Notified date not available";
        }

        SimpleDateFormat formatter =
                new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());

        return formatter.format(timestamp.toDate());
    }

    private Timestamp getBestNotificationTimestamp(DocumentSnapshot doc) {
        Timestamp timestamp = doc.getTimestamp("gsoNotifiedAt");

        if (timestamp == null) {
            timestamp = doc.getTimestamp("notificationUpdatedAt");
        }

        if (timestamp == null) {
            timestamp = doc.getTimestamp("gsoNotificationOpenedAt");
        }

        if (timestamp == null) {
            timestamp = doc.getTimestamp("updatedAt");
        }

        if (timestamp == null) {
            timestamp = doc.getTimestamp("createdAt");
        }

        return timestamp;
    }

    private String getFinalFacility(DocumentSnapshot doc) {
        String finalFacilityName = getStringValue(doc, "finalFacilityName");

        if (!finalFacilityName.isEmpty()) {
            return finalFacilityName;
        }

        String facility = getStringValue(doc, "facility");
        String otherFacility = getStringValue(doc, "otherFacility");

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

        if ("Returned".equalsIgnoreCase(status)) {
            return android.R.drawable.ic_delete;
        }

        return android.R.drawable.ic_dialog_info;
    }

    private int getStatusMainColor(String status) {
        if ("Approved".equalsIgnoreCase(status)) {
            return Color.parseColor("#2E7D32");
        }

        if ("Returned".equalsIgnoreCase(status)) {
            return Color.parseColor("#970705");
        }

        return Color.parseColor("#313131");
    }

    private int getStatusLightColor(String status) {
        if ("Approved".equalsIgnoreCase(status)) {
            return Color.parseColor("#E7F4E8");
        }

        if ("Returned".equalsIgnoreCase(status)) {
            return Color.parseColor("#F3D9D9");
        }

        return Color.parseColor("#EEEEEE");
    }

    private void showEmptyState(String title, String subtitle) {
        if (layoutRequestList != null) {
            layoutRequestList.setVisibility(View.GONE);
        }

        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(View.VISIBLE);
        }

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
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class NonFilteringArrayAdapter extends ArrayAdapter<String> {

        private final String[] items;

        private final Filter noFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                results.values = items;
                results.count = items.length;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint,
                                          FilterResults results) {
                clear();

                if (results != null && results.values instanceof String[]) {
                    addAll((String[]) results.values);
                }

                notifyDataSetChanged();
            }
        };

        public NonFilteringArrayAdapter(
                @NonNull Context context,
                int resource,
                @NonNull String[] objects
        ) {
            super(context, resource, new ArrayList<String>());
            this.items = objects;
            addAll(objects);
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return noFilter;
        }
    }
}