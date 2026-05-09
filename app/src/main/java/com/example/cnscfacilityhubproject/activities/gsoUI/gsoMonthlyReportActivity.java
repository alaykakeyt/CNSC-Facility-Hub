package com.example.cnscfacilityhubproject.activities.gsoUI;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cnscfacilityhubproject.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class gsoMonthlyReportActivity extends AppCompatActivity {

    private ImageView ivBack;

    private TextInputEditText etStartFilterDate;
    private TextInputEditText etEndFilterDate;
 //   private MaterialButton btnClearDateFilter;

  //  private TextView tvReportMonth;
    private TextView tvTotalRequests;
    private TextView tvApprovedRequests;
    private TextView tvPendingRequests;
    private TextView tvReturnedRequests;

    private TextView tvApprovalRate;
    private TextView tvTopFacility;
    private TextView tvTopRequestor;
    private TextView tvNoRequests;

    private ProgressBar progressApproved;
    private ProgressBar progressPending;
    private ProgressBar progressReturned;

    private LinearLayout layoutMonthlyRequestList;
    private MaterialButton btnShareReport;

    private FirebaseFirestore db;
    private ListenerRegistration requestsListener;

    private Calendar startFilterCalendar;
    private Calendar endFilterCalendar;

    private int totalRequests = 0;
    private int approvedRequests = 0;
    private int pendingRequests = 0;
    private int returnedRequests = 0;

    private String topFacility = "No facility data";
    private int topFacilityUses = 0;

    private String topRequestor = "No requestor data";
    private int topRequestorRequests = 0;

    private String reportText = "";

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

    private final SimpleDateFormat reportRangeFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gso_monthly_report);

        db = FirebaseFirestore.getInstance();

        setupDefaultDateRange();
        bindViews();
        setupActions();
        //updateReportRangeText();
        listenForRequests();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }
    }

    private void setupDefaultDateRange() {
        startFilterCalendar = Calendar.getInstance();
        startFilterCalendar.set(Calendar.DAY_OF_MONTH, 1);
        startOfDay(startFilterCalendar);

        endFilterCalendar = Calendar.getInstance();
        endFilterCalendar.set(Calendar.DAY_OF_MONTH, endFilterCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        endOfDay(endFilterCalendar);
    }

    private void bindViews() {
        ivBack = findViewById(R.id.ivBack);

        etStartFilterDate = findViewById(R.id.etStartFilterDate);
        etEndFilterDate = findViewById(R.id.etEndFilterDate);
//        btnClearDateFilter = findViewById(R.id.btnClearDateFilter);
//
//        tvReportMonth = findViewById(R.id.tvReportMonth);
        tvTotalRequests = findViewById(R.id.tvTotalRequests);
        tvApprovedRequests = findViewById(R.id.tvApprovedRequests);
        tvPendingRequests = findViewById(R.id.tvPendingRequests);
        tvReturnedRequests = findViewById(R.id.tvReturnedRequests);

        tvApprovalRate = findViewById(R.id.tvApprovalRate);
        tvTopFacility = findViewById(R.id.tvTopFacility);
        tvTopRequestor = findViewById(R.id.tvTopRequestor);
        tvNoRequests = findViewById(R.id.tvNoRequests);

        progressApproved = findViewById(R.id.progressApproved);
        progressPending = findViewById(R.id.progressPending);
        progressReturned = findViewById(R.id.progressReturned);

        layoutMonthlyRequestList = findViewById(R.id.layoutMonthlyRequestList);
        btnShareReport = findViewById(R.id.btnShareReport);

        etStartFilterDate.setText(displayDateFormat.format(startFilterCalendar.getTime()));
        etEndFilterDate.setText(displayDateFormat.format(endFilterCalendar.getTime()));
    }

    private void setupActions() {
        ivBack.setOnClickListener(v -> finish());

        etStartFilterDate.setOnClickListener(v -> showStartDatePicker());
        etEndFilterDate.setOnClickListener(v -> showEndDatePicker());

//        btnClearDateFilter.setOnClickListener(v -> {
//            setupDefaultDateRange();
//            etStartFilterDate.setText(displayDateFormat.format(startFilterCalendar.getTime()));
//            etEndFilterDate.setText(displayDateFormat.format(endFilterCalendar.getTime()));
//            updateReportRangeText();
//            refreshReport();
//        });

        btnShareReport.setOnClickListener(v -> shareReport());
    }

    private void showStartDatePicker() {
        Calendar pickerCalendar = startFilterCalendar != null
                ? (Calendar) startFilterCalendar.clone()
                : Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    startFilterCalendar = Calendar.getInstance();
                    startFilterCalendar.set(year, month, dayOfMonth);
                    startOfDay(startFilterCalendar);

                    if (endFilterCalendar != null && startFilterCalendar.after(endFilterCalendar)) {
                        endFilterCalendar = (Calendar) startFilterCalendar.clone();
                        endOfDay(endFilterCalendar);
                        etEndFilterDate.setText(displayDateFormat.format(endFilterCalendar.getTime()));
                    }

                    etStartFilterDate.setText(displayDateFormat.format(startFilterCalendar.getTime()));
                   // updateReportRangeText();
                    refreshReport();
                },
                pickerCalendar.get(Calendar.YEAR),
                pickerCalendar.get(Calendar.MONTH),
                pickerCalendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void showEndDatePicker() {
        Calendar pickerCalendar = endFilterCalendar != null
                ? (Calendar) endFilterCalendar.clone()
                : Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    endFilterCalendar = Calendar.getInstance();
                    endFilterCalendar.set(year, month, dayOfMonth);
                    endOfDay(endFilterCalendar);

                    if (startFilterCalendar != null && endFilterCalendar.before(startFilterCalendar)) {
                        Toast.makeText(this, "End date cannot be earlier than start date.", Toast.LENGTH_SHORT).show();

                        endFilterCalendar = (Calendar) startFilterCalendar.clone();
                        endOfDay(endFilterCalendar);
                    }

                    etEndFilterDate.setText(displayDateFormat.format(endFilterCalendar.getTime()));
                   // updateReportRangeText();
                    refreshReport();
                },
                pickerCalendar.get(Calendar.YEAR),
                pickerCalendar.get(Calendar.MONTH),
                pickerCalendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

//    private void updateReportRangeText() {
//        if (startFilterCalendar == null || endFilterCalendar == null) {
//            tvReportMonth.setText("Selected Date Range");
//            return;
//        }
//
//        String start = reportRangeFormat.format(startFilterCalendar.getTime());
//        String end = reportRangeFormat.format(endFilterCalendar.getTime());
//
//        if (isSameDate(startFilterCalendar, endFilterCalendar)) {
//            tvReportMonth.setText(start);
//        } else {
//            tvReportMonth.setText(start + " - " + end);
//        }
//    }

    private void listenForRequests() {
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }

        requestsListener = db.collection("requests")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        Toast.makeText(this, "Failed to load report.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    totalRequests = 0;
                    approvedRequests = 0;
                    pendingRequests = 0;
                    returnedRequests = 0;

                    topFacility = "No facility data";
                    topFacilityUses = 0;

                    topRequestor = "No requestor data";
                    topRequestorRequests = 0;

                    Map<String, Integer> facilityCounts = new HashMap<>();
                    Map<String, Integer> requestorCounts = new HashMap<>();

                    layoutMonthlyRequestList.removeAllViews();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        if (!isGSORequest(doc)) continue;

                        Calendar requestDate = getRequestCalendar(doc);
                        if (requestDate == null) continue;

                        if (!isWithinSelectedRange(requestDate)) continue;

                        totalRequests++;

                        String status = getDisplayStatus(doc);

                        if ("Approved".equalsIgnoreCase(status)) {
                            approvedRequests++;
                        } else if ("Returned".equalsIgnoreCase(status)
                                || "Return".equalsIgnoreCase(status)
                                || "Not Available".equalsIgnoreCase(status)) {
                            returnedRequests++;
                        } else {
                            pendingRequests++;
                        }

                        if (isApprovedRequest(doc)) {
                            String facility = getFinalFacility(doc);

                            if (!facility.isEmpty()) {
                                int current = facilityCounts.containsKey(facility)
                                        ? facilityCounts.get(facility)
                                        : 0;

                                facilityCounts.put(facility, current + 1);
                            }
                        }

                        String requestorName = firstNonEmpty(
                                getStringValue(doc, "requestorName"),
                                getStringValue(doc, "fullName")
                        );

                        if (!requestorName.isEmpty()) {
                            int current = requestorCounts.containsKey(requestorName)
                                    ? requestorCounts.get(requestorName)
                                    : 0;

                            requestorCounts.put(requestorName, current + 1);
                        }

                        layoutMonthlyRequestList.addView(createRequestCard(doc));
                    }

                    updateTopFacility(facilityCounts);
                    updateTopRequestor(requestorCounts);
                    updateSummary();
                    buildReportText();

                    tvNoRequests.setVisibility(totalRequests == 0 ? View.VISIBLE : View.GONE);
                });
    }

    private void refreshReport() {
        listenForRequests();
    }

    private boolean isWithinSelectedRange(Calendar requestDate) {
        if (requestDate == null || startFilterCalendar == null || endFilterCalendar == null) {
            return false;
        }

        Calendar normalizedRequestDate = (Calendar) requestDate.clone();
        startOfDay(normalizedRequestDate);

        return !normalizedRequestDate.before(startFilterCalendar)
                && !normalizedRequestDate.after(endFilterCalendar);
    }

    private void updateSummary() {
        tvTotalRequests.setText(formatCount(totalRequests));
        tvApprovedRequests.setText(formatCount(approvedRequests));
        tvPendingRequests.setText(formatCount(pendingRequests));
        tvReturnedRequests.setText(formatCount(returnedRequests));

        int approvalRate = 0;

        if (totalRequests > 0) {
            approvalRate = Math.round((approvedRequests * 100f) / totalRequests);
        }

        tvApprovalRate.setText(approvalRate + "% approved");

        tvTopFacility.setText(topFacilityUses > 0
                ? topFacility + " • " + topFacilityUses + " use(s)"
                : "No approved facility usage yet");

        tvTopRequestor.setText(topRequestorRequests > 0
                ? topRequestor + " • " + topRequestorRequests + " request(s)"
                : "No requestor data yet");

        progressApproved.setMax(Math.max(totalRequests, 1));
        progressApproved.setProgress(approvedRequests);

        progressPending.setMax(Math.max(totalRequests, 1));
        progressPending.setProgress(pendingRequests);

        progressReturned.setMax(Math.max(totalRequests, 1));
        progressReturned.setProgress(returnedRequests);
    }

    private void updateTopFacility(Map<String, Integer> facilityCounts) {
        topFacility = "No facility data";
        topFacilityUses = 0;

        for (Map.Entry<String, Integer> entry : facilityCounts.entrySet()) {
            if (entry.getValue() > topFacilityUses) {
                topFacility = entry.getKey();
                topFacilityUses = entry.getValue();
            }
        }
    }

    private void updateTopRequestor(Map<String, Integer> requestorCounts) {
        topRequestor = "No requestor data";
        topRequestorRequests = 0;

        for (Map.Entry<String, Integer> entry : requestorCounts.entrySet()) {
            if (entry.getValue() > topRequestorRequests) {
                topRequestor = entry.getKey();
                topRequestorRequests = entry.getValue();
            }
        }
    }

    private MaterialCardView createRequestCard(DocumentSnapshot doc) {
        String purpose = getStringValue(doc, "purpose");
        String facility = getFinalFacility(doc);
        String status = getDisplayStatus(doc);
        String requestorName = firstNonEmpty(
                getStringValue(doc, "requestorName"),
                getStringValue(doc, "fullName")
        );

        String startDate = getStringValue(doc, "startDateText");
        String endDate = getStringValue(doc, "endDateText");
        String timeStart = getStringValue(doc, "timeStartText");
        String timeEnd = getStringValue(doc, "timeEndText");

        if (purpose.isEmpty()) purpose = "Purpose / Activity";
        if (facility.isEmpty()) facility = "Facility";
        if (requestorName.isEmpty()) requestorName = "Unknown requestor";

        MaterialCardView card = new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(12));

        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(24));
        card.setCardElevation(dp(6));
        card.setStrokeColor(Color.parseColor("#313131"));
        card.setStrokeWidth(dp(1));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        MaterialCardView iconCard = new MaterialCardView(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(46), dp(46));
        iconCard.setLayoutParams(iconParams);
        iconCard.setRadius(dp(15));
        iconCard.setCardElevation(0);
        iconCard.setCardBackgroundColor(getStatusColor(status));

        TextView tvIcon = new TextView(this);
        tvIcon.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        tvIcon.setText(getStatusInitial(status));
        tvIcon.setGravity(Gravity.CENTER);
        tvIcon.setTextColor(Color.WHITE);
        tvIcon.setTextSize(16f);
        tvIcon.setTypeface(null, android.graphics.Typeface.BOLD);

        iconCard.addView(tvIcon);

        LinearLayout titleLayout = new LinearLayout(this);
        titleLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        titleParams.setMargins(dp(12), 0, dp(8), 0);
        titleLayout.setLayoutParams(titleParams);

        TextView tvPurpose = new TextView(this);
        tvPurpose.setText(purpose);
        tvPurpose.setTextColor(Color.parseColor("#313131"));
        tvPurpose.setTextSize(15f);
        tvPurpose.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvFacility = new TextView(this);
        tvFacility.setText(facility);
        tvFacility.setTextColor(Color.parseColor("#313131"));
        tvFacility.setTextSize(12f);
        tvFacility.setAlpha(0.70f);

        titleLayout.addView(tvPurpose);
        titleLayout.addView(tvFacility);

        Chip chipStatus = new Chip(this);
        chipStatus.setText(status);
        chipStatus.setTextColor(getStatusColor(status));
        chipStatus.setChipBackgroundColor(ColorStateList.valueOf(getStatusBackgroundColor(status)));
        chipStatus.setCheckable(false);
        chipStatus.setClickable(false);

        topRow.addView(iconCard);
        topRow.addView(titleLayout);
        topRow.addView(chipStatus);

        TextView tvDetails = new TextView(this);
        tvDetails.setText(
                "Requestor: " + requestorName + "\n"
                        + "Date: " + buildDateRange(startDate, endDate) + "\n"
                        + "Time: " + buildTimeRange(timeStart, timeEnd)
        );
        tvDetails.setTextColor(Color.parseColor("#313131"));
        tvDetails.setTextSize(13f);
        tvDetails.setLineSpacing(2f, 1f);
        tvDetails.setAlpha(0.78f);

        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        detailsParams.setMargins(0, dp(12), 0, 0);
        tvDetails.setLayoutParams(detailsParams);

        container.addView(topRow);
        container.addView(tvDetails);

        card.addView(container);

        return card;
    }

    private void buildReportText() {
        reportText =
                "CNSC Facility Hub\n"
                        + "GSO Booking Report\n"
                        //+ "Date Range: " + tvReportMonth.getText().toString() + "\n\n"
                        + "Total Requests: " + totalRequests + "\n"
                        + "Approved Requests: " + approvedRequests + "\n"
                        + "Pending Requests: " + pendingRequests + "\n"
                        + "Returned Requests: " + returnedRequests + "\n\n"
                        + "Approval Rate: " + tvApprovalRate.getText().toString() + "\n"
                        + "Top Facility: " + topFacility + " (" + topFacilityUses + " use/s)\n"
                        + "Top Requestor: " + topRequestor + " (" + topRequestorRequests + " request/s)\n";
    }

    private void shareReport() {
        if (reportText == null || reportText.trim().isEmpty()) {
            buildReportText();
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "GSO Booking Report");
        intent.putExtra(Intent.EXTRA_TEXT, reportText);

        try {
            startActivity(Intent.createChooser(intent, "Share Report"));
        } catch (Exception e) {
            Toast.makeText(this, "No app available to share report.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isGSORequest(DocumentSnapshot doc) {
        String notificationTarget = getStringValue(doc, "notificationTarget");

        if ("GSO".equalsIgnoreCase(notificationTarget)) return true;

        Boolean sendToGSO = doc.getBoolean("sendToGSO");
        if (Boolean.TRUE.equals(sendToGSO)) return true;

        Boolean needsGSO = doc.getBoolean("needsGSO");
        if (Boolean.TRUE.equals(needsGSO)) return true;

        String facility = getStringValue(doc, "facility");
        String otherFacility = getStringValue(doc, "otherFacility");
        String finalFacilityName = getStringValue(doc, "finalFacilityName");

        boolean hasFacility =
                !facility.isEmpty()
                        || !otherFacility.isEmpty()
                        || !finalFacilityName.isEmpty();

        boolean explicitlyITSO = "ITSO".equalsIgnoreCase(notificationTarget);

        return hasFacility && !explicitlyITSO;
    }

    private boolean isApprovedRequest(DocumentSnapshot doc) {
        String status = getStringValue(doc, "status");
        String gsoStatus = getStringValue(doc, "gsoStatus");
        String gsoAvailability = getStringValue(doc, "gsoAvailability");
        String bookingStatus = getStringValue(doc, "bookingStatus");
        String workflowStage = getStringValue(doc, "workflowStage");

        return "Approved".equalsIgnoreCase(status)
                || "Approved".equalsIgnoreCase(gsoStatus)
                || "Available".equalsIgnoreCase(gsoStatus)
                || "Available".equalsIgnoreCase(gsoAvailability)
                || "Booked".equalsIgnoreCase(bookingStatus)
                || "APPROVED".equalsIgnoreCase(workflowStage);
    }

    private String getDisplayStatus(DocumentSnapshot doc) {
        if (isApprovedRequest(doc)) return "Approved";

        String status = getStringValue(doc, "status");

        if (status.isEmpty()) {
            return "Pending";
        }

        return status;
    }

    private int getStatusColor(String status) {
        if ("Approved".equalsIgnoreCase(status)) {
            return Color.parseColor("#2E7D32");
        }

        if ("Returned".equalsIgnoreCase(status)
                || "Return".equalsIgnoreCase(status)
                || "Not Available".equalsIgnoreCase(status)) {
            return Color.parseColor("#970705");
        }

        return Color.parseColor("#313131");
    }

    private int getStatusBackgroundColor(String status) {
        if ("Approved".equalsIgnoreCase(status)) {
            return Color.parseColor("#E7F4E8");
        }

        if ("Returned".equalsIgnoreCase(status)
                || "Return".equalsIgnoreCase(status)
                || "Not Available".equalsIgnoreCase(status)) {
            return Color.parseColor("#F3D9D9");
        }

        return Color.parseColor("#EEEEEE");
    }

    private String getStatusInitial(String status) {
        if ("Approved".equalsIgnoreCase(status)) return "A";

        if ("Returned".equalsIgnoreCase(status)
                || "Return".equalsIgnoreCase(status)
                || "Not Available".equalsIgnoreCase(status)) {
            return "R";
        }

        return "P";
    }

    private Calendar getRequestCalendar(DocumentSnapshot doc) {
        Timestamp startDate = doc.getTimestamp("startDate");

        if (startDate != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate.toDate());
            return calendar;
        }

        String startDateText = getStringValue(doc, "startDateText");

        if (!startDateText.isEmpty()) {
            return parseDateText(startDateText);
        }

        String dateText = getStringValue(doc, "date");

        if (!dateText.isEmpty()) {
            return parseDateText(dateText);
        }

        return null;
    }

    private Calendar parseDateText(String dateText) {
        String[] patterns = {
                "MMMM dd, yyyy",
                "MMM dd, yyyy",
                "yyyy-MM-dd",
                "MM/dd/yyyy",
                "dd/MM/yyyy"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat(pattern, Locale.getDefault());
                parser.setLenient(false);

                java.util.Date parsedDate = parser.parse(dateText.trim());

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

    private String buildDateRange(String startDate, String endDate) {
        if (startDate.isEmpty() && endDate.isEmpty()) return "—";

        if (!startDate.isEmpty() && !endDate.isEmpty()) {
            if (startDate.equalsIgnoreCase(endDate)) return startDate;
            return startDate + " to " + endDate;
        }

        return !startDate.isEmpty() ? startDate : endDate;
    }

    private String buildTimeRange(String start, String end) {
        if (start.isEmpty() && end.isEmpty()) return "—";

        if (!start.isEmpty() && !end.isEmpty()) {
            return start + " - " + end;
        }

        return !start.isEmpty() ? start : end;
    }

    private void startOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private void endOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
    }

    private boolean isSameDate(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.MONTH) == second.get(Calendar.MONTH)
                && first.get(Calendar.DAY_OF_MONTH) == second.get(Calendar.DAY_OF_MONTH);
    }

    private String getStringValue(DocumentSnapshot doc, String field) {
        String value = doc.getString(field);
        return value != null ? value.trim() : "";
    }

    private String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) return first.trim();
        if (second != null && !second.trim().isEmpty()) return second.trim();
        return "";
    }

    private String formatCount(int count) {
        if (count < 0) return "00";
        if (count < 10) return "0" + count;
        return String.valueOf(count);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}