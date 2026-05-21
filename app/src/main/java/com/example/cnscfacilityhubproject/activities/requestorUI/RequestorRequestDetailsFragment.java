package com.example.cnscfacilityhubproject.activities.requestorUI;

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
import com.example.cnscfacilityhubproject.utils.RequestDataHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RequestorRequestDetailsFragment extends Fragment {

    private static final String ARG_REQUEST_ID = "requestId";

    private FirebaseFirestore db;
    private String requestId = "";

    private final List<FirestoreProposalFile> proposalFilesToOpen = new ArrayList<>();

    private MaterialButton btnBack;
    private MaterialButton btnBackBottom;
    private TextView tvDetailsSubtitle;
    private TextView tvDetailPurpose;
    private TextView tvDetailActivityType;
    private Chip chipDetailStatus;
    private TextView tvDetailScheduleSummary;
    private TextView tvDetailFacilitySummary;

    private TextView tvDetailRequestorName;
    private TextView tvDetailContactNumber;
    private TextView tvDetailCollegeDepartment;
    private TextView tvDetailOfficeCourse;

    private TextView tvDetailDateRange;
    private TextView tvDetailTimeRange;
    private TextView tvDetailFacility;

    private TextView tvDetailParticipants;
    private TextView tvDetailNumberOfParticipants;
    private TextView tvDetailPurposeFull;

    private TextView tvDetailTables;
    private TextView tvDetailChairs;
    private TextView tvDetailOtherAmenities;

    private MaterialCardView cardTechnicalDetails;
    private TextView tvDetailNeedsTechnical;
    private TextView tvDetailTechnicalList;
    private TextView tvDetailConnectors;

    private TextView tvDetailProposalFileName;
    private TextView tvDetailNotificationTarget;
    private TextView tvDetailAgreement;
    private LinearLayout layoutProposalFiles;

    private MaterialCardView cardAdminRemarks;
    private TextView tvDetailRemarks;

    public RequestorRequestDetailsFragment() {
        super(R.layout.fragment_requestor_request_details);
    }

    public static RequestorRequestDetailsFragment newInstance(String requestId) {
        RequestorRequestDetailsFragment fragment = new RequestorRequestDetailsFragment();
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
        btnBackBottom = view.findViewById(R.id.btnBackBottom);

        tvDetailsSubtitle = view.findViewById(R.id.tvDetailsSubtitle);
        tvDetailPurpose = view.findViewById(R.id.tvDetailPurpose);
        tvDetailActivityType = view.findViewById(R.id.tvDetailActivityType);
        chipDetailStatus = view.findViewById(R.id.chipDetailStatus);
        tvDetailScheduleSummary = view.findViewById(R.id.tvDetailScheduleSummary);
        tvDetailFacilitySummary = view.findViewById(R.id.tvDetailFacilitySummary);

        tvDetailRequestorName = view.findViewById(R.id.tvDetailRequestorName);
        tvDetailContactNumber = view.findViewById(R.id.tvDetailContactNumber);
        tvDetailCollegeDepartment = view.findViewById(R.id.tvDetailCollegeDepartment);
        tvDetailOfficeCourse = view.findViewById(R.id.tvDetailOfficeCourse);

        tvDetailDateRange = view.findViewById(R.id.tvDetailDateRange);
        tvDetailTimeRange = view.findViewById(R.id.tvDetailTimeRange);
        tvDetailFacility = view.findViewById(R.id.tvDetailFacility);

        tvDetailParticipants = view.findViewById(R.id.tvDetailParticipants);
        tvDetailNumberOfParticipants = view.findViewById(R.id.tvDetailNumberOfParticipants);
        tvDetailPurposeFull = view.findViewById(R.id.tvDetailPurposeFull);

        tvDetailTables = view.findViewById(R.id.tvDetailTables);
        tvDetailChairs = view.findViewById(R.id.tvDetailChairs);
        tvDetailOtherAmenities = view.findViewById(R.id.tvDetailOtherAmenities);

        cardTechnicalDetails = view.findViewById(R.id.cardTechnicalDetails);
        tvDetailNeedsTechnical = view.findViewById(R.id.tvDetailNeedsTechnical);
        tvDetailTechnicalList = view.findViewById(R.id.tvDetailTechnicalList);
        tvDetailConnectors = view.findViewById(R.id.tvDetailConnectors);

        tvDetailProposalFileName = view.findViewById(R.id.tvDetailProposalFileName);
        tvDetailNotificationTarget = view.findViewById(R.id.tvDetailNotificationTarget);
        tvDetailAgreement = view.findViewById(R.id.tvDetailAgreement);
        layoutProposalFiles = view.findViewById(R.id.layoutProposalFiles);

        cardAdminRemarks = view.findViewById(R.id.cardAdminRemarks);
        tvDetailRemarks = view.findViewById(R.id.tvDetailRemarks);
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> goBack());
        btnBackBottom.setOnClickListener(v -> goBack());
    }

    private void loadRequestDetails() {
        db.collection("requests")
                .document(requestId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;

                    if (!documentSnapshot.exists()) {
                        Toast.makeText(requireContext(), "Request not found.", Toast.LENGTH_SHORT).show();
                        goBack();
                        return;
                    }

                    displayRequestDetails(documentSnapshot);
                    loadRequestorInfoIfNeeded(documentSnapshot);
                    markAsSeenIfUnseen(documentSnapshot);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;

                    Toast.makeText(
                            requireContext(),
                            "Failed to load request details: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                    goBack();
                });
    }

    private void displayRequestDetails(DocumentSnapshot doc) {
        String activityType = getStringValue(doc, "activityType");
        String status = getStringValue(doc, "status");
        String purpose = getStringValue(doc, "purpose");

        String requestorName = firstNonEmpty(
                getStringValue(doc, "requestorName"),
                getStringValue(doc, "fullName")
        );
        String contactNumber = firstNonEmpty(
                getStringValue(doc, "contactNumber"),
                getStringValue(doc, "contactNum")
        );
        String collegeDepartment = firstNonEmpty(
                getStringValue(doc, "collegeDepartment"),
                getStringValue(doc, "department")
        );
        String officeCourse = firstNonEmpty(
                getStringValue(doc, "officeCourse"),
                getStringValue(doc, "course")
        );

        String participants = getStringValue(doc, "participants");
        String numberOfParticipants = getLongString(doc, "numberOfParticipants");

        boolean tablesRequested = getBooleanValue(doc, "tablesRequested");
        boolean chairsRequested = getBooleanValue(doc, "chairsRequested");
        String tablesCount = getLongString(doc, "tablesCount");
        String chairsCount = getLongString(doc, "chairsCount");
        String otherAmenities = getStringValue(doc, "otherAmenities");

        boolean technicalNeeded = getBooleanValue(doc, "technicalNeeded") || getBooleanValue(doc, "needsITSO");
        String connectors = getStringValue(doc, "connectors");

        List<FirestoreProposalFile> proposalFiles = getFirestoreProposalFiles(doc);

        String notificationTarget = getStringValue(doc, "notificationTarget");
        boolean agreementAccepted = getBooleanValue(doc, "agreementAccepted");

        String remarks = getRemarks(doc);

        tvDetailsSubtitle.setText("Request ID: " + requestId);

        tvDetailPurpose.setText(!purpose.isEmpty() ? purpose : "Purpose / Activity");
        tvDetailActivityType.setText(!activityType.isEmpty() ? activityType : "No activity type");

        chipDetailStatus.setText(!status.isEmpty() ? status : "Pending");
        styleStatusChip(status);

        tvDetailScheduleSummary.setText(RequestDataHelper.getScheduleDisplay(doc));
        tvDetailFacilitySummary.setText(RequestDataHelper.getFacilitiesDisplay(doc));

        tvDetailRequestorName.setText("Name: " + fallback(requestorName));
        tvDetailContactNumber.setText("Contact Number: " + fallback(contactNumber));
        tvDetailCollegeDepartment.setText("College / Department: " + fallback(collegeDepartment));
        tvDetailOfficeCourse.setText("Office / Course: " + fallback(officeCourse));

        tvDetailDateRange.setText("Schedule:\n" + RequestDataHelper.getScheduleDisplay(doc));
        tvDetailTimeRange.setVisibility(View.GONE);
        tvDetailFacility.setText("Facilities: " + RequestDataHelper.getFacilitiesDisplay(doc));

        tvDetailParticipants.setText("Participants: " + fallback(participants));
        tvDetailNumberOfParticipants.setText("Number of Participants: " + fallback(numberOfParticipants));
        tvDetailPurposeFull.setText("Purpose: " + fallback(purpose));

        if (tablesRequested) {
            tvDetailTables.setText("Tables: Requested" + (!tablesCount.isEmpty() ? " (" + tablesCount + ")" : ""));
        } else {
            tvDetailTables.setText("Tables: Not requested");
        }

        if (chairsRequested) {
            tvDetailChairs.setText("Chairs: Requested" + (!chairsCount.isEmpty() ? " (" + chairsCount + ")" : ""));
        } else {
            tvDetailChairs.setText("Chairs: Not requested");
        }

        tvDetailOtherAmenities.setText("Other Amenities: " + fallback(otherAmenities));

        cardTechnicalDetails.setVisibility(View.VISIBLE);

        if (technicalNeeded) {
            tvDetailNeedsTechnical.setText("Technical Needed: Yes");
            tvDetailTechnicalList.setText("Selected Technicals: " + buildTechnicalList(doc));
            tvDetailConnectors.setText("Connectors / Cables: " + fallback(connectors));
        } else {
            tvDetailNeedsTechnical.setText("Technical Needed: No");
            tvDetailTechnicalList.setText("Selected Technicals: None");
            tvDetailConnectors.setText("Connectors / Cables: None");
        }

        tvDetailNotificationTarget.setText("Sent To: " + fallback(notificationTarget));
        tvDetailAgreement.setText("Agreement Accepted: " + (agreementAccepted ? "Yes" : "No"));

        bindProposalFiles(proposalFiles);

        cardAdminRemarks.setVisibility(View.VISIBLE);
        tvDetailRemarks.setText(!remarks.isEmpty() ? remarks : "No remarks available.");
    }

    private List<FirestoreProposalFile> getFirestoreProposalFiles(DocumentSnapshot doc) {
        List<FirestoreProposalFile> files = new ArrayList<>();
        Object rawFiles = doc.get("proposalFiles");

        if (rawFiles instanceof List<?>) {
            List<?> rawList = (List<?>) rawFiles;

            for (Object item : rawList) {
                if (!(item instanceof Map<?, ?>)) continue;

                Map<?, ?> map = (Map<?, ?>) item;

                FirestoreProposalFile file = new FirestoreProposalFile();
                file.fileName = firstNonEmpty(getMapString(map, "fileName"), getMapString(map, "name"));
                file.fileType = getMapString(map, "fileType");
                file.mimeType = getMapString(map, "mimeType");
                file.storageType = getMapString(map, "storageType");
                file.fileDataBase64 = firstNonEmpty(
                        getMapString(map, "fileDataBase64"),
                        getMapString(map, "base64")
                );
                file.fileUrl = firstNonEmpty(
                        getMapString(map, "fileUrl"),
                        getMapString(map, "url")
                );
                file.sizeBytes = getMapLong(map, "sizeBytes");

                if (file.fileName.isEmpty()) {
                    file.fileName = "proposal_file_" + (files.size() + 1);
                }

                if (file.mimeType.isEmpty()) {
                    file.mimeType = guessMimeType(file);
                }

                files.add(file);
            }
        }

        if (files.isEmpty()) {
            String legacyName = firstNonEmpty(
                    getStringValue(doc, "proposalFileName"),
                    getStringValue(doc, "fileName")
            );
            String legacyUrl = getStringValue(doc, "proposalFileUrl");
            String legacyBase64 = getStringValue(doc, "fileDataBase64");
            String legacyMimeType = getStringValue(doc, "mimeType");

            if (!legacyUrl.isEmpty() || !legacyBase64.isEmpty()) {
                FirestoreProposalFile legacyFile = new FirestoreProposalFile();
                legacyFile.fileName = !legacyName.isEmpty() ? legacyName : "proposal_file";
                legacyFile.fileUrl = legacyUrl;
                legacyFile.fileDataBase64 = legacyBase64;
                legacyFile.mimeType = !legacyMimeType.isEmpty() ? legacyMimeType : guessMimeType(legacyFile);
                legacyFile.storageType = legacyBase64.isEmpty() ? "legacy_url" : "firestore_base64";
                files.add(legacyFile);
            }
        }

        return files;
    }

    private void bindProposalFiles(List<FirestoreProposalFile> files) {
        proposalFilesToOpen.clear();
        proposalFilesToOpen.addAll(files);
        layoutProposalFiles.removeAllViews();

        if (files.isEmpty()) {
            tvDetailProposalFileName.setText("Proposal / Supporting Files: none");
            tvDetailProposalFileName.setTextColor(Color.parseColor("#313131"));
            return;
        }

        tvDetailProposalFileName.setText("Proposal / Supporting Files: " + files.size() + " file(s) • Tap a file to open");
        tvDetailProposalFileName.setTextColor(Color.parseColor("#313131"));

        for (int i = 0; i < files.size(); i++) {
            FirestoreProposalFile file = files.get(i);
            layoutProposalFiles.addView(createProposalFileRow(file, i + 1));
        }
    }

    private View createProposalFileRow(FirestoreProposalFile file, int position) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, dp(8), 0, 0);
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(Color.parseColor("#FAFAFA"));
        card.setRadius(dp(16));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(Color.parseColor("#E0E0E0"));
        card.setCardElevation(0f);

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(12), dp(10), dp(12), dp(10));

        TextView name = new TextView(requireContext());
        name.setText(position + ". " + file.fileName);
        name.setTextColor(Color.parseColor("#313131"));
        name.setTextSize(14f);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        container.addView(name);

        TextView meta = new TextView(requireContext());
        meta.setText(buildFileMeta(file));
        meta.setTextColor(Color.parseColor("#666666"));
        meta.setTextSize(12f);
        meta.setPadding(0, dp(4), 0, 0);
        container.addView(meta);

        TextView tapHint = new TextView(requireContext());
        tapHint.setText("Tap to open");
        tapHint.setTextColor(Color.parseColor("#970705"));
        tapHint.setTextSize(12f);
        tapHint.setPadding(0, dp(6), 0, 0);
        container.addView(tapHint);

        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> openProposalFile(file));
        container.setOnClickListener(v -> openProposalFile(file));

        card.addView(container);
        return card;
    }

    private String buildFileMeta(FirestoreProposalFile file) {
        String mime = !file.mimeType.isEmpty() ? file.mimeType : "unknown type";
        String size = file.sizeBytes > 0 ? " • " + formatBytes(file.sizeBytes) : "";
        String source = file.hasBase64Data() || file.hasDataUri() ? "Firestore Base64" : "External URI";
        return mime + size + " • " + source;
    }

    private void openProposalFile(FirestoreProposalFile file) {
        if (!isAdded()) return;

        try {
            if (file.hasBase64Data() || file.hasDataUri()) {
                Uri cachedFileUri = createCachedFileUri(file);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(cachedFileUri, guessMimeType(file));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
                return;
            }

            if (!file.fileUrl.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(file.fileUrl));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
                return;
            }

            Toast.makeText(requireContext(), "This file has no data to open.", Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "No app found to open this file.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Uri createCachedFileUri(FirestoreProposalFile file) throws IOException {
        String base64 = file.fileDataBase64;

        if (base64 == null || base64.trim().isEmpty()) {
            base64 = extractBase64FromDataUri(file.fileUrl);
        }

        if (base64 == null || base64.trim().isEmpty()) {
            throw new IOException("Missing file data.");
        }

        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);

        File folder = new File(requireContext().getCacheDir(), "proposal_files");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Unable to prepare cache folder.");
        }

        String safeFileName = makeSafeFileName(file.fileName, guessMimeType(file));
        File outputFile = new File(folder, safeFileName);

        FileOutputStream outputStream = new FileOutputStream(outputFile);
        try {
            outputStream.write(bytes);
            outputStream.flush();
        } finally {
            outputStream.close();
        }

        return FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                outputFile
        );
    }

    private String extractBase64FromDataUri(String dataUri) {
        if (dataUri == null) return "";
        int commaIndex = dataUri.indexOf(',');
        if (commaIndex < 0 || commaIndex >= dataUri.length() - 1) return "";
        return dataUri.substring(commaIndex + 1);
    }

    private String makeSafeFileName(String originalName, String mimeType) {
        String name = originalName == null || originalName.trim().isEmpty()
                ? "proposal_file"
                : originalName.trim();

        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");

        if (!name.contains(".")) {
            if ("application/pdf".equalsIgnoreCase(mimeType)) {
                name += ".pdf";
            } else if ("image/png".equalsIgnoreCase(mimeType)) {
                name += ".png";
            } else if ("image/webp".equalsIgnoreCase(mimeType)) {
                name += ".webp";
            } else if (mimeType != null && mimeType.startsWith("image/")) {
                name += ".jpg";
            }
        }

        return name;
    }

    private String guessMimeType(FirestoreProposalFile file) {
        if (file == null) return "application/octet-stream";

        if (file.mimeType != null && !file.mimeType.trim().isEmpty()) {
            return file.mimeType.trim();
        }

        if (file.fileUrl != null && file.fileUrl.startsWith("data:")) {
            int colon = file.fileUrl.indexOf(':');
            int semicolon = file.fileUrl.indexOf(';');
            if (colon >= 0 && semicolon > colon) {
                return file.fileUrl.substring(colon + 1, semicolon);
            }
        }

        String name = file.fileName != null ? file.fileName.toLowerCase(Locale.US) : "";
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";

        String type = file.fileType != null ? file.fileType.toLowerCase(Locale.US) : "";
        if (type.contains("pdf")) return "application/pdf";
        if (type.contains("image")) return "image/jpeg";

        return "application/octet-stream";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        return String.format(Locale.US, "%.2f MB", mb);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String getMapString(Map<?, ?> map, String key) {
        if (map == null || key == null) return "";
        Object value = map.get(key);
        return value != null ? String.valueOf(value).trim() : "";
    }

    private long getMapLong(Map<?, ?> map, String key) {
        if (map == null || key == null) return 0L;
        Object value = map.get(key);

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }

        return 0L;
    }

    private void loadRequestorInfoIfNeeded(DocumentSnapshot requestDoc) {
        String currentName = tvDetailRequestorName.getText().toString();

        if (!currentName.endsWith("—")) {
            return;
        }

        String userId = getStringValue(requestDoc, "userId");

        if (userId.isEmpty()) {
            return;
        }

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (!isAdded() || !userDoc.exists()) return;

                    String fullName = getStringValue(userDoc, "fullName");
                    String contactNum = getStringValue(userDoc, "contactNum");
                    String department = getStringValue(userDoc, "department");
                    String course = getStringValue(userDoc, "course");

                    tvDetailRequestorName.setText("Name: " + fallback(fullName));
                    tvDetailContactNumber.setText("Contact Number: " + fallback(contactNum));
                    tvDetailCollegeDepartment.setText("College / Department: " + fallback(department));
                    tvDetailOfficeCourse.setText("Office / Course: " + fallback(course));
                });
    }

    private void styleStatusChip(String status) {
        if ("Approved".equalsIgnoreCase(status)) {
            chipDetailStatus.setTextColor(Color.parseColor("#2E7D32"));
            chipDetailStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E7F4E8")));
        } else if ("Returned".equalsIgnoreCase(status)) {
            chipDetailStatus.setTextColor(Color.parseColor("#970705"));
            chipDetailStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F3D9D9")));
        } else {
            chipDetailStatus.setTextColor(Color.parseColor("#313131"));
            chipDetailStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EEEEEE")));
        }
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

        if (selected.isEmpty()) {
            return "None";
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < selected.size(); i++) {
            builder.append(selected.get(i));

            if (i < selected.size() - 1) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }

    private String getRemarks(DocumentSnapshot doc) {
        String remarks = getStringValue(doc, "remarks");
        if (!remarks.isEmpty()) return remarks;

        remarks = getStringValue(doc, "returnReason");
        if (!remarks.isEmpty()) return remarks;

        remarks = getStringValue(doc, "adminRemarks");
        if (!remarks.isEmpty()) return remarks;

        remarks = getStringValue(doc, "gsoRemarks");
        if (!remarks.isEmpty()) return remarks;

        remarks = getStringValue(doc, "itsoRemarks");
        return remarks;
    }

    private String getStringValue(DocumentSnapshot doc, String field) {
        String value = doc.getString(field);
        return value != null ? value.trim() : "";
    }

    private boolean getBooleanValue(DocumentSnapshot doc, String field) {
        Boolean value = doc.getBoolean(field);
        return Boolean.TRUE.equals(value);
    }

    private String getLongString(DocumentSnapshot doc, String field) {
        Long value = doc.getLong(field);

        if (value == null) {
            return "";
        }

        return String.valueOf(value);
    }

    private String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }

        if (second != null && !second.trim().isEmpty()) {
            return second.trim();
        }

        return "";
    }

    private String fallback(String value) {
        return value != null && !value.trim().isEmpty() ? value.trim() : "—";
    }

    private void markAsSeenIfUnseen(DocumentSnapshot doc) {
        if (RequestDataHelper.isRequestorNotificationUnseen(doc)) {
            doc.getReference().update(
                    "requestorSeen", true,
                    "requestorNotificationSeen", true,
                    "requestorApprovedSeen", true,
                    "notificationForRequestor", false,
                    "requestorSeenAt", FieldValue.serverTimestamp(),
                    "requestorNotificationOpenedAt", FieldValue.serverTimestamp(),
                    "updatedAt", FieldValue.serverTimestamp()
            );
        }
    }

    private void goBack() {
        if (!isAdded()) return;

        requireActivity()
                .getSupportFragmentManager()
                .popBackStack();
    }

    private static class FirestoreProposalFile {
        String fileName = "";
        String fileType = "";
        String mimeType = "";
        String storageType = "";
        String fileDataBase64 = "";
        String fileUrl = "";
        long sizeBytes = 0L;

        boolean hasBase64Data() {
            return fileDataBase64 != null && !fileDataBase64.trim().isEmpty();
        }

        boolean hasDataUri() {
            return fileUrl != null && fileUrl.startsWith("data:") && fileUrl.contains("base64,");
        }
    }
}
