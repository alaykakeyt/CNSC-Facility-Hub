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
import com.google.firebase.firestore.Source;

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
        clearDynamicFields();

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
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> goBack());
        }

        if (btnBackBottom != null) {
            btnBackBottom.setOnClickListener(v -> goBack());
        }
    }

    private void clearDynamicFields() {
        hideText(tvDetailsSubtitle);
        hideText(tvDetailPurpose);
        hideText(tvDetailActivityType);

        if (chipDetailStatus != null) {
            chipDetailStatus.setText("");
            chipDetailStatus.setVisibility(View.GONE);
        }

        hideText(tvDetailScheduleSummary);
        hideText(tvDetailFacilitySummary);

        hideText(tvDetailRequestorName);
        hideText(tvDetailContactNumber);
        hideText(tvDetailCollegeDepartment);
        hideText(tvDetailOfficeCourse);

        hideText(tvDetailDateRange);
        hideText(tvDetailTimeRange);
        hideText(tvDetailFacility);

        hideText(tvDetailParticipants);
        hideText(tvDetailNumberOfParticipants);
        hideText(tvDetailPurposeFull);

        hideText(tvDetailTables);
        hideText(tvDetailChairs);
        hideText(tvDetailOtherAmenities);

        hideText(tvDetailNeedsTechnical);
        hideText(tvDetailTechnicalList);
        hideText(tvDetailConnectors);

        if (cardTechnicalDetails != null) {
            cardTechnicalDetails.setVisibility(View.GONE);
        }

        hideText(tvDetailProposalFileName);
        hideText(tvDetailNotificationTarget);
        hideText(tvDetailAgreement);

        if (layoutProposalFiles != null) {
            layoutProposalFiles.removeAllViews();
            layoutProposalFiles.setVisibility(View.GONE);
        }

        if (cardAdminRemarks != null) {
            cardAdminRemarks.setVisibility(View.GONE);
        }

        hideText(tvDetailRemarks);
    }

    private void loadRequestDetails() {
        db.collection("requests")
                .document(requestId)
                .get(Source.CACHE)
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;

                    if (documentSnapshot.exists()) {
                        displayRequestDetails(documentSnapshot);
                    }

                    loadRequestDetailsFromServer();
                })
                .addOnFailureListener(e -> loadRequestDetailsFromServer());
    }

    private void loadRequestDetailsFromServer() {
        db.collection("requests")
                .document(requestId)
                .get(Source.SERVER)
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

        Boolean tablesRequested = getNullableBooleanValue(doc, "tablesRequested");
        Boolean chairsRequested = getNullableBooleanValue(doc, "chairsRequested");
        String tablesCount = getLongString(doc, "tablesCount");
        String chairsCount = getLongString(doc, "chairsCount");
        String otherAmenities = getStringValue(doc, "otherAmenities");

        Boolean technicalNeeded = firstNonNull(
                getNullableBooleanValue(doc, "technicalNeeded"),
                getNullableBooleanValue(doc, "needsITSO")
        );

        String selectedTechnicals = getTechnicalRequirementsFromFirestore(doc);
        String connectors = getStringValue(doc, "connectors");

        List<FirestoreProposalFile> proposalFiles = getFirestoreProposalFiles(doc);

        String notificationTarget = getStringValue(doc, "notificationTarget");
        Boolean agreementAccepted = getNullableBooleanValue(doc, "agreementAccepted");

        String remarks = getRemarks(doc);
        String scheduleDisplay = cleanDisplayValue(RequestDataHelper.getScheduleDisplay(doc));
        String facilitiesDisplay = cleanDisplayValue(RequestDataHelper.getFacilitiesDisplay(doc));

        tvDetailsSubtitle.setText("Request ID: " + requestId);

        setPlainTextOrHide(tvDetailPurpose, purpose);
        setPlainTextOrHide(tvDetailActivityType, activityType);

        setChipTextOrHide(chipDetailStatus, status);
        styleStatusChip(status);

        setPlainTextOrHide(tvDetailScheduleSummary, scheduleDisplay);
        setPlainTextOrHide(tvDetailFacilitySummary, facilitiesDisplay);

        setLabeledTextOrHide(tvDetailRequestorName, "Name: ", requestorName);
        setLabeledTextOrHide(tvDetailContactNumber, "Contact Number: ", contactNumber);
        setLabeledTextOrHide(tvDetailCollegeDepartment, "College / Department: ", collegeDepartment);
        setLabeledTextOrHide(tvDetailOfficeCourse, "Office / Course: ", officeCourse);

        setLabeledTextOrHide(tvDetailDateRange, "Schedule:\n", scheduleDisplay);
        tvDetailTimeRange.setVisibility(View.GONE);
        setLabeledTextOrHide(tvDetailFacility, "Facilities: ", facilitiesDisplay);

        setLabeledTextOrHide(tvDetailParticipants, "Participants: ", participants);
        setLabeledTextOrHide(tvDetailNumberOfParticipants, "Number of Participants: ", numberOfParticipants);
        setLabeledTextOrHide(tvDetailPurposeFull, "Purpose: ", purpose);

        if (tablesRequested == null) {
            hideText(tvDetailTables);
        } else {
            String tableValue = tablesRequested
                    ? "Requested" + (hasDisplayValue(tablesCount) ? " (" + tablesCount + ")" : "")
                    : "Not requested";

            setLabeledTextOrHide(tvDetailTables, "Tables: ", tableValue);
        }

        if (chairsRequested == null) {
            hideText(tvDetailChairs);
        } else {
            String chairValue = chairsRequested
                    ? "Requested" + (hasDisplayValue(chairsCount) ? " (" + chairsCount + ")" : "")
                    : "Not requested";

            setLabeledTextOrHide(tvDetailChairs, "Chairs: ", chairValue);
        }

        setLabeledTextOrHide(tvDetailOtherAmenities, "Other Amenities: ", otherAmenities);

        boolean hasTechnicalData = technicalNeeded != null
                || hasDisplayValue(selectedTechnicals)
                || hasDisplayValue(connectors);

        if (hasTechnicalData) {
            cardTechnicalDetails.setVisibility(View.VISIBLE);

            if (technicalNeeded == null) {
                hideText(tvDetailNeedsTechnical);
            } else {
                setLabeledTextOrHide(tvDetailNeedsTechnical, "Technical Needed: ", yesNo(technicalNeeded));
            }

            setLabeledTextOrHide(tvDetailTechnicalList, "Selected Technicals: ", selectedTechnicals);
            setLabeledTextOrHide(tvDetailConnectors, "Connectors / Cables: ", connectors);
        } else {
            cardTechnicalDetails.setVisibility(View.GONE);
        }

        setLabeledTextOrHide(tvDetailNotificationTarget, "Sent To: ", notificationTarget);

        if (agreementAccepted == null) {
            hideText(tvDetailAgreement);
        } else {
            setLabeledTextOrHide(tvDetailAgreement, "Agreement Accepted: ", yesNo(agreementAccepted));
        }

        bindProposalFiles(proposalFiles);

        if (hasDisplayValue(remarks)) {
            cardAdminRemarks.setVisibility(View.VISIBLE);
            setPlainTextOrHide(tvDetailRemarks, remarks);
        } else {
            cardAdminRemarks.setVisibility(View.GONE);
        }
    }

    private String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }
    private void setLabeledTextOrHide(TextView textView, String label, String value) {
        String cleaned = cleanDisplayValue(value);

        if (cleaned.isEmpty()) {
            hideText(textView);
            return;
        }

        textView.setText(label + cleaned);
        textView.setVisibility(View.VISIBLE);
    }


    private String buildBooleanCountValue(Boolean requested, String count) {
        if (requested == null) {
            return "";
        }

        if (hasDisplayValue(count)) {
            return count;
        }

        return String.valueOf(requested);
    }

    private String getTechnicalRequirementsFromFirestore(DocumentSnapshot doc) {
        String directValue = firstNonEmpty(
                getStringValue(doc, "selectedTechnicals"),
                getStringValue(doc, "technicalRequirements")
        );

        directValue = firstNonEmpty(
                directValue,
                getStringValue(doc, "selectedTechnicalRequirements")
        );

        if (hasDisplayValue(directValue)) {
            return directValue;
        }

        Object selectedTechnicalsList = doc.get("selectedTechnicalsList");
        String listValue = stringifyFirestoreValue(selectedTechnicalsList);

        if (hasDisplayValue(listValue)) {
            return listValue;
        }

        Object technicalRequirementsList = doc.get("technicalRequirementsList");
        listValue = stringifyFirestoreValue(technicalRequirementsList);

        if (hasDisplayValue(listValue)) {
            return listValue;
        }

        Object technicals = doc.get("technicals");
        listValue = stringifyFirestoreValue(technicals);

        if (hasDisplayValue(listValue)) {
            return listValue;
        }

        return "";
    }

    private String stringifyFirestoreValue(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof List<?>) {
            List<?> list = (List<?>) value;
            StringBuilder builder = new StringBuilder();

            for (Object item : list) {
                if (item == null) continue;

                String text = String.valueOf(item).trim();
                if (text.isEmpty()) continue;

                if (builder.length() > 0) {
                    builder.append(", ");
                }

                builder.append(text);
            }

            return builder.toString();
        }

        if (value instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) value;
            StringBuilder builder = new StringBuilder();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;

                String mapValue = String.valueOf(entry.getValue()).trim();
                if (mapValue.isEmpty()) continue;

                if (builder.length() > 0) {
                    builder.append(", ");
                }

                builder.append(mapValue);
            }

            return builder.toString();
        }

        return String.valueOf(value).trim();
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

                if (file.mimeType.isEmpty()) {
                    file.mimeType = guessMimeType(file);
                }

                if (hasDisplayValue(file.fileName)
                        || hasDisplayValue(file.fileUrl)
                        || hasDisplayValue(file.fileDataBase64)) {
                    files.add(file);
                }
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
                legacyFile.fileName = legacyName;
                legacyFile.fileUrl = legacyUrl;
                legacyFile.fileDataBase64 = legacyBase64;
                legacyFile.mimeType = !legacyMimeType.isEmpty()
                        ? legacyMimeType
                        : guessMimeType(legacyFile);
                legacyFile.storageType = legacyBase64.isEmpty() ? "" : "firestore_base64";

                files.add(legacyFile);
            }
        }

        return files;
    }

    private void bindProposalFiles(List<FirestoreProposalFile> files) {
        proposalFilesToOpen.clear();
        proposalFilesToOpen.addAll(files);

        if (layoutProposalFiles != null) {
            layoutProposalFiles.removeAllViews();
        }

        hideText(tvDetailProposalFileName);

        if (files.isEmpty()) {
            if (layoutProposalFiles != null) {
                layoutProposalFiles.setVisibility(View.GONE);
            }
            return;
        }

        if (layoutProposalFiles != null) {
            layoutProposalFiles.setVisibility(View.VISIBLE);

            for (FirestoreProposalFile file : files) {
                layoutProposalFiles.addView(createProposalFileRow(file));
            }
        }
    }

    private View createProposalFileRow(FirestoreProposalFile file) {
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

        if (hasDisplayValue(file.fileName)) {
            TextView name = new TextView(requireContext());
            name.setText(file.fileName);
            name.setTextColor(Color.parseColor("#313131"));
            name.setTextSize(14f);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            container.addView(name);
        }

        String fileMeta = buildFileMeta(file);

        if (hasDisplayValue(fileMeta)) {
            TextView meta = new TextView(requireContext());
            meta.setText(fileMeta);
            meta.setTextColor(Color.parseColor("#666666"));
            meta.setTextSize(12f);
            meta.setPadding(0, dp(4), 0, 0);
            container.addView(meta);
        }

        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> openProposalFile(file));
        container.setOnClickListener(v -> openProposalFile(file));

        card.addView(container);

        return card;
    }

    private String buildFileMeta(FirestoreProposalFile file) {
        List<String> metaParts = new ArrayList<>();

        if (hasDisplayValue(file.mimeType)) {
            metaParts.add(file.mimeType);
        }

        if (file.sizeBytes > 0) {
            metaParts.add(String.valueOf(file.sizeBytes));
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < metaParts.size(); i++) {
            builder.append(metaParts.get(i));

            if (i < metaParts.size() - 1) {
                builder.append(" • ");
            }
        }

        return builder.toString();
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

        if (commaIndex < 0 || commaIndex >= dataUri.length() - 1) {
            return "";
        }

        return dataUri.substring(commaIndex + 1);
    }

    private String makeSafeFileName(String originalName, String mimeType) {
        String name = originalName == null || originalName.trim().isEmpty()
                ? requestId
                : originalName.trim();

        if (name == null || name.trim().isEmpty()) {
            name = String.valueOf(System.currentTimeMillis());
        }

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

        String name = file.fileName != null
                ? file.fileName.toLowerCase(Locale.US)
                : "";

        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";

        String type = file.fileType != null
                ? file.fileType.toLowerCase(Locale.US)
                : "";

        if (type.contains("pdf")) return "application/pdf";
        if (type.contains("image")) return "image/jpeg";

        return "application/octet-stream";
    }

    private void loadRequestorInfoIfNeeded(DocumentSnapshot requestDoc) {
        String requestorName = firstNonEmpty(
                getStringValue(requestDoc, "requestorName"),
                getStringValue(requestDoc, "fullName")
        );

        String contactNumber = firstNonEmpty(
                getStringValue(requestDoc, "contactNumber"),
                getStringValue(requestDoc, "contactNum")
        );

        String collegeDepartment = firstNonEmpty(
                getStringValue(requestDoc, "collegeDepartment"),
                getStringValue(requestDoc, "department")
        );

        String officeCourse = firstNonEmpty(
                getStringValue(requestDoc, "officeCourse"),
                getStringValue(requestDoc, "course")
        );

        if (hasDisplayValue(requestorName)
                && hasDisplayValue(contactNumber)
                && hasDisplayValue(collegeDepartment)
                && hasDisplayValue(officeCourse)) {
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

                    if (!hasDisplayValue(requestorName)) {
                        setLabeledTextOrHide(
                                tvDetailRequestorName,
                                "Name: ",
                                getStringValue(userDoc, "fullName")
                        );
                    }

                    if (!hasDisplayValue(contactNumber)) {
                        setLabeledTextOrHide(
                                tvDetailContactNumber,
                                "Contact Number: ",
                                getStringValue(userDoc, "contactNum")
                        );
                    }

                    if (!hasDisplayValue(collegeDepartment)) {
                        setLabeledTextOrHide(
                                tvDetailCollegeDepartment,
                                "College / Department: ",
                                getStringValue(userDoc, "department")
                        );
                    }

                    if (!hasDisplayValue(officeCourse)) {
                        setLabeledTextOrHide(
                                tvDetailOfficeCourse,
                                "Office / Course: ",
                                getStringValue(userDoc, "course")
                        );
                    }
                });
    }

    private void styleStatusChip(String status) {
        if (chipDetailStatus == null) return;

        if ("Approved".equalsIgnoreCase(status)) {
            chipDetailStatus.setTextColor(Color.parseColor("#2E7D32"));
            chipDetailStatus.setChipBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#E7F4E8"))
            );
        } else if ("Returned".equalsIgnoreCase(status)
                || "Rejected".equalsIgnoreCase(status)) {
            chipDetailStatus.setTextColor(Color.parseColor("#970705"));
            chipDetailStatus.setChipBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#F3D9D9"))
            );
        } else {
            chipDetailStatus.setTextColor(Color.parseColor("#313131"));
            chipDetailStatus.setChipBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#EEEEEE"))
            );
        }
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
        if (doc == null || field == null) {
            return "";
        }

        Object value = doc.get(field);

        if (value == null) {
            return "";
        }

        return String.valueOf(value).trim();
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

            if ("true".equalsIgnoreCase(text)) {
                return true;
            }

            if ("false".equalsIgnoreCase(text)) {
                return false;
            }
        }

        return null;
    }

    private String getLongString(DocumentSnapshot doc, String field) {
        if (doc == null || field == null || !doc.contains(field)) {
            return "";
        }

        Object value = doc.get(field);

        if (value == null) {
            return "";
        }

        return String.valueOf(value).trim();
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

    private Boolean firstNonNull(Boolean first, Boolean second) {
        return first != null ? first : second;
    }

    private String cleanDisplayValue(String value) {
        if (value == null) return "";

        String cleaned = value.trim();

        if (cleaned.isEmpty()) return "";

        String lower = cleaned.toLowerCase(Locale.US);

        if (cleaned.equals("—")
                || cleaned.equals("-")
                || lower.equals("null")
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

    private void setPlainTextOrHide(TextView textView, String value) {
        if (textView == null) return;

        String cleaned = cleanDisplayValue(value);

        if (cleaned.isEmpty()) {
            hideText(textView);
            return;
        }

        textView.setText(cleaned);
        textView.setVisibility(View.VISIBLE);
    }

    private void setChipTextOrHide(Chip chip, String value) {
        if (chip == null) return;

        String cleaned = cleanDisplayValue(value);

        if (cleaned.isEmpty()) {
            chip.setText("");
            chip.setVisibility(View.GONE);
            return;
        }

        chip.setText(cleaned);
        chip.setVisibility(View.VISIBLE);
    }

    private void hideText(TextView textView) {
        if (textView == null) return;

        textView.setText("");
        textView.setVisibility(View.GONE);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
            return fileUrl != null
                    && fileUrl.startsWith("data:")
                    && fileUrl.contains("base64,");
        }
    }
}