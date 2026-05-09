package com.example.cnscfacilityhubproject.activities.gsoUI;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cnscfacilityhubproject.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class gsoHomeFragment extends Fragment {

    private TextView tvCalendarMonth;
    private TextView tvSelectedDate;
    private TextView gsoHomeWelcome;

    private TextView tvPendingCount;
    private TextView tvApprovedCount;
    private TextView tvUsersCount;
    private TextView tvReportsCount;
    private TextView badgeGsoNotifications;

    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private GridLayout calendarGrid;

    private MaterialButton btnViewRequests;
    private MaterialButton btnGenerateReport;
    private ImageView ivNotifications;

    private View gsoPendingReq;
    private View gsoApprovedReq;
    private View gsoUsers;
    private View gsoReports;

    private LinearLayout schedsContainer;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private ListenerRegistration requestsListener;
    private ListenerRegistration usersListener;
    private ListenerRegistration reportsListener;
    private ListenerRegistration gsoNotificationBadgeListener;

    private Calendar currentCalendar;
    private Calendar selectedCalendar;

    private final Map<String, Integer> bookedDatesMap = new HashMap<>();
    private final Map<String, List<ScheduleItem>> schedulesByDateMap = new HashMap<>();

    private static final int FULLY_BOOKED_LIMIT = 3;

    public gsoHomeFragment() {
        super(R.layout.fragment_gso_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        currentCalendar = Calendar.getInstance();
        selectedCalendar = Calendar.getInstance();

        bindViews(view);
        setDefaultTexts();
        setupActions();
        setupCalendarNavigation();

        updateHomeWelcome();
        updateSelectedDateSection();

        listenForRequestsAndCalendar();
        listenForUsersCount();
        listenForReportsCount();

        setupGsoBadgeStyle();
        listenForGsoNotificationBadge();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (requestsListener != null) requestsListener.remove();
        if (usersListener != null) usersListener.remove();
        if (reportsListener != null) reportsListener.remove();
        if (gsoNotificationBadgeListener != null) gsoNotificationBadgeListener.remove();
    }

    private void bindViews(View view) {
        tvCalendarMonth = view.findViewById(R.id.tvCalendarMonth);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        gsoHomeWelcome = view.findViewById(R.id.tvGSOName);

        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        tvApprovedCount = view.findViewById(R.id.tvApprovedCount);
        tvUsersCount = view.findViewById(R.id.tvUsersCount);
        tvReportsCount = view.findViewById(R.id.tvReportsCount);

        badgeGsoNotifications = view.findViewById(R.id.badgeGsoNotifications);

        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        calendarGrid = view.findViewById(R.id.calendarGrid);

        btnViewRequests = view.findViewById(R.id.btnViewRequests);
        btnGenerateReport = view.findViewById(R.id.btnGenerateReport);
        ivNotifications = view.findViewById(R.id.ivNotifications);

        gsoPendingReq = view.findViewById(R.id.gsoPendingReq);
        gsoApprovedReq = view.findViewById(R.id.gsoApprovedReq);
        gsoUsers = view.findViewById(R.id.gsoUsers);
        gsoReports = view.findViewById(R.id.gsoReports);

        schedsContainer = view.findViewById(R.id.Scheds);
    }

    private void setDefaultTexts() {
        if (gsoHomeWelcome != null) gsoHomeWelcome.setText("Hello, GSO!");
        if (tvPendingCount != null) tvPendingCount.setText("00");
        if (tvApprovedCount != null) tvApprovedCount.setText("00");
        if (tvUsersCount != null) tvUsersCount.setText("00");
        if (tvReportsCount != null) tvReportsCount.setText("00");

        updateSelectedDateSection();
    }

    private void setupGsoBadgeStyle() {
        if (badgeGsoNotifications == null) return;

        GradientDrawable badgeBackground = new GradientDrawable();
        badgeBackground.setShape(GradientDrawable.OVAL);
        badgeBackground.setColor(Color.parseColor("#970705"));

        badgeGsoNotifications.setBackground(badgeBackground);
        badgeGsoNotifications.setGravity(Gravity.CENTER);
        badgeGsoNotifications.setTextColor(Color.WHITE);
        badgeGsoNotifications.setTextSize(8f);
        badgeGsoNotifications.setTypeface(null, Typeface.BOLD);
        badgeGsoNotifications.setVisibility(View.GONE);
        badgeGsoNotifications.setText("0");
    }

    private void listenForGsoNotificationBadge() {
        updateGsoBadge(0);

        if (gsoNotificationBadgeListener != null) {
            gsoNotificationBadgeListener.remove();
            gsoNotificationBadgeListener = null;
        }

        gsoNotificationBadgeListener = db.collection("requests")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null || snapshot == null) {
                        updateGsoBadge(0);
                        return;
                    }

                    int unseenCount = 0;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (isIncomingGsoNotification(doc)) {
                            unseenCount++;
                        }
                    }

                    updateGsoBadge(unseenCount);
                });
    }

    private boolean isIncomingGsoNotification(DocumentSnapshot doc) {
        String status = getStringValue(doc, "status");
        String notificationTarget = getStringValue(doc, "notificationTarget");
        String workflowStage = getStringValue(doc, "workflowStage");

        Boolean sendToGSO = doc.getBoolean("sendToGSO");
        Boolean gsoSeen = doc.getBoolean("gsoSeen");
        Boolean gsoNotificationSeen = doc.getBoolean("gsoNotificationSeen");

        boolean unseen = !Boolean.TRUE.equals(gsoSeen)
                && !Boolean.TRUE.equals(gsoNotificationSeen);

        boolean pending = "Pending".equalsIgnoreCase(status);

        boolean targetGSO = "GSO".equalsIgnoreCase(notificationTarget)
                || "GSO_REVIEW".equalsIgnoreCase(workflowStage)
                || Boolean.TRUE.equals(sendToGSO);

        return unseen && pending && targetGSO;
    }

    private void updateGsoBadge(int count) {
        if (badgeGsoNotifications == null) return;

        if (count <= 0) {
            badgeGsoNotifications.setVisibility(View.GONE);
            badgeGsoNotifications.setText("0");
            return;
        }

        badgeGsoNotifications.setVisibility(View.VISIBLE);
        badgeGsoNotifications.setText(count > 99 ? "99+" : String.valueOf(count));
    }

    private void markGsoNotificationsAsSeen() {
        db.collection("requests")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null) return;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (isIncomingGsoNotification(doc)) {
                            db.collection("requests")
                                    .document(doc.getId())
                                    .update(
                                            "gsoSeen", true,
                                            "gsoNotificationSeen", true
                                    );
                        }
                    }

                    updateGsoBadge(0);
                });
    }

    private void setupCalendarNavigation() {
        if (btnPrevMonth != null) {
            btnPrevMonth.setOnClickListener(v -> {
                currentCalendar.add(Calendar.MONTH, -1);
                renderCalendar();
                updateSelectedDateSection();
            });
        }

        if (btnNextMonth != null) {
            btnNextMonth.setOnClickListener(v -> {
                currentCalendar.add(Calendar.MONTH, 1);
                renderCalendar();
                updateSelectedDateSection();
            });
        }
    }

    private void setupActions() {
        if (btnViewRequests != null) {
            btnViewRequests.setOnClickListener(v -> openGsoRequests("All"));
        }

        if (gsoPendingReq != null) {
            gsoPendingReq.setOnClickListener(v -> openGsoRequests("Pending"));
        }

        if (gsoApprovedReq != null) {
            gsoApprovedReq.setOnClickListener(v -> openGsoRequests("Approved"));
        }

        if (btnGenerateReport != null) {
            btnGenerateReport.setOnClickListener(v -> openGsoReports());
        }

        if (gsoReports != null) {
            gsoReports.setOnClickListener(v -> openGsoReports());
        }

        if (gsoUsers != null) {
            gsoUsers.setOnClickListener(v -> openGsoUsers());
        }

        if (ivNotifications != null) {
            ivNotifications.setOnClickListener(v -> {
                markGsoNotificationsAsSeen();
                openGsoNotifications();
            });
        }
    }

    private void listenForRequestsAndCalendar() {
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }

        requestsListener = db.collection("requests")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null || snapshot == null) {
                        if (tvPendingCount != null) tvPendingCount.setText("00");
                        if (tvApprovedCount != null) tvApprovedCount.setText("00");

                        bookedDatesMap.clear();
                        schedulesByDateMap.clear();

                        renderCalendar();
                        updateSelectedDateSection();

                        Toast.makeText(requireContext(), "Failed to load request dashboard.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int pendingCount = 0;
                    int approvedCount = 0;

                    bookedDatesMap.clear();
                    schedulesByDateMap.clear();

                    SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : snapshot) {
                        if (!isGSORequest(doc)) continue;

                        String status = getDisplayStatus(doc);

                        if ("Pending".equalsIgnoreCase(status)) {
                            pendingCount++;
                        }

                        if ("Approved".equalsIgnoreCase(status)) {
                            approvedCount++;
                        }

                        List<String> dateKeys = getDateKeysFromDocument(doc, keyFormat);

                        if (shouldShowOnCalendar(doc)) {
                            for (String dateKey : dateKeys) {
                                if (dateKey.isEmpty()) continue;

                                int currentCount = bookedDatesMap.containsKey(dateKey)
                                        ? bookedDatesMap.get(dateKey)
                                        : 0;

                                bookedDatesMap.put(dateKey, currentCount + 1);

                                ScheduleItem item = createScheduleItem(doc, dateKey);

                                if (item == null || item.dateKey.isEmpty()) continue;

                                List<ScheduleItem> list = schedulesByDateMap.get(item.dateKey);

                                if (list == null) {
                                    list = new ArrayList<>();
                                    schedulesByDateMap.put(item.dateKey, list);
                                }

                                list.add(item);
                            }
                        }
                    }

                    sortSchedulesByTime();

                    if (tvPendingCount != null) {
                        tvPendingCount.setText(formatCount(pendingCount));
                    }

                    if (tvApprovedCount != null) {
                        tvApprovedCount.setText(formatCount(approvedCount));
                    }

                    renderCalendar();
                    updateSelectedDateSection();
                });
    }

    private void listenForUsersCount() {
        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }

        usersListener = db.collection("users")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null || snapshot == null) {
                        if (tvUsersCount != null) tvUsersCount.setText("00");
                        return;
                    }

                    if (tvUsersCount != null) {
                        tvUsersCount.setText(formatCount(snapshot.size()));
                    }
                });
    }

    private void listenForReportsCount() {
        if (reportsListener != null) {
            reportsListener.remove();
            reportsListener = null;
        }

        reportsListener = db.collection("reports")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null || snapshot == null) {
                        if (tvReportsCount != null) tvReportsCount.setText("00");
                        return;
                    }

                    if (tvReportsCount != null) {
                        tvReportsCount.setText(formatCount(snapshot.size()));
                    }
                });
    }

    private boolean isGSORequest(DocumentSnapshot doc) {
        String notificationTarget = getStringValue(doc, "notificationTarget");
        String workflowStage = getStringValue(doc, "workflowStage");

        String facility = getStringValue(doc, "facility");
        String otherFacility = getStringValue(doc, "otherFacility");
        String finalFacilityName = getStringValue(doc, "finalFacilityName");

        boolean hasFacility = !facility.isEmpty()
                || !otherFacility.isEmpty()
                || !finalFacilityName.isEmpty();

        if ("GSO".equalsIgnoreCase(notificationTarget)) return true;
        if ("GSO_REVIEW".equalsIgnoreCase(workflowStage)) return true;

        Boolean sendToGSO = doc.getBoolean("sendToGSO");
        if (Boolean.TRUE.equals(sendToGSO)) return true;

        Boolean needsGSO = doc.getBoolean("needsGSO");
        if (Boolean.TRUE.equals(needsGSO)) return true;

        Boolean needsTechnical = doc.getBoolean("needsTechnical");
        Boolean cbNeedsTechnical = doc.getBoolean("cbNeedsTechnical");

        boolean explicitlyITSO = "ITSO".equalsIgnoreCase(notificationTarget);

        if (Boolean.TRUE.equals(needsTechnical) || Boolean.TRUE.equals(cbNeedsTechnical)) {
            return hasFacility && !explicitlyITSO;
        }

        return hasFacility && !explicitlyITSO;
    }

    private String getDisplayStatus(DocumentSnapshot doc) {
        String status = getStringValue(doc, "status");
        String gsoStatus = getStringValue(doc, "gsoStatus");
        String gsoAvailability = getStringValue(doc, "gsoAvailability");
        String bookingStatus = getStringValue(doc, "bookingStatus");
        String workflowStage = getStringValue(doc, "workflowStage");

        if ("Approved".equalsIgnoreCase(status)
                || "Approved".equalsIgnoreCase(gsoStatus)
                || "Available".equalsIgnoreCase(gsoStatus)
                || "Available".equalsIgnoreCase(gsoAvailability)
                || "Booked".equalsIgnoreCase(bookingStatus)
                || "APPROVED".equalsIgnoreCase(workflowStage)) {
            return "Approved";
        }

        if ("Returned".equalsIgnoreCase(status)
                || "Returned".equalsIgnoreCase(gsoStatus)
                || "Rejected".equalsIgnoreCase(status)
                || "Rejected".equalsIgnoreCase(gsoStatus)) {
            return "Returned";
        }

        return "Pending";
    }

    private boolean isApprovedRequest(DocumentSnapshot doc) {
        return "Approved".equalsIgnoreCase(getDisplayStatus(doc));
    }

    private boolean shouldShowOnCalendar(DocumentSnapshot doc) {
        String displayStatus = getDisplayStatus(doc);
        String bookingStatus = getStringValue(doc, "bookingStatus");

        Boolean calendarVisible = doc.getBoolean("calendarVisible");
        Boolean isCalendarBooking = doc.getBoolean("isCalendarBooking");

        if ("Pending".equalsIgnoreCase(displayStatus)) return true;
        if ("Approved".equalsIgnoreCase(displayStatus)) return true;
        if ("Booked".equalsIgnoreCase(bookingStatus)) return true;

        return Boolean.TRUE.equals(calendarVisible) || Boolean.TRUE.equals(isCalendarBooking);
    }

    private List<String> getDateKeysFromDocument(DocumentSnapshot doc, SimpleDateFormat keyFormat) {
        List<String> keys = new ArrayList<>();

        Calendar startCalendar = getStartCalendarFromDocument(doc);
        Calendar endCalendar = getEndCalendarFromDocument(doc);

        if (startCalendar == null) {
            return keys;
        }

        if (endCalendar == null) {
            endCalendar = (Calendar) startCalendar.clone();
        }

        startOfDay(startCalendar);
        startOfDay(endCalendar);

        if (endCalendar.before(startCalendar)) {
            endCalendar = (Calendar) startCalendar.clone();
        }

        Calendar cursor = (Calendar) startCalendar.clone();

        while (!cursor.after(endCalendar)) {
            keys.add(keyFormat.format(cursor.getTime()));
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }

        return keys;
    }

    private Calendar getStartCalendarFromDocument(DocumentSnapshot doc) {
        Timestamp timestamp = doc.getTimestamp("startDate");

        if (timestamp != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(timestamp.toDate());
            return calendar;
        }

        String startDateText = getStringValue(doc, "startDateText");

        if (!startDateText.isEmpty()) {
            return parseDateTextToCalendar(startDateText);
        }

        String dateText = getStringValue(doc, "date");

        if (!dateText.isEmpty()) {
            return parseDateTextToCalendar(dateText);
        }

        return null;
    }

    private Calendar getEndCalendarFromDocument(DocumentSnapshot doc) {
        Timestamp timestamp = doc.getTimestamp("endDate");

        if (timestamp != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(timestamp.toDate());
            return calendar;
        }

        String endDateText = getStringValue(doc, "endDateText");

        if (!endDateText.isEmpty()) {
            return parseDateTextToCalendar(endDateText);
        }

        return null;
    }

    private Calendar parseDateTextToCalendar(String dateText) {
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

    private void startOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private ScheduleItem createScheduleItem(DocumentSnapshot doc, String dateKey) {
        if (dateKey == null || dateKey.trim().isEmpty()) return null;

        String purpose = getStringValue(doc, "purpose");
        String facility = getFinalFacility(doc);
        String startDateText = getStringValue(doc, "startDateText");
        String endDateText = getStringValue(doc, "endDateText");
        String timeStartText = getStringValue(doc, "timeStartText");
        String timeEndText = getStringValue(doc, "timeEndText");
        String status = getDisplayStatus(doc);

        String requestorName = firstNonEmpty(
                getStringValue(doc, "requestorName"),
                getStringValue(doc, "fullName")
        );

        if (purpose.isEmpty()) purpose = "Purpose / Activity";
        if (facility.isEmpty()) facility = "Facility";
        if (status.isEmpty()) status = "Pending";

        return new ScheduleItem(
                doc.getId(),
                dateKey,
                purpose,
                facility,
                startDateText,
                endDateText,
                timeStartText,
                timeEndText,
                requestorName,
                status
        );
    }

    private void sortSchedulesByTime() {
        for (List<ScheduleItem> list : schedulesByDateMap.values()) {
            Collections.sort(list, (a, b) -> {
                long timeA = parseTimeToMillis(a.timeStartText);
                long timeB = parseTimeToMillis(b.timeStartText);
                return Long.compare(timeA, timeB);
            });
        }
    }

    private long parseTimeToMillis(String timeText) {
        if (timeText == null || timeText.trim().isEmpty()) return 0;

        String[] patterns = {"hh:mm a", "h:mm a", "HH:mm"};

        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
                java.util.Date date = sdf.parse(timeText.trim());

                if (date != null) return date.getTime();
            } catch (Exception ignored) {
            }
        }

        return 0;
    }

    private void renderCalendar() {
        if (!isAdded() || calendarGrid == null || tvCalendarMonth == null) return;

        calendarGrid.removeAllViews();

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvCalendarMonth.setText(monthFormat.format(currentCalendar.getTime()));

        Calendar calendar = (Calendar) currentCalendar.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int maxDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < firstDayOfWeek; i++) {
            TextView emptyCell = new TextView(requireContext());
            emptyCell.setLayoutParams(createDayLayoutParams());
            calendarGrid.addView(emptyCell);
        }

        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int day = 1; day <= maxDaysInMonth; day++) {
            TextView dayView = new TextView(requireContext());
            dayView.setLayoutParams(createDayLayoutParams());
            dayView.setGravity(Gravity.CENTER);
            dayView.setText(String.valueOf(day));
            dayView.setTextSize(16f);
            dayView.setPadding(8, 16, 8, 16);

            Calendar dayCalendar = (Calendar) currentCalendar.clone();
            dayCalendar.set(Calendar.DAY_OF_MONTH, day);

            boolean isToday = isSameDate(dayCalendar, Calendar.getInstance());
            boolean isSelected = isSameDate(dayCalendar, selectedCalendar);

            String dateKey = keyFormat.format(dayCalendar.getTime());
            int bookings = bookedDatesMap.containsKey(dateKey) ? bookedDatesMap.get(dateKey) : 0;

            if (isSelected) {
                dayView.setBackgroundColor(Color.parseColor("#313131"));
                dayView.setTextColor(Color.WHITE);
                dayView.setTypeface(null, Typeface.BOLD);
            } else if (bookings >= FULLY_BOOKED_LIMIT) {
                dayView.setBackgroundColor(Color.parseColor("#970705"));
                dayView.setTextColor(Color.WHITE);
                dayView.setTypeface(null, Typeface.BOLD);
            } else if (bookings > 0) {
                dayView.setBackgroundColor(Color.parseColor("#F3D9D9"));
                dayView.setTextColor(Color.parseColor("#313131"));
                dayView.setTypeface(null, Typeface.BOLD);
            } else if (isToday) {
                dayView.setBackgroundColor(Color.parseColor("#F3D9D9"));
                dayView.setTextColor(Color.parseColor("#313131"));
                dayView.setTypeface(null, Typeface.NORMAL);
            } else {
                dayView.setBackgroundColor(Color.TRANSPARENT);
                dayView.setTextColor(Color.parseColor("#313131"));
                dayView.setTypeface(null, Typeface.NORMAL);
            }

            dayView.setOnClickListener(v -> {
                selectedCalendar = (Calendar) dayCalendar.clone();
                renderCalendar();
                updateSelectedDateSection();
            });

            calendarGrid.addView(dayView);
        }
    }

    private GridLayout.LayoutParams createDayLayoutParams() {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(6, 6, 6, 6);
        return params;
    }

    private void updateSelectedDateSection() {
        if (!isAdded() || tvSelectedDate == null) return;

        SimpleDateFormat readableFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        String selectedDateText = readableFormat.format(selectedCalendar.getTime());

        tvSelectedDate.setText(selectedDateText);

        if (schedsContainer == null) return;

        schedsContainer.removeAllViews();

        if (tvSelectedDate.getParent() != null) {
            ((ViewGroup) tvSelectedDate.getParent()).removeView(tvSelectedDate);
        }

        tvSelectedDate.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        schedsContainer.addView(tvSelectedDate);

        String selectedDateKey = getDateKeyFromCalendar(selectedCalendar);
        List<ScheduleItem> schedules = schedulesByDateMap.get(selectedDateKey);

        if (schedules == null || schedules.isEmpty()) {
            schedsContainer.addView(createEmptyScheduleView());
            return;
        }

        for (ScheduleItem item : schedules) {
            schedsContainer.addView(createScheduleRow(item));
        }
    }

    private View createEmptyScheduleView() {
        TextView emptyView = new TextView(requireContext());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(16), 0, 0);

        emptyView.setLayoutParams(params);
        emptyView.setText("No pending or approved bookings for this date.");
        emptyView.setTextColor(Color.parseColor("#313131"));
        emptyView.setTextSize(14f);
        emptyView.setAlpha(0.72f);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(dp(12), dp(18), dp(12), dp(18));

        return emptyView;
    }

    private View createScheduleRow(ScheduleItem item) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(16), 0, 0);

        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        MaterialCardView timeCard = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams timeCardParams = new LinearLayout.LayoutParams(dp(54), dp(54));
        timeCard.setLayoutParams(timeCardParams);
        timeCard.setCardBackgroundColor(
                "Approved".equalsIgnoreCase(item.status)
                        ? Color.parseColor("#2E7D32")
                        : Color.parseColor("#970705")
        );
        timeCard.setRadius(dp(18));
        timeCard.setCardElevation(0);

        TextView timeText = new TextView(requireContext());
        timeText.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        timeText.setGravity(Gravity.CENTER);
        timeText.setText(getTimeBadgeText(item.timeStartText));
        timeText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        timeText.setTextColor(Color.WHITE);
        timeText.setTextSize(12f);
        timeText.setTypeface(null, Typeface.BOLD);

        timeCard.addView(timeText);

        LinearLayout details = new LinearLayout(requireContext());
        details.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        detailsParams.setMargins(dp(14), 0, dp(8), 0);
        details.setLayoutParams(detailsParams);

        TextView title = new TextView(requireContext());
        title.setText(item.purpose);
        title.setTextColor(Color.parseColor("#313131"));
        title.setTextSize(15f);
        title.setTypeface(null, Typeface.BOLD);

        TextView meta = new TextView(requireContext());
        meta.setText(buildScheduleMeta(item));
        meta.setTextColor(Color.parseColor("#313131"));
        meta.setTextSize(12f);
        meta.setAlpha(0.72f);

        TextView requestor = new TextView(requireContext());
        requestor.setText("Requestor: " + fallback(item.requestorName));
        requestor.setTextColor(Color.parseColor("#313131"));
        requestor.setTextSize(12f);
        requestor.setAlpha(0.72f);

        details.addView(title);
        details.addView(meta);
        details.addView(requestor);

        Chip statusChip = new Chip(requireContext());
        statusChip.setText(item.status);
        statusChip.setCheckable(false);
        statusChip.setClickable(false);

        if ("Approved".equalsIgnoreCase(item.status)) {
            statusChip.setTextColor(Color.parseColor("#2E7D32"));
            statusChip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E7F4E8")));
        } else {
            statusChip.setTextColor(Color.parseColor("#970705"));
            statusChip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F3D9D9")));
        }

        row.addView(timeCard);
        row.addView(details);
        row.addView(statusChip);

        row.setOnClickListener(v -> openGsoRequests(item.status));

        return row;
    }

    private String getTimeBadgeText(String timeText) {
        if (timeText == null || timeText.trim().isEmpty()) {
            return "--";
        }

        String[] patterns = {"hh:mm a", "h:mm a", "HH:mm"};

        for (String pattern : patterns) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat(pattern, Locale.getDefault());
                SimpleDateFormat hourFormat = new SimpleDateFormat("hh", Locale.getDefault());
                SimpleDateFormat amPmFormat = new SimpleDateFormat("a", Locale.getDefault());

                java.util.Date parsedTime = inputFormat.parse(timeText.trim());

                if (parsedTime != null) {
                    return hourFormat.format(parsedTime) + "\n" + amPmFormat.format(parsedTime);
                }
            } catch (Exception ignored) {
            }
        }

        return timeText;
    }

    private String buildScheduleMeta(ScheduleItem item) {
        StringBuilder builder = new StringBuilder();

        builder.append(item.facility);

        if (!item.timeStartText.isEmpty() || !item.timeEndText.isEmpty()) {
            builder.append(" • ");

            if (!item.timeStartText.isEmpty() && !item.timeEndText.isEmpty()) {
                builder.append(item.timeStartText).append(" - ").append(item.timeEndText);
            } else if (!item.timeStartText.isEmpty()) {
                builder.append(item.timeStartText);
            } else {
                builder.append(item.timeEndText);
            }
        }

        return builder.toString();
    }

    private String getDateKeyFromCalendar(Calendar calendar) {
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return keyFormat.format(calendar.getTime());
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

    private void updateHomeWelcome() {
        if (!isAdded() || gsoHomeWelcome == null) return;

        if (auth == null || auth.getCurrentUser() == null) {
            gsoHomeWelcome.setText("Hello, GSO!");
            return;
        }

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || gsoHomeWelcome == null) return;

                    String fullName = doc.exists() ? doc.getString("fullName") : "";

                    if (fullName != null && !fullName.trim().isEmpty()) {
                        String firstName = fullName.trim().split("\\s+")[0];
                        gsoHomeWelcome.setText("Hello, " + firstName + "!");
                    } else {
                        gsoHomeWelcome.setText("Hello, GSO!");
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || gsoHomeWelcome == null) return;
                    gsoHomeWelcome.setText("Hello, GSO!");
                });
    }

    private void openGsoRequests(String filter) {
        Bundle bundle = new Bundle();
        bundle.putString("filter", filter);

        gsoRequestsFragment fragment = new gsoRequestsFragment();
        fragment.setArguments(bundle);

        openFragment(fragment);
    }

    private void openGsoUsers() {
        openFragment(new gsoUsersFragment());
    }

    private void openGsoReports() {
        openFragment(new gsoReportsFragment());
    }

    private void openFragment(Fragment fragment) {
        if (!isAdded()) return;

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(getContainerId(), fragment)
                .addToBackStack(null)
                .commit();
    }

    private int getContainerId() {
        if (requireActivity().findViewById(R.id.gso_fragment_container) != null) {
            return R.id.gso_fragment_container;
        }

        return R.id.fragment_container;
    }

    private void openGsoNotifications() {
        if (!isAdded()) return;

        try {
            Intent intent = new Intent(requireContext(), gsoNotificationsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open notifications.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isSameDate(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
                && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
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

    private String fallback(String value) {
        return value != null && !value.trim().isEmpty() ? value.trim() : "—";
    }

    private String formatCount(int count) {
        if (count < 0) return "00";
        if (count < 10) return "0" + count;
        return String.valueOf(count);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class ScheduleItem {
        String requestId;
        String dateKey;
        String purpose;
        String facility;
        String startDateText;
        String endDateText;
        String timeStartText;
        String timeEndText;
        String requestorName;
        String status;

        ScheduleItem(
                String requestId,
                String dateKey,
                String purpose,
                String facility,
                String startDateText,
                String endDateText,
                String timeStartText,
                String timeEndText,
                String requestorName,
                String status
        ) {
            this.requestId = requestId;
            this.dateKey = dateKey;
            this.purpose = purpose;
            this.facility = facility;
            this.startDateText = startDateText;
            this.endDateText = endDateText;
            this.timeStartText = timeStartText;
            this.timeEndText = timeEndText;
            this.requestorName = requestorName;
            this.status = status;
        }
    }
}