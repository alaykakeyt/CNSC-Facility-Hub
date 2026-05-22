package com.example.cnscfacilityhubproject.activities.gsoUI;

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

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Source;

public class gsoRequestsViewDetailsFragment extends Fragment {

    private static final String ARG_REQUEST_ID = "requestId";

    private FirebaseFirestore db;
    private String requestId = "";


    private MaterialButton btnApprove;
    private MaterialButton btnReturn;

    private LinearLayout layoutActionButtons;
    private View cardReturnReason;
    private Chip chipStatus;

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
    private LinearLayout layoutProposalFiles;

    private TextInputEditText etReturnReason;
    private boolean isReturnReasonBoxShown = false;

    private ListenerRegistration requestListener;

    private final List<DisplayProposalFile> currentProposalFiles = new ArrayList<>();

    public gsoRequestsViewDetailsFragment() {
        super(R.layout.fragment_gso_requests_view_details);
    }

    public static gsoRequestsViewDetailsFragment newInstance(String requestId) {
        gsoRequestsViewDetailsFragment fragment = new gsoRequestsViewDetailsFragment();
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
        clearFirebaseBoundTexts();



        loadRequestDetails();
    }

    private void bindViews(View view) {

        btnApprove = view.findViewById(R.id.btnApprove);
        btnReturn = view.findViewById(R.id.btnReturn);
        layoutActionButtons = view.findViewById(R.id.layoutActionButtons);
        cardReturnReason = view.findViewById(R.id.cardReturnReason);
        chipStatus = view.findViewById(R.id.chipStatus);
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
        layoutProposalFiles = view.findViewById(R.id.layoutProposalFiles);
        etReturnReason = view.findViewById(R.id.etReturnReason);
    }

    private void setupButtons() {
        btnApprove.setOnClickListener(v -> approveRequest());
        btnReturn.setOnClickListener(v -> handleReturnButtonClick());
    }

    private void clearFirebaseBoundTexts() {
        // These views are filled only after the request document is loaded from Firestore.
        chipStatus.setText("");
        tvPurpose.setText("");
        tvActivityType.setText("");
        tvSchedule.setText("");
        tvFacility.setText("");
        tvRequestorInfo.setText("");
        tvParticipants.setText("");
        tvPurposeFull.setText("");
        tvAmenities.setText("");
        tvTechnicalList.setText("");
        tvConnectors.setText("");
        tvProposalFile.setText("");
        tvRoute.setText("");
        tvRemarks.setText("");
        layoutProposalFiles.removeAllViews();
    }

    private void loadRequestDetails() {
        db.collection("requests")
                .document(requestId)
                .get(Source.CACHE)
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;

                    if (doc.exists()) {
                        displayRequest(doc);
                    }
                });

        requestListener = db.collection("requests")
                .document(requestId)
                .addSnapshotListener((doc, e) -> {
                    if (!isAdded()) return;

                    if (e != null) {
                        Toast.makeText(requireContext(), "Failed to load request details.", Toast.LENGTH_SHORT).show();
                        return;
                    }



                    displayRequest(doc);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (requestListener != null) {
            requestListener.remove();
            requestListener = null;
        }
    }

    private void displayRequest(DocumentSnapshot doc) {
        String purpose = getStringValue(doc, "purpose");
        String activityType = getStringValue(doc, "activityType");
        String displayStatus = getGSODisplayStatus(doc);

        String requestorName = firstNonEmpty(getStringValue(doc, "requestorName"), getStringValue(doc, "fullName"));
        String contactNumber = firstNonEmpty(getStringValue(doc, "contactNumber"), getStringValue(doc, "contactNum"));
        String department = firstNonEmpty(getStringValue(doc, "collegeDepartment"), getStringValue(doc, "department"));
        String course = firstNonEmpty(getStringValue(doc, "officeCourse"), getStringValue(doc, "course"));

        String participants = getStringValue(doc, "participants");
        String numberOfParticipants = firstNonEmpty(
                getLongString(doc, "numberOfParticipants"),
                getStringValue(doc, "numberOfParticipants")
        );
        String notificationTarget = firstNonEmpty(
                getStringValue(doc, "notificationTarget"),
                getStringValue(doc, "route")
        );

        List<DisplayProposalFile> proposalFiles = getProposalFilesFromFirestore(doc);

        chipStatus.setText(valueOrDash(displayStatus));
        styleStatusChip(displayStatus);

        // Values below come from the Firestore document. Labels are intentionally kept.
        tvPurpose.setText(valueOrDash(purpose));
        tvActivityType.setText(valueOrDash(activityType));
        tvSchedule.setText("Schedule:\n" + valueOrDash(RequestDataHelper.getScheduleDisplay(doc)));
        tvFacility.setText("Facilities: " + valueOrDash(RequestDataHelper.getFacilitiesDisplay(doc)));

        tvRequestorInfo.setText(
                "Name: " + valueOrDash(requestorName) +
                        "\nContact: " + valueOrDash(contactNumber) +
                        "\nCollege / Department: " + valueOrDash(department) +
                        "\nOffice / Course: " + valueOrDash(course)
        );

        tvParticipants.setText(
                "Participants: " + valueOrDash(participants) +
                        "\nNumber of Participants: " + valueOrDash(numberOfParticipants)
        );

        tvPurposeFull.setText("Purpose: " + valueOrDash(purpose));
        tvAmenities.setText(buildAmenities(doc));
        tvTechnicalList.setText("Technical Requirements:\n" + buildTechnicalList(doc));
        tvConnectors.setText("Connectors / Cables: " + valueOrDash(getStringValue(doc, "connectors")));
        tvRoute.setText("Route: " + valueOrDash(notificationTarget));
        tvRemarks.setText("Remarks: " + valueOrDash(getRemarks(doc)));

        bindProposalFiles(proposalFiles);

        if (isPendingStatus(displayStatus)) {
            layoutActionButtons.setVisibility(View.VISIBLE);
            resetReturnReasonBox();
        } else {
            layoutActionButtons.setVisibility(View.GONE);
            hideReturnReasonBox();
        }
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
                    file.fileName = "File " + (files.size() + 1);
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
                legacy.fileName = firstNonEmpty(getStringValue(doc, "proposalFileName"), getStringValue(doc, "fileName"));
                if (legacy.fileName.isEmpty()) legacy.fileName = "File";
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
            tvProposalFile.setText("Proposal / Supporting Files: " + valueOrDash(""));
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
            tvProposalFile.setText("Proposal / Supporting Files: " + proposalFiles.size());
            tvProposalFile.setTextColor(Color.RED);
        } else {
            tvProposalFile.setText("Proposal / Supporting Files: " + proposalFiles.size());
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
        details.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        details.setTextColor(Color.parseColor("#313131"));
        details.setTextSize(13f);
        details.setSingleLine(false);
        details.setText(
                index + ". " + valueOrDash(file.fileName) +
                        "\n" + buildFileSubtitle(file)
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

        if (parts.isEmpty()) return valueOrDash("");
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

    private void approveRequest() {
        setActionButtonsEnabled(false);

        db.collection("requests")
                .document(requestId)
                .update(
                        "status", "Approved",
                        "workflowStage", "APPROVED",

                        "gsoStatus", "Approved",
                        "gsoAvailability", "Available",
                        "gsoApprovedAt", FieldValue.serverTimestamp(),
                        "approvedBy", "GSO",

                        "isCalendarBooking", true,
                        "calendarVisible", true,
                        "bookingStatus", "Booked",

                        "gsoReturnReason", "",
                        "returnReason", "",

                        "requestorSeen", false,
                        "requestorNotificationSeen", false,
                        "requestorApprovedSeen", false,
                        "requestorNotificationType", "Approved",
                        "requestorNotificationTitle", "Request Approved",
                        "requestorNotificationMessage", "Your facility booking request has been approved by GSO.",
                        "notificationForRequestor", true,
                        "notificationUpdatedAt", FieldValue.serverTimestamp(),

                        "updatedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Request approved. Requestor will be notified and calendar will be updated.", Toast.LENGTH_SHORT).show();
                    openGsoRequestsFragment("Approved");
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    setActionButtonsEnabled(true);
                    Toast.makeText(requireContext(), "Failed to approve request.", Toast.LENGTH_SHORT).show();
                });
    }

    private void handleReturnButtonClick() {
        if (!isReturnReasonBoxShown) {
            showReturnReasonBox();
            return;
        }

        String reason = getReturnReason();

        if (reason.isEmpty()) {
            etReturnReason.setError("Return reason is required");
            etReturnReason.requestFocus();
            Toast.makeText(requireContext(), "Please input the reason for returning this request.", Toast.LENGTH_SHORT).show();
            return;
        }

        submitReturnRequest(reason);
    }

    private void showReturnReasonBox() {
        isReturnReasonBoxShown = true;
        if (cardReturnReason != null) cardReturnReason.setVisibility(View.VISIBLE);
        if (etReturnReason != null) {
            etReturnReason.setEnabled(true);
            etReturnReason.requestFocus();
        }
        btnReturn.setText("Submit Return");
        Toast.makeText(requireContext(), "Please enter the reason for returning.", Toast.LENGTH_SHORT).show();
    }

    private void hideReturnReasonBox() {
        isReturnReasonBoxShown = false;
        if (cardReturnReason != null) cardReturnReason.setVisibility(View.GONE);
        if (etReturnReason != null) {
            etReturnReason.setText("");
            etReturnReason.setError(null);
            etReturnReason.setEnabled(false);
        }
        if (btnReturn != null) btnReturn.setText("Return");
    }

    private void resetReturnReasonBox() {
        isReturnReasonBoxShown = false;
        if (cardReturnReason != null) cardReturnReason.setVisibility(View.GONE);
        if (etReturnReason != null) {
            etReturnReason.setText("");
            etReturnReason.setError(null);
            etReturnReason.setEnabled(false);
        }
        if (btnReturn != null) btnReturn.setText("Return");
    }

    private void submitReturnRequest(String reason) {
        setActionButtonsEnabled(false);

        db.collection("requests")
                .document(requestId)
                .update(
                        "status", "Returned",
                        "workflowStage", "RETURNED",

                        "gsoStatus", "Returned",
                        "gsoAvailability", "Not Available",
                        "gsoReturnedAt", FieldValue.serverTimestamp(),
                        "returnedBy", "GSO",

                        "isCalendarBooking", false,
                        "calendarVisible", false,
                        "bookingStatus", "Returned",

                        "gsoReturnReason", reason,
                        "returnReason", reason,

                        "requestorSeen", false,
                        "requestorNotificationSeen", false,
                        "requestorApprovedSeen", false,
                        "requestorNotificationType", "Returned",
                        "requestorNotificationTitle", "Request Returned",
                        "requestorNotificationMessage", reason,
                        "notificationForRequestor", true,
                        "notificationUpdatedAt", FieldValue.serverTimestamp(),

                        "updatedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Request returned successfully.", Toast.LENGTH_SHORT).show();
                    openGsoRequestsFragment("Returned");
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    setActionButtonsEnabled(true);
                    Toast.makeText(requireContext(), "Failed to return request.", Toast.LENGTH_SHORT).show();
                });
    }

    private String getReturnReason() {
        if (etReturnReason == null || etReturnReason.getText() == null) return "";
        return etReturnReason.getText().toString().trim();
    }

    private void openGsoRequestsFragment(String filter) {
        Bundle bundle = new Bundle();
        bundle.putString("filter", filter);
        gsoRequestsFragment fragment = new gsoRequestsFragment();
        fragment.setArguments(bundle);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(getAvailableContainerId(), fragment)
                .addToBackStack(null)
                .commit();
    }

    private int getAvailableContainerId() {
        if (requireActivity().findViewById(R.id.gso_fragment_container) != null) return R.id.gso_fragment_container;
        return R.id.itso_fragment_container;
    }

    private void setActionButtonsEnabled(boolean enabled) {
        btnApprove.setEnabled(enabled);
        btnReturn.setEnabled(enabled);
    }

    private String getGSODisplayStatus(DocumentSnapshot doc) {
        String gsoStatus = getStringValue(doc, "gsoStatus");
        String status = getStringValue(doc, "status");
        String bookingStatus = getStringValue(doc, "bookingStatus");
        String workflowStage = toReadableWorkflowStage(getStringValue(doc, "workflowStage"));

        return firstNonEmpty(gsoStatus, status, bookingStatus, workflowStage);
    }

    private boolean isPendingStatus(String status) {
        return "Pending".equalsIgnoreCase(status)
                || "GSO Pending".equalsIgnoreCase(status)
                || "For GSO".equalsIgnoreCase(status)
                || "FOR_GSO".equalsIgnoreCase(status);
    }

    private String toReadableWorkflowStage(String workflowStage) {
        if (workflowStage == null || workflowStage.trim().isEmpty()) return "";

        String cleaned = workflowStage.trim().replace("_", " ").toLowerCase(Locale.US);
        String[] words = cleaned.split("\\s+");
        StringBuilder builder = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) continue;
            if (builder.length() > 0) builder.append(" ");
            builder.append(word.substring(0, 1).toUpperCase(Locale.US));
            if (word.length() > 1) builder.append(word.substring(1));
        }

        return builder.toString();
    }

    private String buildAmenities(DocumentSnapshot doc) {
        boolean tablesRequested = getBooleanValue(doc, "tablesRequested");
        boolean chairsRequested = getBooleanValue(doc, "chairsRequested");
        String tablesCount = firstNonEmpty(getLongString(doc, "tablesCount"), getStringValue(doc, "tablesCount"));
        String chairsCount = firstNonEmpty(getLongString(doc, "chairsCount"), getStringValue(doc, "chairsCount"));
        String otherAmenities = getStringValue(doc, "otherAmenities");

        String tableValue = tablesRequested
                ? firstNonEmpty(tablesCount, "Requested")
                : "Not requested";

        String chairValue = chairsRequested
                ? firstNonEmpty(chairsCount, "Requested")
                : "Not requested";

        return "Tables: " + valueOrDash(tableValue) +
                "\nChairs: " + valueOrDash(chairValue) +
                "\nOther Amenities: " + valueOrDash(otherAmenities);
    }

    private String buildTechnicalList(DocumentSnapshot doc) {
        // Prefer array/string fields when your Firestore document already stores the selected labels.
        String savedTechnicalText = firstNonEmpty(
                getListOrStringValue(doc, "technicalRequirements"),
                getListOrStringValue(doc, "technicals"),
                getListOrStringValue(doc, "selectedTechnicalRequirements"),
                getListOrStringValue(doc, "technicalNeeds")
        );

        if (!savedTechnicalText.isEmpty()) return savedTechnicalText;

        // Fallback for older documents that only store boolean fields.
        List<String> selected = new ArrayList<>();
        addIfTrue(selected, doc, "soundSystemSetup", "Sound System Setup");
        addIfTrue(selected, doc, "microphones", "Microphones");
        addIfTrue(selected, doc, "portableSpeaker", "Portable Speaker");
        addIfTrue(selected, doc, "lights", "Lights");
        addIfTrue(selected, doc, "livestreamingServices", "Livestreaming Services");
        addIfTrue(selected, doc, "zoomHosting", "Zoom Hosting");
        addIfTrue(selected, doc, "gmeetHosting", "GMeet Hosting");
        addIfTrue(selected, doc, "webCamera", "Web Camera");
        addIfTrue(selected, doc, "tripod", "Tripod");
        addIfTrue(selected, doc, "multimediaProjector", "Multimedia Projector");

        if (selected.isEmpty()) return valueOrDash("");
        return buildBulletList(selected);
    }

    private void addIfTrue(List<String> list, DocumentSnapshot doc, String field, String label) {
        if (getBooleanValue(doc, field)) list.add(label);
    }

    private String buildBulletList(List<String> items) {
        StringBuilder builder = new StringBuilder();
        for (String item : items) {
            if (item == null || item.trim().isEmpty()) continue;
            if (builder.length() > 0) builder.append("\n");
            builder.append("• ").append(item.trim());
        }
        return builder.toString();
    }

    private String getListOrStringValue(DocumentSnapshot doc, String field) {
        Object raw = doc.get(field);
        if (raw == null) return "";

        if (raw instanceof List<?>) {
            List<?> list = (List<?>) raw;
            List<String> values = new ArrayList<>();
            for (Object item : list) {
                if (item == null) continue;
                String value = String.valueOf(item).trim();
                if (!value.isEmpty()) values.add(value);
            }
            return buildBulletList(values);
        }

        return String.valueOf(raw).trim();
    }

    private String getRemarks(DocumentSnapshot doc) {
        String remarks = getStringValue(doc, "gsoRemarks");
        if (!remarks.isEmpty()) return remarks;
        remarks = getStringValue(doc, "gsoReturnReason");
        if (!remarks.isEmpty()) return remarks;
        remarks = getStringValue(doc, "itsoRemarks");
        if (!remarks.isEmpty()) return remarks;
        remarks = getStringValue(doc, "returnReason");
        if (!remarks.isEmpty()) return remarks;
        return getStringValue(doc, "remarks");
    }

    private void styleStatusChip(String status) {
        if ("Approved".equalsIgnoreCase(status) || "Booked".equalsIgnoreCase(status)) {
            chipStatus.setTextColor(Color.parseColor("#2E7D32"));
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E7F4E8")));
        } else if ("Returned".equalsIgnoreCase(status) || "Not Available".equalsIgnoreCase(status)) {
            chipStatus.setTextColor(Color.parseColor("#970705"));
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F3D9D9")));
        } else {
            chipStatus.setTextColor(Color.parseColor("#313131"));
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EEEEEE")));
        }
    }

    private String getStringValue(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value != null ? String.valueOf(value).trim() : "";
    }

    private boolean getBooleanValue(DocumentSnapshot doc, String field) {
        Boolean value = doc.getBoolean(field);
        return Boolean.TRUE.equals(value);
    }

    private String getLongString(DocumentSnapshot doc, String field) {
        Long value = doc.getLong(field);
        return value != null ? String.valueOf(value) : "";
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

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private String valueOrDash(String value) {
        return value != null && !value.trim().isEmpty() ? value.trim() : "—";
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
        if (name == null || name.trim().isEmpty()) return "proposal_file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
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
