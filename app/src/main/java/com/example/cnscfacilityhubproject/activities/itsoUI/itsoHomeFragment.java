package com.example.cnscfacilityhubproject.activities.itsoUI;

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
import com.example.cnscfacilityhubproject.utils.ItsoReminderHelper;
import com.example.cnscfacilityhubproject.utils.RequestDataHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import java.util.Collections;
import java.util.Comparator;

public class itsoHomeFragment extends Fragment {

    private TextView tvGreeting, tvITSOName;
    private TextView tvPendingCount, tvApprovedCount;
    private TextView tvRecentEmpty;

    private MaterialButton btnViewRequests;

    private LinearLayout itsoPendingReq, itsoApprovedReq, layoutRecentRequests;
    private LinearLayout layoutUpcomingReminders;
    private TextView tvUpcomingEmpty;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration dashboardListener;
    private ListenerRegistration userListener;

    public itsoHomeFragment() {
        super(R.layout.fragment_itso_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews(view);
        setGreetingByTime();
        loadITSOName();
        setupActions();
        listenDashboardData();
    }

    @Override
    public void onResume() {
        super.onResume();
        listenDashboardData();
    }

    @Override
    public void onDestroyView() {
        if (dashboardListener != null) {
            dashboardListener.remove();
            dashboardListener = null;
        }

        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }

        super.onDestroyView();
    }

    private void bindViews(View view) {
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvITSOName = view.findViewById(R.id.tvITSOName);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        tvApprovedCount = view.findViewById(R.id.tvApprovedCount);
        tvRecentEmpty = view.findViewById(R.id.tvRecentEmpty);
        btnViewRequests = view.findViewById(R.id.btnViewRequests);
        itsoPendingReq = view.findViewById(R.id.itsoPendingReq);
        itsoApprovedReq = view.findViewById(R.id.itsoApprovedReq);
        layoutRecentRequests = view.findViewById(R.id.layoutRecentRequests);
        layoutUpcomingReminders = view.findViewById(R.id.layoutUpcomingReminders);
        tvUpcomingEmpty = view.findViewById(R.id.tvUpcomingEmpty);
    }

    private void setGreetingByTime() {
        if (tvGreeting == null) return;

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour < 12) {
            tvGreeting.setText("Good Morning");
        } else if (hour < 18) {
            tvGreeting.setText("Good Afternoon");
        } else {
            tvGreeting.setText("Good Evening");
        }
    }

    private void loadITSOName() {
        if (tvITSOName == null) return;

        setHelloName("");

        if (auth == null || auth.getCurrentUser() == null || db == null) {
            return;
        }

        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }

        userListener = db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (!isAdded() || tvITSOName == null) return;

                    if (error != null || documentSnapshot == null || !documentSnapshot.exists()) {
                        setHelloName("");
                        return;
                    }

                    setHelloName(getStringValue(documentSnapshot, "fullName"));
                });
    }

    private void setHelloName(String fullName) {
        String cleanName = cleanText(fullName);

        if (tvITSOName == null) return;

        if (cleanName.isEmpty()) {
            tvITSOName.setText("Hello,");
        } else {
            tvITSOName.setText("Hello, " + cleanName);
        }

        tvITSOName.setVisibility(View.VISIBLE);
    }

    private void setupActions() {
        if (btnViewRequests != null) {
            btnViewRequests.setOnClickListener(v -> openNotificationList("All"));
        }

        if (itsoPendingReq != null) {
            itsoPendingReq.setOnClickListener(v -> openNotificationList("Pending"));
        }

        if (itsoApprovedReq != null) {
            itsoApprovedReq.setOnClickListener(v -> openNotificationList("Approved - Available"));
        }
    }

    private void listenDashboardData() {
        if (db == null) return;

        if (dashboardListener != null) {
            dashboardListener.remove();
            dashboardListener = null;
        }

        dashboardListener = db.collection("requests")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (!isAdded()) return;

                    if (error != null || queryDocumentSnapshots == null) {
                        if (tvPendingCount != null) tvPendingCount.setText("");
                        if (tvApprovedCount != null) tvApprovedCount.setText("");

                        if (layoutRecentRequests != null) layoutRecentRequests.removeAllViews();

                        if (tvRecentEmpty != null) {
                            tvRecentEmpty.setVisibility(View.VISIBLE);
                            tvRecentEmpty.setText("Failed to load recent requests.");
                        }

                        Toast.makeText(requireContext(), "Failed to load ITSO requests.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<DocumentSnapshot> todayRecentRequests = new ArrayList<>();
                    List<DocumentSnapshot> upcomingReminders = new ArrayList<>();

                    int pending = 0;
                    int approvedAvailable = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        if (!isITSORequest(doc)) continue;
                        if (!RequestDataHelper.shouldShowInRequestList(doc)) continue;

                        String displayStatus = getDisplayStatus(doc);

                        if ("Pending".equalsIgnoreCase(displayStatus)) {
                            pending++;
                        } else if ("Approved - Available".equalsIgnoreCase(displayStatus)) {
                            approvedAvailable++;
                        }

                        if (ItsoReminderHelper.isUpcomingTechnicalEvent(doc)) {
                            upcomingReminders.add(doc);
                        }

                        if (isRequestWithinToday(doc)) {
                            todayRecentRequests.add(doc);
                        }
                    }

                    Collections.sort(todayRecentRequests, new Comparator<DocumentSnapshot>() {
                        @Override
                        public int compare(DocumentSnapshot doc1, DocumentSnapshot doc2) {
                            return Long.compare(getSortTime(doc2), getSortTime(doc1));
                        }
                    });

                    if (tvPendingCount != null) tvPendingCount.setText(formatCount(pending));
                    if (tvApprovedCount != null) tvApprovedCount.setText(formatCount(approvedAvailable));

                    renderUpcomingReminders(upcomingReminders);
                    renderRecentRequests(todayRecentRequests);
                });
    }

    private void renderUpcomingReminders(List<DocumentSnapshot> upcoming) {
        if (layoutUpcomingReminders == null || tvUpcomingEmpty == null) {
            return;
        }

        layoutUpcomingReminders.removeAllViews();

        if (upcoming.isEmpty()) {
            tvUpcomingEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvUpcomingEmpty.setVisibility(View.GONE);

        for (DocumentSnapshot doc : upcoming) {
            layoutUpcomingReminders.addView(createUpcomingReminderCard(doc));
        }
    }

    private View createUpcomingReminderCard(DocumentSnapshot doc) {
        String requestId = doc.getId();
        String purpose = getStringValue(doc, "purpose");

        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(requireContext().getColor(R.color.cnsc_warning_light));
        card.setRadius(dp(22));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(requireContext().getColor(R.color.cnsc_warning));

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView title = new TextView(requireContext());
        title.setText(purpose.isEmpty() ? "Tomorrow:" : "Tomorrow: " + purpose);
        title.setTextColor(requireContext().getColor(R.color.cnsc_text_primary));
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView summary = new TextView(requireContext());
        summary.setText(ItsoReminderHelper.buildReminderSummary(doc));
        summary.setTextColor(requireContext().getColor(R.color.cnsc_text_secondary));
        summary.setTextSize(13f);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        summaryParams.topMargin = dp(8);
        summary.setLayoutParams(summaryParams);

        MaterialButton btnView = new MaterialButton(requireContext());
        btnView.setText("View Details");
        btnView.setAllCaps(false);
        btnView.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.cnsc_primary)));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
        );
        btnParams.topMargin = dp(12);
        btnView.setLayoutParams(btnParams);
        btnView.setOnClickListener(v -> openViewDetails(requestId));

        container.addView(title);
        container.addView(summary);
        container.addView(btnView);
        card.addView(container);

        return card;
    }

    private void renderRecentRequests(List<DocumentSnapshot> recentRequests) {
        if (layoutRecentRequests == null || tvRecentEmpty == null) return;

        layoutRecentRequests.removeAllViews();

        if (recentRequests.isEmpty()) {
            tvRecentEmpty.setVisibility(View.VISIBLE);
            tvRecentEmpty.setText("No recent ITSO requests today.");
            return;
        }

        tvRecentEmpty.setVisibility(View.GONE);

        for (DocumentSnapshot doc : recentRequests) {
            layoutRecentRequests.addView(createRecentRequestCard(doc));
        }
    }

    private View createRecentRequestCard(DocumentSnapshot doc) {
        String requestId = doc.getId();

        String purpose = getStringValue(doc, "purpose");
        String facility = getFinalFacility(doc);
        String startDateText = getStringValue(doc, "startDateText");
        String endDateText = getStringValue(doc, "endDateText");
        String timeStart = getStringValue(doc, "timeStartText");
        String timeEnd = getStringValue(doc, "timeEndText");
        String displayStatus = getDisplayStatus(doc);

        MaterialCardView card = new MaterialCardView(requireContext());

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(12));

        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(26));
        card.setCardElevation(dp(8));
        card.setStrokeColor(Color.parseColor("#313131"));
        card.setStrokeWidth(dp(1));

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(18));

        LinearLayout headerRow = new LinearLayout(requireContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        MaterialCardView iconCard = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(46), dp(46));
        iconCard.setLayoutParams(iconParams);
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

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        titleParams.setMargins(dp(12), 0, dp(8), 0);
        titleLayout.setLayoutParams(titleParams);

        TextView tvTitle = new TextView(requireContext());
        setTextOrHide(tvTitle, purpose);
        tvTitle.setTextColor(Color.parseColor("#313131"));
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvMeta = new TextView(requireContext());
        setTextOrHide(tvMeta, buildMetaText(facility, startDateText, endDateText, timeStart, timeEnd));
        tvMeta.setTextColor(Color.parseColor("#313131"));
        tvMeta.setTextSize(12f);
        tvMeta.setAlpha(0.68f);

        titleLayout.addView(tvTitle);
        titleLayout.addView(tvMeta);

        Chip chipStatus = new Chip(requireContext());
        setTextOrHide(chipStatus, displayStatus);
        chipStatus.setTextColor(getStatusMainColor(displayStatus));
        chipStatus.setChipBackgroundColor(ColorStateList.valueOf(getStatusLightColor(displayStatus)));
        chipStatus.setChipStrokeWidth(0);
        chipStatus.setCheckable(false);
        chipStatus.setClickable(false);

        headerRow.addView(iconCard);
        headerRow.addView(titleLayout);
        if (!cleanText(displayStatus).isEmpty()) {
            headerRow.addView(chipStatus);
        }

        TextView tvDescription = new TextView(requireContext());
        setTextOrHide(tvDescription, buildTechnicalSummary(doc));
        tvDescription.setTextColor(Color.parseColor("#313131"));
        tvDescription.setTextSize(14f);
        tvDescription.setLineSpacing(3f, 1f);

        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
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
        btnViewRequest.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#313131")));
        btnViewRequest.setCornerRadius(dp(16));
        btnViewRequest.setElevation(0);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
        );
        buttonParams.setMargins(0, dp(14), 0, 0);
        btnViewRequest.setLayoutParams(buttonParams);

        btnViewRequest.setOnClickListener(v -> openViewDetails(requestId));

        container.addView(headerRow);
        if (tvDescription.getVisibility() == View.VISIBLE) {
            container.addView(tvDescription);
        }
        container.addView(btnViewRequest);

        card.addView(container);

        return card;
    }

    private boolean isRequestWithinToday(DocumentSnapshot doc) {
        Timestamp createdAt = doc.getTimestamp("createdAt");
        Calendar today = Calendar.getInstance();

        if (createdAt != null) {
            Calendar createdCalendar = Calendar.getInstance();
            createdCalendar.setTime(createdAt.toDate());
            return isSameDate(createdCalendar, today);
        }

        Calendar startDateCalendar = parseDateTextToCalendar(getStringValue(doc, "startDateText"));
        return startDateCalendar != null && isSameDate(startDateCalendar, today);
    }

    private long getSortTime(DocumentSnapshot doc) {
        Timestamp createdAt = doc.getTimestamp("createdAt");

        if (createdAt != null) {
            return createdAt.toDate().getTime();
        }

        Calendar calendar = parseDateTextToCalendar(getStringValue(doc, "startDateText"));
        return calendar != null ? calendar.getTimeInMillis() : 0;
    }

    private Calendar parseDateTextToCalendar(String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) return null;

        String[] patterns = {
                "MMMM dd, yyyy",
                "MMM dd, yyyy",
                "yyyy-MM-dd",
                "MM/dd/yyyy",
                "dd/MM/yyyy"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
                sdf.setLenient(false);

                java.util.Date parsedDate = sdf.parse(dateText.trim());

                if (parsedDate != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(parsedDate);
                    return calendar;
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private boolean isSameDate(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.MONTH) == second.get(Calendar.MONTH)
                && first.get(Calendar.DAY_OF_MONTH) == second.get(Calendar.DAY_OF_MONTH);
    }

    private boolean isITSORequest(DocumentSnapshot doc) {
        if (!hasTechnicalRequest(doc)) return false;

        Boolean sendToITSO = doc.getBoolean("sendToITSO");
        if (Boolean.TRUE.equals(sendToITSO)) return true;

        String notificationTarget = getStringValue(doc, "notificationTarget");
        if ("ITSO".equalsIgnoreCase(notificationTarget)) return true;

        String workflowStage = getStringValue(doc, "workflowStage");
        if ("ITSO_REVIEW".equalsIgnoreCase(workflowStage)
                || "GSO_REVIEW".equalsIgnoreCase(workflowStage)) {
            return true;
        }

        String itsoAvailability = getStringValue(doc, "itsoAvailability");
        String itsoStatus = getStringValue(doc, "itsoStatus");

        return "Available".equalsIgnoreCase(itsoAvailability)
                || "Not Available".equalsIgnoreCase(itsoAvailability)
                || "Unavailable".equalsIgnoreCase(itsoAvailability)
                || "Approved".equalsIgnoreCase(itsoStatus)
                || "Available".equalsIgnoreCase(itsoStatus)
                || "Not Available".equalsIgnoreCase(itsoStatus)
                || "Unavailable".equalsIgnoreCase(itsoStatus)
                || "Rejected".equalsIgnoreCase(itsoStatus);
    }

    private boolean hasTechnicalRequest(DocumentSnapshot doc) {
        return Boolean.TRUE.equals(doc.getBoolean("soundSystemSetup"))
                || Boolean.TRUE.equals(doc.getBoolean("microphones"))
                || Boolean.TRUE.equals(doc.getBoolean("portableSpeaker"))
                || Boolean.TRUE.equals(doc.getBoolean("lights"))
                || Boolean.TRUE.equals(doc.getBoolean("livestreamingServices"))
                || Boolean.TRUE.equals(doc.getBoolean("zoomHosting"))
                || Boolean.TRUE.equals(doc.getBoolean("gmeetHosting"))
                || Boolean.TRUE.equals(doc.getBoolean("webCamera"))
                || Boolean.TRUE.equals(doc.getBoolean("tripod"))
                || Boolean.TRUE.equals(doc.getBoolean("multimediaProjector"))
                || !getStringValue(doc, "connectors").isEmpty();
    }

    private String getDisplayStatus(DocumentSnapshot doc) {
        String itsoAvailability = getStringValue(doc, "itsoAvailability");
        String itsoStatus = getStringValue(doc, "itsoStatus");
        String status = getStringValue(doc, "status");
        String notificationTarget = getStringValue(doc, "notificationTarget");
        Boolean sendToITSO = doc.getBoolean("sendToITSO");

        if ("Not Available".equalsIgnoreCase(itsoAvailability)
                || "Unavailable".equalsIgnoreCase(itsoAvailability)
                || "Not Available".equalsIgnoreCase(itsoStatus)
                || "Unavailable".equalsIgnoreCase(itsoStatus)
                || "Rejected".equalsIgnoreCase(itsoStatus)
                || "Not Available".equalsIgnoreCase(status)
                || "Unavailable".equalsIgnoreCase(status)
                || "Returned".equalsIgnoreCase(status)) {
            return "Not Available";
        }

        if ("Available".equalsIgnoreCase(itsoAvailability)
                || "Approved".equalsIgnoreCase(itsoStatus)
                || "Available".equalsIgnoreCase(itsoStatus)
                || "Approved - Available".equalsIgnoreCase(status)) {
            return "Approved - Available";
        }

        if ("Pending".equalsIgnoreCase(itsoStatus)
                || "Pending".equalsIgnoreCase(status)
                || Boolean.TRUE.equals(sendToITSO)
                || "ITSO".equalsIgnoreCase(notificationTarget)) {
            return "Pending";
        }

        return firstNonEmpty(itsoAvailability, firstNonEmpty(itsoStatus, status));
    }

    private String buildTechnicalSummary(DocumentSnapshot doc) {
        List<String> selected = new ArrayList<>();

        if (Boolean.TRUE.equals(doc.getBoolean("soundSystemSetup"))) selected.add("Sound system");
        if (Boolean.TRUE.equals(doc.getBoolean("microphones"))) selected.add("Microphones");
        if (Boolean.TRUE.equals(doc.getBoolean("portableSpeaker"))) selected.add("Portable speaker");
        if (Boolean.TRUE.equals(doc.getBoolean("lights"))) selected.add("Lights");
        if (Boolean.TRUE.equals(doc.getBoolean("livestreamingServices"))) selected.add("Livestreaming");
        if (Boolean.TRUE.equals(doc.getBoolean("zoomHosting"))) selected.add("Zoom hosting");
        if (Boolean.TRUE.equals(doc.getBoolean("gmeetHosting"))) selected.add("GMeet hosting");
        if (Boolean.TRUE.equals(doc.getBoolean("webCamera"))) selected.add("Web camera");
        if (Boolean.TRUE.equals(doc.getBoolean("tripod"))) selected.add("Tripod");
        if (Boolean.TRUE.equals(doc.getBoolean("multimediaProjector"))) selected.add("Projector");

        String connectors = getStringValue(doc, "connectors");

        if (!connectors.isEmpty()) {
            selected.add("Connectors: " + connectors);
        }

        if (selected.isEmpty()) return "";

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < selected.size(); i++) {
            builder.append(selected.get(i));
            if (i < selected.size() - 1) builder.append(", ");
        }

        return builder.toString();
    }

    private String buildMetaText(String facility, String startDate, String endDate, String startTime, String endTime) {
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

        return builder.toString();
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
        if ("Approved - Available".equalsIgnoreCase(status)) {
            return android.R.drawable.checkbox_on_background;
        }

        if ("Not Available".equalsIgnoreCase(status)) {
            return android.R.drawable.ic_delete;
        }

        return android.R.drawable.ic_menu_recent_history;
    }

    private int getStatusMainColor(String status) {
        if ("Approved - Available".equalsIgnoreCase(status)) {
            return Color.parseColor("#2E7D32");
        }

        if ("Not Available".equalsIgnoreCase(status)) {
            return Color.parseColor("#970705");
        }

        return Color.parseColor("#313131");
    }

    private int getStatusLightColor(String status) {
        if ("Approved - Available".equalsIgnoreCase(status)) {
            return Color.parseColor("#E7F4E8");
        }

        if ("Not Available".equalsIgnoreCase(status)) {
            return Color.parseColor("#F3D9D9");
        }

        return Color.parseColor("#EEEEEE");
    }

    private void openNotificationList(String filter) {
        itsoNotificationFragment fragment = new itsoNotificationFragment();

        Bundle bundle = new Bundle();
        bundle.putString("filter", filter);
        fragment.setArguments(bundle);

        highlightNotificationTab();

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.itso_fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void highlightNotificationTab() {
        int activeColor = Color.parseColor("#970705");
        int inactiveColor = Color.parseColor("#313131");

        ImageView iconHome = requireActivity().findViewById(R.id.iconHome);
        ImageView iconRequests = requireActivity().findViewById(R.id.iconRequests);
        ImageView iconNotifications = requireActivity().findViewById(R.id.iconNotifications);
        ImageView iconProfile = requireActivity().findViewById(R.id.iconProfile);

        TextView textHome = requireActivity().findViewById(R.id.itsotextHome);
        TextView textNotifications = requireActivity().findViewById(R.id.itsotextNotifications);
        TextView textProfile = requireActivity().findViewById(R.id.itsotextProfile);

        if (iconHome != null) iconHome.setColorFilter(inactiveColor);
        if (iconRequests != null) iconRequests.setColorFilter(inactiveColor);
        if (iconNotifications != null) iconNotifications.setColorFilter(activeColor);
        if (iconProfile != null) iconProfile.setColorFilter(inactiveColor);

        if (textHome != null) textHome.setTextColor(inactiveColor);
        if (textNotifications != null) textNotifications.setTextColor(activeColor);
        if (textProfile != null) textProfile.setTextColor(inactiveColor);
    }

    private void openViewDetails(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Request ID not found.", Toast.LENGTH_SHORT).show();
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

    private void setTextOrHide(TextView textView, String value) {
        if (textView == null) return;

        String cleanValue = cleanText(value);
        textView.setText(cleanValue);
        textView.setVisibility(cleanValue.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String cleanText(String value) {
        return value != null ? value.trim() : "";
    }

    private String firstNonEmpty(String first, String second) {
        String cleanFirst = cleanText(first);
        if (!cleanFirst.isEmpty()) return cleanFirst;

        return cleanText(second);
    }

    private String getStringValue(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value != null ? String.valueOf(value).trim() : "";
    }

    private String formatCount(int count) {
        return count < 10 ? "0" + count : String.valueOf(count);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}