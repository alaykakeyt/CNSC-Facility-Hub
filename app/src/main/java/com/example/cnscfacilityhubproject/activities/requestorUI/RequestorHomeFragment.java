package com.example.cnscfacilityhubproject.activities.requestorUI;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
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
        btnNewRequest.setOnClickListener(v -> openRequestorRequestFragment());
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

        // Show all relevant booking statuses for calendar display:
        // - Pending: Awaiting approval
        // - Approved: Approved and scheduled
        // - Approved - Available: Approved but availability varies
        // - Booked: Confirmed booking
        bookingsListener = db.collection("requests")
                .whereIn("status", Arrays.asList("Pending", "Approved", "Approved - Available", "Booked"))
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

                            List<String> dateKeys = getDateKeysFromDocument(doc);

                            for (String dateKey : dateKeys) {
                                if (dateKey.isEmpty()) continue;

                                BookingItem item = createBookingItemFromDocument(doc, dateKey);
                                if (item == null) continue;

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

    private BookingItem createBookingItemFromDocument(DocumentSnapshot doc, String dateKey) {
        if (dateKey == null || dateKey.trim().isEmpty()) {
            return null;
        }

        String purpose = getSafeString(doc.getString("purpose"), "Purpose / Activity");
        String facility = getSafeString(doc.getString("finalFacilityName"), "");

        if (facility.isEmpty()) {
            String rawFacility = getSafeString(doc.getString("facility"), "Facility");
            String otherFacility = getSafeString(doc.getString("otherFacility"), "");

            if ("Others".equalsIgnoreCase(rawFacility) && !otherFacility.isEmpty()) {
                facility = otherFacility;
            } else {
                facility = rawFacility;
            }
        }

        String startDateText = getSafeString(doc.getString("startDateText"), "");
        String endDateText = getSafeString(doc.getString("endDateText"), "");
        String timeStartText = getSafeString(doc.getString("timeStartText"), "");
        String timeEndText = getSafeString(doc.getString("timeEndText"), "");
        String status = getSafeString(doc.getString("status"), "Pending");

        return new BookingItem(
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

    private List<String> getDateKeysFromDocument(DocumentSnapshot doc) {
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

    private Calendar getStartCalendarFromDocument(DocumentSnapshot doc) {
        Timestamp timestamp = doc.getTimestamp("startDate");

        if (timestamp != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(timestamp.toDate());
            return calendar;
        }

        String startDateText = getSafeString(doc.getString("startDateText"), "");

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

        String endDateText = getSafeString(doc.getString("endDateText"), "");

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
        try {
            if (timeText == null || timeText.trim().isEmpty()) {
                return Long.MAX_VALUE;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            java.util.Date date = sdf.parse(timeText.trim());

            return date != null ? date.getTime() : Long.MAX_VALUE;
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
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

        MaterialCardView timeCard = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams timeCardParams = new LinearLayout.LayoutParams(dp(54), dp(54));
        timeCard.setLayoutParams(timeCardParams);
        timeCard.setCardBackgroundColor(Color.parseColor("#970705"));
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
        timeText.setTypeface(null, android.graphics.Typeface.BOLD);

        timeCard.addView(timeText);

        LinearLayout detailsLayout = new LinearLayout(requireContext());
        detailsLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        detailsParams.setMargins(dp(14), 0, dp(10), 0);
        detailsLayout.setLayoutParams(detailsParams);

        TextView titleText = new TextView(requireContext());
        titleText.setText(item.purpose);
        titleText.setTextColor(Color.parseColor("#313131"));
        titleText.setTextSize(15f);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView subtitleText = new TextView(requireContext());
        subtitleText.setText(item.facility + " • " + item.timeStartText + " - " + item.timeEndText);
        subtitleText.setTextColor(Color.parseColor("#313131"));
        subtitleText.setTextSize(12f);
        subtitleText.setAlpha(0.72f);

        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.setMargins(0, dp(4), 0, 0);
        subtitleText.setLayoutParams(subtitleParams);

        detailsLayout.addView(titleText);
        detailsLayout.addView(subtitleText);

        Chip statusChip = new Chip(requireContext());
        statusChip.setText(item.status);
        statusChip.setTextSize(12f);
        statusChip.setTextColor(Color.parseColor("#970705"));
        statusChip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F3D9D9")));
        statusChip.setChipStrokeWidth(0);
        statusChip.setCheckable(false);
        statusChip.setClickable(false);

        row.addView(timeCard);
        row.addView(detailsLayout);
        row.addView(statusChip);

        return row;
    }

    private String requestorNameLabel = "";

    private String getTimeBadgeText(String timeStartText) {
        try {
            if (timeStartText == null || timeStartText.trim().isEmpty()) {
                return "--\n--";
            }

            SimpleDateFormat inputFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            SimpleDateFormat hourFormat = new SimpleDateFormat("hh", Locale.getDefault());
            SimpleDateFormat amPmFormat = new SimpleDateFormat("a", Locale.getDefault());

            java.util.Date date = inputFormat.parse(timeStartText.trim());

            if (date != null) {
                return hourFormat.format(date) + "\n" + amPmFormat.format(date);
            }
        } catch (Exception ignored) {
        }

        if (timeStartText == null || timeStartText.trim().isEmpty()) {
            return "--\n--";
        }

        String[] parts = timeStartText.trim().split(" ");

        if (parts.length >= 2) {
            String hour = parts[0].contains(":") ? parts[0].split(":")[0] : parts[0];
            return hour + "\n" + parts[1];
        }

        return timeStartText;
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
        String dateKey;
        String purpose;
        String facility;
        String startDateText;
        String endDateText;
        String timeStartText;
        String timeEndText;
        String status;

        BookingItem(
                String dateKey,
                String purpose,
                String facility,
                String startDateText,
                String endDateText,
                String timeStartText,
                String timeEndText,
                String status
        ) {
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