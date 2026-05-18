package com.example.cnscfacilityhubproject.activities.sacUI;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.models.ProposalFileItem;
import com.example.cnscfacilityhubproject.utils.ProposalFilesUiHelper;
import com.example.cnscfacilityhubproject.utils.RequestDataHelper;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class sacRequestsViewDetailsFragment extends Fragment {

    private static final String ARG_REQUEST_ID = "requestId";

    private FirebaseFirestore db;
    private String requestId = "";
    private String proposalFileUrl = "";

    private MaterialButton btnBack, btnOpenProposal, btnApprove, btnReject;
    private LinearLayout layoutApprovalActions;
    private Chip chipStatus;
    private TextInputEditText etSacRemarks;

    private TextView tvPurpose, tvActivityType, tvSchedule, tvFacility;
    private TextView tvRequestorInfo, tvParticipants, tvPurposeFull, tvAmenities;
    private TextView tvProposalFile, tvRoute;
    private LinearLayout layoutProposalFiles;

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
        btnOpenProposal = view.findViewById(R.id.btnOpenProposal);
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
        tvProposalFile = view.findViewById(R.id.tvProposalFile);
        tvRoute = view.findViewById(R.id.tvRoute);
        layoutProposalFiles = view.findViewById(R.id.layoutProposalFiles);
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> goBack());
        btnApprove.setOnClickListener(v -> approveAndForwardRequest());
        btnReject.setOnClickListener(v -> rejectRequest());

        btnOpenProposal.setOnClickListener(v -> {
            if (proposalFileUrl.isEmpty()) {
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
        List<ProposalFileItem> proposalFiles = RequestDataHelper.getProposalFiles(doc);
        if (!proposalFiles.isEmpty()) {
            proposalFileUrl = proposalFiles.get(0).getFileUrl();
        } else {
            proposalFileUrl = getStringValue(doc, "proposalFileUrl");
        }

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
        tvProposalFile.setText(proposalFiles.isEmpty()
                ? "Proposal files: none"
                : "Proposal files: " + proposalFiles.size());
        tvRoute.setText(buildRouteText(doc));

        etSacRemarks.setText(getStringValue(doc, "sacRemarks"));

        ProposalFilesUiHelper.bindFiles(
                requireContext(),
                layoutProposalFiles,
                tvProposalFile,
                btnOpenProposal,
                proposalFiles
        );

        layoutApprovalActions.setVisibility(
                "Pending".equalsIgnoreCase(displayStatus) ? View.VISIBLE : View.GONE
        );
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

    private String buildSchedule(
            String startDate,
            String endDate,
            String startTime,
            String endTime
    ) {
        return buildDate(startDate, endDate) + " • " + buildTime(startTime, endTime);
    }

    private String buildDate(String startDate, String endDate) {
        if (startDate.isEmpty() && endDate.isEmpty()) {
            return "No date";
        }

        if (!startDate.isEmpty()
                && !endDate.isEmpty()
                && !startDate.equalsIgnoreCase(endDate)) {
            return startDate + " - " + endDate;
        }

        return !startDate.isEmpty() ? startDate : endDate;
    }

    private String buildTime(String startTime, String endTime) {
        if (startTime.isEmpty() && endTime.isEmpty()) {
            return "No time";
        }

        if (!startTime.isEmpty() && !endTime.isEmpty()) {
            return startTime + " - " + endTime;
        }

        return !startTime.isEmpty() ? startTime : endTime;
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
        return editText.getText() != null
                ? editText.getText().toString().trim()
                : "";
    }

    private String fallback(String value) {
        return value == null || value.trim().isEmpty()
                ? "—"
                : value.trim();
    }

    private String firstNonEmpty(String first, String second) {
        return !first.isEmpty() ? first : second;
    }
}