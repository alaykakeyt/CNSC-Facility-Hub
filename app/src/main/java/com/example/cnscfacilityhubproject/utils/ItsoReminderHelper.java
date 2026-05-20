package com.example.cnscfacilityhubproject.utils;

import com.example.cnscfacilityhubproject.models.ScheduleDayItem;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Client-side helper for ITSO upcoming technical support reminders (1 day before event).
 */
public final class ItsoReminderHelper {

    private ItsoReminderHelper() {
    }

    public static boolean isUpcomingTechnicalEvent(DocumentSnapshot doc) {
        if (doc == null) {
            return false;
        }

        if (!Boolean.TRUE.equals(doc.getBoolean("needsITSO"))) {
            return false;
        }

        String status = safe(doc.getString("status"));
        String itsoStatus = safe(doc.getString("itsoStatus"));

        if (!isApprovedStatus(status) && !isApprovedStatus(itsoStatus)) {
            return false;
        }

        return hasScheduleOnTargetDay(doc, getTomorrowStartCalendar());
    }

    public static boolean isReminderUnseen(DocumentSnapshot doc) {
        Boolean seen = doc.getBoolean("itsoReminderSeen");
        return !Boolean.TRUE.equals(seen);
    }

    public static String buildReminderSummary(DocumentSnapshot doc) {
        String purpose = safe(doc.getString("purpose"));
        String facilities = RequestDataHelper.getFacilitiesDisplay(doc);
        String schedule = RequestDataHelper.getScheduleDisplay(doc);
        String requestor = firstNonEmpty(
                safe(doc.getString("requestorName")),
                safe(doc.getString("fullName"))
        );
        String contact = firstNonEmpty(
                safe(doc.getString("contactNumber")),
                safe(doc.getString("contactNum"))
        );

        return "Event: " + (purpose.isEmpty() ? "Technical Support" : purpose)
                + "\nFacilities: " + facilities
                + "\nSchedule:\n" + schedule
                + "\nEquipment:\n" + buildEquipmentList(doc)
                + "\nRequestor: " + requestor
                + "\nContact: " + contact;
    }

    public static String buildEquipmentList(DocumentSnapshot doc) {
        StringBuilder builder = new StringBuilder();
        appendIfTrue(builder, doc, "soundSystemSetup", "Sound System");
        appendIfTrue(builder, doc, "microphones", "Microphones");
        appendIfTrue(builder, doc, "portableSpeaker", "Portable Speaker");
        appendIfTrue(builder, doc, "lights", "Lights");
        appendIfTrue(builder, doc, "livestreamingServices", "Livestreaming");
        appendIfTrue(builder, doc, "zoomHosting", "Zoom Hosting");
        appendIfTrue(builder, doc, "gmeetHosting", "Google Meet Hosting");
        appendIfTrue(builder, doc, "webCamera", "Web Camera");
        appendIfTrue(builder, doc, "tripod", "Tripod");
        appendIfTrue(builder, doc, "multimediaProjector", "Multimedia Projector");

        String connectors = safe(doc.getString("connectors"));
        if (!connectors.isEmpty()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("Connectors: ").append(connectors);
        }

        return builder.length() == 0 ? "None listed" : builder.toString();
    }

    private static boolean isApprovedStatus(String status) {
        if (status.isEmpty()) {
            return false;
        }
        String lower = status.toLowerCase(Locale.getDefault());
        return lower.equals("approved")
                || lower.equals("approved - available")
                || lower.equals("booked");
    }

    private static boolean hasScheduleOnTargetDay(DocumentSnapshot doc, Calendar targetDay) {
        List<ScheduleDayItem> days = RequestDataHelper.getScheduleDays(doc);
        if (days.isEmpty()) {
            return false;
        }

        Calendar targetStart = (Calendar) targetDay.clone();
        targetStart.set(Calendar.HOUR_OF_DAY, 0);
        targetStart.set(Calendar.MINUTE, 0);
        targetStart.set(Calendar.SECOND, 0);
        targetStart.set(Calendar.MILLISECOND, 0);

        Calendar targetEnd = (Calendar) targetStart.clone();
        targetEnd.add(Calendar.DAY_OF_MONTH, 1);

        long targetStartMs = targetStart.getTimeInMillis();
        long targetEndMs = targetEnd.getTimeInMillis();

        for (ScheduleDayItem day : days) {
            Timestamp ts = day.getDate();
            if (ts == null) {
                long parsed = RequestDataHelper.parseDateToMillis(day.getDateText());
                if (parsed == -1) {
                    continue;
                }
                if (parsed >= targetStartMs && parsed < targetEndMs) {
                    return true;
                }
            } else {
                long dayMs = ts.toDate().getTime();
                if (dayMs >= targetStartMs && dayMs < targetEndMs) {
                    return true;
                }
            }
        }

        return false;
    }

    public static Calendar getTomorrowStartCalendar() {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        return tomorrow;
    }

    private static void appendIfTrue(StringBuilder builder, DocumentSnapshot doc, String field, String label) {
        if (Boolean.TRUE.equals(doc.getBoolean(field))) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(label);
        }
    }

    private static String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        return second != null ? second.trim() : "";
    }

    private static String safe(String value) {
        return value != null ? value.trim() : "";
    }
}
