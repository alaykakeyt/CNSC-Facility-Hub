package com.example.cnscfacilityhubproject.utils;

import com.example.cnscfacilityhubproject.models.ProposalFileItem;
import com.example.cnscfacilityhubproject.models.ScheduleDayItem;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reads request fields from Firestore with backward compatibility for legacy documents.
 */
public final class RequestDataHelper {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

    private RequestDataHelper() {
    }

    public static List<String> getFacilityNames(DocumentSnapshot doc) {
        List<String> facilities = readStringList(doc, "facilities");
        if (!facilities.isEmpty()) {
            return facilities;
        }

        String finalName = safeString(doc.getString("finalFacilityName"));
        if (!finalName.isEmpty()) {
            List<String> split = new ArrayList<>();
            for (String part : finalName.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    split.add(trimmed);
                }
            }
            if (!split.isEmpty()) {
                return split;
            }
        }

        String facility = safeString(doc.getString("facility"));
        String other = safeString(doc.getString("otherFacility"));
        if ("Others".equalsIgnoreCase(facility) && !other.isEmpty()) {
            return singleList(other);
        }
        if (!facility.isEmpty()) {
            return singleList(facility);
        }
        return new ArrayList<>();
    }

    public static List<String> getFacilityKeys(DocumentSnapshot doc) {
        List<String> keys = readStringList(doc, "facilityKeys");
        if (!keys.isEmpty()) {
            return keys;
        }

        List<String> names = getFacilityNames(doc);
        List<String> normalized = new ArrayList<>();
        for (String name : names) {
            String key = normalizeFacilityKey(name);
            if (!key.isEmpty()) {
                normalized.add(key);
            }
        }
        if (!normalized.isEmpty()) {
            return normalized;
        }

        String legacyKey = safeString(doc.getString("facilityKey"));
        if (!legacyKey.isEmpty()) {
            return singleList(legacyKey);
        }
        return new ArrayList<>();
    }

    public static String getFacilitiesDisplay(DocumentSnapshot doc) {
        List<String> facilities = getFacilityNames(doc);
        if (facilities.isEmpty()) {
            return "Not specified";
        }
        return TextJoin.join(facilities, ", ");
    }

    public static List<ScheduleDayItem> getScheduleDays(DocumentSnapshot doc) {
        List<ScheduleDayItem> days = new ArrayList<>();

        if (doc.contains("scheduleDays")) {
            Object raw = doc.get("scheduleDays");
            if (raw instanceof List) {
                for (Object item : (List<?>) raw) {
                    if (item instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) item;
                        String dateText = String.valueOf(map.get("dateText"));
                        Timestamp date = map.get("date") instanceof Timestamp
                                ? (Timestamp) map.get("date") : null;
                        String start = String.valueOf(map.get("startTimeText"));
                        String end = String.valueOf(map.get("endTimeText"));
                        days.add(new ScheduleDayItem(dateText, date, start, end));
                    }
                }
            }
        }

        if (!days.isEmpty()) {
            return days;
        }

        String startDateText = safeString(doc.getString("startDateText"));
        String endDateText = safeString(doc.getString("endDateText"));
        String startTime = safeString(doc.getString("timeStartText"));
        String endTime = safeString(doc.getString("timeEndText"));

        Timestamp startTs = doc.getTimestamp("startDate");
        Timestamp endTs = doc.getTimestamp("endDate");

        if (!startDateText.isEmpty()) {
            days.add(new ScheduleDayItem(
                    startDateText,
                    startTs,
                    startTime,
                    endTime
            ));
        } else if (endTs != null || startTs != null) {
            Timestamp use = startTs != null ? startTs : endTs;
            days.add(new ScheduleDayItem(
                    DATE_FORMAT.format(use.toDate()),
                    use,
                    startTime,
                    endTime
            ));
        }

        return days;
    }

    public static String getScheduleDisplay(DocumentSnapshot doc) {
        List<ScheduleDayItem> days = getScheduleDays(doc);
        if (days.isEmpty()) {
            return "Not specified";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < days.size(); i++) {
            ScheduleDayItem day = days.get(i);
            if (i > 0) {
                builder.append("\n");
            }
            builder.append(day.getDateText());
            if (!day.getStartTimeText().isEmpty() && !day.getEndTimeText().isEmpty()) {
                builder.append(" • ")
                        .append(day.getStartTimeText())
                        .append(" – ")
                        .append(day.getEndTimeText());
            }
        }
        return builder.toString();
    }

    public static List<ProposalFileItem> getProposalFiles(DocumentSnapshot doc) {
        List<ProposalFileItem> files = new ArrayList<>();

        if (doc.contains("proposalFiles")) {
            Object raw = doc.get("proposalFiles");
            if (raw instanceof List) {
                for (Object item : (List<?>) raw) {
                    if (item instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) item;
                        ProposalFileItem file = new ProposalFileItem(
                                String.valueOf(map.get("fileName")),
                                String.valueOf(map.get("fileUrl")),
                                String.valueOf(map.get("fileType")),
                                String.valueOf(map.get("source") != null ? map.get("source") : "external_link")
                        );
                        files.add(file);
                    }
                }
            }
        }

        if (!files.isEmpty()) {
            return files;
        }

        String legacyUrl = safeString(doc.getString("proposalFileUrl"));
        String legacyName = safeString(doc.getString("proposalFileName"));
        if (!legacyUrl.isEmpty()) {
            files.add(new ProposalFileItem(
                    legacyName.isEmpty() ? "Proposal file" : legacyName,
                    legacyUrl,
                    guessTypeFromName(legacyName),
                    ""
            ));
        }

        return files;
    }

    public static boolean hasStudentCenter(List<String> facilityNames) {
        for (String name : facilityNames) {
            if ("Student Center".equalsIgnoreCase(name.trim())) {
                return true;
            }
        }
        return false;
    }

    public static String normalizeFacilityKey(String facilityName) {
        if (facilityName == null) {
            return "";
        }
        return facilityName.trim()
                .toLowerCase(Locale.getDefault())
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
    }

    public static List<String> buildFacilityKeys(List<String> facilityNames) {
        List<String> keys = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String name : facilityNames) {
            String key = normalizeFacilityKey(name);
            if (!key.isEmpty() && seen.add(key)) {
                keys.add(key);
            }
        }
        return keys;
    }

    public static String buildFinalFacilityName(List<String> facilityNames) {
        return TextJoin.join(facilityNames, ", ");
    }

    public static long parseDateToMillis(String dateText) {
        try {
            if (dateText == null || dateText.trim().isEmpty()) {
                return -1;
            }
            java.util.Date date = DATE_FORMAT.parse(dateText.trim());
            if (date != null) {
                return date.getTime();
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    public static long parseTimeToMillis(String timeText) {
        try {
            if (timeText == null || timeText.trim().isEmpty()) {
                return -1;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            java.util.Date date = sdf.parse(timeText.trim());
            if (date != null) {
                return date.getTime();
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    public static Timestamp parseDateToTimestamp(String dateText) {
        try {
            java.util.Date date = DATE_FORMAT.parse(dateText);
            if (date != null) {
                return new Timestamp(date);
            }
        } catch (Exception ignored) {
        }
        return Timestamp.now();
    }

    public static List<ScheduleDayItem> buildScheduleDaysBetween(
            String startDateText,
            String endDateText,
            String singleStartTime,
            String singleEndTime,
            Map<String, String> perDayEndTimes,
            Map<String, String> perDayStartTimes
    ) {
        List<ScheduleDayItem> days = new ArrayList<>();
        long startMillis = parseDateToMillis(startDateText);
        long endMillis = parseDateToMillis(endDateText);
        if (startMillis == -1 || endMillis == -1) {
            return days;
        }

        Calendar cursor = Calendar.getInstance();
        cursor.setTimeInMillis(startMillis);

        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(endMillis);

        boolean multiDay = !startDateText.equalsIgnoreCase(endDateText);

        while (!cursor.after(end)) {
            String dateText = DATE_FORMAT.format(cursor.getTime());
            Timestamp ts = new Timestamp(cursor.getTime());

            String dayStart = multiDay
                    ? perDayStartTimes.get(dateText)
                    : singleStartTime;
            String dayEnd = multiDay
                    ? perDayEndTimes.get(dateText)
                    : singleEndTime;

            if (dayStart == null) {
                dayStart = "";
            }
            if (dayEnd == null) {
                dayEnd = "";
            }

            days.add(new ScheduleDayItem(dateText, ts, dayStart, dayEnd));
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }

        return days;
    }

    public static String guessTypeFromName(String fileName) {
        String lower = fileName != null ? fileName.toLowerCase(Locale.getDefault()) : "";
        if (lower.endsWith(".pdf")) {
            return "pdf";
        }
        if (lower.endsWith(".docx")) {
            return "docx";
        }
        if (lower.endsWith(".png")) {
            return "png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "jpg";
        }
        return "file";
    }

    public static String guessMimeType(String fileName) {
        String type = guessTypeFromName(fileName);
        switch (type) {
            case "pdf":
                return "application/pdf";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "png":
                return "image/png";
            case "jpg":
                return "image/jpeg";
            default:
                return "*/*";
        }
    }

    private static List<String> readStringList(DocumentSnapshot doc, String field) {
        List<String> result = new ArrayList<>();
        Object raw = doc.get(field);
        if (raw instanceof List) {
            for (Object item : (List<?>) raw) {
                if (item != null) {
                    String value = String.valueOf(item).trim();
                    if (!value.isEmpty()) {
                        result.add(value);
                    }
                }
            }
        }
        return result;
    }

    private static List<String> singleList(String value) {
        List<String> list = new ArrayList<>();
        list.add(value);
        return list;
    }

    private static String safeString(String value) {
        return value != null ? value.trim() : "";
    }

    /**
     * Whether a request should appear in normal request lists.
     * Hides incomplete uploads; keeps legacy documents without isSubmissionComplete.
     */
    public static boolean shouldShowInRequestList(DocumentSnapshot doc) {
        if (doc == null) {
            return false;
        }

        Boolean isSubmissionComplete = doc.getBoolean("isSubmissionComplete");
        if (Boolean.FALSE.equals(isSubmissionComplete)) {
            return false;
        }

        if (Boolean.TRUE.equals(isSubmissionComplete)) {
            return true;
        }

        String status = safeString(doc.getString("status"));
        String uploadStatus = safeString(doc.getString("uploadStatus"));

        if ("Uploading".equalsIgnoreCase(status)
                || "Upload Failed".equalsIgnoreCase(status)
                || "Uploading".equalsIgnoreCase(uploadStatus)
                || "Failed".equalsIgnoreCase(uploadStatus)) {
            return false;
        }

        return true;
    }

    public static boolean isActiveAppointment(DocumentSnapshot doc) {
        if (doc == null) return false;

        // Basic requirement: must be submitted
        if (!shouldShowInRequestList(doc)) return false;

        String status = doc.getString("status");
        if (status == null) status = "";

        String workflowStage = doc.getString("workflowStage");
        if (workflowStage == null) workflowStage = "";

        // Non-active statuses (if it's already definitively finished)
        if (status.equalsIgnoreCase("Rejected") ||
                status.equalsIgnoreCase("Returned") ||
                status.equalsIgnoreCase("Cancelled") ||
                status.equalsIgnoreCase("Completed") ||
                status.equalsIgnoreCase("Upload Failed")) {
            return false;
        }

        // Active statuses or workflow stages
        List<String> activeStatuses = Arrays.asList(
                "Pending", "Approved", "Approved - Available", "Booked",
                "For SAC Review", "For ITSO Review", "For GSO Review",
                "SAC_REVIEW", "ITSO_REVIEW", "GSO_REVIEW"
        );

        List<String> activeStages = Arrays.asList(
                "SAC_REVIEW", "ITSO_REVIEW", "WAITING_ITSO_APPROVAL",
                "WAITING_SAC_APPROVAL", "GSO_REVIEW", "APPROVED"
        );

        boolean isStatusActive = false;
        for (String s : activeStatuses) {
            if (status.equalsIgnoreCase(s)) {
                isStatusActive = true;
                break;
            }
        }

        if (!isStatusActive) {
            for (String s : activeStages) {
                if (workflowStage.equalsIgnoreCase(s)) {
                    isStatusActive = true;
                    break;
                }
            }
        }

        // If it's one of the active statuses/stages, check if it has ended yet.
        if (isStatusActive) {
            return !hasAppointmentEnded(doc);
        }

        return false;
    }

    public static boolean hasAppointmentEnded(DocumentSnapshot doc) {
        long endMillis = getAppointmentEndMillis(doc);
        if (endMillis == -1) return false;
        return System.currentTimeMillis() > endMillis;
    }

    public static long getAppointmentEndMillis(DocumentSnapshot doc) {
        List<ScheduleDayItem> days = getScheduleDays(doc);
        if (days.isEmpty()) {
            return -1;
        }

        // Find the day with the latest date
        ScheduleDayItem latestDay = null;
        long latestDateMillis = -1;

        for (ScheduleDayItem day : days) {
            long dateMillis = parseDateToMillis(day.getDateText());
            if (dateMillis > latestDateMillis) {
                latestDateMillis = dateMillis;
                latestDay = day;
            }
        }

        if (latestDay == null) return -1;

        // Combine date and time
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(latestDateMillis);

            String endTime = latestDay.getEndTimeText();
            if (endTime.isEmpty()) {
                // If no end time, assume end of day (11:59 PM)
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                return calendar.getTimeInMillis();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            java.util.Date time = sdf.parse(endTime);
            if (time != null) {
                Calendar timeCal = Calendar.getInstance();
                timeCal.setTime(time);
                calendar.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
                calendar.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
            }
            return calendar.getTimeInMillis();
        } catch (Exception e) {
            return latestDateMillis;
        }
    }

    public static boolean isRequestorNotificationUnseen(DocumentSnapshot doc) {
        if (doc == null) return false;
        
        Boolean notificationForRequestor = doc.getBoolean("notificationForRequestor");
        Boolean requestorSeen = doc.getBoolean("requestorSeen");
        Boolean requestorNotificationSeen = doc.getBoolean("requestorNotificationSeen");
        Boolean requestorApprovedSeen = doc.getBoolean("requestorApprovedSeen");

        return Boolean.TRUE.equals(notificationForRequestor)
                && !Boolean.TRUE.equals(requestorSeen)
                && !Boolean.TRUE.equals(requestorNotificationSeen)
                && !Boolean.TRUE.equals(requestorApprovedSeen);
    }

    public static boolean isSACRelevantRequest(DocumentSnapshot doc) {
        if (doc == null) return false;

        // Check explicit routing fields
        if (Boolean.TRUE.equals(doc.getBoolean("sendToSAC"))) return true;
        if (Boolean.TRUE.equals(doc.getBoolean("notificationForSAC"))) return true;
        if (Boolean.TRUE.equals(doc.getBoolean("notificationForSac"))) return true;

        String target = safeString(doc.getString("notificationTarget"));
        if ("SAC".equalsIgnoreCase(target)) return true;

        String stage = safeString(doc.getString("workflowStage"));
        if (stage.startsWith("SAC_") || stage.contains("_SAC_")) return true;

        String sacStatus = safeString(doc.getString("sacStatus"));
        if (!sacStatus.isEmpty()) return true;

        // Check if Student Center is among requested facilities
        List<String> facilities = getFacilityNames(doc);
        for (String f : facilities) {
            String lower = f.toLowerCase(Locale.ROOT);
            if (lower.contains("student center")) return true;
        }

        return false;
    }

    public static boolean isSACPendingAction(DocumentSnapshot doc) {
        if (!isSACRelevantRequest(doc)) return false;
        if (!shouldShowInRequestList(doc)) return false;

        String status = safeString(doc.getString("status"));
        if (status.equalsIgnoreCase("Rejected") ||
                status.equalsIgnoreCase("Returned") ||
                status.equalsIgnoreCase("Cancelled") ||
                status.equalsIgnoreCase("Completed")) {
            return false;
        }

        String sacStatus = safeString(doc.getString("sacStatus"));
        if (sacStatus.equalsIgnoreCase("Approved") || sacStatus.equalsIgnoreCase("Rejected")) {
            return false;
        }

        // It is relevant, not terminal, and SAC hasn't approved/rejected yet.
        return true;
    }

    public static boolean isSACUnseenNotification(DocumentSnapshot doc) {
        if (!isSACRelevantRequest(doc)) return false;

        Boolean sacNotificationSeen = doc.getBoolean("sacNotificationSeen");
        Boolean sacSeen = doc.getBoolean("sacSeen");

        return !Boolean.TRUE.equals(sacNotificationSeen) && !Boolean.TRUE.equals(sacSeen);
    }

    public static boolean needsITSO(DocumentSnapshot doc) {
        if (doc == null) return false;

        Boolean technicalNeeded = doc.getBoolean("technicalNeeded");
        if (Boolean.FALSE.equals(technicalNeeded)) return false;

        // If technicalNeeded is null or true, check the specific flags
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
                || !safeString(doc.getString("connectors")).isEmpty();
    }

    /** Tiny helper to avoid importing android.text.TextUtils in a util used widely. */
    public static final class TextJoin {
        public static String join(List<String> items, String separator) {
            if (items == null || items.isEmpty()) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    builder.append(separator);
                }
                builder.append(items.get(i));
            }
            return builder.toString();
        }
    }
}
