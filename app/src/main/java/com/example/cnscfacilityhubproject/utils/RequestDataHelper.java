package com.example.cnscfacilityhubproject.utils;

import com.example.cnscfacilityhubproject.models.ProposalFileItem;
import com.example.cnscfacilityhubproject.models.ScheduleDayItem;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
                                String.valueOf(map.get("fileType"))
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
                    guessTypeFromName(legacyName)
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
