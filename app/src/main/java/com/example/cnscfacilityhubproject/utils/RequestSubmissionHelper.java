package com.example.cnscfacilityhubproject.utils;

import android.content.Context;
import android.net.Uri;

import com.example.cnscfacilityhubproject.models.LocalFileItem;
import com.example.cnscfacilityhubproject.models.ProposalFileItem;
import com.example.cnscfacilityhubproject.models.ScheduleDayItem;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds Firestore request maps and uploads proposal files to Firebase Storage.
 */
public final class RequestSubmissionHelper {

    public interface UploadCallback {
        void onSuccess(List<ProposalFileItem> uploadedFiles);

        void onFailure(String message);
    }

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
            String workflowStage
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
            requestMap.put("startDate", first.getDate() != null ? first.getDate() : Timestamp.now());
            requestMap.put("endDate", last.getDate() != null ? last.getDate() : Timestamp.now());
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

        requestMap.put("proposalFiles", new ArrayList<Map<String, Object>>());
        requestMap.put("proposalFileName", "");
        requestMap.put("proposalFileUrl", "");

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
        requestMap.put("status", "Pending");
        requestMap.put("createdAt", FieldValue.serverTimestamp());
        requestMap.put("updatedAt", FieldValue.serverTimestamp());

        return requestMap;
    }

    public static void uploadProposalFiles(
            Context context,
            String requestId,
            List<LocalFileItem> files,
            UploadCallback callback
    ) {
        if (files == null || files.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        List<com.google.android.gms.tasks.Task<ProposalFileItem>> tasks = new ArrayList<>();

        for (LocalFileItem file : files) {
            String safeName = file.getFileName().replaceAll("[^a-zA-Z0-9._-]", "_");
            String storagePath = "proposal_files/" + requestId + "/" + UUID.randomUUID() + "_" + safeName;
            StorageReference reference = storage.getReference().child(storagePath);

            com.google.android.gms.tasks.Task<ProposalFileItem> task = reference.putFile(file.getUri())
                    .continueWithTask(taskSnapshot -> reference.getDownloadUrl())
                    .continueWith(urlTask -> {
                        if (!urlTask.isSuccessful() || urlTask.getResult() == null) {
                            throw urlTask.getException() != null
                                    ? urlTask.getException()
                                    : new Exception("Upload failed.");
                        }
                        Uri downloadUrl = urlTask.getResult();
                        return new ProposalFileItem(
                                file.getFileName(),
                                downloadUrl.toString(),
                                RequestDataHelper.guessTypeFromName(file.getFileName())
                        );
                    });

            tasks.add(task);
        }

        Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    List<ProposalFileItem> uploaded = new ArrayList<>();
                    for (Object item : results) {
                        if (item instanceof ProposalFileItem) {
                            uploaded.add((ProposalFileItem) item);
                        }
                    }
                    callback.onSuccess(uploaded);
                })
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Upload failed."
                ));
    }

    public interface FailureCallback {
        void onFailure(String message);
    }

    public static void attachUploadedFiles(
            String requestId,
            List<ProposalFileItem> uploadedFiles,
            Runnable onSuccess,
            FailureCallback onFailure
    ) {
        List<Map<String, Object>> fileMaps = new ArrayList<>();
        for (ProposalFileItem file : uploadedFiles) {
            fileMaps.add(file.toFirestoreMap());
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("proposalFiles", fileMaps);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        if (!uploadedFiles.isEmpty()) {
            ProposalFileItem first = uploadedFiles.get(0);
            updates.put("proposalFileName", first.getFileName());
            updates.put("proposalFileUrl", first.getFileUrl());
        }

        FirebaseFirestore.getInstance()
                .collection("requests")
                .document(requestId)
                .update(updates)
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(e -> onFailure.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to save file links."
                ));
    }
}
