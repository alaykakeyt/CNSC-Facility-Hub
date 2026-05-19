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
 * 
 * CONFLICT DETECTION RULES:
 * - Checks if requested facilities overlap with existing requests
 * - Compares each day's schedule for overlapping time slots
 * - Only considers requests with conflict statuses: Pending, Approved, Approved - Available, Booked
 * - Excludes: Rejected, Returned, Cancelled, Upload Failed, etc.
 * 
 * SAFETY APPROACH:
 * - If time parsing fails due to invalid format, returns a conflict (blocks submission)
 * - This is conservative - better to block invalid data than to allow silent conflicts
 * - Trims and normalizes all time strings before comparison
 * 
 * LIMITATIONS:
 * - Scans all requests (no Firestore query optimization available)
 * - Firestore doesn't support complex array/time queries needed for filtering
 * - At scale, consider Cloud Functions or Firestore collection indexes
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
            String status = doc.getString("status");
            if (!isConflictStatus(status)) {
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

                    TimeOverlapResult overlap = isTimeOverlappingWithValidation(
                            newDay.getStartTimeText(),
                            newDay.getEndTimeText(),
                            existingDay.getStartTimeText(),
                            existingDay.getEndTimeText()
                    );
                    
                    if (overlap.hasConflict) {
                        return buildConflictMessage(
                                facilityLabel,
                                existingDay.getDateText(),
                                existingDay.getStartTimeText(),
                                existingDay.getEndTimeText()
                        );
                    }
                    
                    // If time parsing failed, treat it as a conflict (conservative approach)
                    if (overlap.parsingFailed) {
                        return "Unable to verify schedule availability due to invalid time format. "
                                + "Please check your times and try again.";
                    }
                }
            }
        }

        return "";
    }

    /**
     * Helper class to track both conflict detection and parsing errors
     */
    private static class TimeOverlapResult {
        boolean hasConflict;
        boolean parsingFailed;
        
        TimeOverlapResult(boolean hasConflict, boolean parsingFailed) {
            this.hasConflict = hasConflict;
            this.parsingFailed = parsingFailed;
        }
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

    /**
     * Check time overlap with validation and error tracking
     * 
     * @return TimeOverlapResult with hasConflict and parsingFailed flags
     */
    private static TimeOverlapResult isTimeOverlappingWithValidation(
            String newStart,
            String newEnd,
            String existingStart,
            String existingEnd
    ) {
        long newStartMs = RequestDataHelper.parseTimeToMillis(newStart);
        long newEndMs = RequestDataHelper.parseTimeToMillis(newEnd);
        long existingStartMs = RequestDataHelper.parseTimeToMillis(existingStart);
        long existingEndMs = RequestDataHelper.parseTimeToMillis(existingEnd);

        // If any time parsing failed, report parsing error (conservative approach)
        if (newStartMs == -1 || newEndMs == -1 || existingStartMs == -1 || existingEndMs == -1) {
            return new TimeOverlapResult(false, true);
        }

        // Check if times overlap: new event starts before existing event ends AND new event ends after existing starts
        boolean overlap = newStartMs < existingEndMs && newEndMs > existingStartMs;
        return new TimeOverlapResult(overlap, false);
    }

    /**
     * Legacy method for backward compatibility (treats parse errors as no conflict)
     * Deprecated: Use isTimeOverlappingWithValidation instead
     */
    @Deprecated
    private static boolean isTimeOverlapping(
            String newStart,
            String newEnd,
            String existingStart,
            String existingEnd
    ) {
        TimeOverlapResult result = isTimeOverlappingWithValidation(newStart, newEnd, existingStart, existingEnd);
        // Legacy behavior: if parsing failed, treat as no conflict (less safe)
        return result.hasConflict;
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
