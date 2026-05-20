package com.example.cnscfacilityhubproject.activities.gsoUI;

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
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class gsoRequestsViewDetailsFragment extends Fragment {

    private static final String ARG_REQUEST_ID = "requestId";

    private FirebaseFirestore db;
    private String requestId = "";
    private String proposalFileUrl = "";

    private MaterialButton btnBack;
    private MaterialButton btnOpenProposal;
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
        btnBack.setOnClickListener(v -> goBack());
        btnApprove.setOnClickListener(v -> approveRequest());
        btnReturn.setOnClickListener(v -> handleReturnButtonClick());

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
        String displayStatus = getGSODisplayStatus(doc);

        String requestorName = firstNonEmpty(getStringValue(doc, "requestorName"), getStringValue(doc, "fullName"));
        String contactNumber = firstNonEmpty(getStringValue(doc, "contactNumber"), getStringValue(doc, "contactNum"));
        String department = firstNonEmpty(getStringValue(doc, "collegeDepartment"), getStringValue(doc, "department"));
        String course = firstNonEmpty(getStringValue(doc, "officeCourse"), getStringValue(doc, "course"));

        String participants = getStringValue(doc, "participants");
        String numberOfParticipants = getLongString(doc, "numberOfParticipants");
        String notificationTarget = getStringValue(doc, "notificationTarget");
        List<ProposalFileItem> proposalFiles = RequestDataHelper.getProposalFiles(doc);
        boolean hasContentUri = false;
        if (!proposalFiles.isEmpty()) {
            proposalFileUrl = proposalFiles.get(0).getFileUrl();
            for (ProposalFileItem f : proposalFiles) {
                if (f.getFileUrl().startsWith("content://")) {
                    hasContentUri = true;
                    break;
                }
            }
        } else {
            proposalFileUrl = getStringValue(doc, "proposalFileUrl");
            if (proposalFileUrl.startsWith("content://")) {
                hasContentUri = true;
            }
        }

        chipStatus.setText(displayStatus);
        styleStatusChip(displayStatus);

        tvPurpose.setText(!purpose.isEmpty() ? purpose : "Request Details");
        tvActivityType.setText(!activityType.isEmpty() ? activityType : "Facility booking request");
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
        
        if (hasContentUri) {
            tvProposalFile.setText("Warning: This request contains files with local URIs that may not open correctly.");
            tvProposalFile.setTextColor(Color.RED);
        } else {
            tvProposalFile.setText(proposalFiles.isEmpty()
                    ? "Proposal files: none"
                    : "Proposal files: " + proposalFiles.size());
            tvProposalFile.setTextColor(Color.parseColor("#313131"));
        }

        tvRoute.setText("Route: " + fallback(notificationTarget));
        tvRemarks.setText("Remarks: " + fallback(getRemarks(doc)));

        ProposalFilesUiHelper.bindFiles(
                requireContext(),
                layoutProposalFiles,
                tvProposalFile,
                btnOpenProposal,
                proposalFiles
        );

        if ("Pending".equalsIgnoreCase(displayStatus)) {
            layoutActionButtons.setVisibility(View.VISIBLE);
            resetReturnReasonBox();
        } else {
            layoutActionButtons.setVisibility(View.GONE);
            hideReturnReasonBox();
        }
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
        if ("Approved".equalsIgnoreCase(gsoStatus) || "Approved".equalsIgnoreCase(status)) return "Approved";
        if ("Returned".equalsIgnoreCase(gsoStatus) || "Returned".equalsIgnoreCase(status)) return "Returned";
        return "Pending";
    }

    private String buildAmenities(DocumentSnapshot doc) {
        boolean tablesRequested = getBooleanValue(doc, "tablesRequested");
        boolean chairsRequested = getBooleanValue(doc, "chairsRequested");
        String tablesCount = getLongString(doc, "tablesCount");
        String chairsCount = getLongString(doc, "chairsCount");
        String otherAmenities = getStringValue(doc, "otherAmenities");

        return "Tables: " + (tablesRequested ? "Requested (" + fallback(tablesCount) + ")" : "Not requested") +
                "\nChairs: " + (chairsRequested ? "Requested (" + fallback(chairsCount) + ")" : "Not requested") +
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
        if ("Approved".equalsIgnoreCase(status)) {
            chipStatus.setTextColor(Color.parseColor("#2E7D32"));
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E7F4E8")));
        } else if ("Returned".equalsIgnoreCase(status)) {
            chipStatus.setTextColor(Color.parseColor("#970705"));
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F3D9D9")));
        } else {
            chipStatus.setTextColor(Color.parseColor("#313131"));
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EEEEEE")));
        }
    }

    private String getFinalFacility(DocumentSnapshot doc) {
        String finalFacilityName = getStringValue(doc, "finalFacilityName");
        if (!finalFacilityName.isEmpty()) return finalFacilityName;
        String facility = getStringValue(doc, "facility");
        String otherFacility = getStringValue(doc, "otherFacility");
        if ("Others".equalsIgnoreCase(facility) && !otherFacility.isEmpty()) return otherFacility;
        return facility;
    }

    private String buildSchedule(String startDate, String endDate, String startTime, String endTime) {
        return buildDate(startDate, endDate) + " • " + buildTime(startTime, endTime);
    }

    private String buildDate(String startDate, String endDate) {
        if (startDate.isEmpty() && endDate.isEmpty()) return "No date";
        if (!startDate.isEmpty() && !endDate.isEmpty() && !startDate.equalsIgnoreCase(endDate)) return startDate + " - " + endDate;
        return !startDate.isEmpty() ? startDate : endDate;
    }

    private String buildTime(String startTime, String endTime) {
        if (startTime.isEmpty() && endTime.isEmpty()) return "No time";
        if (!startTime.isEmpty() && !endTime.isEmpty()) return startTime + " - " + endTime;
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
        return value != null ? String.valueOf(value) : "";
    }

    private String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) return first.trim();
        if (second != null && !second.trim().isEmpty()) return second.trim();
        return "";
    }

    private String fallback(String value) {
        return value != null && !value.trim().isEmpty() ? value.trim() : "—";
    }

    private void goBack() {
        if (!isAdded()) return;
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}
