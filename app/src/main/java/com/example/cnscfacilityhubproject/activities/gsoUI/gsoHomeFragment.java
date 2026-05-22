package com.example.cnscfacilityhubproject.activities.gsoUI;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import android.text.TextUtils;

public class gsoHomeFragment extends Fragment {

    private TextView tvCalendarMonth;
    private TextView tvSelectedDate;
    private TextView tvGreeting;
    private TextView gsoHomeWelcome;

    private TextView tvPendingCount;
    private TextView tvApprovedCount;
    private TextView tvReturnedCount;
    private TextView tvTotalCount;
    private TextView tvUsersCount;
    private TextView tvReportsCount;


    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private GridLayout calendarGrid;

    private MaterialButton btnViewRequests;
    private MaterialButton btnGenerateReport;


    private MaterialCardView cardPending;
    private MaterialCardView cardApproved;
    private MaterialCardView cardReturned;
    private MaterialCardView cardTotal;
    private MaterialCardView cardUsers;
    private MaterialCardView cardReports;

    private LinearLayout schedsContainer;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private ListenerRegistration requestsListener;
    private ListenerRegistration usersListener;
    private ListenerRegistration reportsListener;
    private ListenerRegistration gsoNotificationBadgeListener;
    private ListenerRegistration gsoProfileListener;

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

        setupCalendarNavigation();

        listenForGsoProfile();
        updateSelectedDateSection();

        listenForRequestsAndCalendar();
        listenForUsersCount();
        listenForReportsCount();

        setupActions();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (requestsListener != null) requestsListener.remove();
        if (usersListener != null) usersListener.remove();
        if (reportsListener != null) reportsListener.remove();
        if (gsoNotificationBadgeListener != null) gsoNotificationBadgeListener.remove();
        if (gsoProfileListener != null) gsoProfileListener.remove();

        requestsListener = null;
        usersListener = null;
        reportsListener = null;
        gsoNotificationBadgeListener = null;
        gsoProfileListener = null;
    }

    private void bindViews(View view) {
        tvCalendarMonth = view.findViewById(R.id.tvCalendarMonth);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvGreeting = view.findViewById(R.id.tvGreeting);
        gsoHomeWelcome = view.findViewById(R.id.tvGSOName);

        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        tvApprovedCount = view.findViewById(R.id.tvApprovedCount);
        tvReturnedCount = view.findViewById(R.id.tvReturnedCount);
        tvTotalCount = view.findViewById(R.id.tvTotalCount);
        tvUsersCount = view.findViewById(R.id.tvUsersCount);
        tvReportsCount = view.findViewById(R.id.tvReportsCount);


        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        calendarGrid = view.findViewById(R.id.calendarGrid);

        btnViewRequests = view.findViewById(R.id.btnViewRequests);
        btnGenerateReport = view.findViewById(R.id.btnGenerateReport);

        cardPending = view.findViewById(R.id.cardPending);
        cardApproved = view.findViewById(R.id.cardApproved);
        cardReturned = view.findViewById(R.id.cardReturned);
        cardTotal = view.findViewById(R.id.cardTotal);
        cardUsers = view.findViewById(R.id.cardUsers);
        cardReports = view.findViewById(R.id.cardReports);

        android.util.Log.d("CNSC_GSO_Dashboard", "Dashboard cards bound to views.");

        schedsContainer = view.findViewById(R.id.Scheds);
    }

    private void setupActions() {
        if (btnViewRequests != null) {
            btnViewRequests.setOnClickListener(v -> {
                android.util.Log.d("CNSC_GSO_Dashboard", "View Requests button clicked.");
                if (requireActivity() instanceof gsoNavBarActivity) {
                    ((gsoNavBarActivity) requireActivity()).navigateToRequests("All");
                }
            });
        }

        if (btnGenerateReport != null) {
            btnGenerateReport.setOnClickListener(v -> {
                android.util.Log.d("CNSC_GSO_Dashboard", "Reports button clicked.");
                if (requireActivity() instanceof gsoNavBarActivity) {
                    ((gsoNavBarActivity) requireActivity()).navigateToReports();
                }
            });
        }

        if (cardPending != null) {
            cardPending.setOnClickListener(v -> {
                android.util.Log.d("CNSC_GSO_Dashboard", "Pending card clicked. Opening requests filter=Pending");
                if (requireActivity() instanceof gsoNavBarActivity) {
                    ((gsoNavBarActivity) requireActivity()).navigateToRequests("Pending");
                }
            });
        }

        if (cardApproved != null) {
            cardApproved.setOnClickListener(v -> {
                android.util.Log.d("CNSC_GSO_Dashboard", "Approved card clicked. Opening requests filter=Approved");
                if (requireActivity() instanceof gsoNavBarActivity) {
                    ((gsoNavBarActivity) requireActivity()).navigateToRequests("Approved");
                }
            });
        }

        if (cardReturned != null) {
            cardReturned.setOnClickListener(v -> {
                android.util.Log.d("CNSC_GSO_Dashboard", "Returned card clicked. Opening requests filter=Returned");
                if (requireActivity() instanceof gsoNavBarActivity) {
                    ((gsoNavBarActivity) requireActivity()).navigateToRequests("Returned");
                }
            });
        }

        if (cardTotal != null) {
            cardTotal.setOnClickListener(v -> {
                android.util.Log.d("CNSC_GSO_Dashboard", "Total card clicked. Opening requests filter=All");
                if (requireActivity() instanceof gsoNavBarActivity) {
                    ((gsoNavBarActivity) requireActivity()).navigateToRequests("All");
                }
            });
        }

        if (cardUsers != null) {
            cardUsers.setOnClickListener(v -> {
                android.util.Log.d("CNSC_GSO_Dashboard", "Users card clicked. Opening Users tab.");
                if (requireActivity() instanceof gsoNavBarActivity) {
                    ((gsoNavBarActivity) requireActivity()).navigateToUsers();
                }
            });
        }

        if (cardReports != null) {
            cardReports.setOnClickListener(v -> {
                android.util.Log.d("CNSC_GSO_Dashboard", "Reports card clicked. Opening Reports tab.");
                if (requireActivity() instanceof gsoNavBarActivity) {
                    ((gsoNavBarActivity) requireActivity()).navigateToReports();
                }
            });
        }
    }

    private void setDefaultTexts() {
        setGreetingByTime();

        if (gsoHomeWelcome != null) gsoHomeWelcome.setText("Hello,");
        if (tvPendingCount != null) tvPendingCount.setText("");
        if (tvApprovedCount != null) tvApprovedCount.setText("");
        if (tvReturnedCount != null) tvReturnedCount.setText("");
        if (tvTotalCount != null) tvTotalCount.setText("");
        if (tvUsersCount != null) tvUsersCount.setText("");
        if (tvReportsCount != null) tvReportsCount.setText("");
        if (tvCalendarMonth != null) tvCalendarMonth.setText("");
        if (tvSelectedDate != null) tvSelectedDate.setText("");
    }

    private void setGreetingByTime() {
        if (tvGreeting == null) return;

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

    private void listenForGsoProfile() {
        if (gsoProfileListener != null) {
            gsoProfileListener.remove();
            gsoProfileListener = null;
        }

        if (auth == null || auth.getCurrentUser() == null) {
            if (gsoHomeWelcome != null) gsoHomeWelcome.setText("Hello,");
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        String label = gsoHomeWelcome != null && gsoHomeWelcome.getText() != null
                ? gsoHomeWelcome.getText().toString().trim()
                : "";

        if (!label.endsWith(",")) {
            label = "Hello,";
        }

        final String prefix = label + " ";

        SharedPreferences prefs = requireContext().getSharedPreferences("user_cache", Context.MODE_PRIVATE);
        String cachedFullName = prefs.getString("fullName_" + userId, "");

        if (gsoHomeWelcome != null && cachedFullName != null && !cachedFullName.trim().isEmpty()) {
            gsoHomeWelcome.setText(prefix + cachedFullName.trim());
        } else if (gsoHomeWelcome != null) {
            gsoHomeWelcome.setText(label);
        }

        gsoProfileListener = db.collection("users")
                .document(userId)
                .addSnapshotListener((doc, error) -> {
                    if (!isAdded() || gsoHomeWelcome == null) return;

                    if (error != null || doc == null || !doc.exists()) {
                        return;
                    }

                    String fullName = getStringValue(doc, "fullName");
                    if (!fullName.isEmpty()) {
                        String cleanName = fullName.trim();

                        prefs.edit()
                                .putString("fullName_" + userId, cleanName)
                                .apply();

                        gsoHomeWelcome.setText(prefix + cleanName);
                    }
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



    private void listenForRequestsAndCalendar() {
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }

        requestsListener = db.collection("requests")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null || snapshot == null) {
                        if (tvPendingCount != null) tvPendingCount.setText("");
                        if (tvApprovedCount != null) tvApprovedCount.setText("");

                        bookedDatesMap.clear();
                        schedulesByDateMap.clear();

                        renderCalendar();
                        updateSelectedDateSection();

                        Toast.makeText(requireContext(), "Failed to load request dashboard.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int pendingCount = 0;
                    int approvedCount = 0;
                    int returnedCount = 0;
                    int totalCount = 0;

                    bookedDatesMap.clear();
                    schedulesByDateMap.clear();

                    SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : snapshot) {
                        if (!isGSORequest(doc)) continue;
                        if (!RequestDataHelper.shouldShowInRequestList(doc)) continue;

                        totalCount++;
                        String status = getDisplayStatus(doc);

                        if ("Pending".equalsIgnoreCase(status)) {
                            pendingCount++;
                        } else if ("Approved".equalsIgnoreCase(status)) {
                            approvedCount++;
                        } else if ("Returned".equalsIgnoreCase(status)) {
                            returnedCount++;
                        }

                        if (shouldShowOnCalendar(doc)) {
                            List<String> dateKeys = getDateKeysFromDocument(doc, keyFormat);

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

                    if (tvReturnedCount != null) {
                        tvReturnedCount.setText(formatCount(returnedCount));
                    }

                    if (tvTotalCount != null) {
                        tvTotalCount.setText(formatCount(totalCount));
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
                        if (tvUsersCount != null) tvUsersCount.setText("");
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
                        if (tvReportsCount != null) tvReportsCount.setText("");
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

        if ("Pending".equalsIgnoreCase(status) || "Pending".equalsIgnoreCase(gsoStatus)) {
            return "Pending";
        }

        return status;
    }

    private boolean shouldShowOnCalendar(DocumentSnapshot doc) {
        String displayStatus = getDisplayStatus(doc);
        String bookingStatus = getStringValue(doc, "bookingStatus");

        Boolean calendarVisible = doc.getBoolean("calendarVisible");
        Boolean isCalendarBooking = doc.getBoolean("isCalendarBooking");

        if ("Returned".equalsIgnoreCase(displayStatus)
                || "Rejected".equalsIgnoreCase(displayStatus)
                || "Cancelled".equalsIgnoreCase(displayStatus)) {
            return false;
        }

        if ("Pending".equalsIgnoreCase(displayStatus)) return true;
        if ("Approved".equalsIgnoreCase(displayStatus)) return true;
        if ("Approved - Available".equalsIgnoreCase(displayStatus)) return true;
        if ("Booked".equalsIgnoreCase(bookingStatus)) return true;
        if (Boolean.TRUE.equals(calendarVisible) || Boolean.TRUE.equals(isCalendarBooking)) return true;

        return hasAnyScheduleDate(doc);
    }

    private List<String> getDateKeysFromDocument(DocumentSnapshot doc, SimpleDateFormat keyFormat) {
        List<String> keys = new ArrayList<>();

        addScheduleDayDateKeys(doc, keys, keyFormat);

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
            addDateKeyIfValid(keys, keyFormat.format(cursor.getTime()));
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }

        return keys;
    }

    private void addScheduleDayDateKeys(DocumentSnapshot doc, List<String> keys, SimpleDateFormat keyFormat) {
        Object rawScheduleDays = doc.get("scheduleDays");

        if (!(rawScheduleDays instanceof List<?>)) {
            return;
        }

        List<?> scheduleDays = (List<?>) rawScheduleDays;

        for (Object item : scheduleDays) {
            if (!(item instanceof Map<?, ?>)) continue;

            Map<?, ?> rawMap = (Map<?, ?>) item;

            String directKey = firstNonEmpty(
                    getRawMapString(rawMap, "dateKey"),
                    getRawMapString(rawMap, "key")
            );
            addDateKeyIfValid(keys, directKey);

            addDateKeyIfValid(keys, getDateKeyFromRawMapValue(rawMap, "dateText", keyFormat));
            addDateKeyIfValid(keys, getDateKeyFromRawMapValue(rawMap, "date", keyFormat));
            addDateKeyIfValid(keys, getDateKeyFromRawMapValue(rawMap, "selectedDate", keyFormat));
            addDateKeyIfValid(keys, getDateKeyFromRawMapValue(rawMap, "startDate", keyFormat));
        }
    }

    private boolean hasAnyScheduleDate(DocumentSnapshot doc) {
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return !getDateKeysFromDocument(doc, keyFormat).isEmpty();
    }

    private void addDateKeyIfValid(List<String> keys, String key) {
        if (key == null) return;

        String cleanKey = key.trim();
        if (cleanKey.isEmpty()) return;

        if (!keys.contains(cleanKey)) {
            keys.add(cleanKey);
        }
    }

    private String getDateKeyFromRawMapValue(Map<?, ?> map, String key, SimpleDateFormat keyFormat) {
        if (map == null || key == null) return "";
        return getDateKeyFromValue(map.get(key), keyFormat);
    }

    private String getDateKeyFromValue(Object value, SimpleDateFormat keyFormat) {
        if (value == null) return "";

        if (value instanceof Timestamp) {
            return keyFormat.format(((Timestamp) value).toDate());
        }

        if (value instanceof java.util.Date) {
            return keyFormat.format((java.util.Date) value);
        }

        String textValue = String.valueOf(value).trim();
        if (textValue.isEmpty()) return "";

        if (textValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return textValue;
        }

        Calendar calendar = parseDateTextToCalendar(textValue);
        if (calendar == null) return "";

        return keyFormat.format(calendar.getTime());
    }

    private String getRawMapString(Map<?, ?> map, String key) {
        if (map == null || key == null) return "";
        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Calendar getStartCalendarFromDocument(DocumentSnapshot doc) {
        Timestamp timestamp = doc.getTimestamp("startDate");

        if (timestamp != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(timestamp.toDate());
            return calendar;
        }

        String startDateText = firstNonEmpty(
                getStringValue(doc, "startDateText"),
                firstNonEmpty(
                        getStringValue(doc, "dateText"),
                        firstNonEmpty(
                                getStringValue(doc, "date"),
                                getStringValue(doc, "selectedDate")
                        )
                )
        );
        if (!startDateText.isEmpty()) return parseDateTextToCalendar(startDateText);

        return null;
    }

    private Calendar getEndCalendarFromDocument(DocumentSnapshot doc) {
        Timestamp timestamp = doc.getTimestamp("endDate");

        if (timestamp != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(timestamp.toDate());
            return calendar;
        }

        String endDateText = firstNonEmpty(
                getStringValue(doc, "endDateText"),
                getStringValue(doc, "dateEndText")
        );
        if (!endDateText.isEmpty()) return parseDateTextToCalendar(endDateText);

        return null;
    }

    private Calendar parseDateTextToCalendar(String dateText) {
        String[] patterns = {
                "MMMM dd, yyyy",
                "MMMM d, yyyy",
                "MMM dd, yyyy",
                "MMM d, yyyy",
                "yyyy-MM-dd",
                "MM/dd/yyyy",
                "M/d/yyyy",
                "dd/MM/yyyy",
                "d/M/yyyy"
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

        String purpose = firstNonEmpty(
                getStringValue(doc, "purpose"),
                getStringValue(doc, "activityType")
        );
        String facility = getFinalFacility(doc);
        String startDateText = firstNonEmpty(
                getStringValue(doc, "startDateText"),
                firstNonEmpty(getStringValue(doc, "dateText"), getStringValue(doc, "date"))
        );
        String endDateText = firstNonEmpty(
                getStringValue(doc, "endDateText"),
                getStringValue(doc, "dateEndText")
        );
        String timeStartText = firstNonEmpty(
                getStringValue(doc, "timeStartText"),
                firstNonEmpty(getStringValue(doc, "startTimeText"), getStringValue(doc, "startTime"))
        );
        String timeEndText = firstNonEmpty(
                getStringValue(doc, "timeEndText"),
                firstNonEmpty(getStringValue(doc, "endTimeText"), getStringValue(doc, "endTime"))
        );
        String status = getDisplayStatus(doc);

        Map<String, Object> scheduleDay = getScheduleDayForDate(doc, dateKey);

        if (scheduleDay != null) {
            String dayDateText = firstNonEmpty(
                    getMapString(scheduleDay, "dateText"),
                    firstNonEmpty(
                            getMapString(scheduleDay, "date"),
                            getMapString(scheduleDay, "selectedDate")
                    )
            );
            String dayStartTime = firstNonEmpty(
                    getMapString(scheduleDay, "startTimeText"),
                    firstNonEmpty(
                            getMapString(scheduleDay, "timeStartText"),
                            getMapString(scheduleDay, "startTime")
                    )
            );
            String dayEndTime = firstNonEmpty(
                    getMapString(scheduleDay, "endTimeText"),
                    firstNonEmpty(
                            getMapString(scheduleDay, "timeEndText"),
                            getMapString(scheduleDay, "endTime")
                    )
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

        String requestorName = firstNonEmpty(
                getStringValue(doc, "requestorName"),
                getStringValue(doc, "fullName")
        );

        if (purpose.isEmpty()
                && facility.isEmpty()
                && startDateText.isEmpty()
                && endDateText.isEmpty()
                && timeStartText.isEmpty()
                && timeEndText.isEmpty()
                && requestorName.isEmpty()
                && status.isEmpty()) {
            return null;
        }

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
            String key = keyFormat.format(dayCalendar.getTime());

            boolean isToday = isSameDate(dayCalendar, Calendar.getInstance());
            boolean isSelected = isSameDate(dayCalendar, selectedCalendar);
            int bookedCount = bookedDatesMap.containsKey(key) ? bookedDatesMap.get(key) : 0;

            styleCalendarDay(dayView, isSelected, isToday, bookedCount);

            dayView.setOnClickListener(v -> {
                selectedCalendar = (Calendar) dayCalendar.clone();
                renderCalendar();
                updateSelectedDateSection();
            });

            calendarGrid.addView(dayView);
        }
    }

    private ViewGroup.LayoutParams createDayLayoutParams() {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(6, 6, 6, 6);
        return params;
    }

    private void styleCalendarDay(TextView dayView, boolean selected, boolean today, int bookedCount) {
        // Match the Requestor Home calendar shade exactly: same colors and square shade corners.
        dayView.setTypeface(null, Typeface.NORMAL);

        if (selected) {
            dayView.setBackgroundColor(Color.parseColor("#313131"));
            dayView.setTextColor(Color.WHITE);
        } else if (bookedCount >= FULLY_BOOKED_LIMIT) {
            dayView.setBackgroundColor(Color.parseColor("#970705"));
            dayView.setTextColor(Color.WHITE);
        } else if (bookedCount > 0) {
            dayView.setBackgroundColor(Color.parseColor("#F3D9D9"));
            dayView.setTextColor(Color.parseColor("#313131"));
        } else if (today) {
            dayView.setBackgroundColor(Color.parseColor("#FFF0D8"));
            dayView.setTextColor(Color.parseColor("#313131"));
        } else {
            dayView.setBackgroundColor(Color.TRANSPARENT);
            dayView.setTextColor(Color.parseColor("#313131"));
        }
    }

    private void updateSelectedDateSection() {
        if (!isAdded()) return;

        updateSelectedDateLabel();

        if (schedsContainer == null) return;

        schedsContainer.removeAllViews();

        if (tvSelectedDate != null) {
            if (tvSelectedDate.getParent() != null) {
                ((ViewGroup) tvSelectedDate.getParent()).removeView(tvSelectedDate);
            }

            tvSelectedDate.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            schedsContainer.addView(tvSelectedDate);
        }

        String selectedKey = getDateKey(selectedCalendar);
        List<ScheduleItem> items = schedulesByDateMap.get(selectedKey);

        if (items == null || items.isEmpty()) {
            schedsContainer.addView(createEmptyScheduleView());
            return;
        }

        for (ScheduleItem item : items) {
            schedsContainer.addView(createScheduleCard(item));
        }
    }

    private View createScheduleCard(ScheduleItem item) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(16), 0, 0);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        row.setLayoutParams(rowParams);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> openGsoRequestDetails(item.requestId));

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
            timeText.setTypeface(null, Typeface.BOLD);

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
            titleText.setTypeface(null, Typeface.BOLD);
            detailsLayout.addView(titleText);
        }

        String subtitle = buildScheduleMeta(item, title);
        if (!subtitle.isEmpty()) {
            TextView subtitleText = new TextView(requireContext());
            subtitleText.setText(subtitle);
            subtitleText.setTextColor(Color.parseColor("#313131"));
            subtitleText.setTextSize(12f);
            subtitleText.setAlpha(0.72f);

            LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            subtitleParams.setMargins(0, dp(4), 0, 0);
            subtitleText.setLayoutParams(subtitleParams);
            detailsLayout.addView(subtitleText);
        }



        row.addView(detailsLayout);

        if (!item.status.isEmpty()) {
            Chip statusChip = new Chip(requireContext());

            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                    dp(104),
                    dp(34)
            );
            chipParams.setMargins(dp(4), 0, 0, 0);
            statusChip.setLayoutParams(chipParams);

            statusChip.setText(item.status);
            statusChip.setTextSize(11f);
            statusChip.setGravity(Gravity.CENTER);
            statusChip.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            statusChip.setSingleLine(true);
            statusChip.setEllipsize(TextUtils.TruncateAt.END);

            statusChip.setMinWidth(dp(104));
            statusChip.setMaxWidth(dp(104));
            statusChip.setMinHeight(dp(34));
            statusChip.setHeight(dp(34));
            statusChip.setChipMinHeight(dp(34));
            statusChip.setEnsureMinTouchTargetSize(false);

            statusChip.setTextColor(getStatusTextColor(item.status));
            statusChip.setChipBackgroundColor(ColorStateList.valueOf(getStatusBackgroundColor(item.status)));
            statusChip.setChipStrokeWidth(0);
            statusChip.setCheckable(false);
            statusChip.setClickable(false);
            statusChip.setFocusable(false);

            row.addView(statusChip);
        }

        return row;
    }

    private String buildScheduleMeta(ScheduleItem item, String title) {
        List<String> parts = new ArrayList<>();

        if (!item.facility.isEmpty() && !item.facility.equalsIgnoreCase(title)) {
            parts.add(item.facility);
        }

        String dateRange = buildDateRange(item.startDateText, item.endDateText);
        if (!dateRange.isEmpty()) parts.add(dateRange);

        String timeRange = buildTimeRange(item.timeStartText, item.timeEndText);
        if (!timeRange.isEmpty()) parts.add(timeRange);

        return joinParts(parts, " • ");
    }

    private String buildDateRange(String startDate, String endDate) {
        if (!startDate.isEmpty() && !endDate.isEmpty() && !startDate.equalsIgnoreCase(endDate)) {
            return startDate + " - " + endDate;
        }
        return !startDate.isEmpty() ? startDate : endDate;
    }

    private String buildTimeRange(String startTime, String endTime) {
        if (!startTime.isEmpty() && !endTime.isEmpty()) return startTime + " - " + endTime;
        return !startTime.isEmpty() ? startTime : endTime;
    }

    private String joinParts(List<String> parts, String separator) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) continue;
            if (builder.length() > 0) builder.append(separator);
            builder.append(part.trim());
        }
        return builder.toString();
    }


    private void updateSelectedDateLabel() {
        if (tvSelectedDate == null) return;

        SimpleDateFormat selectedFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        tvSelectedDate.setText(selectedFormat.format(selectedCalendar.getTime()));
    }

    private View createEmptyScheduleView() {
        TextView emptyView = new TextView(requireContext());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(16), 0, 0);

        emptyView.setLayoutParams(params);
        emptyView.setText("No scheduled requests for this date.");
        emptyView.setTextColor(Color.parseColor("#313131"));
        emptyView.setTextSize(14f);
        emptyView.setAlpha(0.72f);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(dp(12), dp(18), dp(12), dp(18));

        return emptyView;
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

    private int getStatusTextColor(String status) {
        if ("Approved".equalsIgnoreCase(status)) return Color.parseColor("#2E7D32");
        if ("Returned".equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(status)) return Color.parseColor("#970705");
        return Color.parseColor("#313131");
    }

    private int getStatusBackgroundColor(String status) {
        if ("Approved".equalsIgnoreCase(status)) return Color.parseColor("#E7F4E8");
        if ("Returned".equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(status)) return Color.parseColor("#F3D9D9");
        return Color.parseColor("#EEEEEE");
    }

    private String getDateKey(Calendar calendar) {
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return keyFormat.format(calendar.getTime());
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

    private String convertDateTextToKey(String dateText) {
        Calendar calendar = parseDateTextToCalendar(dateText);

        if (calendar == null) {
            return "";
        }

        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return keyFormat.format(calendar.getTime());
    }

    private String getMapString(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return "";
        }

        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value).trim();
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

    private void openGsoRequests(String filter) {
        Bundle bundle = new Bundle();
        bundle.putString("filter", filter);

        gsoRequestsFragment fragment = new gsoRequestsFragment();
        fragment.setArguments(bundle);

        openFragment(fragment);
    }

    private void openGsoRequestDetails(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) return;
        openFragment(gsoRequestsViewDetailsFragment.newInstance(requestId));
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



    private boolean isSameDate(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
                && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    private String getStringValue(DocumentSnapshot doc, String field) {
        if (doc == null || field == null) return "";
        Object value = doc.get(field);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) return first.trim();
        if (second != null && !second.trim().isEmpty()) return second.trim();
        return "";
    }

    private String formatCount(int count) {
        return String.format(Locale.getDefault(), "%02d", count);
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
