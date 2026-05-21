package com.example.cnscfacilityhubproject.activities.sacUI;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.utils.RequestDataHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class sacRequestsViewDetailsFragment extends Fragment {

    private static final String ARG_REQUEST_ID = "requestId";

    private FirebaseFirestore db;
    private String requestId = "";

    private MaterialButton btnBack;
    private MaterialButton btnApprove;
    private MaterialButton btnReject;

    private LinearLayout layoutApprovalActions;
    private Chip chipStatus;
    private TextInputEditText etSacRemarks;

    private TextView tvPurpose;
    private TextView tvActivityType;
    private TextView tvSchedule;
    private TextView tvFacility;
    private TextView tvRequestorInfo;
    private TextView tvParticipants;
    private TextView tvPurposeFull;
    private TextView tvAmenities;
    private TextView tvTechnicalList;
    private TextView tvConnectors;
    private TextView tvProposalFile;
    private TextView tvRoute;
    private LinearLayout layoutProposalFiles;

    private final List<DisplayProposalFile> currentProposalFiles = new ArrayList<>();

    public sacRequestsViewDetailsFragment() {
        super(R.layout.fragment_sac_requests_view_details);
    }

    public static sacRequestsViewDetailsFragment newInstance(String requestId) {
        sacRequestsViewDetailsFragment fragment = new sacRequestsViewDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_ID, requestId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            requestId = getArguments().getString(ARG_REQUEST_ID, "");
        }

        bindViews(view);
        setupButtons();

        if (TextUtils.isEmpty(requestId)) {
            Toast.makeText(requireContext(), "Request ID not found.", Toast.LENGTH_SHORT).show();
            goBack();
            return;
        }

        loadRequestDetails();
    }

    private void bindViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        btnApprove = view.findViewById(R.id.btnApprove);
        btnReject = view.findViewById(R.id.btnReject);

        layoutApprovalActions = view.findViewById(R.id.layoutApprovalActions);
        chipStatus = view.findViewById(R.id.chipStatus);
        etSacRemarks = view.findViewById(R.id.etSacRemarks);

        tvPurpose = view.findViewById(R.id.tvPurpose);
        tvActivityType = view.findViewById(R.id.tvActivityType);
        tvSchedule = view.findViewById(R.id.tvSchedule);
        tvFacility = view.findViewById(R.id.tvFacility);
        tvRequestorInfo = view.findViewById(R.id.tvRequestorInfo);
        tvParticipants = view.findViewById(R.id.tvParticipants);
        tvPurposeFull = view.findViewById(R.id.tvPurposeFull);
        tvAmenities = view.findViewById(R.id.tvAmenities);
        tvTechnicalList = view.findViewById(R.id.tvTechnicalList);
        tvConnectors = view.findViewById(R.id.tvConnectors);
        tvProposalFile = view.findViewById(R.id.tvProposalFile);
        tvRoute = view.findViewById(R.id.tvRoute);
        layoutProposalFiles = view.findViewById(R.id.layoutProposalFiles);
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> goBack());
        btnApprove.setOnClickListener(v -> approveAndForwardRequest());
        btnReject.setOnClickListener(v -> rejectRequest());
    }

    private void loadRequestDetails() {
        db.collection("requests")
                .document(requestId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;

                    if (!doc.exists()) {
                        Toast.makeText(requireContext(), "Request not found.", Toast.LENGTH_SHORT).show();
                        goBack();
                        return;
                    }

                    displayRequest(doc);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to load request details.", Toast.LENGTH_SHORT).show();
                    goBack();
                });
    }

    private void displayRequest(DocumentSnapshot doc) {
        String purpose = getStringValue(doc, "purpose");
        String activityType = getStringValue(doc, "activityType");
        String displayStatus = getDisplayStatus(doc);

        String requestorName = firstNonEmpty(getStringValue(doc, "requestorName"), getStringValue(doc, "fullName"));
        String contactNumber = firstNonEmpty(getStringValue(doc, "contactNumber"), getStringValue(doc, "contactNum"));
        String department = firstNonEmpty(getStringValue(doc, "collegeDepartment"), getStringValue(doc, "department"));
        String course = firstNonEmpty(getStringValue(doc, "officeCourse"), getStringValue(doc, "course"));

        String participants = getStringValue(doc, "participants");
        String numberOfParticipants = getLongString(doc, "numberOfParticipants");
        List<DisplayProposalFile> proposalFiles = getProposalFilesFromFirestore(doc);

        chipStatus.setText(displayStatus);
        styleStatusChip(displayStatus);

        tvPurpose.setText(!purpose.isEmpty() ? purpose : "Request Details");
        tvActivityType.setText(!activityType.isEmpty() ? activityType : "Student Center booking request");
        tvSchedule.setText("Schedule:\n" + RequestDataHelper.getScheduleDisplay(doc));
        tvFacility.setText("Facilities: " + RequestDataHelper.getFacilitiesDisplay(doc));

        tvRequestorInfo.setText(
                "Name: " + fallback(requestorName) +
                        "\nContact: " + fallback(contactNumber) +
                        "\nCollege / Department: " + fallback(department) +
                        "\nOffice / Course: " + fallback(course)
        );

        tvParticipants.setText(
                "Participants: " + fallback(participants) +
                        "\nNumber of Participants: " + fallback(numberOfParticipants)
        );

        tvPurposeFull.setText("Purpose: " + fallback(purpose));
        tvAmenities.setText(buildAmenities(doc));
        tvTechnicalList.setText("Technical Requirements:\n" + buildTechnicalList(doc));
        tvConnectors.setText("Connectors / Cables: " + fallback(getStringValue(doc, "connectors")));
        tvRoute.setText(buildRouteText(doc));

        if (etSacRemarks != null) {
            etSacRemarks.setText(getStringValue(doc, "sacRemarks"));
        }

        bindProposalFiles(proposalFiles);

        layoutApprovalActions.setVisibility(
                "Pending".equalsIgnoreCase(displayStatus) ? View.VISIBLE : View.GONE
        );
    }

    private List<DisplayProposalFile> getProposalFilesFromFirestore(DocumentSnapshot doc) {
        List<DisplayProposalFile> files = new ArrayList<>();

        Object rawFiles = doc.get("proposalFiles");
        if (rawFiles instanceof List<?>) {
            List<?> list = (List<?>) rawFiles;
            for (Object item : list) {
                if (!(item instanceof Map<?, ?>)) continue;

                Map<?, ?> map = (Map<?, ?>) item;

                DisplayProposalFile file = new DisplayProposalFile();
                file.fileName = firstNonEmpty(getMapString(map, "fileName"), getMapString(map, "name"));
                file.fileUrl = firstNonEmpty(getMapString(map, "fileUrl"), getMapString(map, "url"));
                file.fileType = getMapString(map, "fileType");
                file.mimeType = getMapString(map, "mimeType");
                file.fileDataBase64 = firstNonEmpty(
                        getMapString(map, "fileDataBase64"),
                        getMapString(map, "base64")
                );
                file.storageType = getMapString(map, "storageType");
                file.sizeBytes = getMapLong(map, "sizeBytes");

                if (file.fileName.isEmpty()) {
                    file.fileName = "Proposal File " + (files.size() + 1);
                }
                if (file.mimeType.isEmpty()) {
                    file.mimeType = guessMimeType(file);
                }
                if (file.fileType.isEmpty()) {
                    file.fileType = guessFileType(file.mimeType, file.fileName);
                }
                if (file.sizeBytes <= 0 && !file.fileDataBase64.isEmpty()) {
                    file.sizeBytes = estimateBytesFromBase64(file.fileDataBase64);
                }

                files.add(file);
            }
        }

        // Legacy support: old single proposalFileUrl field.
        if (files.isEmpty()) {
            String legacyUrl = getStringValue(doc, "proposalFileUrl");
            if (!legacyUrl.isEmpty()) {
                DisplayProposalFile legacy = new DisplayProposalFile();
                legacy.fileName = firstNonEmpty(getStringValue(doc, "proposalFileName"), "Proposal File");
                legacy.fileUrl = legacyUrl;
                legacy.mimeType = guessMimeType(legacy);
                legacy.fileType = guessFileType(legacy.mimeType, legacy.fileName);
                files.add(legacy);
            }
        }

        return files;
    }

    private void bindProposalFiles(List<DisplayProposalFile> proposalFiles) {
        currentProposalFiles.clear();
        currentProposalFiles.addAll(proposalFiles);
        layoutProposalFiles.removeAllViews();

        if (proposalFiles.isEmpty()) {
            tvProposalFile.setText("Proposal / Supporting Files: none");
            tvProposalFile.setTextColor(Color.parseColor("#313131"));
            return;
        }

        boolean hasLocalContentUri = false;
        for (DisplayProposalFile file : proposalFiles) {
            if (file.fileUrl != null && file.fileUrl.startsWith("content://")) {
                hasLocalContentUri = true;
                break;
            }
        }

        if (hasLocalContentUri) {
            tvProposalFile.setText("Warning: This request contains local device URIs. Ask the requestor to resubmit using the updated Firestore-only upload.");
            tvProposalFile.setTextColor(Color.RED);
        } else {
            tvProposalFile.setText("Proposal / Supporting Files: " + proposalFiles.size() + " file(s). Tap a file below to open it.");
            tvProposalFile.setTextColor(Color.parseColor("#313131"));
        }

        for (int i = 0; i < proposalFiles.size(); i++) {
            DisplayProposalFile file = proposalFiles.get(i);
            layoutProposalFiles.addView(createProposalFileRow(file, i + 1));
        }
    }

    private View createProposalFileRow(DisplayProposalFile file, int index) {
        MaterialCardView row = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.bottomMargin = dp(8);
        row.setLayoutParams(rowParams);
        row.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
        row.setRadius(dp(14));
        row.setStrokeWidth(dp(1));
        row.setStrokeColor(Color.parseColor("#DDDDDD"));
        row.setCardElevation(0f);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> openProposalFile(file));

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        container.setPadding(dp(12), dp(10), dp(12), dp(10));
        container.setClickable(true);
        container.setFocusable(true);
        container.setOnClickListener(v -> openProposalFile(file));

        TextView details = new TextView(requireContext());
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        details.setLayoutParams(detailParams);
        details.setTextColor(Color.parseColor("#313131"));
        details.setTextSize(13f);
        details.setSingleLine(false);
        details.setText(
                index + ". " + file.fileName +
                        "\n" + buildFileSubtitle(file) +
                        "\nTap to open"
        );
        details.setOnClickListener(v -> openProposalFile(file));

        container.addView(details);
        row.addView(container);
        return row;
    }

    private String buildFileSubtitle(DisplayProposalFile file) {
        List<String> parts = new ArrayList<>();

        if (!file.mimeType.isEmpty()) parts.add(file.mimeType);
        if (file.sizeBytes > 0) parts.add(formatBytes(file.sizeBytes));
        if (!file.storageType.isEmpty()) parts.add(file.storageType);
        else if (file.hasBase64Data()) parts.add("firestore_base64");
        else if (!file.fileUrl.isEmpty()) parts.add("link/url");

        if (parts.isEmpty()) return "File attached";
        return TextUtils.join(" • ", parts);
    }

    private void openProposalFile(DisplayProposalFile file) {
        if (file == null) {
            Toast.makeText(requireContext(), "No file selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (file.hasBase64Data()) {
                File cachedFile = writeBase64FileToCache(file);
                Uri fileUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        cachedFile
                );

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(fileUri, file.mimeType.isEmpty() ? "application/octet-stream" : file.mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Open file"));
                return;
            }

            if (!file.fileUrl.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(file.fileUrl));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
                return;
            }

            Toast.makeText(requireContext(), "This file has no readable data.", Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException e) {
            Toast.makeText(requireContext(), "FileProvider is missing. Add the manifest provider and file_paths.xml.", Toast.LENGTH_LONG).show();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "No app found to open this file.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private File writeBase64FileToCache(DisplayProposalFile file) throws Exception {
        String base64Data = file.fileDataBase64;

        if (base64Data.isEmpty() && file.fileUrl.startsWith("data:")) {
            int commaIndex = file.fileUrl.indexOf(',');
            if (commaIndex >= 0 && commaIndex < file.fileUrl.length() - 1) {
                base64Data = file.fileUrl.substring(commaIndex + 1);
            }
        }

        if (base64Data.isEmpty()) {
            throw new IllegalArgumentException("Missing file data.");
        }

        byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);

        File dir = new File(requireContext().getCacheDir(), "proposal_files");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Unable to create cache folder.");
        }

        String safeName = sanitizeFileName(file.fileName);
        if (!hasFileExtension(safeName)) {
            safeName = safeName + extensionForMime(file.mimeType);
        }

        File outFile = new File(dir, safeName);
        FileOutputStream outputStream = new FileOutputStream(outFile);
        try {
            outputStream.write(bytes);
            outputStream.flush();
        } finally {
            outputStream.close();
        }

        return outFile;
    }

    private void approveAndForwardRequest() {
        setActionButtonsEnabled(false);

        String remarks = getText(etSacRemarks);

        db.collection("requests")
                .document(requestId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;

                    boolean needsITSO = getBooleanValue(doc, "needsITSO") || needsITSO(doc);
                    boolean itsoApproved = "Approved".equalsIgnoreCase(getStringValue(doc, "itsoStatus"));
                    boolean canSendToGSO = !needsITSO || itsoApproved;

                    String nextTarget;
                    String nextStage;
                    boolean sendToGSO;
                    String gsoStatus;

                    if (canSendToGSO) {
                        nextTarget = "GSO";
                        nextStage = "GSO_REVIEW";
                        sendToGSO = true;
                        gsoStatus = "Pending";
                    } else {
                        nextTarget = "ITSO";
                        nextStage = "WAITING_ITSO_APPROVAL";
                        sendToGSO = false;
                        gsoStatus = "Waiting";
                    }

                    db.collection("requests")
                            .document(requestId)
                            .update(
                                    "sacStatus", "Approved",
                                    "sacApproved", true,
                                    "sacRemarks", remarks,
                                    "sacCheckedAt", FieldValue.serverTimestamp(),

                                    "sendToSAC", false,
                                    "needsSAC", true,
                                    "sacNotificationSeen", true,
                                    "sacSeen", true,

                                    "sendToITSO", needsITSO && !itsoApproved,
                                    "itsoStatus", needsITSO && getStringValue(doc, "itsoStatus").isEmpty()
                                            ? "Pending"
                                            : getStringValue(doc, "itsoStatus"),
                                    "itsoNotificationSeen", false,
                                    "itsoSeen", false,

                                    "sendToGSO", sendToGSO,
                                    "gsoStatus", gsoStatus,
                                    "gsoNotificationSeen", false,
                                    "gsoSeen", false,

                                    "requestorSeen", false,
                                    "requestorNotificationSeen", false,
                                    "requestorNotificationType", "SAC Approved",
                                    "requestorNotificationTitle", "SAC Approved Your Request",
                                    "requestorNotificationMessage", "SAC approved your Student Center request.",
                                    "notificationForRequestor", true,
                                    "notificationUpdatedAt", FieldValue.serverTimestamp(),

                                    "notificationTarget", nextTarget,
                                    "workflowStage", nextStage,

                                    "status", "Pending",
                                    "updatedAt", FieldValue.serverTimestamp()
                            )
                            .addOnSuccessListener(unused -> {
                                if (!isAdded()) return;

                                Toast.makeText(
                                        requireContext(),
                                        canSendToGSO
                                                ? "Request approved and sent to GSO."
                                                : "Request approved. Waiting for ITSO approval before GSO.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                goBack();
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;

                                setActionButtonsEnabled(true);

                                Toast.makeText(
                                        requireContext(),
                                        "Failed to approve request.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;

                    setActionButtonsEnabled(true);

                    Toast.makeText(
                            requireContext(),
                            "Failed to check request route.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void rejectRequest() {
        setActionButtonsEnabled(false);

        String remarks = getText(etSacRemarks);
        String finalRemarks = remarks.isEmpty() ? "Rejected by SAC." : remarks;

        db.collection("requests")
                .document(requestId)
                .update(
                        "status", "Rejected",
                        "workflowStage", "REJECTED_BY_SAC",
                        "notificationTarget", "Requestor",

                        "sacStatus", "Rejected",
                        "sacApproved", false,
                        "sacRemarks", finalRemarks,
                        "sacCheckedAt", FieldValue.serverTimestamp(),

                        "sendToSAC", false,
                        "sacNotificationSeen", true,
                        "sacSeen", true,

                        "sendToITSO", false,
                        "sendToGSO", false,
                        "gsoStatus", "Not Required",

                        "requestorSeen", false,
                        "requestorNotificationSeen", false,
                        "requestorNotificationType", "SAC Rejected",
                        "requestorNotificationTitle", "Request Rejected by SAC",
                        "requestorNotificationMessage", finalRemarks,
                        "notificationForRequestor", true,
                        "notificationUpdatedAt", FieldValue.serverTimestamp(),

                        "updatedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;

                    Toast.makeText(
                            requireContext(),
                            "Request rejected by SAC.",
                            Toast.LENGTH_SHORT
                    ).show();

                    goBack();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;

                    setActionButtonsEnabled(true);

                    Toast.makeText(
                            requireContext(),
                            "Failed to reject request.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private boolean needsITSO(DocumentSnapshot doc) {
        Boolean technicalNeeded = doc.getBoolean("technicalNeeded");

        return Boolean.TRUE.equals(technicalNeeded) && (
                Boolean.TRUE.equals(doc.getBoolean("soundSystemSetup"))
                        || Boolean.TRUE.equals(doc.getBoolean("microphones"))
                        || Boolean.TRUE.equals(doc.getBoolean("portableSpeaker"))
                        || Boolean.TRUE.equals(doc.getBoolean("lights"))
                        || Boolean.TRUE.equals(doc.getBoolean("livestreamingServices"))
                        || Boolean.TRUE.equals(doc.getBoolean("zoomHosting"))
                        || Boolean.TRUE.equals(doc.getBoolean("gmeetHosting"))
                        || Boolean.TRUE.equals(doc.getBoolean("webCamera"))
                        || Boolean.TRUE.equals(doc.getBoolean("tripod"))
                        || Boolean.TRUE.equals(doc.getBoolean("multimediaProjector"))
                        || !getStringValue(doc, "connectors").isEmpty()
        );
    }

    private void setActionButtonsEnabled(boolean enabled) {
        btnApprove.setEnabled(enabled);
        btnReject.setEnabled(enabled);
    }

    private void goBack() {
        if (!isAdded()) return;

        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.sac_fragment_container, new sacRequestsFragment())
                    .commit();
        }
    }

    private String getDisplayStatus(DocumentSnapshot doc) {
        String sacStatus = getStringValue(doc, "sacStatus");
        String status = getStringValue(doc, "status");

        if ("Rejected".equalsIgnoreCase(sacStatus)
                || "Rejected".equalsIgnoreCase(status)) {
            return "Rejected";
        }

        if ("Approved".equalsIgnoreCase(sacStatus)) {
            return "Approved";
        }

        if ("Pending".equalsIgnoreCase(sacStatus)
                || "Pending".equalsIgnoreCase(status)
                || status.isEmpty()) {
            return "Pending";
        }

        return status;
    }

    private void styleStatusChip(String status) {
        if ("Approved".equalsIgnoreCase(status)) {
            chipStatus.setTextColor(Color.parseColor("#2E7D32"));
            chipStatus.setChipBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#E7F4E8"))
            );
        } else if ("Rejected".equalsIgnoreCase(status)) {
            chipStatus.setTextColor(Color.parseColor("#970705"));
            chipStatus.setChipBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#F3D9D9"))
            );
        } else {
            chipStatus.setTextColor(Color.parseColor("#313131"));
            chipStatus.setChipBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#EEEEEE"))
            );
        }
    }

    private String buildRouteText(DocumentSnapshot doc) {
        String notificationTarget = getStringValue(doc, "notificationTarget");
        String sacStatus = getStringValue(doc, "sacStatus");

        if ("Approved".equalsIgnoreCase(sacStatus)) {
            if ("ITSO".equalsIgnoreCase(notificationTarget)) {
                return "Route: Requestor → SAC Approved → ITSO → GSO";
            }

            if ("GSO".equalsIgnoreCase(notificationTarget)) {
                return "Route: Requestor → SAC Approved → GSO";
            }

            return "Route: Requestor → SAC Approved";
        }

        return "Route: Requestor → SAC Approval → GSO / ITSO";
    }

    private String buildAmenities(DocumentSnapshot doc) {
        boolean tablesRequested = getBooleanValue(doc, "tablesRequested");
        boolean chairsRequested = getBooleanValue(doc, "chairsRequested");

        String tablesCount = getLongString(doc, "tablesCount");
        String chairsCount = getLongString(doc, "chairsCount");
        String otherAmenities = getStringValue(doc, "otherAmenities");

        return "Tables: " + (tablesRequested
                ? "Requested (" + fallback(tablesCount) + ")"
                : "Not requested") +
                "\nChairs: " + (chairsRequested
                ? "Requested (" + fallback(chairsCount) + ")"
                : "Not requested") +
                "\nOther Amenities: " + fallback(otherAmenities);
    }

    private String buildTechnicalList(DocumentSnapshot doc) {
        List<String> selected = new ArrayList<>();
        if (getBooleanValue(doc, "soundSystemSetup")) selected.add("Sound System Setup");
        if (getBooleanValue(doc, "microphones")) selected.add("Microphones");
        if (getBooleanValue(doc, "portableSpeaker")) selected.add("Portable Speaker");
        if (getBooleanValue(doc, "lights")) selected.add("Lights");
        if (getBooleanValue(doc, "livestreamingServices")) selected.add("Livestreaming Services");
        if (getBooleanValue(doc, "zoomHosting")) selected.add("Zoom Hosting");
        if (getBooleanValue(doc, "gmeetHosting")) selected.add("GMeet Hosting");
        if (getBooleanValue(doc, "webCamera")) selected.add("Web Camera");
        if (getBooleanValue(doc, "tripod")) selected.add("Tripod");
        if (getBooleanValue(doc, "multimediaProjector")) selected.add("Multimedia Projector");

        if (selected.isEmpty()) return "None";

        StringBuilder builder = new StringBuilder();
        for (String item : selected) builder.append("• ").append(item).append("\n");
        return builder.toString().trim();
    }

    private String buildSchedule(String startDate, String endDate, String startTime, String endTime) {
        return buildDate(startDate, endDate) + " • " + buildTime(startTime, endTime);
    }

    private String buildDate(String startDate, String endDate) {
        if (startDate.isEmpty() && endDate.isEmpty()) return "No date";
        if (!startDate.isEmpty() && !endDate.isEmpty() && !startDate.equalsIgnoreCase(endDate)) {
            return startDate + " - " + endDate;
        }
        return !startDate.isEmpty() ? startDate : endDate;
    }

    private String buildTime(String startTime, String endTime) {
        if (startTime.isEmpty() && endTime.isEmpty()) return "No time";
        if (!startTime.isEmpty() && !endTime.isEmpty()) return startTime + " - " + endTime;
        return !startTime.isEmpty() ? startTime : endTime;
    }

    private String getFinalFacility(DocumentSnapshot doc) {
        String finalFacilityName = getStringValue(doc, "finalFacilityName");
        if (!finalFacilityName.isEmpty()) return finalFacilityName;

        String facility = getStringValue(doc, "facility");
        String otherFacility = getStringValue(doc, "otherFacility");

        if ("Others".equalsIgnoreCase(facility) && !otherFacility.isEmpty()) return otherFacility;
        return facility;
    }

    private String getMapString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private long getMapLong(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) return ((Number) value).longValue();
        if (value != null) {
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private String guessMimeType(DisplayProposalFile file) {
        String name = file.fileName == null ? "" : file.fileName.toLowerCase(Locale.US);
        String url = file.fileUrl == null ? "" : file.fileUrl.toLowerCase(Locale.US);

        if (url.startsWith("data:")) {
            int semicolonIndex = url.indexOf(';');
            if (semicolonIndex > 5) return url.substring(5, semicolonIndex);
        }

        if (name.endsWith(".pdf") || url.contains("application/pdf")) return "application/pdf";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (url.contains("image/png")) return "image/png";
        if (url.contains("image/webp")) return "image/webp";
        if (url.contains("image/jpeg") || url.contains("image/jpg")) return "image/jpeg";

        return "application/octet-stream";
    }

    private String guessFileType(String mimeType, String fileName) {
        String lowerMime = mimeType == null ? "" : mimeType.toLowerCase(Locale.US);
        String lowerName = fileName == null ? "" : fileName.toLowerCase(Locale.US);

        if (lowerMime.startsWith("image/") || lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png")
                || lowerName.endsWith(".webp")) return "image";
        if ("application/pdf".equals(lowerMime) || lowerName.endsWith(".pdf")) return "pdf";
        return "file";
    }

    private int estimateBytesFromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return 0;
        String clean = base64.replace("\n", "").replace("\r", "").trim();
        int padding = 0;
        if (clean.endsWith("==")) padding = 2;
        else if (clean.endsWith("=")) padding = 1;
        return Math.max(0, (clean.length() * 3 / 4) - padding);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        return String.format(Locale.US, "%.1f MB", kb / 1024.0);
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) return "proposal_file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private boolean hasFileExtension(String name) {
        return name != null && name.matches("(?i).+\\.(pdf|jpg|jpeg|png|webp)$");
    }

    private String extensionForMime(String mimeType) {
        if (mimeType == null) return ".bin";
        String lower = mimeType.toLowerCase(Locale.US);
        if ("application/pdf".equals(lower)) return ".pdf";
        if ("image/png".equals(lower)) return ".png";
        if ("image/webp".equals(lower)) return ".webp";
        if ("image/jpeg".equals(lower) || "image/jpg".equals(lower)) return ".jpg";
        return ".bin";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private boolean getBooleanValue(DocumentSnapshot doc, String field) {
        Boolean value = doc.getBoolean(field);
        return Boolean.TRUE.equals(value);
    }

    private String getLongString(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value == null ? "" : String.valueOf(value);
    }

    private String getStringValue(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String getText(TextInputEditText editText) {
        return editText != null && editText.getText() != null
                ? editText.getText().toString().trim()
                : "";
    }

    private String fallback(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value.trim();
    }

    private String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) return first.trim();
        if (second != null && !second.trim().isEmpty()) return second.trim();
        return "";
    }

    private static class DisplayProposalFile {
        String fileName = "";
        String fileUrl = "";
        String fileType = "";
        String mimeType = "";
        String fileDataBase64 = "";
        String storageType = "";
        long sizeBytes = 0;

        boolean hasBase64Data() {
            return fileDataBase64 != null && !fileDataBase64.trim().isEmpty()
                    || fileUrl != null && fileUrl.startsWith("data:");
        }
    }
}
