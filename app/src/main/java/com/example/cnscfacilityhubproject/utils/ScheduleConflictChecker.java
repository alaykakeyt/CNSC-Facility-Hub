package com.example.cnscfacilityhubproject.utils;

import com.example.cnscfacilityhubproject.models.ScheduleDayItem;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Detects schedule conflicts for multi-facility and multi-day requests.
 */
public final class ScheduleConflictChecker {

    private static final Set<String> CONFLICT_STATUSES = new HashSet<>(Arrays.asList(
            "pending",
            "approved",
            "approved - available",
            "booked"
    ));

    private ScheduleConflictChecker() {
    }

    public static String findConflictMessage(
            List<String> requestedFacilityNames,
            List<String> requestedFacilityKeys,
            List<ScheduleDayItem> requestedDays,
            Iterable<QueryDocumentSnapshot> existingRequests
    ) {
        if (requestedFacilityKeys.isEmpty() || requestedDays.isEmpty()) {
            return "";
        }

        Set<String> requestedKeys = new HashSet<>(requestedFacilityKeys);

        for (QueryDocumentSnapshot doc : existingRequests) {
            if (!isConflictStatus(doc.getString("status"))) {
                continue;
            }

            List<String> existingKeys = RequestDataHelper.getFacilityKeys(doc);
            String conflictingKey = findOverlappingFacility(requestedKeys, existingKeys);
            if (conflictingKey == null) {
                continue;
            }

            String facilityLabel = resolveFacilityDisplayName(
                    conflictingKey,
                    requestedFacilityNames,
                    requestedFacilityKeys
            );

            List<ScheduleDayItem> existingDays = RequestDataHelper.getScheduleDays(doc);

            for (ScheduleDayItem newDay : requestedDays) {
                for (ScheduleDayItem existingDay : existingDays) {
                    if (!isSameDate(newDay.getDateText(), existingDay.getDateText())) {
                        continue;
                    }

                    if (isTimeOverlapping(
                            newDay.getStartTimeText(),
                            newDay.getEndTimeText(),
                            existingDay.getStartTimeText(),
                            existingDay.getEndTimeText()
                    )) {
                        return buildConflictMessage(
                                facilityLabel,
                                existingDay.getDateText(),
                                existingDay.getStartTimeText(),
                                existingDay.getEndTimeText()
                        );
                    }
                }
            }
        }

        return "";
    }

    public static boolean isConflictStatus(String status) {
        if (status == null) {
            return false;
        }
        return CONFLICT_STATUSES.contains(status.trim().toLowerCase(Locale.getDefault()));
    }

    private static String findOverlappingFacility(Set<String> requestedKeys, List<String> existingKeys) {
        for (String existingKey : existingKeys) {
            if (requestedKeys.contains(existingKey)) {
                return existingKey;
            }
        }
        return null;
    }

    private static boolean isSameDate(String dateA, String dateB) {
        if (dateA == null || dateB == null) {
            return false;
        }
        return dateA.trim().equalsIgnoreCase(dateB.trim());
    }

    private static boolean isTimeOverlapping(
            String newStart,
            String newEnd,
            String existingStart,
            String existingEnd
    ) {
        long newStartMs = RequestDataHelper.parseTimeToMillis(newStart);
        long newEndMs = RequestDataHelper.parseTimeToMillis(newEnd);
        long existingStartMs = RequestDataHelper.parseTimeToMillis(existingStart);
        long existingEndMs = RequestDataHelper.parseTimeToMillis(existingEnd);

        if (newStartMs == -1 || newEndMs == -1 || existingStartMs == -1 || existingEndMs == -1) {
            return false;
        }

        return newStartMs < existingEndMs && newEndMs > existingStartMs;
    }

    public static String buildConflictMessage(
            String facilityLabel,
            String dateText,
            String startTime,
            String endTime
    ) {
        if (facilityLabel == null || facilityLabel.trim().isEmpty()) {
            facilityLabel = "the selected facility";
        }

        return "This request conflicts with an existing schedule for "
                + facilityLabel
                + " on "
                + dateText
                + " from "
                + startTime
                + " to "
                + endTime
                + ".";
    }

    /** Resolve display name for conflict message from facility keys and names. */
    public static String resolveFacilityDisplayName(
            String facilityKey,
            List<String> facilityNames,
            List<String> facilityKeys
    ) {
        for (int i = 0; i < facilityKeys.size() && i < facilityNames.size(); i++) {
            if (facilityKey.equals(facilityKeys.get(i))) {
                return facilityNames.get(i);
            }
        }
        return facilityKey.replace('_', ' ');
    }
}
