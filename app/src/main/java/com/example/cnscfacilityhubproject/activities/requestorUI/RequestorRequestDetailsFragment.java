package com.example.cnscfacilityhubproject.activities.requestorUI;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cnscfacilityhubproject.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class RequestorRequestDetailsFragment extends Fragment {

    private static final String ARG_REQUEST_ID = "requestId";

    private FirebaseFirestore db;
    private String requestId = "";
    private String proposalFileUrl = "";

    private MaterialButton btnBack;
    private MaterialButton btnBackBottom;
    private MaterialButton btnOpenProposal;

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
        btnOpenProposal = view.findViewById(R.id.btnOpenProposal);

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

        cardAdminRemarks = view.findViewById(R.id.cardAdminRemarks);
        tvDetailRemarks = view.findViewById(R.id.tvDetailRemarks);
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> goBack());
        btnBackBottom.setOnClickListener(v -> goBack());

        btnOpenProposal.setOnClickListener(v -> {
            if (TextUtils.isEmpty(proposalFileUrl)) {
                Toast.makeText(requireContext(), "No proposal file available.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(proposalFileUrl));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Unable to open proposal file.", Toast.LENGTH_SHORT).show();
            }
        });
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

        String startDate = getStringValue(doc, "startDateText");
        String endDate = getStringValue(doc, "endDateText");
        String startTime = getStringValue(doc, "timeStartText");
        String endTime = getStringValue(doc, "timeEndText");

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

        String facility = getFinalFacility(doc);
        String participants = getStringValue(doc, "participants");
        String numberOfParticipants = getLongString(doc, "numberOfParticipants");

        boolean tablesRequested = getBooleanValue(doc, "tablesRequested");
        boolean chairsRequested = getBooleanValue(doc, "chairsRequested");
        String tablesCount = getLongString(doc, "tablesCount");
        String chairsCount = getLongString(doc, "chairsCount");
        String otherAmenities = getStringValue(doc, "otherAmenities");

        boolean technicalNeeded = getBooleanValue(doc, "technicalNeeded") || getBooleanValue(doc, "needsITSO");
        String connectors = getStringValue(doc, "connectors");

        String proposalFileName = getStringValue(doc, "proposalFileName");
        proposalFileUrl = getStringValue(doc, "proposalFileUrl");

        String notificationTarget = getStringValue(doc, "notificationTarget");
        boolean agreementAccepted = getBooleanValue(doc, "agreementAccepted");

        String remarks = getRemarks(doc);

        tvDetailsSubtitle.setText("Request ID: " + requestId);

        tvDetailPurpose.setText(!purpose.isEmpty() ? purpose : "Purpose / Activity");
        tvDetailActivityType.setText(!activityType.isEmpty() ? activityType : "No activity type");

        chipDetailStatus.setText(!status.isEmpty() ? status : "Pending");
        styleStatusChip(status);

        tvDetailScheduleSummary.setText(buildScheduleText(startDate, endDate, startTime, endTime));
        tvDetailFacilitySummary.setText(!facility.isEmpty() ? facility : "No facility selected");

        tvDetailRequestorName.setText("Name: " + fallback(requestorName));
        tvDetailContactNumber.setText("Contact Number: " + fallback(contactNumber));
        tvDetailCollegeDepartment.setText("College / Department: " + fallback(collegeDepartment));
        tvDetailOfficeCourse.setText("Office / Course: " + fallback(officeCourse));

        tvDetailDateRange.setText("Date: " + buildDateText(startDate, endDate));
        tvDetailTimeRange.setText("Time: " + buildTimeText(startTime, endTime));
        tvDetailFacility.setText("Facility: " + fallback(facility));

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

        tvDetailProposalFileName.setText("Proposal File: " + fallback(proposalFileName));
        tvDetailNotificationTarget.setText("Sent To: " + fallback(notificationTarget));
        tvDetailAgreement.setText("Agreement Accepted: " + (agreementAccepted ? "Yes" : "No"));

        btnOpenProposal.setVisibility(!proposalFileUrl.isEmpty() ? View.VISIBLE : View.GONE);

        cardAdminRemarks.setVisibility(View.VISIBLE);
        tvDetailRemarks.setText(!remarks.isEmpty() ? remarks : "No remarks available.");
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

    private String getFinalFacility(DocumentSnapshot doc) {
        String finalFacilityName = getStringValue(doc, "finalFacilityName");

        if (!finalFacilityName.isEmpty()) {
            return finalFacilityName;
        }

        String facility = getStringValue(doc, "facility");
        String otherFacility = getStringValue(doc, "otherFacility");

        if ("Others".equalsIgnoreCase(facility) && !otherFacility.isEmpty()) {
            return otherFacility;
        }

        return facility;
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

    private String buildScheduleText(String startDate, String endDate, String startTime, String endTime) {
        return buildDateText(startDate, endDate) + " • " + buildTimeText(startTime, endTime);
    }

    private String buildDateText(String startDate, String endDate) {
        if (startDate.isEmpty() && endDate.isEmpty()) {
            return "No date";
        }

        if (!startDate.isEmpty() && !endDate.isEmpty() && !startDate.equalsIgnoreCase(endDate)) {
            return startDate + " - " + endDate;
        }

        return !startDate.isEmpty() ? startDate : endDate;
    }

    private String buildTimeText(String startTime, String endTime) {
        if (startTime.isEmpty() && endTime.isEmpty()) {
            return "No time";
        }

        if (!startTime.isEmpty() && !endTime.isEmpty()) {
            return startTime + " - " + endTime;
        }

        return !startTime.isEmpty() ? startTime : endTime;
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

    private void goBack() {
        if (!isAdded()) return;

        requireActivity()
                .getSupportFragmentManager()
                .popBackStack();
    }

}