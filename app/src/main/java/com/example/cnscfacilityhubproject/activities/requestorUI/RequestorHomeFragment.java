package com.example.cnscfacilityhubproject.activities.requestorUI;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.example.cnscfacilityhubproject.utils.RequestDataHelper;
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

public class RequestorHomeFragment extends Fragment {

    private TextView tvGreeting;
    private TextView tvRequestorName;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private TextView tvCalendarMonth;
    private GridLayout calendarGrid;
    private TextView tvSelectedDate;
    private TextView badgeNotification;
    private MaterialButton btnNewRequest;
    private LinearLayout schedsContainer;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private Calendar currentMonth;
    private Calendar selectedDate;

    private ListenerRegistration bookingsListener;
    private ListenerRegistration approvedNotificationListener;

    private final Map<String, Integer> bookedDatesMap = new HashMap<>();
    private final Map<String, List<BookingItem>> schedulesByDateMap = new HashMap<>();

    private static final int FULLY_BOOKED_LIMIT = 3;

    private TextView textNewRequest;
    private ImageView iconNewRequest;

    private TextView textHome;
    private ImageView iconHome;

    private String requestorNameLabel = "";

    private static final int COLOR_PRIMARY = Color.parseColor("#970705");
    private static final int COLOR_DEFAULT = Color.parseColor("#313131");

    public RequestorHomeFragment() {
        super(R.layout.fragment_requestor_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        currentMonth = Calendar.getInstance();
        selectedDate = Calendar.getInstance();

        bindViews(view);
        setGreetingByTime();
        loadHeaderRequestorInfo();
        setupCalendarNavigation();
        setupActions();

        updateSelectedDateLabel();
        listenForScheduledBookings();
        updateSchedsSection();

        textNewRequest = requireActivity().findViewById(R.id.textRequest);
        iconNewRequest = requireActivity().findViewById(R.id.iconRequest);

        textHome = requireActivity().findViewById(R.id.textHome);
        iconHome = requireActivity().findViewById(R.id.iconHome);
    }

    private void bindViews(View view) {
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvRequestorName = view.findViewById(R.id.tvRequestorName);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        tvCalendarMonth = view.findViewById(R.id.tvCalendarMonth);
        calendarGrid = view.findViewById(R.id.calendarGrid);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        btnNewRequest = view.findViewById(R.id.btnNewRequest);
        schedsContainer = view.findViewById(R.id.Scheds);

        textNewRequest = requireActivity().findViewById(R.id.textRequest);
        iconNewRequest = requireActivity().findViewById(R.id.iconRequest);

        if (tvRequestorName.getText() != null) {
            requestorNameLabel = tvRequestorName.getText().toString().trim();
        }
    }

    private void setGreetingByTime() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour < 12) {
            tvGreeting.setText("Good Morning");
        } else if (hour < 18) {
            tvGreeting.setText("Good Afternoon");
        } else {
            tvGreeting.setText("Good Evening");
        }
    }

    private void loadHeaderRequestorInfo() {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        String label = tvRequestorName.getText().toString().trim();

        if (!label.endsWith(",")) {
            label = "Hello,";
        }

        final String prefix = label + " ";

        SharedPreferences prefs = requireContext().getSharedPreferences("user_cache", Context.MODE_PRIVATE);
        String cachedFullName = prefs.getString("fullName_" + userId, "");

        if (cachedFullName != null && !cachedFullName.trim().isEmpty()) {
            tvRequestorName.setText(prefix + cachedFullName.trim());
        }

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;

                    String fullName = documentSnapshot.getString("fullName");

                    if (fullName != null && !fullName.trim().isEmpty()) {
                        String cleanName = fullName.trim();

                        prefs.edit()
                                .putString("fullName_" + userId, cleanName)
                                .apply();

                        tvRequestorName.setText(prefix + cleanName);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                });
    }

    private void setupCalendarNavigation() {
        btnPrevMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            renderCalendar();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            renderCalendar();
        });
    }

    private void setupActions() {
        if (btnNewRequest != null) {
            btnNewRequest.setOnClickListener(v -> {
                selectNewRequestTab();
                openRequestorRequestFragment();
            });
        }
    }

    private void selectNewRequestTab() {
        resetNavigationTabs();

        if (textNewRequest != null) {
            textNewRequest.setTextColor(COLOR_PRIMARY);
        }

        if (iconNewRequest != null) {
            iconNewRequest.setImageTintList(ColorStateList.valueOf(COLOR_PRIMARY));
        }
    }

    private void resetNavigationTabs() {
        if (textHome != null) {
            textHome.setTextColor(COLOR_DEFAULT);
        }

        if (iconHome != null) {
            iconHome.setImageTintList(ColorStateList.valueOf(COLOR_DEFAULT));
        }

        if (textNewRequest != null) {
            textNewRequest.setTextColor(COLOR_DEFAULT);
        }

        if (iconNewRequest != null) {
            iconNewRequest.setImageTintList(ColorStateList.valueOf(COLOR_DEFAULT));
        }
    }

    private void openRequestorRequestFragment() {
        try {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new RequestorRequestFragment())
                    .addToBackStack(null)
                    .commit();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open Request form.", Toast.LENGTH_SHORT).show();
        }
    }

    private void listenForScheduledBookings() {
        if (bookingsListener != null) {
            bookingsListener.remove();
        }

        /*
         * Calendar display logic:
         *
         * Newly booked by current user:
         * - Show in calendar
         * - Chip status = Pending
         * - Chip color = gray
         *
         * GSO Approved:
         * - Show in calendar
         * - Chip status = Approved
         * - Chip color = green
         *
         * GSO Returned:
         * - Do not show in calendar
         *
         * ITSO marked Available / Not Available:
         * - Show in calendar
         * - Chip status = Pending
         * - Chip color = gray
         */
        bookingsListener = db.collection("requests")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null) {
                        Toast.makeText(requireContext(), "Failed to load schedules.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    bookedDatesMap.clear();
                    schedulesByDateMap.clear();

                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            if (!RequestDataHelper.shouldShowInRequestList(doc)) {
                                continue;
                            }

                            if (!shouldShowRequestInCalendar(doc)) {
                                continue;
                            }

                            List<String> dateKeys = getDateKeysFromDocument(doc);

                            for (String dateKey : dateKeys) {
                                if (dateKey == null || dateKey.trim().isEmpty()) {
                                    continue;
                                }

                                BookingItem item = createBookingItemFromDocument(doc, dateKey);

                                if (item == null) {
                                    continue;
                                }

                                int currentCount = bookedDatesMap.containsKey(dateKey)
                                        ? bookedDatesMap.get(dateKey)
                                        : 0;

                                bookedDatesMap.put(dateKey, currentCount + 1);

                                List<BookingItem> itemsForDate = schedulesByDateMap.get(dateKey);

                                if (itemsForDate == null) {
                                    itemsForDate = new ArrayList<>();
                                    schedulesByDateMap.put(dateKey, itemsForDate);
                                }

                                itemsForDate.add(item);
                            }
                        }
                    }

                    sortSchedulesByTime();
                    renderCalendar();
                    updateSchedsSection();
                });
    }

    private boolean shouldShowRequestInCalendar(DocumentSnapshot doc) {
        String status = getDocumentString(doc, "status");
        String workflowStage = getDocumentString(doc, "workflowStage");
        String gsoStatus = getDocumentString(doc, "gsoStatus");
        String approvedBy = getDocumentString(doc, "approvedBy");
        String bookingStatus = getDocumentString(doc, "bookingStatus");

        if (isGsoReturnedOrBlocked(status, workflowStage, gsoStatus, approvedBy, bookingStatus)) {
            return false;
        }

        if (isGsoApprovedCalendarBooking(status, workflowStage, gsoStatus, approvedBy, bookingStatus)) {
            return true;
        }

        if (isNewUserPendingBooking(doc)) {
            return true;
        }

        return isItsoAvailabilityMarked(doc);
    }

    private String getRequestorScheduleDisplayStatus(DocumentSnapshot doc) {
        String status = getDocumentString(doc, "status");
        String workflowStage = getDocumentString(doc, "workflowStage");
        String gsoStatus = getDocumentString(doc, "gsoStatus");
        String approvedBy = getDocumentString(doc, "approvedBy");
        String bookingStatus = getDocumentString(doc, "bookingStatus");

        if (isGsoApprovedCalendarBooking(status, workflowStage, gsoStatus, approvedBy, bookingStatus)) {
            return "Approved";
        }

        return "Pending";
    }

    private boolean isGsoApprovedCalendarBooking(
            String status,
            String workflowStage,
            String gsoStatus,
            String approvedBy,
            String bookingStatus
    ) {
        String cleanStatus = normalizeStatus(status);
        String cleanStage = normalizeStatus(workflowStage);
        String cleanGsoStatus = normalizeStatus(gsoStatus);
        String cleanApprovedBy = normalizeStatus(approvedBy);
        String cleanBookingStatus = normalizeStatus(bookingStatus);

        boolean booked = "BOOKED".equals(cleanBookingStatus);

        boolean approvedStatus =
                "APPROVED".equals(cleanStatus)
                        || "APPROVED".equals(cleanGsoStatus)
                        || "GSO_APPROVED".equals(cleanStage)
                        || booked;

        boolean approvedByGso =
                "GSO".equals(cleanApprovedBy)
                        || "APPROVED".equals(cleanGsoStatus)
                        || "GSO_APPROVED".equals(cleanStage)
                        || booked;

        return approvedStatus && approvedByGso;
    }

    private boolean isGsoReturnedOrBlocked(
            String status,
            String workflowStage,
            String gsoStatus,
            String approvedBy,
            String bookingStatus
    ) {
        String cleanStatus = normalizeStatus(status);
        String cleanStage = normalizeStatus(workflowStage);
        String cleanGsoStatus = normalizeStatus(gsoStatus);
        String cleanApprovedBy = normalizeStatus(approvedBy);
        String cleanBookingStatus = normalizeStatus(bookingStatus);

        if (isBlockedGsoStatus(cleanStage)
                || isBlockedGsoStatus(cleanGsoStatus)
                || isBlockedGsoStatus(cleanBookingStatus)) {
            return true;
        }

        if ("RETURNED".equals(cleanStatus)
                || "REJECTED".equals(cleanStatus)
                || "DECLINED".equals(cleanStatus)
                || "CANCELLED".equals(cleanStatus)) {
            return true;
        }

        return "GSO".equals(cleanApprovedBy) && isBlockedGsoStatus(cleanStatus);
    }

    private boolean isBlockedGsoStatus(String value) {
        String clean = normalizeStatus(value);

        return "RETURNED".equals(clean)
                || "REJECTED".equals(clean)
                || "DECLINED".equals(clean)
                || "CANCELLED".equals(clean)
                || "GSO_RETURNED".equals(clean)
                || "GSO_REJECTED".equals(clean)
                || "GSO_DECLINED".equals(clean)
                || "GSO_CANCELLED".equals(clean);
    }

    private boolean isNewUserPendingBooking(DocumentSnapshot doc) {
        if (doc == null) {
            return false;
        }

        List<String> dateKeys = getDateKeysFromDocument(doc);

        if (dateKeys == null || dateKeys.isEmpty()) {
            return false;
        }

        if (!isCurrentUserRequest(doc)) {
            return false;
        }

        String status = getDocumentString(doc, "status");
        String workflowStage = getDocumentString(doc, "workflowStage");
        String bookingStatus = getDocumentString(doc, "bookingStatus");
        String gsoStatus = getDocumentString(doc, "gsoStatus");
        String itsoStatus = getDocumentString(doc, "itsoStatus");

        return isPendingBookingValue(status)
                || isPendingBookingValue(workflowStage)
                || isPendingBookingValue(bookingStatus)
                || isPendingBookingValue(gsoStatus)
                || isPendingBookingValue(itsoStatus)
                || areAllStatusFieldsEmpty(status, workflowStage, bookingStatus, gsoStatus, itsoStatus);
    }

    private boolean isPendingBookingValue(String value) {
        String clean = normalizeStatus(value);

        return "PENDING".equals(clean)
                || "SUBMITTED".equals(clean)
                || "REQUESTED".equals(clean)
                || "FOR_REVIEW".equals(clean)
                || "FOR_ITSO_REVIEW".equals(clean)
                || "FOR_GSO_REVIEW".equals(clean)
                || "ITSO_PENDING".equals(clean)
                || "GSO_PENDING".equals(clean)
                || "WAITING".equals(clean)
                || "WAITING_FOR_APPROVAL".equals(clean);
    }

    private boolean areAllStatusFieldsEmpty(String... values) {
        if (values == null) {
            return true;
        }

        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private boolean isCurrentUserRequest(DocumentSnapshot doc) {
        if (auth == null || auth.getCurrentUser() == null || doc == null) {
            return false;
        }

        String currentUid = auth.getCurrentUser().getUid();
        String currentEmail = auth.getCurrentUser().getEmail();

        String requestorId = getDocumentString(doc, "requestorId");
        String requestorUid = getDocumentString(doc, "requestorUid");
        String userId = getDocumentString(doc, "userId");
        String uid = getDocumentString(doc, "uid");
        String createdBy = getDocumentString(doc, "createdBy");
        String createdByUid = getDocumentString(doc, "createdByUid");
        String submittedBy = getDocumentString(doc, "submittedBy");
        String submittedByUid = getDocumentString(doc, "submittedByUid");

        String requestorEmail = getDocumentString(doc, "requestorEmail");
        String userEmail = getDocumentString(doc, "userEmail");
        String email = getDocumentString(doc, "email");
        String createdByEmail = getDocumentString(doc, "createdByEmail");
        String submittedByEmail = getDocumentString(doc, "submittedByEmail");

        if (currentUid != null && !currentUid.trim().isEmpty()) {
            if (currentUid.equals(requestorId)
                    || currentUid.equals(requestorUid)
                    || currentUid.equals(userId)
                    || currentUid.equals(uid)
                    || currentUid.equals(createdBy)
                    || currentUid.equals(createdByUid)
                    || currentUid.equals(submittedBy)
                    || currentUid.equals(submittedByUid)) {
                return true;
            }
        }

        if (currentEmail != null && !currentEmail.trim().isEmpty()) {
            if (currentEmail.equalsIgnoreCase(requestorEmail)
                    || currentEmail.equalsIgnoreCase(userEmail)
                    || currentEmail.equalsIgnoreCase(email)
                    || currentEmail.equalsIgnoreCase(createdByEmail)
                    || currentEmail.equalsIgnoreCase(submittedByEmail)) {
                return true;
            }
        }

        return false;
    }

    private boolean isItsoAvailabilityMarked(DocumentSnapshot doc) {
        return isAvailabilityValue(getDocumentString(doc, "itsoAvailability"))
                || isAvailabilityValue(getDocumentString(doc, "itsoStatus"))
                || isAvailabilityValue(getDocumentString(doc, "availabilityStatus"))
                || isAvailabilityValue(getDocumentString(doc, "availability"))
                || isAvailabilityValue(getDocumentString(doc, "gsoAvailability"));
    }

    private boolean isAvailabilityValue(String value) {
        String clean = normalizeStatus(value);

        return "AVAILABLE".equals(clean)
                || "NOT_AVAILABLE".equals(clean)
                || "NOTAVAILABLE".equals(clean)
                || "UNAVAILABLE".equals(clean);
    }

    private String normalizeStatus(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .replace("-", "_")
                .replace(" ", "_")
                .toUpperCase(Locale.ROOT);
    }

    private BookingItem createBookingItemFromDocument(DocumentSnapshot doc, String dateKey) {
        if (dateKey == null || dateKey.trim().isEmpty()) {
            return null;
        }

        String purpose = getSafeString(getDocumentString(doc, "purpose"), "Purpose / Activity");
        String facility = getSafeString(getDocumentString(doc, "finalFacilityName"), "");

        if (facility.isEmpty()) {
            String rawFacility = getSafeString(getDocumentString(doc, "facility"), "Facility");
            String otherFacility = getSafeString(getDocumentString(doc, "otherFacility"), "");

            if ("Others".equalsIgnoreCase(rawFacility) && !otherFacility.isEmpty()) {
                facility = otherFacility;
            } else {
                facility = rawFacility;
            }
        }

        String startDateText = getSafeString(getDocumentString(doc, "startDateText"), "");
        String endDateText = getSafeString(getDocumentString(doc, "endDateText"), "");
        String timeStartText = getSafeString(getDocumentString(doc, "timeStartText"), "");
        String timeEndText = getSafeString(getDocumentString(doc, "timeEndText"), "");

        Map<String, Object> scheduleDay = getScheduleDayForDate(doc, dateKey);

        if (scheduleDay != null) {
            String dayDateText = getMapString(scheduleDay, "dateText");

            String dayStartTime = firstNonEmpty(
                    getMapString(scheduleDay, "startTimeText"),
                    getMapString(scheduleDay, "timeStartText"),
                    getMapString(scheduleDay, "startTime")
            );

            String dayEndTime = firstNonEmpty(
                    getMapString(scheduleDay, "endTimeText"),
                    getMapString(scheduleDay, "timeEndText"),
                    getMapString(scheduleDay, "endTime")
            );

            if (!dayDateText.isEmpty()) {
                startDateText = dayDateText;
                endDateText = dayDateText;
            }

            if (!dayStartTime.isEmpty()) {
                timeStartText = dayStartTime;
            }

            if (!dayEndTime.isEmpty()) {
                timeEndText = dayEndTime;
            }
        }

        String status = getRequestorScheduleDisplayStatus(doc);

        return new BookingItem(
                doc.getId(),
                dateKey,
                purpose,
                facility,
                startDateText,
                endDateText,
                timeStartText,
                timeEndText,
                status
        );
    }

    private Map<String, Object> getScheduleDayForDate(DocumentSnapshot doc, String targetDateKey) {
        Object rawScheduleDays = doc.get("scheduleDays");

        if (!(rawScheduleDays instanceof List<?>)) {
            return null;
        }

        List<?> scheduleDays = (List<?>) rawScheduleDays;

        for (Object item : scheduleDays) {
            if (!(item instanceof Map<?, ?>)) {
                continue;
            }

            Map<?, ?> rawMap = (Map<?, ?>) item;
            Map<String, Object> dayMap = new HashMap<>();

            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() != null) {
                    dayMap.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }

            String dateText = getMapString(dayMap, "dateText");
            String dateKey = convertDateTextToKey(dateText);

            if (targetDateKey.equals(dateKey)) {
                return dayMap;
            }
        }

        return null;
    }

    private List<String> getDateKeysFromDocument(DocumentSnapshot doc) {
        List<String> scheduleDayKeys = getDateKeysFromScheduleDays(doc);

        if (!scheduleDayKeys.isEmpty()) {
            return scheduleDayKeys;
        }

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

        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Calendar cursor = (Calendar) startCalendar.clone();

        while (!cursor.after(endCalendar)) {
            keys.add(keyFormat.format(cursor.getTime()));
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }

        return keys;
    }

    private List<String> getDateKeysFromScheduleDays(DocumentSnapshot doc) {
        List<String> keys = new ArrayList<>();

        Object rawScheduleDays = doc.get("scheduleDays");

        if (!(rawScheduleDays instanceof List<?>)) {
            return keys;
        }

        List<?> scheduleDays = (List<?>) rawScheduleDays;

        for (Object item : scheduleDays) {
            if (!(item instanceof Map<?, ?>)) {
                continue;
            }

            Map<?, ?> rawMap = (Map<?, ?>) item;

            String dateText = getMapString(rawMap, "dateText");
            String dateKey = convertDateTextToKey(dateText);

            if (!dateKey.isEmpty() && !keys.contains(dateKey)) {
                keys.add(dateKey);
            }
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

        String startDateText = getSafeString(getDocumentString(doc, "startDateText"), "");

        if (!startDateText.isEmpty()) {
            return parseDateTextToCalendar(startDateText);
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

        String endDateText = getSafeString(getDocumentString(doc, "endDateText"), "");

        if (!endDateText.isEmpty()) {
            return parseDateTextToCalendar(endDateText);
        }

        return null;
    }

    private Calendar parseDateTextToCalendar(String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return null;
        }

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

    private String convertDateTextToKey(String dateText) {
        Calendar calendar = parseDateTextToCalendar(dateText);

        if (calendar == null) {
            return "";
        }

        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return keyFormat.format(calendar.getTime());
    }

    private void startOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private void sortSchedulesByTime() {
        for (List<BookingItem> list : schedulesByDateMap.values()) {
            Collections.sort(list, (a, b) -> {
                long timeA = parseTimeToMillis(a.timeStartText);
                long timeB = parseTimeToMillis(b.timeStartText);
                return Long.compare(timeA, timeB);
            });
        }
    }

    private long parseTimeToMillis(String timeText) {
        if (timeText == null || timeText.trim().isEmpty()) {
            return Long.MAX_VALUE;
        }

        String cleanTime = timeText.trim();
        String[] patterns = {"hh:mm a", "h:mm a", "HH:mm"};

        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
                java.util.Date date = sdf.parse(cleanTime);

                if (date != null) {
                    return date.getTime();
                }
            } catch (Exception ignored) {
            }
        }

        return Long.MAX_VALUE;
    }

    private void renderCalendar() {
        if (!isAdded()) return;

        calendarGrid.removeAllViews();

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvCalendarMonth.setText(monthFormat.format(currentMonth.getTime()));

        Calendar calendar = (Calendar) currentMonth.clone();
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

            Calendar dayCalendar = (Calendar) currentMonth.clone();
            dayCalendar.set(Calendar.DAY_OF_MONTH, day);

            boolean isToday = isSameDate(dayCalendar, Calendar.getInstance());
            boolean isSelected = isSameDate(dayCalendar, selectedDate);

            String dateKey = keyFormat.format(dayCalendar.getTime());
            int bookings = bookedDatesMap.containsKey(dateKey) ? bookedDatesMap.get(dateKey) : 0;

            if (isSelected) {
                dayView.setBackgroundColor(Color.parseColor("#313131"));
                dayView.setTextColor(Color.WHITE);
            } else if (bookings >= FULLY_BOOKED_LIMIT) {
                dayView.setBackgroundColor(Color.parseColor("#970705"));
                dayView.setTextColor(Color.WHITE);
            } else if (bookings > 0) {
                dayView.setBackgroundColor(Color.parseColor("#F3D9D9"));
                dayView.setTextColor(Color.parseColor("#313131"));
            } else if (isToday) {
                dayView.setBackgroundColor(Color.parseColor("#FFF0D8"));
                dayView.setTextColor(Color.parseColor("#313131"));
            } else {
                dayView.setBackgroundColor(Color.TRANSPARENT);
                dayView.setTextColor(Color.parseColor("#313131"));
            }

            dayView.setOnClickListener(v -> {
                selectedDate = (Calendar) dayCalendar.clone();
                updateSelectedDateLabel();
                renderCalendar();
                updateSchedsSection();
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

    private void updateSelectedDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        tvSelectedDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void updateSchedsSection() {
        if (!isAdded()) return;

        updateSelectedDateLabel();

        schedsContainer.removeAllViews();

        if (tvSelectedDate.getParent() != null) {
            ((ViewGroup) tvSelectedDate.getParent()).removeView(tvSelectedDate);
        }

        tvSelectedDate.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        schedsContainer.addView(tvSelectedDate);

        String selectedDateKey = getDateKeyFromCalendar(selectedDate);
        List<BookingItem> schedules = schedulesByDateMap.get(selectedDateKey);

        if (schedules == null || schedules.isEmpty()) {
            schedsContainer.addView(createEmptyScheduleView());
            return;
        }

        for (BookingItem item : schedules) {
            schedsContainer.addView(createScheduleCard(item));
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
        emptyView.setText("No scheduled bookings for this date.");
        emptyView.setTextColor(Color.parseColor("#313131"));
        emptyView.setTextSize(14f);
        emptyView.setAlpha(0.72f);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(dp(12), dp(18), dp(12), dp(18));

        return emptyView;
    }

    private View createScheduleCard(BookingItem item) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(16), 0, 0);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        row.setLayoutParams(rowParams);

        String timeBadge = getTimeBadgeText(item.timeStartText);

        if (!timeBadge.isEmpty()) {
            MaterialCardView timeCard = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams timeCardParams = new LinearLayout.LayoutParams(dp(54), dp(54));
            timeCard.setLayoutParams(timeCardParams);
            timeCard.setCardBackgroundColor(Color.parseColor("#970705"));
            timeCard.setRadius(dp(18));
            timeCard.setCardElevation(0f);

            TextView timeText = new TextView(requireContext());
            timeText.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            timeText.setGravity(Gravity.CENTER);
            timeText.setText(timeBadge);
            timeText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            timeText.setTextColor(Color.WHITE);
            timeText.setTextSize(12f);
            timeText.setTypeface(null, android.graphics.Typeface.BOLD);

            timeCard.addView(timeText);
            row.addView(timeCard);
        }

        LinearLayout detailsLayout = new LinearLayout(requireContext());
        detailsLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        detailsParams.setMargins(timeBadge.isEmpty() ? 0 : dp(14), 0, dp(10), 0);
        detailsLayout.setLayoutParams(detailsParams);

        String title = firstNonEmpty(item.purpose, item.facility);

        if (!title.isEmpty()) {
            TextView titleText = new TextView(requireContext());
            titleText.setText(title);
            titleText.setTextColor(Color.parseColor("#313131"));
            titleText.setTextSize(15f);
            titleText.setTypeface(null, android.graphics.Typeface.BOLD);
            titleText.setSingleLine(true);
            titleText.setEllipsize(TextUtils.TruncateAt.END);
            detailsLayout.addView(titleText);
        }

        String facilityMeta = buildFacilityMeta(item, title);

        if (!facilityMeta.isEmpty()) {
            detailsLayout.addView(createScheduleInfoText(facilityMeta, false));
        }

        String dateRange = buildDateRange(item.startDateText, item.endDateText);

        if (!dateRange.isEmpty()) {
            detailsLayout.addView(createScheduleInfoText(dateRange, true));
        }

        String timeRange = buildTimeRange(item.timeStartText, item.timeEndText);

        if (!timeRange.isEmpty()) {
            detailsLayout.addView(createScheduleInfoText(timeRange, true));
        }

        row.addView(detailsLayout);

        if (item.status != null && !item.status.trim().isEmpty()) {
            Chip statusChip = new Chip(requireContext());

            statusChip.setText(item.status);
            statusChip.setTextSize(11f);

            applyFixedStatusChipSize(statusChip);

            statusChip.setTextColor(getStatusTextColor(item.status));
            statusChip.setChipBackgroundColor(ColorStateList.valueOf(getStatusBackgroundColor(item.status)));
            statusChip.setChipStrokeWidth(0);
            statusChip.setCheckable(false);
            statusChip.setClickable(true);
            statusChip.setFocusable(true);
            statusChip.setOnClickListener(v -> openRequestDetailsFromSchedule(item));

            row.addView(statusChip);
        }

        return row;
    }

    private void openRequestDetailsFromSchedule(BookingItem item) {
        if (!isAdded() || item == null || item.requestId == null || item.requestId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Request details not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            RequestorRequestDetailsFragment.newInstance(
                                    item.requestId,
                                    true,
                                    true
                            )
                    )
                    .addToBackStack(null)
                    .commit();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open request details.", Toast.LENGTH_SHORT).show();
        }
    }

    private TextView createScheduleInfoText(String text, boolean importantLine) {
        TextView infoText = new TextView(requireContext());
        infoText.setText(text);
        infoText.setTextColor(Color.parseColor("#313131"));
        infoText.setTextSize(12f);
        infoText.setAlpha(importantLine ? 0.82f : 0.72f);
        infoText.setSingleLine(true);
        infoText.setEllipsize(TextUtils.TruncateAt.END);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        infoParams.setMargins(0, dp(4), 0, 0);
        infoText.setLayoutParams(infoParams);

        return infoText;
    }

    private String buildFacilityMeta(BookingItem item, String title) {
        String facility = item.facility == null ? "" : item.facility.trim();

        if (!facility.isEmpty() && !facility.equalsIgnoreCase(title)) {
            return facility;
        }

        return "";
    }

    private String buildDateRange(String startDate, String endDate) {
        String cleanStart = startDate == null ? "" : startDate.trim();
        String cleanEnd = endDate == null ? "" : endDate.trim();

        if (!cleanStart.isEmpty() && !cleanEnd.isEmpty() && !cleanStart.equalsIgnoreCase(cleanEnd)) {
            return cleanStart + " - " + cleanEnd;
        }

        return !cleanStart.isEmpty() ? cleanStart : cleanEnd;
    }

    private String buildTimeRange(String startTime, String endTime) {
        String cleanStart = startTime == null ? "" : startTime.trim();
        String cleanEnd = endTime == null ? "" : endTime.trim();

        if (!cleanStart.isEmpty() && !cleanEnd.isEmpty()) {
            return cleanStart + " - " + cleanEnd;
        }

        return !cleanStart.isEmpty() ? cleanStart : cleanEnd;
    }

    private int getStatusTextColor(String status) {
        if ("Approved".equalsIgnoreCase(status)) {
            return Color.parseColor("#2E7D32");
        }

        if ("Pending".equalsIgnoreCase(status)) {
            return Color.parseColor("#313131");
        }

        if ("Returned".equalsIgnoreCase(status)
                || "Rejected".equalsIgnoreCase(status)
                || "Not Available".equalsIgnoreCase(status)
                || "Unavailable".equalsIgnoreCase(status)) {
            return Color.parseColor("#970705");
        }

        return Color.parseColor("#313131");
    }

    private int getStatusBackgroundColor(String status) {
        if ("Approved".equalsIgnoreCase(status)) {
            return Color.parseColor("#E7F4E8");
        }

        if ("Pending".equalsIgnoreCase(status)) {
            return Color.parseColor("#EEEEEE");
        }

        if ("Returned".equalsIgnoreCase(status)
                || "Rejected".equalsIgnoreCase(status)
                || "Not Available".equalsIgnoreCase(status)
                || "Unavailable".equalsIgnoreCase(status)) {
            return Color.parseColor("#F3D9D9");
        }

        return Color.parseColor("#EEEEEE");
    }

    private void applyFixedStatusChipSize(Chip statusChip) {
        if (statusChip == null) return;

        int chipWidth = dp(104);
        int chipHeight = dp(34);

        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(chipWidth, chipHeight);
        chipParams.setMargins(dp(4), 0, 0, 0);
        statusChip.setLayoutParams(chipParams);

        statusChip.setWidth(chipWidth);
        statusChip.setMinWidth(chipWidth);
        statusChip.setMaxWidth(chipWidth);

        statusChip.setHeight(chipHeight);
        statusChip.setMinHeight(chipHeight);
        statusChip.setMinimumHeight(chipHeight);
        statusChip.setChipMinHeight(chipHeight);

        statusChip.setEnsureMinTouchTargetSize(false);

        statusChip.setSingleLine(true);
        statusChip.setMaxLines(1);
        statusChip.setEllipsize(TextUtils.TruncateAt.END);
        statusChip.setGravity(Gravity.CENTER);
        statusChip.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        statusChip.setIncludeFontPadding(false);

        statusChip.setChipStartPadding(0f);
        statusChip.setChipEndPadding(0f);
        statusChip.setTextStartPadding(0f);
        statusChip.setTextEndPadding(0f);
        statusChip.setPadding(0, 0, 0, 0);
    }

    private String getTimeBadgeText(String timeStartText) {
        if (timeStartText == null || timeStartText.trim().isEmpty()) {
            return "";
        }

        String cleanTime = timeStartText.trim();
        String[] patterns = {"hh:mm a", "h:mm a", "HH:mm"};

        for (String pattern : patterns) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat(pattern, Locale.getDefault());
                SimpleDateFormat hourFormat = new SimpleDateFormat("hh", Locale.getDefault());
                SimpleDateFormat amPmFormat = new SimpleDateFormat("a", Locale.getDefault());

                java.util.Date parsedTime = inputFormat.parse(cleanTime);

                if (parsedTime != null) {
                    return hourFormat.format(parsedTime) + "\n" + amPmFormat.format(parsedTime);
                }
            } catch (Exception ignored) {
            }
        }

        String[] parts = cleanTime.split("\\s+");

        if (parts.length >= 2) {
            String hour = parts[0].contains(":") ? parts[0].split(":")[0] : parts[0];
            return hour + "\n" + parts[1];
        }

        return cleanTime;
    }

    private String getDateKeyFromCalendar(Calendar calendar) {
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return keyFormat.format(calendar.getTime());
    }

    private boolean isSameDate(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
                && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    private String getDocumentString(DocumentSnapshot doc, String field) {
        if (doc == null || field == null) {
            return "";
        }

        Object value = doc.get(field);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String getMapString(Map<?, ?> map, String key) {
        if (map == null || key == null) {
            return "";
        }

        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean getBooleanValue(DocumentSnapshot doc, String field) {
        if (doc == null || field == null) {
            return false;
        }

        Object value = doc.get(field);

        if (value instanceof Boolean) {
            return Boolean.TRUE.equals(value);
        }

        if (value instanceof String) {
            return "true".equalsIgnoreCase(((String) value).trim());
        }

        return false;
    }

    private String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }

        return "";
    }

    private String getSafeString(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (bookingsListener != null) {
            bookingsListener.remove();
            bookingsListener = null;
        }

        if (approvedNotificationListener != null) {
            approvedNotificationListener.remove();
            approvedNotificationListener = null;
        }
    }

    private static class BookingItem {
        String requestId;
        String dateKey;
        String purpose;
        String facility;
        String startDateText;
        String endDateText;
        String timeStartText;
        String timeEndText;
        String status;

        BookingItem(
                String requestId,
                String dateKey,
                String purpose,
                String facility,
                String startDateText,
                String endDateText,
                String timeStartText,
                String timeEndText,
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
            this.status = status;
        }
    }
}


//
//
//package com.example.cnscfacilityhubproject.activities.requestorUI;
//
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.content.res.ColorStateList;
//import android.graphics.Color;
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.view.Gravity;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.GridLayout;
//import android.widget.ImageButton;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//
//import com.example.cnscfacilityhubproject.R;
//import com.example.cnscfacilityhubproject.utils.RequestDataHelper;
//import com.google.android.material.button.MaterialButton;
//import com.google.android.material.card.MaterialCardView;
//import com.google.android.material.chip.Chip;
//import com.google.firebase.Timestamp;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.ListenerRegistration;
//import com.google.firebase.firestore.QueryDocumentSnapshot;
//
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//
//public class RequestorHomeFragment extends Fragment {
//
//    private TextView tvGreeting;
//    private TextView tvRequestorName;
//    private ImageButton btnPrevMonth;
//    private ImageButton btnNextMonth;
//    private TextView tvCalendarMonth;
//    private GridLayout calendarGrid;
//    private TextView tvSelectedDate;
//    private TextView badgeNotification;
//    private MaterialButton btnNewRequest;
//    private LinearLayout schedsContainer;
//
//    private FirebaseAuth auth;
//    private FirebaseFirestore db;
//
//    private Calendar currentMonth;
//    private Calendar selectedDate;
//
//    private ListenerRegistration bookingsListener;
//    private ListenerRegistration approvedNotificationListener;
//
//    private final Map<String, Integer> bookedDatesMap = new HashMap<>();
//    private final Map<String, List<BookingItem>> schedulesByDateMap = new HashMap<>();
//
//    private static final int FULLY_BOOKED_LIMIT = 3;
//
//    private TextView textNewRequest;
//    private ImageView iconNewRequest;
//
//    private TextView textHome;
//    private ImageView iconHome;
//
//    private String requestorNameLabel = "";
//
//    private static final int COLOR_PRIMARY = Color.parseColor("#970705");
//    private static final int COLOR_DEFAULT = Color.parseColor("#313131");
//
//    public RequestorHomeFragment() {
//        super(R.layout.fragment_requestor_home);
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        auth = FirebaseAuth.getInstance();
//        db = FirebaseFirestore.getInstance();
//
//        currentMonth = Calendar.getInstance();
//        selectedDate = Calendar.getInstance();
//
//        bindViews(view);
//        setGreetingByTime();
//        loadHeaderRequestorInfo();
//        setupCalendarNavigation();
//        setupActions();
//
//        updateSelectedDateLabel();
//        listenForScheduledBookings();
//        updateSchedsSection();
//
//        textNewRequest = requireActivity().findViewById(R.id.textRequest);
//        iconNewRequest = requireActivity().findViewById(R.id.iconRequest);
//
//        textHome = requireActivity().findViewById(R.id.textHome);
//        iconHome = requireActivity().findViewById(R.id.iconHome);
//    }
//
//    private void bindViews(View view) {
//        tvGreeting = view.findViewById(R.id.tvGreeting);
//        tvRequestorName = view.findViewById(R.id.tvRequestorName);
//        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
//        btnNextMonth = view.findViewById(R.id.btnNextMonth);
//        tvCalendarMonth = view.findViewById(R.id.tvCalendarMonth);
//        calendarGrid = view.findViewById(R.id.calendarGrid);
//        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
//        btnNewRequest = view.findViewById(R.id.btnNewRequest);
//        schedsContainer = view.findViewById(R.id.Scheds);
//
//        textNewRequest = requireActivity().findViewById(R.id.textRequest);
//        iconNewRequest = requireActivity().findViewById(R.id.iconRequest);
//
//        if (tvRequestorName.getText() != null) {
//            requestorNameLabel = tvRequestorName.getText().toString().trim();
//        }
//    }
//
//    private void setGreetingByTime() {
//        Calendar calendar = Calendar.getInstance();
//        int hour = calendar.get(Calendar.HOUR_OF_DAY);
//
//        if (hour < 12) {
//            tvGreeting.setText("Good Morning");
//        } else if (hour < 18) {
//            tvGreeting.setText("Good Afternoon");
//        } else {
//            tvGreeting.setText("Good Evening");
//        }
//    }
//
//    private void loadHeaderRequestorInfo() {
//        if (auth.getCurrentUser() == null) {
//            return;
//        }
//
//        String userId = auth.getCurrentUser().getUid();
//        String label = tvRequestorName.getText().toString().trim();
//
//        if (!label.endsWith(",")) {
//            label = "Hello,";
//        }
//
//        final String prefix = label + " ";
//
//        SharedPreferences prefs = requireContext().getSharedPreferences("user_cache", Context.MODE_PRIVATE);
//        String cachedFullName = prefs.getString("fullName_" + userId, "");
//
//        if (cachedFullName != null && !cachedFullName.trim().isEmpty()) {
//            tvRequestorName.setText(prefix + cachedFullName.trim());
//        }
//
//        db.collection("users")
//                .document(userId)
//                .get()
//                .addOnSuccessListener(documentSnapshot -> {
//                    if (!isAdded()) return;
//
//                    String fullName = documentSnapshot.getString("fullName");
//
//                    if (fullName != null && !fullName.trim().isEmpty()) {
//                        String cleanName = fullName.trim();
//
//                        prefs.edit()
//                                .putString("fullName_" + userId, cleanName)
//                                .apply();
//
//                        tvRequestorName.setText(prefix + cleanName);
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    if (!isAdded()) return;
//                });
//    }
//
//    private void setupCalendarNavigation() {
//        btnPrevMonth.setOnClickListener(v -> {
//            currentMonth.add(Calendar.MONTH, -1);
//            renderCalendar();
//        });
//
//        btnNextMonth.setOnClickListener(v -> {
//            currentMonth.add(Calendar.MONTH, 1);
//            renderCalendar();
//        });
//    }
//
//    private void setupActions() {
//        if (btnNewRequest != null) {
//            btnNewRequest.setOnClickListener(v -> {
//                selectNewRequestTab();
//                openRequestorRequestFragment();
//            });
//        }
//    }
//
//    private void selectNewRequestTab() {
//        resetNavigationTabs();
//
//        if (textNewRequest != null) {
//            textNewRequest.setTextColor(COLOR_PRIMARY);
//        }
//
//        if (iconNewRequest != null) {
//            iconNewRequest.setImageTintList(ColorStateList.valueOf(COLOR_PRIMARY));
//        }
//    }
//
//    private void resetNavigationTabs() {
//        if (textHome != null) {
//            textHome.setTextColor(COLOR_DEFAULT);
//        }
//
//        if (iconHome != null) {
//            iconHome.setImageTintList(ColorStateList.valueOf(COLOR_DEFAULT));
//        }
//
//        if (textNewRequest != null) {
//            textNewRequest.setTextColor(COLOR_DEFAULT);
//        }
//
//        if (iconNewRequest != null) {
//            iconNewRequest.setImageTintList(ColorStateList.valueOf(COLOR_DEFAULT));
//        }
//    }
//
//    private void openRequestorRequestFragment() {
//        try {
//            requireActivity()
//                    .getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.fragment_container, new RequestorRequestFragment())
//                    .addToBackStack(null)
//                    .commit();
//        } catch (Exception e) {
//            Toast.makeText(requireContext(), "Unable to open Request form.", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private void listenForScheduledBookings() {
//        if (bookingsListener != null) {
//            bookingsListener.remove();
//        }
//
//        /*
//         * Calendar and Scheduled Request display logic:
//         *
//         * Show all valid scheduled requests, same behavior as GSO Home.
//         * Requests from different requestors are allowed to appear here.
//         *
//         * GSO Returned / Rejected / Cancelled:
//         * - Do not show in calendar or Scheduled Request section
//         */
//        bookingsListener = db.collection("requests")
//                .addSnapshotListener((snapshot, error) -> {
//                    if (!isAdded()) return;
//
//                    if (error != null) {
//                        Toast.makeText(requireContext(), "Failed to load schedules.", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    bookedDatesMap.clear();
//                    schedulesByDateMap.clear();
//
//                    if (snapshot != null) {
//                        for (QueryDocumentSnapshot doc : snapshot) {
//
//                            if (!RequestDataHelper.shouldShowInRequestList(doc)) {
//                                continue;
//                            }
//
//                            if (!shouldShowRequestInCalendar(doc)) {
//                                continue;
//                            }
//
//                            List<String> dateKeys = getDateKeysFromDocument(doc);
//
//                            for (String dateKey : dateKeys) {
//                                if (dateKey == null || dateKey.trim().isEmpty()) {
//                                    continue;
//                                }
//
//                                BookingItem item = createBookingItemFromDocument(doc, dateKey);
//
//                                if (item == null) {
//                                    continue;
//                                }
//
//                                int currentCount = bookedDatesMap.containsKey(dateKey)
//                                        ? bookedDatesMap.get(dateKey)
//                                        : 0;
//
//                                bookedDatesMap.put(dateKey, currentCount + 1);
//
//                                List<BookingItem> itemsForDate = schedulesByDateMap.get(dateKey);
//
//                                if (itemsForDate == null) {
//                                    itemsForDate = new ArrayList<>();
//                                    schedulesByDateMap.put(dateKey, itemsForDate);
//                                }
//
//                                itemsForDate.add(item);
//                            }
//                        }
//                    }
//
//                    sortSchedulesByTime();
//                    renderCalendar();
//                    updateSchedsSection();
//                });
//    }
//
//    private boolean shouldShowRequestInCalendar(DocumentSnapshot doc) {
//        if (doc == null) {
//            return false;
//        }
//
//        String status = getDocumentString(doc, "status");
//        String workflowStage = getDocumentString(doc, "workflowStage");
//        String gsoStatus = getDocumentString(doc, "gsoStatus");
//        String approvedBy = getDocumentString(doc, "approvedBy");
//        String bookingStatus = getDocumentString(doc, "bookingStatus");
//
//        if (isGsoReturnedOrBlocked(status, workflowStage, gsoStatus, approvedBy, bookingStatus)) {
//            return false;
//        }
//
//        if (isGsoApprovedCalendarBooking(status, workflowStage, gsoStatus, approvedBy, bookingStatus)) {
//            return true;
//        }
//
//        if (isNewUserPendingBooking(doc)) {
//            return true;
//        }
//
//        return isItsoAvailabilityMarked(doc);
//    }
//
//    private String getRequestorScheduleDisplayStatus(DocumentSnapshot doc) {
//        String status = getDocumentString(doc, "status");
//        String workflowStage = getDocumentString(doc, "workflowStage");
//        String gsoStatus = getDocumentString(doc, "gsoStatus");
//        String approvedBy = getDocumentString(doc, "approvedBy");
//        String bookingStatus = getDocumentString(doc, "bookingStatus");
//
//        if (isGsoApprovedCalendarBooking(status, workflowStage, gsoStatus, approvedBy, bookingStatus)) {
//            return "Approved";
//        }
//
//        return "Pending";
//    }
//
//    private boolean isGsoApprovedCalendarBooking(
//            String status,
//            String workflowStage,
//            String gsoStatus,
//            String approvedBy,
//            String bookingStatus
//    ) {
//        String cleanStatus = normalizeStatus(status);
//        String cleanStage = normalizeStatus(workflowStage);
//        String cleanGsoStatus = normalizeStatus(gsoStatus);
//        String cleanApprovedBy = normalizeStatus(approvedBy);
//        String cleanBookingStatus = normalizeStatus(bookingStatus);
//
//        boolean booked = "BOOKED".equals(cleanBookingStatus);
//
//        boolean approvedStatus =
//                "APPROVED".equals(cleanStatus)
//                        || "APPROVED".equals(cleanGsoStatus)
//                        || "GSO_APPROVED".equals(cleanStage)
//                        || booked;
//
//        boolean approvedByGso =
//                "GSO".equals(cleanApprovedBy)
//                        || "APPROVED".equals(cleanGsoStatus)
//                        || "GSO_APPROVED".equals(cleanStage)
//                        || booked;
//
//        return approvedStatus && approvedByGso;
//    }
//
//    private boolean isGsoReturnedOrBlocked(
//            String status,
//            String workflowStage,
//            String gsoStatus,
//            String approvedBy,
//            String bookingStatus
//    ) {
//        String cleanStatus = normalizeStatus(status);
//        String cleanStage = normalizeStatus(workflowStage);
//        String cleanGsoStatus = normalizeStatus(gsoStatus);
//        String cleanApprovedBy = normalizeStatus(approvedBy);
//        String cleanBookingStatus = normalizeStatus(bookingStatus);
//
//        if (isBlockedGsoStatus(cleanStage)
//                || isBlockedGsoStatus(cleanGsoStatus)
//                || isBlockedGsoStatus(cleanBookingStatus)) {
//            return true;
//        }
//
//        if ("RETURNED".equals(cleanStatus)
//                || "REJECTED".equals(cleanStatus)
//                || "DECLINED".equals(cleanStatus)
//                || "CANCELLED".equals(cleanStatus)) {
//            return true;
//        }
//
//        return "GSO".equals(cleanApprovedBy) && isBlockedGsoStatus(cleanStatus);
//    }
//
//    private boolean isBlockedGsoStatus(String value) {
//        String clean = normalizeStatus(value);
//
//        return "RETURNED".equals(clean)
//                || "REJECTED".equals(clean)
//                || "DECLINED".equals(clean)
//                || "CANCELLED".equals(clean)
//                || "GSO_RETURNED".equals(clean)
//                || "GSO_REJECTED".equals(clean)
//                || "GSO_DECLINED".equals(clean)
//                || "GSO_CANCELLED".equals(clean);
//    }
//
//    private boolean isNewUserPendingBooking(DocumentSnapshot doc) {
//        if (doc == null) {
//            return false;
//        }
//
//        List<String> dateKeys = getDateKeysFromDocument(doc);
//
//        if (dateKeys == null || dateKeys.isEmpty()) {
//            return false;
//        }
//        String status = getDocumentString(doc, "status");
//        String workflowStage = getDocumentString(doc, "workflowStage");
//        String bookingStatus = getDocumentString(doc, "bookingStatus");
//        String gsoStatus = getDocumentString(doc, "gsoStatus");
//        String itsoStatus = getDocumentString(doc, "itsoStatus");
//
//        return isPendingBookingValue(status)
//                || isPendingBookingValue(workflowStage)
//                || isPendingBookingValue(bookingStatus)
//                || isPendingBookingValue(gsoStatus)
//                || isPendingBookingValue(itsoStatus)
//                || areAllStatusFieldsEmpty(status, workflowStage, bookingStatus, gsoStatus, itsoStatus);
//    }
//
//    private boolean isPendingBookingValue(String value) {
//        String clean = normalizeStatus(value);
//
//        return "PENDING".equals(clean)
//                || "SUBMITTED".equals(clean)
//                || "REQUESTED".equals(clean)
//                || "FOR_REVIEW".equals(clean)
//                || "FOR_ITSO_REVIEW".equals(clean)
//                || "FOR_GSO_REVIEW".equals(clean)
//                || "ITSO_PENDING".equals(clean)
//                || "GSO_PENDING".equals(clean)
//                || "WAITING".equals(clean)
//                || "WAITING_FOR_APPROVAL".equals(clean);
//    }
//
//    private boolean areAllStatusFieldsEmpty(String... values) {
//        if (values == null) {
//            return true;
//        }
//
//        for (String value : values) {
//            if (value != null && !value.trim().isEmpty()) {
//                return false;
//            }
//        }
//
//        return true;
//    }
//
//    private boolean isCurrentUserRequest(DocumentSnapshot doc) {
//        if (auth == null || auth.getCurrentUser() == null || doc == null) {
//            return false;
//        }
//
//        String currentUid = auth.getCurrentUser().getUid();
//        String currentEmail = auth.getCurrentUser().getEmail();
//
//        String requestorId = getDocumentString(doc, "requestorId");
//        String requestorUid = getDocumentString(doc, "requestorUid");
//        String userId = getDocumentString(doc, "userId");
//        String uid = getDocumentString(doc, "uid");
//        String createdBy = getDocumentString(doc, "createdBy");
//        String createdByUid = getDocumentString(doc, "createdByUid");
//        String submittedBy = getDocumentString(doc, "submittedBy");
//        String submittedByUid = getDocumentString(doc, "submittedByUid");
//
//        String requestorEmail = getDocumentString(doc, "requestorEmail");
//        String userEmail = getDocumentString(doc, "userEmail");
//        String email = getDocumentString(doc, "email");
//        String createdByEmail = getDocumentString(doc, "createdByEmail");
//        String submittedByEmail = getDocumentString(doc, "submittedByEmail");
//
//        if (currentUid != null && !currentUid.trim().isEmpty()) {
//            if (currentUid.equals(requestorId)
//                    || currentUid.equals(requestorUid)
//                    || currentUid.equals(userId)
//                    || currentUid.equals(uid)
//                    || currentUid.equals(createdBy)
//                    || currentUid.equals(createdByUid)
//                    || currentUid.equals(submittedBy)
//                    || currentUid.equals(submittedByUid)) {
//                return true;
//            }
//        }
//
//        if (currentEmail != null && !currentEmail.trim().isEmpty()) {
//            if (currentEmail.equalsIgnoreCase(requestorEmail)
//                    || currentEmail.equalsIgnoreCase(userEmail)
//                    || currentEmail.equalsIgnoreCase(email)
//                    || currentEmail.equalsIgnoreCase(createdByEmail)
//                    || currentEmail.equalsIgnoreCase(submittedByEmail)) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//
//    private boolean isItsoAvailabilityMarked(DocumentSnapshot doc) {
//        return isAvailabilityValue(getDocumentString(doc, "itsoAvailability"))
//                || isAvailabilityValue(getDocumentString(doc, "itsoStatus"))
//                || isAvailabilityValue(getDocumentString(doc, "availabilityStatus"))
//                || isAvailabilityValue(getDocumentString(doc, "availability"))
//                || isAvailabilityValue(getDocumentString(doc, "gsoAvailability"));
//    }
//
//    private boolean isAvailabilityValue(String value) {
//        String clean = normalizeStatus(value);
//
//        return "AVAILABLE".equals(clean)
//                || "NOT_AVAILABLE".equals(clean)
//                || "NOTAVAILABLE".equals(clean)
//                || "UNAVAILABLE".equals(clean);
//    }
//
//    private String normalizeStatus(String value) {
//        if (value == null) {
//            return "";
//        }
//
//        return value.trim()
//                .replace("-", "_")
//                .replace(" ", "_")
//                .toUpperCase(Locale.ROOT);
//    }
//
//    private BookingItem createBookingItemFromDocument(DocumentSnapshot doc, String dateKey) {
//        if (dateKey == null || dateKey.trim().isEmpty()) {
//            return null;
//        }
//
//        String purpose = getSafeString(getDocumentString(doc, "purpose"), "Purpose / Activity");
//        String facility = getSafeString(getDocumentString(doc, "finalFacilityName"), "");
//
//        if (facility.isEmpty()) {
//            String rawFacility = getSafeString(getDocumentString(doc, "facility"), "Facility");
//            String otherFacility = getSafeString(getDocumentString(doc, "otherFacility"), "");
//
//            if ("Others".equalsIgnoreCase(rawFacility) && !otherFacility.isEmpty()) {
//                facility = otherFacility;
//            } else {
//                facility = rawFacility;
//            }
//        }
//
//        String startDateText = getSafeString(getDocumentString(doc, "startDateText"), "");
//        String endDateText = getSafeString(getDocumentString(doc, "endDateText"), "");
//        String timeStartText = getSafeString(getDocumentString(doc, "timeStartText"), "");
//        String timeEndText = getSafeString(getDocumentString(doc, "timeEndText"), "");
//
//        Map<String, Object> scheduleDay = getScheduleDayForDate(doc, dateKey);
//
//        if (scheduleDay != null) {
//            String dayDateText = getMapString(scheduleDay, "dateText");
//
//            String dayStartTime = firstNonEmpty(
//                    getMapString(scheduleDay, "startTimeText"),
//                    getMapString(scheduleDay, "timeStartText"),
//                    getMapString(scheduleDay, "startTime")
//            );
//
//            String dayEndTime = firstNonEmpty(
//                    getMapString(scheduleDay, "endTimeText"),
//                    getMapString(scheduleDay, "timeEndText"),
//                    getMapString(scheduleDay, "endTime")
//            );
//
//            if (!dayDateText.isEmpty()) {
//                startDateText = dayDateText;
//                endDateText = dayDateText;
//            }
//
//            if (!dayStartTime.isEmpty()) {
//                timeStartText = dayStartTime;
//            }
//
//            if (!dayEndTime.isEmpty()) {
//                timeEndText = dayEndTime;
//            }
//        }
//
//        String status = getRequestorScheduleDisplayStatus(doc);
//
//        return new BookingItem(
//                doc.getId(),
//                dateKey,
//                purpose,
//                facility,
//                startDateText,
//                endDateText,
//                timeStartText,
//                timeEndText,
//                status
//        );
//    }
//
//    private Map<String, Object> getScheduleDayForDate(DocumentSnapshot doc, String targetDateKey) {
//        Object rawScheduleDays = doc.get("scheduleDays");
//
//        if (!(rawScheduleDays instanceof List<?>)) {
//            return null;
//        }
//
//        List<?> scheduleDays = (List<?>) rawScheduleDays;
//
//        for (Object item : scheduleDays) {
//            if (!(item instanceof Map<?, ?>)) {
//                continue;
//            }
//
//            Map<?, ?> rawMap = (Map<?, ?>) item;
//            Map<String, Object> dayMap = new HashMap<>();
//
//            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
//                if (entry.getKey() != null) {
//                    dayMap.put(String.valueOf(entry.getKey()), entry.getValue());
//                }
//            }
//
//            String dateText = getMapString(dayMap, "dateText");
//            String dateKey = convertDateTextToKey(dateText);
//
//            if (targetDateKey.equals(dateKey)) {
//                return dayMap;
//            }
//        }
//
//        return null;
//    }
//
//    private List<String> getDateKeysFromDocument(DocumentSnapshot doc) {
//        List<String> scheduleDayKeys = getDateKeysFromScheduleDays(doc);
//
//        if (!scheduleDayKeys.isEmpty()) {
//            return scheduleDayKeys;
//        }
//
//        List<String> keys = new ArrayList<>();
//
//        Calendar startCalendar = getStartCalendarFromDocument(doc);
//        Calendar endCalendar = getEndCalendarFromDocument(doc);
//
//        if (startCalendar == null) {
//            return keys;
//        }
//
//        if (endCalendar == null) {
//            endCalendar = (Calendar) startCalendar.clone();
//        }
//
//        startOfDay(startCalendar);
//        startOfDay(endCalendar);
//
//        if (endCalendar.before(startCalendar)) {
//            endCalendar = (Calendar) startCalendar.clone();
//        }
//
//        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//
//        Calendar cursor = (Calendar) startCalendar.clone();
//
//        while (!cursor.after(endCalendar)) {
//            keys.add(keyFormat.format(cursor.getTime()));
//            cursor.add(Calendar.DAY_OF_MONTH, 1);
//        }
//
//        return keys;
//    }
//
//    private List<String> getDateKeysFromScheduleDays(DocumentSnapshot doc) {
//        List<String> keys = new ArrayList<>();
//
//        Object rawScheduleDays = doc.get("scheduleDays");
//
//        if (!(rawScheduleDays instanceof List<?>)) {
//            return keys;
//        }
//
//        List<?> scheduleDays = (List<?>) rawScheduleDays;
//
//        for (Object item : scheduleDays) {
//            if (!(item instanceof Map<?, ?>)) {
//                continue;
//            }
//
//            Map<?, ?> rawMap = (Map<?, ?>) item;
//
//            String dateText = getMapString(rawMap, "dateText");
//            String dateKey = convertDateTextToKey(dateText);
//
//            if (!dateKey.isEmpty() && !keys.contains(dateKey)) {
//                keys.add(dateKey);
//            }
//        }
//
//        return keys;
//    }
//
//    private Calendar getStartCalendarFromDocument(DocumentSnapshot doc) {
//        Timestamp timestamp = doc.getTimestamp("startDate");
//
//        if (timestamp != null) {
//            Calendar calendar = Calendar.getInstance();
//            calendar.setTime(timestamp.toDate());
//            return calendar;
//        }
//
//        String startDateText = getSafeString(getDocumentString(doc, "startDateText"), "");
//
//        if (!startDateText.isEmpty()) {
//            return parseDateTextToCalendar(startDateText);
//        }
//
//        return null;
//    }
//
//    private Calendar getEndCalendarFromDocument(DocumentSnapshot doc) {
//        Timestamp timestamp = doc.getTimestamp("endDate");
//
//        if (timestamp != null) {
//            Calendar calendar = Calendar.getInstance();
//            calendar.setTime(timestamp.toDate());
//            return calendar;
//        }
//
//        String endDateText = getSafeString(getDocumentString(doc, "endDateText"), "");
//
//        if (!endDateText.isEmpty()) {
//            return parseDateTextToCalendar(endDateText);
//        }
//
//        return null;
//    }
//
//    private Calendar parseDateTextToCalendar(String dateText) {
//        if (dateText == null || dateText.trim().isEmpty()) {
//            return null;
//        }
//
//        String[] patterns = {
//                "MMMM dd, yyyy",
//                "MMM dd, yyyy",
//                "yyyy-MM-dd",
//                "MM/dd/yyyy",
//                "dd/MM/yyyy"
//        };
//
//        for (String pattern : patterns) {
//            try {
//                SimpleDateFormat parser = new SimpleDateFormat(pattern, Locale.getDefault());
//                parser.setLenient(false);
//
//                java.util.Date parsedDate = parser.parse(dateText.trim());
//
//                if (parsedDate != null) {
//                    Calendar calendar = Calendar.getInstance();
//                    calendar.setTime(parsedDate);
//                    return calendar;
//                }
//            } catch (Exception ignored) {
//            }
//        }
//
//        return null;
//    }
//
//    private String convertDateTextToKey(String dateText) {
//        Calendar calendar = parseDateTextToCalendar(dateText);
//
//        if (calendar == null) {
//            return "";
//        }
//
//        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//        return keyFormat.format(calendar.getTime());
//    }
//
//    private void startOfDay(Calendar calendar) {
//        calendar.set(Calendar.HOUR_OF_DAY, 0);
//        calendar.set(Calendar.MINUTE, 0);
//        calendar.set(Calendar.SECOND, 0);
//        calendar.set(Calendar.MILLISECOND, 0);
//    }
//
//    private void sortSchedulesByTime() {
//        for (List<BookingItem> list : schedulesByDateMap.values()) {
//            Collections.sort(list, (a, b) -> {
//                long timeA = parseTimeToMillis(a.timeStartText);
//                long timeB = parseTimeToMillis(b.timeStartText);
//                return Long.compare(timeA, timeB);
//            });
//        }
//    }
//
//    private long parseTimeToMillis(String timeText) {
//        if (timeText == null || timeText.trim().isEmpty()) {
//            return Long.MAX_VALUE;
//        }
//
//        String cleanTime = timeText.trim();
//        String[] patterns = {"hh:mm a", "h:mm a", "HH:mm"};
//
//        for (String pattern : patterns) {
//            try {
//                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
//                java.util.Date date = sdf.parse(cleanTime);
//
//                if (date != null) {
//                    return date.getTime();
//                }
//            } catch (Exception ignored) {
//            }
//        }
//
//        return Long.MAX_VALUE;
//    }
//
//    private void renderCalendar() {
//        if (!isAdded()) return;
//
//        calendarGrid.removeAllViews();
//
//        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
//        tvCalendarMonth.setText(monthFormat.format(currentMonth.getTime()));
//
//        Calendar calendar = (Calendar) currentMonth.clone();
//        calendar.set(Calendar.DAY_OF_MONTH, 1);
//
//        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
//        int maxDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
//
//        for (int i = 0; i < firstDayOfWeek; i++) {
//            TextView emptyCell = new TextView(requireContext());
//            emptyCell.setLayoutParams(createDayLayoutParams());
//            calendarGrid.addView(emptyCell);
//        }
//
//        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//
//        for (int day = 1; day <= maxDaysInMonth; day++) {
//            TextView dayView = new TextView(requireContext());
//            dayView.setLayoutParams(createDayLayoutParams());
//            dayView.setGravity(Gravity.CENTER);
//            dayView.setText(String.valueOf(day));
//            dayView.setTextSize(16f);
//            dayView.setPadding(8, 16, 8, 16);
//
//            Calendar dayCalendar = (Calendar) currentMonth.clone();
//            dayCalendar.set(Calendar.DAY_OF_MONTH, day);
//
//            boolean isToday = isSameDate(dayCalendar, Calendar.getInstance());
//            boolean isSelected = isSameDate(dayCalendar, selectedDate);
//
//            String dateKey = keyFormat.format(dayCalendar.getTime());
//            int bookings = bookedDatesMap.containsKey(dateKey) ? bookedDatesMap.get(dateKey) : 0;
//
//            if (isSelected) {
//                dayView.setBackgroundColor(Color.parseColor("#313131"));
//                dayView.setTextColor(Color.WHITE);
//            } else if (bookings >= FULLY_BOOKED_LIMIT) {
//                dayView.setBackgroundColor(Color.parseColor("#970705"));
//                dayView.setTextColor(Color.WHITE);
//            } else if (bookings > 0) {
//                dayView.setBackgroundColor(Color.parseColor("#F3D9D9"));
//                dayView.setTextColor(Color.parseColor("#313131"));
//            } else if (isToday) {
//                dayView.setBackgroundColor(Color.parseColor("#FFF0D8"));
//                dayView.setTextColor(Color.parseColor("#313131"));
//            } else {
//                dayView.setBackgroundColor(Color.TRANSPARENT);
//                dayView.setTextColor(Color.parseColor("#313131"));
//            }
//
//            dayView.setOnClickListener(v -> {
//                selectedDate = (Calendar) dayCalendar.clone();
//                updateSelectedDateLabel();
//                renderCalendar();
//                updateSchedsSection();
//            });
//
//            calendarGrid.addView(dayView);
//        }
//    }
//
//    private GridLayout.LayoutParams createDayLayoutParams() {
//        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
//        params.width = 0;
//        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
//        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
//        params.setMargins(6, 6, 6, 6);
//        return params;
//    }
//
//    private void updateSelectedDateLabel() {
//        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
//        tvSelectedDate.setText(sdf.format(selectedDate.getTime()));
//    }
//
//    private void updateSchedsSection() {
//        if (!isAdded()) return;
//
//        updateSelectedDateLabel();
//
//        schedsContainer.removeAllViews();
//
//        if (tvSelectedDate.getParent() != null) {
//            ((ViewGroup) tvSelectedDate.getParent()).removeView(tvSelectedDate);
//        }
//
//        tvSelectedDate.setLayoutParams(new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.WRAP_CONTENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT
//        ));
//
//        schedsContainer.addView(tvSelectedDate);
//
//        String selectedDateKey = getDateKeyFromCalendar(selectedDate);
//        List<BookingItem> schedules = schedulesByDateMap.get(selectedDateKey);
//
//        if (schedules == null || schedules.isEmpty()) {
//            schedsContainer.addView(createEmptyScheduleView());
//            return;
//        }
//
//        for (BookingItem item : schedules) {
//            schedsContainer.addView(createScheduleCard(item));
//        }
//    }
//
//    private View createEmptyScheduleView() {
//        TextView emptyView = new TextView(requireContext());
//
//        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT
//        );
//        params.setMargins(0, dp(16), 0, 0);
//
//        emptyView.setLayoutParams(params);
//        emptyView.setText("No scheduled bookings for this date.");
//        emptyView.setTextColor(Color.parseColor("#313131"));
//        emptyView.setTextSize(14f);
//        emptyView.setAlpha(0.72f);
//        emptyView.setGravity(Gravity.CENTER);
//        emptyView.setPadding(dp(12), dp(18), dp(12), dp(18));
//
//        return emptyView;
//    }
//
//    private View createScheduleCard(BookingItem item) {
//        LinearLayout row = new LinearLayout(requireContext());
//        row.setOrientation(LinearLayout.HORIZONTAL);
//        row.setGravity(Gravity.CENTER_VERTICAL);
//        row.setPadding(0, dp(16), 0, 0);
//
//        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT
//        );
//        row.setLayoutParams(rowParams);
//
//        String timeBadge = getTimeBadgeText(item.timeStartText);
//
//        if (!timeBadge.isEmpty()) {
//            MaterialCardView timeCard = new MaterialCardView(requireContext());
//            LinearLayout.LayoutParams timeCardParams = new LinearLayout.LayoutParams(dp(54), dp(54));
//            timeCard.setLayoutParams(timeCardParams);
//            timeCard.setCardBackgroundColor(Color.parseColor("#970705"));
//            timeCard.setRadius(dp(18));
//            timeCard.setCardElevation(0f);
//
//            TextView timeText = new TextView(requireContext());
//            timeText.setLayoutParams(new ViewGroup.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.MATCH_PARENT
//            ));
//            timeText.setGravity(Gravity.CENTER);
//            timeText.setText(timeBadge);
//            timeText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
//            timeText.setTextColor(Color.WHITE);
//            timeText.setTextSize(12f);
//            timeText.setTypeface(null, android.graphics.Typeface.BOLD);
//
//            timeCard.addView(timeText);
//            row.addView(timeCard);
//        }
//
//        LinearLayout detailsLayout = new LinearLayout(requireContext());
//        detailsLayout.setOrientation(LinearLayout.VERTICAL);
//
//        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
//                0,
//                ViewGroup.LayoutParams.WRAP_CONTENT,
//                1f
//        );
//        detailsParams.setMargins(timeBadge.isEmpty() ? 0 : dp(14), 0, dp(10), 0);
//        detailsLayout.setLayoutParams(detailsParams);
//
//        String title = firstNonEmpty(item.purpose, item.facility);
//
//        if (!title.isEmpty()) {
//            TextView titleText = new TextView(requireContext());
//            titleText.setText(title);
//            titleText.setTextColor(Color.parseColor("#313131"));
//            titleText.setTextSize(15f);
//            titleText.setTypeface(null, android.graphics.Typeface.BOLD);
//            titleText.setSingleLine(true);
//            titleText.setEllipsize(TextUtils.TruncateAt.END);
//            detailsLayout.addView(titleText);
//        }
//
//        String facilityMeta = buildFacilityMeta(item, title);
//
//        if (!facilityMeta.isEmpty()) {
//            detailsLayout.addView(createScheduleInfoText(facilityMeta, false));
//        }
//
//        String dateRange = buildDateRange(item.startDateText, item.endDateText);
//
//        if (!dateRange.isEmpty()) {
//            detailsLayout.addView(createScheduleInfoText(dateRange, true));
//        }
//
//        String timeRange = buildTimeRange(item.timeStartText, item.timeEndText);
//
//        if (!timeRange.isEmpty()) {
//            detailsLayout.addView(createScheduleInfoText(timeRange, true));
//        }
//
//        row.addView(detailsLayout);
//
//        if (item.status != null && !item.status.trim().isEmpty()) {
//            Chip statusChip = new Chip(requireContext());
//
//            statusChip.setText(item.status);
//            statusChip.setTextSize(11f);
//
//            applyFixedStatusChipSize(statusChip);
//
//            statusChip.setTextColor(getStatusTextColor(item.status));
//            statusChip.setChipBackgroundColor(ColorStateList.valueOf(getStatusBackgroundColor(item.status)));
//            statusChip.setChipStrokeWidth(0);
//            statusChip.setCheckable(false);
//            statusChip.setClickable(true);
//            statusChip.setFocusable(true);
//            statusChip.setOnClickListener(v -> openRequestDetailsFromSchedule(item));
//
//            row.addView(statusChip);
//        }
//
//        return row;
//    }
//
//    private void openRequestDetailsFromSchedule(BookingItem item) {
//        if (!isAdded() || item == null || item.requestId == null || item.requestId.trim().isEmpty()) {
//            Toast.makeText(requireContext(), "Request details not available.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        try {
//            requireActivity()
//                    .getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(
//                            R.id.fragment_container,
//                            RequestorRequestDetailsFragment.newInstance(
//                                    item.requestId,
//                                    true,
//                                    true
//                            )
//                    )
//                    .addToBackStack(null)
//                    .commit();
//        } catch (Exception e) {
//            Toast.makeText(requireContext(), "Unable to open request details.", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private TextView createScheduleInfoText(String text, boolean importantLine) {
//        TextView infoText = new TextView(requireContext());
//        infoText.setText(text);
//        infoText.setTextColor(Color.parseColor("#313131"));
//        infoText.setTextSize(12f);
//        infoText.setAlpha(importantLine ? 0.82f : 0.72f);
//        infoText.setSingleLine(true);
//        infoText.setEllipsize(TextUtils.TruncateAt.END);
//
//        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.WRAP_CONTENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT
//        );
//        infoParams.setMargins(0, dp(4), 0, 0);
//        infoText.setLayoutParams(infoParams);
//
//        return infoText;
//    }
//
//    private String buildFacilityMeta(BookingItem item, String title) {
//        String facility = item.facility == null ? "" : item.facility.trim();
//
//        if (!facility.isEmpty() && !facility.equalsIgnoreCase(title)) {
//            return facility;
//        }
//
//        return "";
//    }
//
//    private String buildDateRange(String startDate, String endDate) {
//        String cleanStart = startDate == null ? "" : startDate.trim();
//        String cleanEnd = endDate == null ? "" : endDate.trim();
//
//        if (!cleanStart.isEmpty() && !cleanEnd.isEmpty() && !cleanStart.equalsIgnoreCase(cleanEnd)) {
//            return cleanStart + " - " + cleanEnd;
//        }
//
//        return !cleanStart.isEmpty() ? cleanStart : cleanEnd;
//    }
//
//    private String buildTimeRange(String startTime, String endTime) {
//        String cleanStart = startTime == null ? "" : startTime.trim();
//        String cleanEnd = endTime == null ? "" : endTime.trim();
//
//        if (!cleanStart.isEmpty() && !cleanEnd.isEmpty()) {
//            return cleanStart + " - " + cleanEnd;
//        }
//
//        return !cleanStart.isEmpty() ? cleanStart : cleanEnd;
//    }
//
//    private int getStatusTextColor(String status) {
//        if ("Approved".equalsIgnoreCase(status)) {
//            return Color.parseColor("#2E7D32");
//        }
//
//        if ("Pending".equalsIgnoreCase(status)) {
//            return Color.parseColor("#313131");
//        }
//
//        if ("Returned".equalsIgnoreCase(status)
//                || "Rejected".equalsIgnoreCase(status)
//                || "Not Available".equalsIgnoreCase(status)
//                || "Unavailable".equalsIgnoreCase(status)) {
//            return Color.parseColor("#970705");
//        }
//
//        return Color.parseColor("#313131");
//    }
//
//    private int getStatusBackgroundColor(String status) {
//        if ("Approved".equalsIgnoreCase(status)) {
//            return Color.parseColor("#E7F4E8");
//        }
//
//        if ("Pending".equalsIgnoreCase(status)) {
//            return Color.parseColor("#EEEEEE");
//        }
//
//        if ("Returned".equalsIgnoreCase(status)
//                || "Rejected".equalsIgnoreCase(status)
//                || "Not Available".equalsIgnoreCase(status)
//                || "Unavailable".equalsIgnoreCase(status)) {
//            return Color.parseColor("#F3D9D9");
//        }
//
//        return Color.parseColor("#EEEEEE");
//    }
//
//    private void applyFixedStatusChipSize(Chip statusChip) {
//        if (statusChip == null) return;
//
//        int chipWidth = dp(104);
//        int chipHeight = dp(34);
//
//        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(chipWidth, chipHeight);
//        chipParams.setMargins(dp(4), 0, 0, 0);
//        statusChip.setLayoutParams(chipParams);
//
//        statusChip.setWidth(chipWidth);
//        statusChip.setMinWidth(chipWidth);
//        statusChip.setMaxWidth(chipWidth);
//
//        statusChip.setHeight(chipHeight);
//        statusChip.setMinHeight(chipHeight);
//        statusChip.setMinimumHeight(chipHeight);
//        statusChip.setChipMinHeight(chipHeight);
//
//        statusChip.setEnsureMinTouchTargetSize(false);
//
//        statusChip.setSingleLine(true);
//        statusChip.setMaxLines(1);
//        statusChip.setEllipsize(TextUtils.TruncateAt.END);
//        statusChip.setGravity(Gravity.CENTER);
//        statusChip.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
//        statusChip.setIncludeFontPadding(false);
//
//        statusChip.setChipStartPadding(0f);
//        statusChip.setChipEndPadding(0f);
//        statusChip.setTextStartPadding(0f);
//        statusChip.setTextEndPadding(0f);
//        statusChip.setPadding(0, 0, 0, 0);
//    }
//
//    private String getTimeBadgeText(String timeStartText) {
//        if (timeStartText == null || timeStartText.trim().isEmpty()) {
//            return "";
//        }
//
//        String cleanTime = timeStartText.trim();
//        String[] patterns = {"hh:mm a", "h:mm a", "HH:mm"};
//
//        for (String pattern : patterns) {
//            try {
//                SimpleDateFormat inputFormat = new SimpleDateFormat(pattern, Locale.getDefault());
//                SimpleDateFormat hourFormat = new SimpleDateFormat("hh", Locale.getDefault());
//                SimpleDateFormat amPmFormat = new SimpleDateFormat("a", Locale.getDefault());
//
//                java.util.Date parsedTime = inputFormat.parse(cleanTime);
//
//                if (parsedTime != null) {
//                    return hourFormat.format(parsedTime) + "\n" + amPmFormat.format(parsedTime);
//                }
//            } catch (Exception ignored) {
//            }
//        }
//
//        String[] parts = cleanTime.split("\\s+");
//
//        if (parts.length >= 2) {
//            String hour = parts[0].contains(":") ? parts[0].split(":")[0] : parts[0];
//            return hour + "\n" + parts[1];
//        }
//
//        return cleanTime;
//    }
//
//    private String getDateKeyFromCalendar(Calendar calendar) {
//        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//        return keyFormat.format(calendar.getTime());
//    }
//
//    private boolean isSameDate(Calendar cal1, Calendar cal2) {
//        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
//                && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
//                && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
//    }
//
//    private String getDocumentString(DocumentSnapshot doc, String field) {
//        if (doc == null || field == null) {
//            return "";
//        }
//
//        Object value = doc.get(field);
//        return value == null ? "" : String.valueOf(value).trim();
//    }
//
//    private String getMapString(Map<?, ?> map, String key) {
//        if (map == null || key == null) {
//            return "";
//        }
//
//        Object value = map.get(key);
//        return value == null ? "" : String.valueOf(value).trim();
//    }
//
//    private boolean getBooleanValue(DocumentSnapshot doc, String field) {
//        if (doc == null || field == null) {
//            return false;
//        }
//
//        Object value = doc.get(field);
//
//        if (value instanceof Boolean) {
//            return Boolean.TRUE.equals(value);
//        }
//
//        if (value instanceof String) {
//            return "true".equalsIgnoreCase(((String) value).trim());
//        }
//
//        return false;
//    }
//
//    private String firstNonEmpty(String... values) {
//        if (values == null) {
//            return "";
//        }
//
//        for (String value : values) {
//            if (value != null && !value.trim().isEmpty()) {
//                return value.trim();
//            }
//        }
//
//        return "";
//    }
//
//    private String getSafeString(String value, String fallback) {
//        if (value == null || value.trim().isEmpty()) {
//            return fallback;
//        }
//
//        return value.trim();
//    }
//
//    private int dp(int value) {
//        return Math.round(value * getResources().getDisplayMetrics().density);
//    }
//
//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//
//        if (bookingsListener != null) {
//            bookingsListener.remove();
//            bookingsListener = null;
//        }
//
//        if (approvedNotificationListener != null) {
//            approvedNotificationListener.remove();
//            approvedNotificationListener = null;
//        }
//    }
//
//    private static class BookingItem {
//        String requestId;
//        String dateKey;
//        String purpose;
//        String facility;
//        String startDateText;
//        String endDateText;
//        String timeStartText;
//        String timeEndText;
//        String status;
//
//        BookingItem(
//                String requestId,
//                String dateKey,
//                String purpose,
//                String facility,
//                String startDateText,
//                String endDateText,
//                String timeStartText,
//                String timeEndText,
//                String status
//        ) {
//            this.requestId = requestId;
//            this.dateKey = dateKey;
//            this.purpose = purpose;
//            this.facility = facility;
//            this.startDateText = startDateText;
//            this.endDateText = endDateText;
//            this.timeStartText = timeStartText;
//            this.timeEndText = timeEndText;
//            this.status = status;
//        }
//    }
//}
