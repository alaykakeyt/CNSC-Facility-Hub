package com.example.cnscfacilityhubproject.activities.gsoUI;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cnscfacilityhubproject.R;
import com.google.android.material.button.MaterialButton;
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

public class gsoReportsFragment extends Fragment {

    private AutoCompleteTextView actvReportFilter;

    private View cardMonthlyReport;
    private View cardFacilityReport;
    private View cardUserReport;
    private View layoutEmptyState;
    private View layoutReportList;

    private MaterialButton btnViewMonthly;
    private MaterialButton btnExportMonthly;
    private MaterialButton btnViewFacility;
    private MaterialButton btnOpenUserReport;

    private TextView tvMonthlyTitle;
    private TextView tvMonthlyMeta;
    private TextView tvMonthlyDesc;

    private TextView tvFacilityTitle;
    private TextView tvFacilityMeta;
    private TextView tvFacilityDesc;

    private TextView tvUserTitle;
    private TextView tvUserMeta;
    private TextView tvUserDesc;

    private FirebaseFirestore db;
    private ListenerRegistration requestsListener;
    private ListenerRegistration usersListener;

    private int totalRequests = 0;
    private int pendingRequests = 0;
    private int approvedRequests = 0;
    private int returnedRequests = 0;
    private int currentMonthRequests = 0;
    private int currentMonthApproved = 0;
    private int totalUsers = 0;
    private int activeUsers = 0;

    private String topFacility = "No facility data";
    private int topFacilityUses = 0;

    private String topRequestor = "No requestor data";
    private int topRequestorRequests = 0;

    private String selectedFilter = "All";

    public gsoReportsFragment() {
        super(R.layout.fragment_gso_reports);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        bindViews(view);
        setupFilter();
        setupActions();
        setLoadingTexts();

        listenForRequests();
        listenForUsers();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }

        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }
    }

    private void bindViews(View view) {
        actvReportFilter = view.findViewById(R.id.actvReportFilter);

        cardMonthlyReport = view.findViewById(R.id.cardMonthlyReport);
        cardFacilityReport = view.findViewById(R.id.cardFacilityReport);
        cardUserReport = view.findViewById(R.id.cardUserReport);

        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        layoutReportList = view.findViewById(R.id.layoutReportList);

        btnViewMonthly = view.findViewById(R.id.btnViewMonthly);
        btnExportMonthly = view.findViewById(R.id.btnExportMonthly);
        btnViewFacility = view.findViewById(R.id.btnViewFacility);
        btnOpenUserReport = view.findViewById(R.id.btnOpenUserReport);

        tvMonthlyTitle = view.findViewById(R.id.tvMonthlyTitle);
        tvMonthlyMeta = view.findViewById(R.id.tvMonthlyMeta);
        tvMonthlyDesc = view.findViewById(R.id.tvMonthlyDesc);

        tvFacilityTitle = view.findViewById(R.id.tvFacilityTitle);
        tvFacilityMeta = view.findViewById(R.id.tvFacilityMeta);
        tvFacilityDesc = view.findViewById(R.id.tvFacilityDesc);

        tvUserTitle = view.findViewById(R.id.tvUserTitle);
        tvUserMeta = view.findViewById(R.id.tvUserMeta);
        tvUserDesc = view.findViewById(R.id.tvUserDesc);
    }

    private void setupFilter() {
        String[] filterOptions = {"All", "Monthly", "Facility", "Users"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                filterOptions
        );

        actvReportFilter.setAdapter(adapter);
        actvReportFilter.setText(selectedFilter, false);

        actvReportFilter.setOnItemClickListener((parent, view, position, id) -> {
            selectedFilter = filterOptions[position];
            applyFilter(selectedFilter);
        });

        applyFilter(selectedFilter);
    }

    private void applyFilter(String filter) {
        switch (filter) {
            case "Monthly":
                showCards(true, false, false);
                break;

            case "Facility":
                showCards(false, true, false);
                break;

            case "Users":
                showCards(false, false, true);
                break;

            case "All":
            default:
                showCards(true, true, true);
                break;
        }
    }

    private void showCards(boolean showMonthly, boolean showFacility, boolean showUsers) {
        cardMonthlyReport.setVisibility(showMonthly ? View.VISIBLE : View.GONE);
        cardFacilityReport.setVisibility(showFacility ? View.VISIBLE : View.GONE);
        cardUserReport.setVisibility(showUsers ? View.VISIBLE : View.GONE);

        boolean hasVisibleItems = showMonthly || showFacility || showUsers;
        layoutReportList.setVisibility(hasVisibleItems ? View.VISIBLE : View.GONE);
        layoutEmptyState.setVisibility(hasVisibleItems ? View.GONE : View.VISIBLE);
    }

    private void setupActions() {
        btnViewMonthly.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), gsoMonthlyReportActivity.class);
            startActivity(intent);
        });

        btnExportMonthly.setOnClickListener(v ->
                exportReport("Monthly Booking Report", buildMonthlyReportText())
        );

        btnViewFacility.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), gsoFacilityReportActivity.class);
            startActivity(intent);
        });

        btnOpenUserReport.setOnClickListener(v ->
                showReportDialog("User Activity Report", buildUserReportText())
        );
    }

    private void setLoadingTexts() {
        tvMonthlyTitle.setText("Monthly Booking Report");
        tvMonthlyMeta.setText("Loading booking data...");
        tvMonthlyDesc.setText("Please wait while the system prepares the monthly request summary.");

        tvFacilityTitle.setText("Facility Usage Report");
        tvFacilityMeta.setText("Loading facility data...");
        tvFacilityDesc.setText("Please wait while the system analyzes facility usage.");

        tvUserTitle.setText("User Activity Report");
        tvUserMeta.setText("Loading user data...");
        tvUserDesc.setText("Please wait while the system prepares user activity details.");
    }

    private void listenForRequests() {
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }

        requestsListener = db.collection("requests")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null || snapshot == null) {
                        Toast.makeText(requireContext(), "Failed to load report data.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    totalRequests = 0;
                    pendingRequests = 0;
                    approvedRequests = 0;
                    returnedRequests = 0;
                    currentMonthRequests = 0;
                    currentMonthApproved = 0;

                    Map<String, Integer> facilityCounts = new HashMap<>();
                    Map<String, Integer> requestorCounts = new HashMap<>();

                    Calendar now = Calendar.getInstance();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        if (!isGSORequest(doc)) continue;

                        totalRequests++;

                        String status = getStringValue(doc, "status");

                        if ("Pending".equalsIgnoreCase(status)) {
                            pendingRequests++;
                        } else if (isApprovedRequest(doc)) {
                            approvedRequests++;
                        } else if ("Returned".equalsIgnoreCase(status)
                                || "Return".equalsIgnoreCase(status)
                                || "Not Available".equalsIgnoreCase(status)) {
                            returnedRequests++;
                        }

                        Calendar requestDate = getRequestCalendar(doc);

                        boolean isCurrentMonth = requestDate != null
                                && requestDate.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                                && requestDate.get(Calendar.MONTH) == now.get(Calendar.MONTH);

                        if (isCurrentMonth) {
                            currentMonthRequests++;

                            if (isApprovedRequest(doc)) {
                                currentMonthApproved++;
                            }
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

                        String requestor = firstNonEmpty(
                                getStringValue(doc, "requestorName"),
                                getStringValue(doc, "fullName")
                        );

                        if (!requestor.isEmpty()) {
                            int current = requestorCounts.containsKey(requestor)
                                    ? requestorCounts.get(requestor)
                                    : 0;

                            requestorCounts.put(requestor, current + 1);
                        }
                    }

                    updateTopFacility(facilityCounts);
                    updateTopRequestor(requestorCounts);
                    updateReportCards();
                });
    }

    private void listenForUsers() {
        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }

        usersListener = db.collection("users")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null || snapshot == null) {
                        totalUsers = 0;
                        activeUsers = 0;
                        updateReportCards();
                        return;
                    }

                    totalUsers = snapshot.size();
                    activeUsers = 0;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String status = getStringValue(doc, "status");

                        if (status.isEmpty() || "Active".equalsIgnoreCase(status)) {
                            activeUsers++;
                        }
                    }

                    updateReportCards();
                });
    }

    private void updateReportCards() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String currentMonth = monthFormat.format(Calendar.getInstance().getTime());

        tvMonthlyTitle.setText("Monthly Booking Report");
        tvMonthlyMeta.setText(currentMonth + " • " + currentMonthRequests + " request(s)");
        tvMonthlyDesc.setText(
                "Summary: " + currentMonthApproved + " approved, "
                        + pendingRequests + " pending, "
                        + returnedRequests + " returned. "
                        + "Total GSO requests recorded: " + totalRequests + "."
        );

        tvFacilityTitle.setText("Facility Usage Report");

        if (topFacilityUses > 0) {
            tvFacilityMeta.setText(topFacility + " • " + topFacilityUses + " approved use(s)");
            tvFacilityDesc.setText(
                    "Most-used approved facility: " + topFacility
                            + ". This report helps GSO monitor facility demand and booking distribution."
            );
        } else {
            tvFacilityMeta.setText("No approved facility usage yet");
            tvFacilityDesc.setText("Approved bookings will appear here once GSO approves facility requests.");
        }

        tvUserTitle.setText("User Activity Report");

        if (topRequestorRequests > 0) {
            tvUserMeta.setText(topRequestor + " • " + topRequestorRequests + " request(s)");
        } else {
            tvUserMeta.setText(totalUsers + " user(s) • " + activeUsers + " active");
        }

        tvUserDesc.setText(
                "User summary: " + totalUsers + " total account(s), "
                        + activeUsers + " active account(s). "
                        + "Top requestor data is based on submitted booking requests."
        );
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

    private void showReportDialog(String title, String message) {
        if (!isAdded()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Close", null)
                .setNegativeButton("Share", (dialog, which) -> exportReport(title, message))
                .show();
    }

    private void exportReport(String title, String reportText) {
        if (!isAdded()) return;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.putExtra(Intent.EXTRA_TEXT, reportText);

        try {
            startActivity(Intent.createChooser(intent, "Export report"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No app available to export report.", Toast.LENGTH_SHORT).show();
        }
    }

    private String buildMonthlyReportText() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String currentMonth = monthFormat.format(Calendar.getInstance().getTime());

        return "CNSC Facility Hub\n"
                + "Monthly Booking Report\n"
                + "Month: " + currentMonth + "\n\n"
                + "Current Month Requests: " + currentMonthRequests + "\n"
                + "Current Month Approved: " + currentMonthApproved + "\n\n"
                + "Overall GSO Requests\n"
                + "Total Requests: " + totalRequests + "\n"
                + "Pending Requests: " + pendingRequests + "\n"
                + "Approved Requests: " + approvedRequests + "\n"
                + "Returned Requests: " + returnedRequests + "\n\n"
                + "Top Facility: " + topFacility + " (" + topFacilityUses + " approved use/s)\n";
    }

    private String buildUserReportText() {
        return "CNSC Facility Hub\n"
                + "User Activity Report\n\n"
                + "Total Users: " + totalUsers + "\n"
                + "Active Users: " + activeUsers + "\n"
                + "Top Requestor: " + topRequestor + "\n"
                + "Top Requestor Requests: " + topRequestorRequests + "\n\n"
                + "Total GSO Requests: " + totalRequests + "\n";
    }

    private boolean isGSORequest(DocumentSnapshot doc) {
        String notificationTarget = getStringValue(doc, "notificationTarget");

        if ("GSO".equalsIgnoreCase(notificationTarget)) {
            return true;
        }

        Boolean sendToGSO = doc.getBoolean("sendToGSO");
        if (Boolean.TRUE.equals(sendToGSO)) {
            return true;
        }

        Boolean needsGSO = doc.getBoolean("needsGSO");
        if (Boolean.TRUE.equals(needsGSO)) {
            return true;
        }

        String facility = getStringValue(doc, "facility");
        String otherFacility = getStringValue(doc, "otherFacility");
        String finalFacilityName = getStringValue(doc, "finalFacilityName");

        boolean hasFacility = !facility.isEmpty()
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

    private String getStringValue(DocumentSnapshot doc, String field) {
        String value = doc.getString(field);
        return value != null ? value.trim() : "";
    }

    private String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) return first.trim();
        if (second != null && !second.trim().isEmpty()) return second.trim();
        return "";
    }
}