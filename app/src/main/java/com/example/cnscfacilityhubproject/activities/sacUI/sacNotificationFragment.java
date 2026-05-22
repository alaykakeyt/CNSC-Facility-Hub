package com.example.cnscfacilityhubproject.activities.sacUI;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

public class sacNotificationFragment extends Fragment {

    private View layoutEmptyState;
    private LinearLayout layoutNotificationList;
    private TextView tvIncomingCount;

    private FirebaseFirestore db;
    private ListenerRegistration notificationListener;

    private final List<DocumentSnapshot> notificationList = new ArrayList<>();

    private int incomingRequestCount = 0;
    private int unseenNotificationCount = 0;

    private AutoCompleteTextView actvNotificationFilter;
    private String selectedFilter = "All";

    public sacNotificationFragment() {
        super(R.layout.fragment_sac_notification);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            selectedFilter = getArguments().getString("filter", "All");
        }

        actvNotificationFilter = view.findViewById(R.id.actvNotificationFilter);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        layoutNotificationList = view.findViewById(R.id.layoutNotificationList);
        tvIncomingCount = view.findViewById(R.id.tvIncomingCount);

        setupFilter();
        listenForSACNotifications();
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

        actvNotificationFilter.setAdapter(adapter);
        actvNotificationFilter.setText(selectedFilter, false);

        actvNotificationFilter.setOnItemClickListener((parent, view, position, id) -> {
            selectedFilter = filterOptions[position];
            renderNotifications();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }

    private void listenForSACNotifications() {
        if (notificationListener != null) {
            return;
        }

        notificationListener =
                db.collection("requests")
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

                            List<DocumentSnapshot> docs =
                                    new ArrayList<>(snapshot.getDocuments());

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

                            notificationList.clear();
                            incomingRequestCount = 0;
                            unseenNotificationCount = 0;

                            for (DocumentSnapshot doc : docs) {
                                if (!RequestDataHelper.isSACRelevantRequest(doc)
                                        || !RequestDataHelper.shouldShowInRequestList(doc)) {
                                    continue;
                                }

                                notificationList.add(doc);

                                if (RequestDataHelper.isSACPendingAction(doc)) {
                                    incomingRequestCount++;
                                }

                                if (RequestDataHelper.isSACUnseenNotification(doc)) {
                                    unseenNotificationCount++;
                                }
                            }

                            renderNotifications();
                        });
    }

    private void renderNotifications() {
        if (layoutNotificationList == null || layoutEmptyState == null) {
            return;
        }

        layoutNotificationList.removeAllViews();

        List<DocumentSnapshot> filtered = new ArrayList<>();
        List<DocumentSnapshot> toMarkSeen = new ArrayList<>();

        for (DocumentSnapshot doc : notificationList) {
            String status = getDisplayStatus(doc);

            if ("All".equalsIgnoreCase(selectedFilter)
                    || selectedFilter.equalsIgnoreCase(status)) {
                filtered.add(doc);
                
                if (RequestDataHelper.isSACUnseenNotification(doc)) {
                    toMarkSeen.add(doc);
                }
            }
        }

        if (tvIncomingCount != null) {
            tvIncomingCount.setText(
                    incomingRequestCount + " pending SAC action" +
                            (incomingRequestCount == 1 ? "" : "s") +
                            " • " +
                            filtered.size() + " total notification" +
                            (filtered.size() == 1 ? "" : "s")
            );
        }

        if (filtered.isEmpty()) {
            showEmptyStateForFilter();
            return;
        }

        layoutEmptyState.setVisibility(View.GONE);
        layoutNotificationList.setVisibility(View.VISIBLE);

        for (DocumentSnapshot doc : filtered) {
            layoutNotificationList.addView(createNotificationCard(doc));
        }

        if (!toMarkSeen.isEmpty()) {
            markNotificationsAsSeen(toMarkSeen);
        }
    }

    private void markNotificationsAsSeen(List<DocumentSnapshot> docs) {
        for (DocumentSnapshot doc : docs) {
            db.collection("requests")
                    .document(doc.getId())
                    .update(
                            "sacNotificationSeen", true,
                            "sacSeen", true,
                            "sacNotificationOpenedAt", FieldValue.serverTimestamp(),
                            "updatedAt", FieldValue.serverTimestamp()
                    );
        }
    }

    private void showEmptyStateForFilter() {
        if (layoutNotificationList != null) {
            layoutNotificationList.setVisibility(View.GONE);
        }

        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(View.VISIBLE);
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
        card.setStrokeColor(getStatusMainColor(status));

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

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
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

        LinearLayout.LayoutParams descParams =
                new LinearLayout.LayoutParams(
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

        db.collection("requests")
                .document(requestId)
                .update(
                        "sacSeen", true,
                        "sacNotificationSeen", true,
                        "sacSeenAt", FieldValue.serverTimestamp(),
                        "sacNotificationOpenedAt", FieldValue.serverTimestamp()
                );

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
        String status = getStringValue(doc, "status");
        String sacStatus = getStringValue(doc, "sacStatus");

        if ("Rejected".equalsIgnoreCase(status)
                || "Rejected".equalsIgnoreCase(sacStatus)
                || "Returned".equalsIgnoreCase(status)
                || "Returned".equalsIgnoreCase(sacStatus)) {
            return "Rejected";
        }

        if ("Approved".equalsIgnoreCase(sacStatus)
                || "Approved".equalsIgnoreCase(status)) {
            return "Approved";
        }

        return "Pending";
    }

    private String buildDescriptionText(
            DocumentSnapshot doc,
            String status
    ) {
        String title = getStringValue(doc, "sacNotificationTitle");
        String message = getStringValue(doc, "sacNotificationMessage");

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

        if ("Approved".equalsIgnoreCase(status)) {
            return "This Student Center request has already been approved by SAC.";
        }

        if ("Returned".equalsIgnoreCase(status)) {
            return "This Student Center request has been returned or rejected.";
        }

        if (!nameToShow.isEmpty()) {
            return nameToShow + " submitted a Student Center request that is waiting for SAC review.";
        }

        return "A new Student Center request is waiting for SAC review.";
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
        Timestamp timestamp = doc.getTimestamp("sacNotifiedAt");

        if (timestamp == null) {
            timestamp = doc.getTimestamp("notificationUpdatedAt");
        }

        if (timestamp == null) {
            timestamp = doc.getTimestamp("sacNotificationOpenedAt");
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
        if ("Approved".equalsIgnoreCase(status)) {
            return android.R.drawable.checkbox_on_background;
        }

        if ("Rejected".equalsIgnoreCase(status)) {
            return android.R.drawable.ic_delete;
        }

        return android.R.drawable.ic_dialog_info;
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

    private void showEmptyState() {
        if (layoutNotificationList != null) {
            layoutNotificationList.setVisibility(View.GONE);
        }

        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(View.VISIBLE);
        }

        if (tvIncomingCount != null) {
            tvIncomingCount.setText("0 incoming booking requests • 0 recent notifications");
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