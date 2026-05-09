package com.example.cnscfacilityhubproject.activities.gsoUI;

import android.app.DatePickerDialog;
import android.content.Intent;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class gsoFacilityReportActivity extends AppCompatActivity {

    private ImageView ivBack;

    private AutoCompleteTextView actvFacilityFilter;
    private AutoCompleteTextView actvStatusFilter;

    private TextInputEditText etStartFilterDate;
    private TextInputEditText etEndFilterDate;

    private TextView tvTotalFacilities;
    private TextView tvTotalBookings;
    private TextView tvApprovedBookings;
    private TextView tvPendingBookings;
    private TextView tvTopFacility;
    private TextView tvSelectedFacility;
    private TextView tvDateRange;
    private TextView tvNoFacilityData;

    private LinearLayout layoutFacilitySummaryList;
    private LinearLayout layoutFacilityBookingList;

    private MaterialButton btnShareFacilityReport;
    private MaterialButton btnResetFilters;

    private FirebaseFirestore db;
    private ListenerRegistration requestsListener;

    private final ArrayList<DocumentSnapshot> allGsoBookings = new ArrayList<>();
    private final ArrayList<DocumentSnapshot> filteredBookings = new ArrayList<>();
    private final ArrayList<String> facilityOptions = new ArrayList<>();

    private String selectedFacilityFilter = "All Facilities";
    private String selectedStatusFilter = "All Status";

    private Calendar startFilterCalendar;
    private Calendar endFilterCalendar;

    private int totalBookings = 0;
    private int totalFacilities = 0;
    private int approvedBookings = 0;
    private int pendingBookings = 0;
    private int returnedBookings = 0;

    private String topFacility = "No facility data";
    private int topFacilityUses = 0;

    private String reportText = "";

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

    private final SimpleDateFormat shortDateFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gso_facility_report);

        db = FirebaseFirestore.getInstance();

        setupDefaultDateRange();
        bindViews();
        setupActions();
        setupFilters();
        updateDateFields();
        listenForFacilityUsage();
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

        actvFacilityFilter = findViewById(R.id.actvFacilityFilter);
        actvStatusFilter = findViewById(R.id.actvStatusFilter);

        etStartFilterDate = findViewById(R.id.etStartFilterDate);
        etEndFilterDate = findViewById(R.id.etEndFilterDate);

        tvTotalFacilities = findViewById(R.id.tvTotalFacilities);
        tvTotalBookings = findViewById(R.id.tvTotalBookings);
        tvApprovedBookings = findViewById(R.id.tvApprovedBookings);
        tvPendingBookings = findViewById(R.id.tvPendingBookings);
        tvTopFacility = findViewById(R.id.tvTopFacility);
        tvSelectedFacility = findViewById(R.id.tvSelectedFacility);
        tvDateRange = findViewById(R.id.tvDateRange);
        tvNoFacilityData = findViewById(R.id.tvNoFacilityData);

        layoutFacilitySummaryList = findViewById(R.id.layoutFacilitySummaryList);
        layoutFacilityBookingList = findViewById(R.id.layoutFacilityBookingList);

        btnShareFacilityReport = findViewById(R.id.btnShareFacilityReport);
        btnResetFilters = findViewById(R.id.btnResetFilters);
    }

    private void setupActions() {
        ivBack.setOnClickListener(v -> finish());

        etStartFilterDate.setOnClickListener(v -> showStartDatePicker());
        etEndFilterDate.setOnClickListener(v -> showEndDatePicker());

        btnResetFilters.setOnClickListener(v -> {
            selectedFacilityFilter = "All Facilities";
            selectedStatusFilter = "All Status";

            setupDefaultDateRange();
            updateDateFields();

            actvFacilityFilter.setText(selectedFacilityFilter, false);
            actvStatusFilter.setText(selectedStatusFilter, false);

            renderFacilityReport();
        });

        btnShareFacilityReport.setOnClickListener(v -> shareFacilityReport());
    }

    private void setupFilters() {
        setupStatusFilter();

        facilityOptions.clear();
        facilityOptions.add("All Facilities");

        ArrayAdapter<String> facilityAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                facilityOptions
        );

        actvFacilityFilter.setAdapter(facilityAdapter);
        actvFacilityFilter.setText(selectedFacilityFilter, false);

        actvFacilityFilter.setOnItemClickListener((parent, view, position, id) -> {
            selectedFacilityFilter = facilityOptions.get(position);
            renderFacilityReport();
        });
    }

    private void setupStatusFilter() {
        String[] statusOptions = {"All Status", "Approved", "Pending", "Returned"};

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                statusOptions
        );

        actvStatusFilter.setAdapter(statusAdapter);
        actvStatusFilter.setText(selectedStatusFilter, false);

        actvStatusFilter.setOnItemClickListener((parent, view, position, id) -> {
            selectedStatusFilter = statusOptions[position];
            renderFacilityReport();
        });
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
                    }

                    updateDateFields();
                    renderFacilityReport();
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

                    updateDateFields();
                    renderFacilityReport();
                },
                pickerCalendar.get(Calendar.YEAR),
                pickerCalendar.get(Calendar.MONTH),
                pickerCalendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void updateDateFields() {
        if (startFilterCalendar != null) {
            etStartFilterDate.setText(displayDateFormat.format(startFilterCalendar.getTime()));
        }

        if (endFilterCalendar != null) {
            etEndFilterDate.setText(displayDateFormat.format(endFilterCalendar.getTime()));
        }

        if (startFilterCalendar != null && endFilterCalendar != null) {
            String start = shortDateFormat.format(startFilterCalendar.getTime());
            String end = shortDateFormat.format(endFilterCalendar.getTime());

            if (isSameDate(startFilterCalendar, endFilterCalendar)) {
                tvDateRange.setText(start);
            } else {
                tvDateRange.setText(start + " - " + end);
            }
        }
    }

    private void listenForFacilityUsage() {
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }

        requestsListener = db.collection("requests")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        Toast.makeText(this, "Failed to load facility report.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allGsoBookings.clear();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        if (!isGSORequest(doc)) continue;

                        String facility = getFinalFacility(doc);
                        if (facility.isEmpty()) continue;

                        allGsoBookings.add(doc);
                    }

                    updateFacilityFilterOptions();
                    renderFacilityReport();
                });
    }

    private void updateFacilityFilterOptions() {
        Set<String> uniqueFacilities = new HashSet<>();

        for (DocumentSnapshot doc : allGsoBookings) {
            String facility = getFinalFacility(doc);

            if (!facility.isEmpty()) {
                uniqueFacilities.add(facility);
            }
        }

        ArrayList<String> sortedFacilities = new ArrayList<>(uniqueFacilities);
        Collections.sort(sortedFacilities);

        facilityOptions.clear();
        facilityOptions.add("All Facilities");
        facilityOptions.addAll(sortedFacilities);

        if (!facilityOptions.contains(selectedFacilityFilter)) {
            selectedFacilityFilter = "All Facilities";
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                facilityOptions
        );

        actvFacilityFilter.setAdapter(adapter);
        actvFacilityFilter.setText(selectedFacilityFilter, false);
    }

    private void renderFacilityReport() {
        filteredBookings.clear();

        Map<String, Integer> facilityCounts = new HashMap<>();

        layoutFacilitySummaryList.removeAllViews();
        layoutFacilityBookingList.removeAllViews();

        totalBookings = 0;
        totalFacilities = 0;
        approvedBookings = 0;
        pendingBookings = 0;
        returnedBookings = 0;

        for (DocumentSnapshot doc : allGsoBookings) {
            String facility = getFinalFacility(doc);
            String status = getDisplayStatus(doc);
            Calendar requestDate = getRequestCalendar(doc);

            if (facility.isEmpty()) continue;
            if (requestDate == null) continue;
            if (!isWithinSelectedRange(requestDate)) continue;

            boolean matchesFacility =
                    "All Facilities".equalsIgnoreCase(selectedFacilityFilter)
                            || selectedFacilityFilter.equalsIgnoreCase(facility);

            boolean matchesStatus =
                    "All Status".equalsIgnoreCase(selectedStatusFilter)
                            || selectedStatusFilter.equalsIgnoreCase(status);

            if (!matchesFacility || !matchesStatus) continue;

            filteredBookings.add(doc);
            totalBookings++;

            if ("Approved".equalsIgnoreCase(status)) {
                approvedBookings++;
            } else if ("Returned".equalsIgnoreCase(status)
                    || "Return".equalsIgnoreCase(status)
                    || "Not Available".equalsIgnoreCase(status)) {
                returnedBookings++;
            } else {
                pendingBookings++;
            }

            int current = facilityCounts.containsKey(facility) ? facilityCounts.get(facility) : 0;
            facilityCounts.put(facility, current + 1);
        }

        totalFacilities = facilityCounts.size();

        updateTopFacility(facilityCounts);
        updateSummaryCards();

        if (filteredBookings.isEmpty()) {
            tvNoFacilityData.setVisibility(View.VISIBLE);
            tvNoFacilityData.setText("No facility bookings found for the selected filters.");
            buildReportText(facilityCounts);
            return;
        }

        tvNoFacilityData.setVisibility(View.GONE);

        ArrayList<Map.Entry<String, Integer>> facilityEntries = new ArrayList<>(facilityCounts.entrySet());
        Collections.sort(facilityEntries, (a, b) -> b.getValue().compareTo(a.getValue()));

        for (Map.Entry<String, Integer> entry : facilityEntries) {
            layoutFacilitySummaryList.addView(createFacilitySummaryCard(entry.getKey(), entry.getValue()));
        }

        Collections.sort(filteredBookings, (a, b) -> {
            Calendar dateA = getRequestCalendar(a);
            Calendar dateB = getRequestCalendar(b);

            if (dateA == null && dateB == null) return 0;
            if (dateA == null) return 1;
            if (dateB == null) return -1;

            return dateA.compareTo(dateB);
        });

        for (DocumentSnapshot doc : filteredBookings) {
            layoutFacilityBookingList.addView(createBookingCard(doc));
        }

        buildReportText(facilityCounts);
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

    private void updateSummaryCards() {
        tvTotalFacilities.setText(formatCount(totalFacilities));
        tvTotalBookings.setText(formatCount(totalBookings));
        tvApprovedBookings.setText(formatCount(approvedBookings));
        tvPendingBookings.setText(formatCount(pendingBookings));

        if (topFacilityUses > 0) {
            tvTopFacility.setText(topFacility + " • " + topFacilityUses + " booking(s)");
        } else {
            tvTopFacility.setText("No facility data");
        }

        tvSelectedFacility.setText(selectedFacilityFilter + " • " + selectedStatusFilter);
    }

    private View createFacilitySummaryCard(String facilityName, int useCount) {
        MaterialCardView card = new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(10));

        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(22));
        card.setCardElevation(dp(5));
        card.setStrokeColor(Color.parseColor("#313131"));
        card.setStrokeWidth(dp(1));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));

        MaterialCardView icon = new MaterialCardView(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(46), dp(46));
        icon.setLayoutParams(iconParams);
        icon.setRadius(dp(15));
        icon.setCardElevation(0);
        icon.setCardBackgroundColor(Color.parseColor("#970705"));

        TextView iconText = new TextView(this);
        iconText.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        iconText.setGravity(Gravity.CENTER);
        iconText.setText(getInitial(facilityName));
        iconText.setTextColor(Color.WHITE);
        iconText.setTextSize(16f);
        iconText.setTypeface(null, android.graphics.Typeface.BOLD);
        icon.addView(iconText);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        detailParams.setMargins(dp(12), 0, dp(8), 0);
        details.setLayoutParams(detailParams);

        TextView tvFacility = new TextView(this);
        tvFacility.setText(facilityName);
        tvFacility.setTextColor(Color.parseColor("#313131"));
        tvFacility.setTextSize(15f);
        tvFacility.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvUsage = new TextView(this);
        tvUsage.setText(useCount + " booking(s) in selected filters");
        tvUsage.setTextColor(Color.parseColor("#313131"));
        tvUsage.setTextSize(13f);
        tvUsage.setAlpha(0.72f);

        details.addView(tvFacility);
        details.addView(tvUsage);

        Chip chip = new Chip(this);
        chip.setText(useCount + "");
        chip.setTextColor(Color.parseColor("#970705"));
        chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F3D9D9")));
        chip.setCheckable(false);
        chip.setClickable(false);

        row.addView(icon);
        row.addView(details);
        row.addView(chip);

        card.addView(row);

        card.setOnClickListener(v -> {
            selectedFacilityFilter = facilityName;
            actvFacilityFilter.setText(selectedFacilityFilter, false);
            renderFacilityReport();
        });

        return card;
    }

    private View createBookingCard(DocumentSnapshot doc) {
        String purpose = getStringValue(doc, "purpose");
        String facility = getFinalFacility(doc);
        String status = getDisplayStatus(doc);

        String requestor = firstNonEmpty(
                getStringValue(doc, "requestorName"),
                getStringValue(doc, "fullName")
        );

        String startDate = getStringValue(doc, "startDateText");
        String endDate = getStringValue(doc, "endDateText");
        String timeStart = getStringValue(doc, "timeStartText");
        String timeEnd = getStringValue(doc, "timeEndText");

        if (purpose.isEmpty()) purpose = "Purpose / Activity";
        if (requestor.isEmpty()) requestor = "Unknown requestor";

        MaterialCardView card = new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(10));

        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(22));
        card.setCardElevation(dp(5));
        card.setStrokeColor(Color.parseColor("#313131"));
        card.setStrokeWidth(dp(1));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(purpose);
        tvTitle.setTextColor(Color.parseColor("#313131"));
        tvTitle.setTextSize(15f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        tvTitle.setLayoutParams(titleParams);

        Chip chip = new Chip(this);
        chip.setText(status);
        chip.setTextColor(getStatusTextColor(status));
        chip.setChipBackgroundColor(ColorStateList.valueOf(getStatusBgColor(status)));
        chip.setCheckable(false);
        chip.setClickable(false);

        topRow.addView(tvTitle);
        topRow.addView(chip);

        TextView tvDetails = new TextView(this);
        tvDetails.setText(
                "Facility: " + facility + "\n"
                        + "Requestor: " + requestor + "\n"
                        + "Date: " + buildDateRange(startDate, endDate) + "\n"
                        + "Time: " + buildTimeRange(timeStart, timeEnd)
        );
        tvDetails.setTextColor(Color.parseColor("#313131"));
        tvDetails.setTextSize(13f);
        tvDetails.setLineSpacing(2f, 1f);
        tvDetails.setAlpha(0.78f);

        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        detailParams.setMargins(0, dp(10), 0, 0);
        tvDetails.setLayoutParams(detailParams);

        container.addView(topRow);
        container.addView(tvDetails);

        card.addView(container);

        return card;
    }

    private void buildReportText(Map<String, Integer> facilityCounts) {
        StringBuilder builder = new StringBuilder();

        builder.append("CNSC Facility Hub\n");
        builder.append("Facility Usage Report\n\n");
        builder.append("Date Range: ").append(tvDateRange.getText().toString()).append("\n");
        builder.append("Facility Filter: ").append(selectedFacilityFilter).append("\n");
        builder.append("Status Filter: ").append(selectedStatusFilter).append("\n\n");
        builder.append("Total Facilities: ").append(totalFacilities).append("\n");
        builder.append("Total Bookings: ").append(totalBookings).append("\n");
        builder.append("Approved: ").append(approvedBookings).append("\n");
        builder.append("Pending: ").append(pendingBookings).append("\n");
        builder.append("Returned: ").append(returnedBookings).append("\n\n");
        builder.append("Top Facility: ").append(topFacility).append(" (").append(topFacilityUses).append(" booking/s)\n\n");
        builder.append("Facility Summary:\n");

        if (facilityCounts.isEmpty()) {
            builder.append("No facility usage found for the selected filters.\n");
        } else {
            ArrayList<Map.Entry<String, Integer>> entries = new ArrayList<>(facilityCounts.entrySet());
            Collections.sort(entries, (a, b) -> b.getValue().compareTo(a.getValue()));

            for (Map.Entry<String, Integer> entry : entries) {
                builder.append("- ")
                        .append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue())
                        .append(" booking/s\n");
            }
        }

        reportText = builder.toString();
    }

    private void shareFacilityReport() {
        if (reportText == null || reportText.trim().isEmpty()) {
            reportText = "CNSC Facility Hub\nFacility Usage Report";
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "GSO Facility Usage Report");
        intent.putExtra(Intent.EXTRA_TEXT, reportText);

        try {
            startActivity(Intent.createChooser(intent, "Share Facility Report"));
        } catch (Exception e) {
            Toast.makeText(this, "No app available to share report.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isWithinSelectedRange(Calendar requestDate) {
        if (requestDate == null || startFilterCalendar == null || endFilterCalendar == null) {
            return false;
        }

        Calendar normalizedDate = (Calendar) requestDate.clone();
        startOfDay(normalizedDate);

        return !normalizedDate.before(startFilterCalendar)
                && !normalizedDate.after(endFilterCalendar);
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
        String gsoStatus = getStringValue(doc, "gsoStatus");
        String gsoAvailability = getStringValue(doc, "gsoAvailability");

        if ("Returned".equalsIgnoreCase(status)
                || "Return".equalsIgnoreCase(status)
                || "Not Available".equalsIgnoreCase(status)
                || "Not Available".equalsIgnoreCase(gsoStatus)
                || "Not Available".equalsIgnoreCase(gsoAvailability)) {
            return "Returned";
        }

        if (status.isEmpty()) return "Pending";

        return status;
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
        if (!start.isEmpty() && !end.isEmpty()) return start + " - " + end;
        return !start.isEmpty() ? start : end;
    }

    private int getStatusTextColor(String status) {
        if ("Approved".equalsIgnoreCase(status)) return Color.parseColor("#2E7D32");
        if ("Returned".equalsIgnoreCase(status)) return Color.parseColor("#970705");
        return Color.parseColor("#313131");
    }

    private int getStatusBgColor(String status) {
        if ("Approved".equalsIgnoreCase(status)) return Color.parseColor("#E7F4E8");
        if ("Returned".equalsIgnoreCase(status)) return Color.parseColor("#F3D9D9");
        return Color.parseColor("#EEEEEE");
    }

    private String getInitial(String text) {
        if (text == null || text.trim().isEmpty()) return "F";
        return text.trim().substring(0, 1).toUpperCase();
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