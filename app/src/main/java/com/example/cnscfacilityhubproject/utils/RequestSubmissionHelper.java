package com.example.cnscfacilityhubproject.utils;

import android.net.Uri;
import android.util.Log;

import com.example.cnscfacilityhubproject.models.ProposalFileItem;
import com.example.cnscfacilityhubproject.models.ScheduleDayItem;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds Firestore request maps and handles proposal links.
 * Firebase Storage upload has been removed to avoid billing dependencies.
 */
public final class RequestSubmissionHelper {

    private RequestSubmissionHelper() {
    }

    public static Map<String, Object> buildRequestMap(
            String userId,
            String activityType,
            String requestorName,
            String contactNumber,
            String collegeDepartment,
            String officeCourse,
            List<String> facilityNames,
            List<String> facilityKeys,
            String facilityLegacy,
            String otherFacility,
            String finalFacilityName,
            String facilityKeyLegacy,
            List<ScheduleDayItem> scheduleDays,
            String participants,
            long numberOfParticipants,
            String purpose,
            boolean tablesRequested,
            long tablesCount,
            boolean chairsRequested,
            long chairsCount,
            String otherAmenities,
            boolean agreementAccepted,
            boolean technicalNeeded,
            String connectors,
            boolean soundSystemSetup,
            boolean microphones,
            boolean portableSpeaker,
            boolean lights,
            boolean livestreamingServices,
            boolean zoomHosting,
            boolean gmeetHosting,
            boolean webCamera,
            boolean tripod,
            boolean multimediaProjector,
            boolean needsSAC,
            boolean needsITSO,
            boolean sendToSAC,
            boolean sendToITSO,
            boolean sendToGSO,
            String notificationTarget,
            String workflowStage,
            List<ProposalFileItem> proposalLinks
    ) {
        Map<String, Object> requestMap = new HashMap<>();

        requestMap.put("userId", userId);
        requestMap.put("activityType", activityType);
        requestMap.put("requestorName", requestorName);
        requestMap.put("contactNumber", contactNumber);
        requestMap.put("collegeDepartment", collegeDepartment);
        requestMap.put("officeCourse", officeCourse);

        requestMap.put("facilities", facilityNames);
        requestMap.put("facilityKeys", facilityKeys);
        requestMap.put("hasMultipleFacilities", facilityNames.size() > 1);

        requestMap.put("facility", facilityLegacy);
        requestMap.put("otherFacility", otherFacility);
        requestMap.put("finalFacilityName", finalFacilityName);
        requestMap.put("facilityKey", facilityKeyLegacy);

        List<Map<String, Object>> scheduleDayMaps = new ArrayList<>();
        for (ScheduleDayItem day : scheduleDays) {
            scheduleDayMaps.add(day.toFirestoreMap());
        }
        requestMap.put("scheduleDays", scheduleDayMaps);
        requestMap.put("hasMultipleScheduleDays", scheduleDays.size() > 1);

        if (!scheduleDays.isEmpty()) {
            ScheduleDayItem first = scheduleDays.get(0);
            ScheduleDayItem last = scheduleDays.get(scheduleDays.size() - 1);
            requestMap.put("startDateText", first.getDateText());
            requestMap.put("endDateText", last.getDateText());
            requestMap.put("timeStartText", first.getStartTimeText());
            requestMap.put("timeEndText", last.getEndTimeText());
            requestMap.put("startDate", first.getDate() != null ? first.getDate() : com.google.firebase.Timestamp.now());
            requestMap.put("endDate", last.getDate() != null ? last.getDate() : com.google.firebase.Timestamp.now());
        }

        requestMap.put("participants", participants);
        requestMap.put("numberOfParticipants", numberOfParticipants);
        requestMap.put("purpose", purpose);

        requestMap.put("tablesRequested", tablesRequested);
        requestMap.put("tablesCount", tablesCount);
        requestMap.put("chairsRequested", chairsRequested);
        requestMap.put("chairsCount", chairsCount);
        requestMap.put("otherAmenities", otherAmenities);
        requestMap.put("agreementAccepted", agreementAccepted);

        // Handle Proposal Links
        List<Map<String, Object>> linkMaps = new ArrayList<>();
        String firstLinkName = "";
        String firstLinkUrl = "";

        if (proposalLinks != null && !proposalLinks.isEmpty()) {
            for (ProposalFileItem link : proposalLinks) {
                linkMaps.add(link.toFirestoreMap());
            }
            firstLinkName = proposalLinks.get(0).getFileName();
            firstLinkUrl = proposalLinks.get(0).getFileUrl();
        }

        requestMap.put("proposalFiles", linkMaps);
        requestMap.put("proposalFileName", firstLinkName);
        requestMap.put("proposalFileUrl", firstLinkUrl);
        requestMap.put("proposalSource", "external_link");
        requestMap.put("hasProposalLinks", !linkMaps.isEmpty());

        requestMap.put("technicalNeeded", technicalNeeded);
        requestMap.put("connectors", technicalNeeded ? connectors : "");
        requestMap.put("soundSystemSetup", soundSystemSetup);
        requestMap.put("microphones", microphones);
        requestMap.put("portableSpeaker", portableSpeaker);
        requestMap.put("lights", lights);
        requestMap.put("livestreamingServices", livestreamingServices);
        requestMap.put("zoomHosting", zoomHosting);
        requestMap.put("gmeetHosting", gmeetHosting);
        requestMap.put("webCamera", webCamera);
        requestMap.put("tripod", tripod);
        requestMap.put("multimediaProjector", multimediaProjector);

        requestMap.put("needsSAC", needsSAC);
        requestMap.put("sendToSAC", sendToSAC);
        requestMap.put("sacStatus", needsSAC ? "Pending" : "Not Required");
        requestMap.put("sacApproved", false);
        requestMap.put("sacRemarks", "");
        requestMap.put("sacNotificationSeen", false);
        requestMap.put("sacSeen", false);

        requestMap.put("needsITSO", needsITSO);
        requestMap.put("sendToITSO", sendToITSO);
        requestMap.put("itsoStatus", needsITSO ? (sendToITSO ? "Pending" : "Waiting") : "Not Required");
        requestMap.put("itsoAvailability", "");
        requestMap.put("itsoRemarks", "");
        requestMap.put("itsoNotificationSeen", false);
        requestMap.put("itsoSeen", false);
        requestMap.put("itsoReminderSeen", false);
        requestMap.put("itsoUpcomingReminder", false);

        requestMap.put("needsGSO", true);
        requestMap.put("sendToGSO", sendToGSO);
        requestMap.put("gsoStatus", sendToGSO ? "Pending" : "Waiting");
        requestMap.put("gsoAvailability", "");
        requestMap.put("gsoNotificationSeen", false);
        requestMap.put("gsoSeen", false);

        requestMap.put("notificationTarget", notificationTarget);
        requestMap.put("workflowStage", workflowStage);

        // New submissions are complete by default as they use external links
        requestMap.put("status", "Pending");
        requestMap.put("uploadStatus", "Not Required");
        requestMap.put("isSubmissionComplete", true);

        requestMap.put("createdAt", FieldValue.serverTimestamp());
        requestMap.put("updatedAt", FieldValue.serverTimestamp());

        return requestMap;
    }
}
