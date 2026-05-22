package com.example.cnscfacilityhubproject.activities.itsoUI;

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

public class itsoNotificationFragment extends Fragment {

    private static final String KEY_SELECTED_FILTER = "selectedFilter";

    private AutoCompleteTextView actvNotificationFilter;
    private View layoutEmptyState;
    private LinearLayout layoutNotificationList;

    private FirebaseFirestore db;
    private ListenerRegistration notificationListener;

    private final List<DocumentSnapshot> notificationList = new ArrayList<>();

    private String selectedFilter = "All";

    public itsoNotificationFragment() {
        super(R.layout.fragment_itso_notification);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        if (savedInstanceState != null) {
            selectedFilter = normalizeFilter(savedInstanceState.getString(KEY_SELECTED_FILTER, "All"));
        } else if (getArguments() != null) {
            selectedFilter = normalizeFilter(getArguments().getString("filter", "All"));
        } else {
            selectedFilter = normalizeFilter(selectedFilter);
        }

        actvNotificationFilter = view.findViewById(R.id.actvNotificationFilter);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        layoutNotificationList = view.findViewById(R.id.layoutNotificationList);

        setupFilter();
        listenForITSONotifications();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SELECTED_FILTER, normalizeFilter(selectedFilter));
    }

    @Override
    public void onDestroyView() {
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }

        super.onDestroyView();
    }

    private void setupFilter() {
        String[] filterOptions = {
                "All",
                "Pending",
                "Available",
                "Not Available"
        };

        ArrayAdapter<String> adapter = new NoFilterArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                filterOptions
        );

        selectedFilter = normalizeFilter(selectedFilter);

        if (actvNotificationFilter != null) {
            actvNotificationFilter.setAdapter(adapter);
            actvNotificationFilter.setThreshold(0);
            actvNotificationFilter.setText(selectedFilter, false);

            actvNotificationFilter.setOnClickListener(v -> {
                actvNotificationFilter.post(() -> actvNotificationFilter.showDropDown());
            });

            actvNotificationFilter.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    actvNotificationFilter.post(() -> actvNotificationFilter.showDropDown());
                }
            });

            actvNotificationFilter.setOnItemClickListener((parent, view, position, id) -> {
                Object selectedItem = parent.getItemAtPosition(position);
                selectedFilter = normalizeFilter(selectedItem == null ? "All" : selectedItem.toString());
                actvNotificationFilter.setText(selectedFilter, false);
                renderNotifications();
            });
        }
    }

    private void listenForITSONotifications() {
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }

        notificationListener = db.collection("requests")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null || snapshot == null) {
                        Toast.makeText(
                                requireContext(),
                                "Failed to load notifications.",
                                Toast.LENGTH_SHORT
                        ).show();

                        showEmptyState();
                        return;
                    }

                    List<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());

                    Collections.sort(docs, new Comparator<DocumentSnapshot>() {
                        @Override
                        public int compare(DocumentSnapshot a, DocumentSnapshot b) {
                            Timestamp timeA = getBestNotificationTimestamp(a);
                            Timestamp timeB = getBestNotificationTimestamp(b);

                            if (timeA == null && timeB == null) return 0;
                            if (timeA == null) return 1;
                            if (timeB == null) return -1;

                            return timeB.compareTo(timeA);
                        }
                    });

                    notificationList.clear();

                    for (DocumentSnapshot doc : docs) {
                        if (!RequestDataHelper.shouldShowInRequestList(doc)) {
                            continue;
                        }

                        if (!shouldShowITSONotificationCard(doc)) {
                            continue;
                        }

                        notificationList.add(doc);
                    }

                    renderNotifications();
                });
    }

    private void renderNotifications() {
        if (layoutNotificationList == null || layoutEmptyState == null) {
            return;
        }

        selectedFilter = normalizeFilter(selectedFilter);

        if (actvNotificationFilter != null) {
            String currentText = actvNotificationFilter.getText() == null
                    ? ""
                    : actvNotificationFilter.getText().toString();

            if (!selectedFilter.equalsIgnoreCase(currentText)) {
                actvNotificationFilter.setText(selectedFilter, false);
            }
        }

        layoutNotificationList.removeAllViews();

        List<DocumentSnapshot> filtered = new ArrayList<>();

        for (DocumentSnapshot doc : notificationList) {
            String status = normalizeFilter(getDisplayStatus(doc));

            if ("All".equalsIgnoreCase(selectedFilter)
                    || selectedFilter.equalsIgnoreCase(status)) {
                filtered.add(doc);
            }
        }

        if (filtered.isEmpty()) {
            showEmptyState();
            return;
        }

        layoutEmptyState.setVisibility(View.GONE);
        layoutNotificationList.setVisibility(View.VISIBLE);

        for (DocumentSnapshot doc : filtered) {
            layoutNotificationList.addView(createNotificationCard(doc));
        }
    }

    private View createNotificationCard(DocumentSnapshot doc) {
        String requestId = doc.getId();

        String status = normalizeFilter(getDisplayStatus(doc));
        String purpose = getStringValue(doc, "purpose");
        String facility = getFinalFacility(doc);

        String startDate = getStringValue(doc, "startDateText");
        String endDate = getStringValue(doc, "endDateText");
        String startTime = getStringValue(doc, "timeStartText");
        String endTime = getStringValue(doc, "timeEndText");

        String notifiedDateText = buildNotifiedDateText(doc);

        LinearLayout outerLayout = new LinearLayout(requireContext());
        outerLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams outerParams = new LinearLayout.LayoutParams(
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

        LinearLayout.LayoutParams notifiedDateParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        notifiedDateParams.setMargins(dp(4), 0, 0, dp(6));
        tvNotifiedDate.setLayoutParams(notifiedDateParams);

        MaterialCardView card = new MaterialCardView(requireContext());

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(24));
        card.setCardElevation(dp(6));
        card.setStrokeWidth(dp(2));
        card.setStrokeColor(getStatusMainColor(status));

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(18));

        LinearLayout headerRow = new LinearLayout(requireContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        MaterialCardView iconCard = new MaterialCardView(requireContext());

        LinearLayout.LayoutParams iconCardParams = new LinearLayout.LayoutParams(
                dp(46),
                dp(46)
        );

        iconCard.setLayoutParams(iconCardParams);
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
        tvTitle.setText(!purpose.isEmpty() ? purpose : "ITSO Request");
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
        btnViewDetails.setText("View Request");
        btnViewDetails.setAllCaps(false);
        btnViewDetails.setTextColor(Color.WHITE);
        btnViewDetails.setTypeface(null, android.graphics.Typeface.BOLD);
        btnViewDetails.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#313131"))
        );
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

        outerLayout.addView(tvNotifiedDate);
        outerLayout.addView(card);

        return outerLayout;
    }

    private void openRequestDetails(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    "No request found.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        itsoHomeViewDetailsFragment fragment =
                itsoHomeViewDetailsFragment.newInstance(requestId);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.itso_fragment_container,
                        fragment
                )
                .addToBackStack(null)
                .commit();
    }

    private boolean shouldShowITSONotificationCard(DocumentSnapshot doc) {
        return hasAnyTechnicalRequest(doc);
    }

    private boolean hasAnyTechnicalRequest(DocumentSnapshot doc) {
        if (isTrue(doc, "needsITSO")) {
            return true;
        }

        if (isTrue(doc, "soundSystemSetup")
                || isTrue(doc, "microphones")
                || isTrue(doc, "portableSpeaker")
                || isTrue(doc, "lights")
                || isTrue(doc, "livestreamingServices")
                || isTrue(doc, "zoomHosting")
                || isTrue(doc, "gmeetHosting")
                || isTrue(doc, "webCamera")
                || isTrue(doc, "tripod")
                || isTrue(doc, "multimediaProjector")) {
            return true;
        }

        return !getStringValue(doc, "connectors").isEmpty();
    }

    private boolean isTrue(DocumentSnapshot doc, String fieldName) {
        Boolean value = doc.getBoolean(fieldName);
        return Boolean.TRUE.equals(value);
    }

    private String getDisplayStatus(DocumentSnapshot doc) {
        String status = getStringValue(doc, "status");
        String itsoStatus = getStringValue(doc, "itsoStatus");
        String itsoAvailability = getStringValue(doc, "itsoAvailability");

        if ("Not Available".equalsIgnoreCase(itsoAvailability)
                || "Unavailable".equalsIgnoreCase(itsoAvailability)
                || "Not Available".equalsIgnoreCase(itsoStatus)
                || "Unavailable".equalsIgnoreCase(itsoStatus)
                || "Rejected".equalsIgnoreCase(itsoStatus)
                || "Returned".equalsIgnoreCase(itsoStatus)
                || "Not Available".equalsIgnoreCase(status)
                || "Unavailable".equalsIgnoreCase(status)
                || "Rejected".equalsIgnoreCase(status)
                || "Returned".equalsIgnoreCase(status)) {
            return "Not Available";
        }

        if ("Available".equalsIgnoreCase(itsoAvailability)
                || "Approved".equalsIgnoreCase(itsoAvailability)
                || "Available".equalsIgnoreCase(itsoStatus)
                || "Approved".equalsIgnoreCase(itsoStatus)
                || "Approved - Available".equalsIgnoreCase(itsoStatus)
                || "Available".equalsIgnoreCase(status)
                || "Approved".equalsIgnoreCase(status)
                || "Approved - Available".equalsIgnoreCase(status)) {
            return "Available";
        }

        return "Pending";
    }

    private String normalizeFilter(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            return "All";
        }

        String clean = filter.trim();

        if ("Approved - Available".equalsIgnoreCase(clean)
                || "Approved".equalsIgnoreCase(clean)) {
            return "Available";
        }

        if ("Rejected".equalsIgnoreCase(clean)
                || "Returned".equalsIgnoreCase(clean)
                || "Unavailable".equalsIgnoreCase(clean)) {
            return "Not Available";
        }

        if ("All".equalsIgnoreCase(clean)) {
            return "All";
        }

        if ("Pending".equalsIgnoreCase(clean)) {
            return "Pending";
        }

        if ("Available".equalsIgnoreCase(clean)) {
            return "Available";
        }

        if ("Not Available".equalsIgnoreCase(clean)) {
            return "Not Available";
        }

        return "All";
    }

    private String buildDescriptionText(DocumentSnapshot doc, String status) {
        String title = getStringValue(doc, "itsoNotificationTitle");
        String message = getStringValue(doc, "itsoNotificationMessage");

        if (title.isEmpty()) {
            title = getStringValue(doc, "notificationTitle");
        }

        if (message.isEmpty()) {
            message = getStringValue(doc, "notificationMessage");
        }

        if (!title.isEmpty() && !message.isEmpty()) {
            return title + "\n" + message;
        }

        if (!message.isEmpty()) {
            return message;
        }

        String requestorName = getStringValue(doc, "requestorName");
        String fullName = getStringValue(doc, "fullName");
        String nameToShow = !requestorName.isEmpty() ? requestorName : fullName;

        if ("Available".equalsIgnoreCase(status)) {
            return "This technical support request has been marked as available.";
        }

        if ("Not Available".equalsIgnoreCase(status)) {
            return "This technical support request has been marked as not available.";
        }

        if (!nameToShow.isEmpty()) {
            return nameToShow + " submitted a request that requires ITSO review.";
        }

        return "A request is waiting for ITSO review.";
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
        Timestamp timestamp = doc.getTimestamp("itsoNotifiedAt");

        if (timestamp == null) {
            timestamp = doc.getTimestamp("notificationUpdatedAt");
        }

        if (timestamp == null) {
            timestamp = doc.getTimestamp("itsoNotificationOpenedAt");
        }

        if (timestamp == null) {
            timestamp = doc.getTimestamp("updatedAt");
        }

        if (timestamp == null) {
            timestamp = doc.getTimestamp("createdAt");
        }

        return timestamp;
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
        if ("Available".equalsIgnoreCase(status)) {
            return android.R.drawable.checkbox_on_background;
        }

        if ("Not Available".equalsIgnoreCase(status)) {
            return android.R.drawable.ic_dialog_alert;
        }

        return android.R.drawable.ic_dialog_info;
    }

    private int getStatusMainColor(String status) {
        if ("Available".equalsIgnoreCase(status)) {
            return Color.parseColor("#2E7D32");
        }

        if ("Not Available".equalsIgnoreCase(status)) {
            return Color.parseColor("#F57C00");
        }

        return Color.parseColor("#313131");
    }

    private int getStatusLightColor(String status) {
        if ("Available".equalsIgnoreCase(status)) {
            return Color.parseColor("#E7F4E8");
        }

        if ("Not Available".equalsIgnoreCase(status)) {
            return Color.parseColor("#FFF3E0");
        }

        return Color.parseColor("#EEEEEE");
    }

    private void showEmptyState() {
        if (layoutNotificationList != null) {
            layoutNotificationList.setVisibility(View.GONE);
        }

        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(View.VISIBLE);
        }
    }

    private String getStringValue(DocumentSnapshot doc, String field) {
        if (doc == null || field == null) {
            return "";
        }

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

    private static class NoFilterArrayAdapter extends ArrayAdapter<String> {

        private final String[] items;

        public NoFilterArrayAdapter(
                @NonNull Context context,
                int resource,
                @NonNull String[] objects
        ) {
            super(context, resource, objects);
            this.items = objects;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    results.values = items;
                    results.count = items.length;
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    clear();

                    if (results != null && results.values instanceof String[]) {
                        String[] values = (String[]) results.values;

                        for (String item : values) {
                            add(item);
                        }
                    }

                    notifyDataSetChanged();
                }

                @Override
                public CharSequence convertResultToString(Object resultValue) {
                    return resultValue == null ? "" : resultValue.toString();
                }
            };
        }
    }
}