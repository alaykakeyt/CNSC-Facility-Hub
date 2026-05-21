package com.example.cnscfacilityhubproject.activities.itsoUI;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
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
import com.example.cnscfacilityhubproject.utils.ItsoReminderHelper;
import com.example.cnscfacilityhubproject.utils.RequestDataHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class itsoHomeViewDetailsFragment extends Fragment {

    private static final String ARG_REQUEST_ID = "requestId";

    private FirebaseFirestore db;
    private String requestId = "";
    private ListenerRegistration requestListener;

    private MaterialButton btnBack;
    private MaterialButton btnAvailable;
    private MaterialButton btnNotAvailable;

    private LinearLayout layoutAvailabilityActions;
    private Chip chipStatus;
    private TextInputEditText etItsoRemarks;

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
    private TextView tvRemarks;
    private MaterialCardView cardRemarks;
    private LinearLayout layoutProposalFiles;

    private final List<DisplayProposalFile> currentProposalFiles = new ArrayList<>();

    public itsoHomeViewDetailsFragment() {
        super(R.layout.fragment_itso_home_view_details);
    }

    public static itsoHomeViewDetailsFragment newInstance(String requestId) {
        itsoHomeViewDetailsFragment fragment = new itsoHomeViewDetailsFragment();
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
        setInitialLabels();

        if (TextUtils.isEmpty(requestId)) {
            Toast.makeText(requireContext(), "Request ID not found.", Toast.LENGTH_SHORT).show();
            goBack();
            return;
        }

        loadRequestDetails();
    }

    @Override
    public void onDestroyView() {
        if (requestListener != null) {
            requestListener.remove();
            requestListener = null;
        }

        super.onDestroyView();
    }

    private void bindViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        btnAvailable = view.findViewById(R.id.btnAvailable);
        btnNotAvailable = view.findViewById(R.id.btnNotAvailable);

        layoutAvailabilityActions = view.findViewById(R.id.layoutAvailabilityActions);
        chipStatus = view.findViewById(R.id.chipStatus);
        etItsoRemarks = view.findViewById(R.id.etItsoRemarks);

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
        tvRemarks = view.findViewById(R.id.tvRemarks);
        cardRemarks = view.findViewById(R.id.cardRemarks);
        layoutProposalFiles = view.findViewById(R.id.layoutProposalFiles);
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> goBack());

        btnAvailable.setOnClickListener(v ->
                markTechnicalStatusAndRoute("Approved", "Available")
        );

        btnNotAvailable.setOnClickListener(v ->
                markTechnicalStatusAndRoute("Rejected", "Not Available")
        );
    }

    private void setInitialLabels() {
        setPlainText(tvPurpose, "");
        setPlainText(tvActivityType, "");
        setChipText(chipStatus, "");

        setLabeledText(tvSchedule, "Schedule:\n", "");
        setLabeledText(tvFacility, "Facilities: ", "");
        tvRequestorInfo.setText(
                labelValue("Name: ", "") +
                        "\n" + labelValue("Contact: ", "") +
                        "\n" + labelValue("College / Department: ", "") +
                        "\n" + labelValue("Office / Course: ", "")
        );
        tvParticipants.setText(
                labelValue("Participants: ", "") +
                        "\n" + labelValue("Number of Participants: ", "")
        );
        setLabeledText(tvPurposeFull, "Purpose: ", "");
        tvAmenities.setText(buildEmptyAmenitiesText());
        setLabeledText(tvTechnicalList, "Technical Requirements:\n", "");
        setLabeledText(tvConnectors, "Connectors / Cables: ", "");
        setLabeledText(tvProposalFile, "Proposal / Supporting Files: ", "");
        setLabeledText(tvRoute, "Route: ", "");
        setLabeledText(tvRemarks, "Remarks: ", "");
    }

    private void loadRequestDetails() {
        if (requestListener != null) {
            requestListener.remove();
            requestListener = null;
        }

        requestListener = db.collection("requests")
                .document(requestId)
                .addSnapshotListener((doc, error) -> {
                    if (!isAdded()) return;

                    if (error != null) {
                        Toast.makeText(requireContext(), "Failed to load request details.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (doc == null || !doc.exists()) {
                        Toast.makeText(requireContext(), "Request not found.", Toast.LENGTH_SHORT).show();
                        goBack();
                        return;
                    }

                    displayRequest(doc);
                });
    }

    private void displayRequest(DocumentSnapshot doc) {
        String purpose = getStringValue(doc, "purpose");
        String activityType = getStringValue(doc, "activityType");
        String displayStatus = getITSODisplayStatus(doc);

        String requestorName = firstNonEmpty(
                getStringValue(doc, "requestorName"),
                getStringValue(doc, "fullName")
        );

        String contactNumber = firstNonEmpty(
                getStringValue(doc, "contactNumber"),
                getStringValue(doc, "contactNum")
        );

        String department = firstNonEmpty(
                getStringValue(doc, "collegeDepartment"),
                getStringValue(doc, "department")
        );

        String course = firstNonEmpty(
                getStringValue(doc, "officeCourse"),
                getStringValue(doc, "course")
        );

        String participants = getStringValue(doc, "participants");
        String numberOfParticipants = getLongString(doc, "numberOfParticipants");
        String notificationTarget = getStringValue(doc, "notificationTarget");
        String scheduleDisplay = cleanDisplayValue(RequestDataHelper.getScheduleDisplay(doc));
        String facilitiesDisplay = cleanDisplayValue(RequestDataHelper.getFacilitiesDisplay(doc));
        List<DisplayProposalFile> proposalFiles = getProposalFilesFromFirestore(doc);

        setPlainText(tvPurpose, purpose);
        setPlainText(tvActivityType, activityType);
        setChipText(chipStatus, displayStatus);
        styleStatusChip(displayStatus);

        setLabeledText(tvSchedule, "Schedule:\n", scheduleDisplay);
        setLabeledText(tvFacility, "Facilities: ", facilitiesDisplay);

        tvRequestorInfo.setText(
                labelValue("Name: ", requestorName) +
                        "\n" + labelValue("Contact: ", contactNumber) +
                        "\n" + labelValue("College / Department: ", department) +
                        "\n" + labelValue("Office / Course: ", course)
        );

        tvParticipants.setText(
                labelValue("Participants: ", participants) +
                        "\n" + labelValue("Number of Participants: ", numberOfParticipants)
        );

        setLabeledText(tvPurposeFull, "Purpose: ", purpose);
        tvAmenities.setText(buildAmenities(doc));
        setLabeledText(tvTechnicalList, "Technical Requirements:\n", buildTechnicalList(doc));
        setLabeledText(tvConnectors, "Connectors / Cables: ", getStringValue(doc, "connectors"));
        setLabeledText(tvRoute, "Route: ", notificationTarget);

        if (tvRemarks != null) {
            setLabeledText(tvRemarks, "Remarks: ", getRemarks(doc));
        }

        if (cardRemarks != null) {
            cardRemarks.setVisibility(View.VISIBLE);
        }

        if (etItsoRemarks != null) {
            etItsoRemarks.setText(getStringValue(doc, "itsoRemarks"));
        }

        bindProposalFiles(proposalFiles);

        layoutAvailabilityActions.setVisibility(
                "Pending".equalsIgnoreCase(displayStatus) ? View.VISIBLE : View.GONE
        );

        markReminderSeenIfUpcoming(doc);
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

                if (file.mimeType.isEmpty()) {
                    file.mimeType = guessMimeType(file);
                }

                if (file.fileType.isEmpty()) {
                    file.fileType = guessFileType(file.mimeType, file.fileName);
                }

                if (file.sizeBytes <= 0 && !file.fileDataBase64.isEmpty()) {
                    file.sizeBytes = estimateBytesFromBase64(file.fileDataBase64);
                }

                if (hasDisplayValue(file.fileName)
                        || hasDisplayValue(file.fileUrl)
                        || hasDisplayValue(file.fileDataBase64)) {
                    files.add(file);
                }
            }
        }

        if (files.isEmpty()) {
            String legacyUrl = getStringValue(doc, "proposalFileUrl");
            String legacyBase64 = getStringValue(doc, "fileDataBase64");

            if (!legacyUrl.isEmpty() || !legacyBase64.isEmpty()) {
                DisplayProposalFile legacy = new DisplayProposalFile();
                legacy.fileName = getStringValue(doc, "proposalFileName");
                legacy.fileUrl = legacyUrl;
                legacy.fileDataBase64 = legacyBase64;
                legacy.mimeType = getStringValue(doc, "mimeType");

                if (legacy.mimeType.isEmpty()) {
                    legacy.mimeType = guessMimeType(legacy);
                }

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
            tvProposalFile.setText("Proposal / Supporting Files: ");
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
            tvProposalFile.setText("Proposal / Supporting Files: Local device URI detected");
            tvProposalFile.setTextColor(Color.RED);
        } else {
            tvProposalFile.setText("Proposal / Supporting Files: " + proposalFiles.size() + " file(s)");
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
        container.setPadding(dp(12), dp(10), dp(12), dp(10));
        container.setClickable(true);
        container.setOnClickListener(v -> openProposalFile(file));

        String fileTitle = hasDisplayValue(file.fileName)
                ? index + ". " + file.fileName
                : String.valueOf(index) + ".";
        String fileSubtitle = buildFileSubtitle(file);

        TextView details = new TextView(requireContext());
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        details.setLayoutParams(detailParams);
        details.setTextColor(Color.parseColor("#313131"));
        details.setTextSize(13f);
        details.setSingleLine(false);
        details.setText(hasDisplayValue(fileSubtitle) ? fileTitle + "\n" + fileSubtitle : fileTitle);
        details.setOnClickListener(v -> openProposalFile(file));

        TextView tapHint = new TextView(requireContext());
        tapHint.setText("Tap to open");
        tapHint.setTextColor(Color.parseColor("#970705"));
        tapHint.setTextSize(12f);
        tapHint.setPadding(0, dp(6), 0, 0);
        tapHint.setOnClickListener(v -> openProposalFile(file));

        container.addView(details);
        container.addView(tapHint);
        row.addView(container);
        return row;
    }

    private String buildFileSubtitle(DisplayProposalFile file) {
        List<String> parts = new ArrayList<>();

        if (hasDisplayValue(file.mimeType)
                && !"application/octet-stream".equalsIgnoreCase(file.mimeType)) {
            parts.add(file.mimeType);
        }

        if (file.sizeBytes > 0) {
            parts.add(formatBytes(file.sizeBytes));
        }

        if (hasDisplayValue(file.storageType)) {
            parts.add(file.storageType);
        }

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

    private void markReminderSeenIfUpcoming(DocumentSnapshot doc) {
        if (!ItsoReminderHelper.isUpcomingTechnicalEvent(doc)) {
            return;
        }

        db.collection("requests")
                .document(requestId)
                .update(
                        "itsoReminderSeen", true,
                        "itsoUpcomingReminder", true,
                        "updatedAt", FieldValue.serverTimestamp()
                );
    }

    private void markTechnicalStatusAndRoute(String itsoStatus, String itsoAvailability) {
        setActionButtonsEnabled(false);

        db.collection("requests")
                .document(requestId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;

                    boolean needsSAC = getBooleanValue(doc, "needsSAC");
                    boolean sacApproved = "Approved".equalsIgnoreCase(getStringValue(doc, "sacStatus"));
                    boolean canSendToGSO = !needsSAC || sacApproved;

                    String nextTarget = canSendToGSO ? "GSO" : "SAC";
                    String nextStage = canSendToGSO ? "GSO_REVIEW" : "WAITING_SAC_APPROVAL";

                    boolean sendToGSO = canSendToGSO;
                    boolean sendToSAC = needsSAC && !sacApproved;

                    String gsoStatus = canSendToGSO ? "Pending" : "Waiting";

                    boolean markedAvailable = "Available".equalsIgnoreCase(itsoAvailability);
                    String displayStatus = markedAvailable ? "Approved - Available" : "Not Available";

                    String notificationType = markedAvailable
                            ? "ITSO Approved"
                            : "ITSO Not Available";

                    String notificationTitle = markedAvailable
                            ? "Technical Support Approved"
                            : "Technical Support Not Available";

                    String typedRemarks = getText(etItsoRemarks);

                    String defaultRemarks = markedAvailable
                            ? "ITSO marked the requested technical support as available."
                            : "ITSO marked the requested technical support as not available.";

                    String itsoRemarks = typedRemarks.isEmpty()
                            ? defaultRemarks
                            : typedRemarks;

                    db.collection("requests")
                            .document(requestId)
                            .update(
                                    "itsoStatus", itsoStatus,
                                    "itsoAvailability", itsoAvailability,
                                    "itsoCheckedAt", FieldValue.serverTimestamp(),
                                    "itsoRemarks", itsoRemarks,

                                    "sendToITSO", false,
                                    "itsoNotificationSeen", true,
                                    "itsoSeen", true,

                                    "sendToSAC", sendToSAC,
                                    "sacNotificationSeen", false,
                                    "sacSeen", false,

                                    "sendToGSO", sendToGSO,
                                    "gsoStatus", gsoStatus,
                                    "gsoAvailability", "",
                                    "gsoNotificationSeen", false,
                                    "gsoSeen", false,

                                    "requestorSeen", false,
                                    "requestorNotificationSeen", false,
                                    "requestorNotificationType", notificationType,
                                    "requestorNotificationTitle", notificationTitle,
                                    "requestorNotificationMessage", itsoRemarks,
                                    "notificationForRequestor", true,
                                    "notificationUpdatedAt", FieldValue.serverTimestamp(),

                                    "notificationTarget", nextTarget,
                                    "workflowStage", nextStage,
                                    "status", displayStatus,
                                    "updatedAt", FieldValue.serverTimestamp()
                            )
                            .addOnSuccessListener(unused -> {
                                if (!isAdded()) return;

                                Toast.makeText(
                                        requireContext(),
                                        canSendToGSO
                                                ? "ITSO marked " + itsoAvailability + ". Request sent to GSO."
                                                : "ITSO marked " + itsoAvailability + ". Waiting for SAC approval before GSO.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                goBack();
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;

                                setActionButtonsEnabled(true);
                                Toast.makeText(requireContext(), "Failed to update ITSO status.", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;

                    setActionButtonsEnabled(true);
                    Toast.makeText(requireContext(), "Failed to check request route.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setActionButtonsEnabled(boolean enabled) {
        btnAvailable.setEnabled(enabled);
        btnNotAvailable.setEnabled(enabled);
    }

    private String getITSODisplayStatus(DocumentSnapshot doc) {
        String itsoAvailability = getStringValue(doc, "itsoAvailability");
        String itsoStatus = getStringValue(doc, "itsoStatus");
        String status = getStringValue(doc, "status");

        if ("Not Available".equalsIgnoreCase(itsoAvailability)
                || "Unavailable".equalsIgnoreCase(itsoAvailability)
                || "Not Available".equalsIgnoreCase(itsoStatus)
                || "Unavailable".equalsIgnoreCase(itsoStatus)
                || "Rejected".equalsIgnoreCase(itsoStatus)
                || "Not Available".equalsIgnoreCase(status)
                || "Unavailable".equalsIgnoreCase(status)
                || "Returned".equalsIgnoreCase(status)) {
            return "Not Available";
        }

        if ("Available".equalsIgnoreCase(itsoAvailability)
                || "Approved".equalsIgnoreCase(itsoStatus)
                || "Available".equalsIgnoreCase(itsoStatus)
                || "Approved - Available".equalsIgnoreCase(status)) {
            return "Approved - Available";
        }

        if (hasDisplayValue(itsoStatus)) {
            return itsoStatus;
        }

        if (hasDisplayValue(status)) {
            return status;
        }

        return "";
    }

    private String buildEmptyAmenitiesText() {
        return labelValue("Tables: ", "") +
                "\n" + labelValue("Chairs: ", "") +
                "\n" + labelValue("Other Amenities: ", "");
    }

    private String buildAmenities(DocumentSnapshot doc) {
        Boolean tablesRequested = getNullableBooleanValue(doc, "tablesRequested");
        Boolean chairsRequested = getNullableBooleanValue(doc, "chairsRequested");

        String tablesCount = getLongString(doc, "tablesCount");
        String chairsCount = getLongString(doc, "chairsCount");
        String otherAmenities = getStringValue(doc, "otherAmenities");

        String tablesValue = "";
        if (tablesRequested != null) {
            tablesValue = tablesRequested
                    ? "Requested" + (hasDisplayValue(tablesCount) ? " (" + tablesCount + ")" : "")
                    : "Not requested";
        }

        String chairsValue = "";
        if (chairsRequested != null) {
            chairsValue = chairsRequested
                    ? "Requested" + (hasDisplayValue(chairsCount) ? " (" + chairsCount + ")" : "")
                    : "Not requested";
        }

        return labelValue("Tables: ", tablesValue) +
                "\n" + labelValue("Chairs: ", chairsValue) +
                "\n" + labelValue("Other Amenities: ", otherAmenities);
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

        String directTechnicals = firstNonEmpty(
                getStringValue(doc, "selectedTechnicals"),
                getStringValue(doc, "technicalRequirements")
        );

        if (hasDisplayValue(directTechnicals)) {
            selected.add(directTechnicals);
        }

        StringBuilder builder = new StringBuilder();
        for (String item : selected) {
            if (!hasDisplayValue(item)) continue;

            if (builder.length() > 0) {
                builder.append("\n");
            }

            builder.append("• ").append(item.trim());
        }

        return builder.toString();
    }

    private String getRemarks(DocumentSnapshot doc) {
        String remarks = getStringValue(doc, "itsoRemarks");
        if (!remarks.isEmpty()) return remarks;

        remarks = getStringValue(doc, "adminRemarks");
        if (!remarks.isEmpty()) return remarks;

        remarks = getStringValue(doc, "returnReason");
        if (!remarks.isEmpty()) return remarks;

        return getStringValue(doc, "remarks");
    }

    private void styleStatusChip(String status) {
        if (chipStatus == null) return;

        if ("Approved - Available".equalsIgnoreCase(status)
                || "Available".equalsIgnoreCase(status)) {
            chipStatus.setTextColor(Color.parseColor("#2E7D32"));
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E7F4E8")));
        } else if ("Not Available".equalsIgnoreCase(status)
                || "Returned".equalsIgnoreCase(status)) {
            chipStatus.setTextColor(Color.parseColor("#970705"));
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F3D9D9")));
        } else {
            chipStatus.setTextColor(Color.parseColor("#313131"));
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EEEEEE")));
        }
    }

    private void setPlainText(TextView textView, String value) {
        if (textView == null) return;
        textView.setText(cleanDisplayValue(value));
    }

    private void setLabeledText(TextView textView, String label, String value) {
        if (textView == null) return;
        textView.setText(labelValue(label, value));
    }

    private String labelValue(String label, String value) {
        return label + cleanDisplayValue(value);
    }

    private void setChipText(Chip chip, String value) {
        if (chip == null) return;

        String cleaned = cleanDisplayValue(value);
        chip.setText(cleaned);
        chip.setVisibility(cleaned.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String getText(TextInputEditText editText) {
        return editText != null && editText.getText() != null
                ? editText.getText().toString().trim()
                : "";
    }

    private String getStringValue(DocumentSnapshot doc, String field) {
        if (doc == null || field == null) return "";
        Object value = doc.get(field);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean getBooleanValue(DocumentSnapshot doc, String field) {
        Boolean value = doc.getBoolean(field);
        return Boolean.TRUE.equals(value);
    }

    private Boolean getNullableBooleanValue(DocumentSnapshot doc, String field) {
        if (doc == null || field == null || !doc.contains(field)) {
            return null;
        }

        Object value = doc.get(field);

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof String) {
            String text = ((String) value).trim();

            if ("true".equalsIgnoreCase(text)) return true;
            if ("false".equalsIgnoreCase(text)) return false;
        }

        return null;
    }

    private String getLongString(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String getMapString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value).trim() : "";
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

    private String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) return first.trim();
        if (second != null && !second.trim().isEmpty()) return second.trim();
        return "";
    }

    private String cleanDisplayValue(String value) {
        if (value == null) return "";

        String cleaned = value.trim();
        if (cleaned.isEmpty()) return "";

        String lower = cleaned.toLowerCase(Locale.US);
        if (cleaned.equals("—")
                || cleaned.equals("-")
                || lower.equals("null")
                || lower.equals("none")
                || lower.equals("n/a")
                || lower.equals("not available")
                || lower.equals("not set")
                || lower.equals("no schedule")
                || lower.equals("no facility")
                || lower.equals("no facilities")) {
            return "";
        }

        return cleaned;
    }

    private boolean hasDisplayValue(String value) {
        return !cleanDisplayValue(value).isEmpty();
    }

    private String guessMimeType(DisplayProposalFile file) {
        if (file.fileUrl != null && file.fileUrl.startsWith("data:")) {
            int semicolonIndex = file.fileUrl.indexOf(';');
            if (semicolonIndex > 5) {
                return file.fileUrl.substring(5, semicolonIndex);
            }
        }

        String lower = file.fileName == null ? "" : file.fileName.toLowerCase(Locale.US);
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    private String guessFileType(String mimeType, String fileName) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.US);
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.US);

        if (mime.startsWith("image/") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".png") || name.endsWith(".webp")) {
            return "image";
        }
        if ("application/pdf".equals(mime) || name.endsWith(".pdf")) {
            return "pdf";
        }
        return "file";
    }

    private long estimateBytesFromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return 0;
        int commaIndex = base64.indexOf(',');
        String clean = commaIndex >= 0 ? base64.substring(commaIndex + 1) : base64;
        return (clean.length() * 3L) / 4L;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        return String.format(Locale.US, "%.2f MB", mb);
    }

    private String sanitizeFileName(String name) {
        String safeName = name == null || name.trim().isEmpty()
                ? requestId
                : name.trim();

        if (safeName.isEmpty()) {
            safeName = String.valueOf(System.currentTimeMillis());
        }

        return safeName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private boolean hasFileExtension(String name) {
        return name != null && name.lastIndexOf('.') > 0 && name.lastIndexOf('.') < name.length() - 1;
    }

    private String extensionForMime(String mimeType) {
        if ("application/pdf".equalsIgnoreCase(mimeType)) return ".pdf";
        if ("image/png".equalsIgnoreCase(mimeType)) return ".png";
        if ("image/jpeg".equalsIgnoreCase(mimeType)) return ".jpg";
        if ("image/webp".equalsIgnoreCase(mimeType)) return ".webp";
        return ".bin";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void goBack() {
        if (!isAdded()) return;
        requireActivity().getSupportFragmentManager().popBackStack();
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
            return (fileDataBase64 != null && !fileDataBase64.trim().isEmpty())
                    || (fileUrl != null && fileUrl.startsWith("data:"));
        }
    }
}