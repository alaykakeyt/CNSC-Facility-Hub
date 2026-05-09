package com.example.cnscfacilityhubproject.activities.requestorUI;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.models.FileUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RequestorRequestFragment extends Fragment {

    private TextInputEditText etStartDate;
    private TextInputEditText etEndDate;
    private TextInputEditText etTimeStart;
    private TextInputEditText etTimeEnd;
    private TextInputEditText etRequestorName;
    private TextInputEditText etContactNumber;
    private TextInputEditText etCollegeDepartment;
    private TextInputEditText etOfficeCourse;
    private TextInputEditText etParticipants;
    private TextInputEditText etNumberOfParticipants;
    private TextInputEditText etPurpose;
    private TextInputEditText etOtherFacility;
    private TextInputEditText etConnectors;
    private TextInputEditText etTablesCount;
    private TextInputEditText etChairsCount;
    private TextInputEditText etOtherAmenities;

    private MaterialCheckBox cbTables;
    private MaterialCheckBox cbChairs;
    private MaterialCheckBox cbAgreement;

    private MaterialCheckBox cbNeedsTechnical;
    private LinearLayout layoutTechnicalOptions;

    private MaterialCheckBox cbSoundSystem;
    private MaterialCheckBox cbMicrophones;
    private MaterialCheckBox cbPortableSpeaker;
    private MaterialCheckBox cbLights;
    private MaterialCheckBox cbLivestreaming;
    private MaterialCheckBox cbZoom;
    private MaterialCheckBox cbGmeet;
    private MaterialCheckBox cbWebcam;
    private MaterialCheckBox cbTripod;
    private MaterialCheckBox cbProjector;

    private ChipGroup chipGroupActivityType;
    private Chip chipInstitutional;
    private Chip chipLocal;
    private Chip chipExternal;

    private ChipGroup chipGroupFacility;
    private Chip chipAmphitheater;
    private Chip chipCoveredCourt;
    private Chip chipEntrancePavilion;
    private Chip chipStudentCenter;
    private Chip chipOthersFacility;

    private MaterialButton btnUploadProposal;
    private MaterialButton btnSubmitRequest;
    private TextView tvSelectedFile;

    private Uri selectedFileUri;

    private String selectedActivityType = "Institutional";
    private String selectedFacility = "";

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    selectedFileUri = uri;

                    try {
                        requireContext().getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (SecurityException ignored) {
                    }

                    String fileName = FileUtils.getFileName(requireContext(), uri);
                    if (tvSelectedFile != null) {
                        tvSelectedFile.setText(fileName != null ? fileName : "1 file selected");
                    }
                }
            });

    public RequestorRequestFragment() {
        super(R.layout.fragment_requestor_request);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews(view);
        setupReadOnlyPickers();
        setupActivityTypeChips();
        setupFacilityChips();
        setupDatePickers();
        setupTimePickers();
        setupAmenitiesInputs();
        setupTechnicalOptions();
        setupFilePicker();
        setupSubmit();
        loadRequestorInformation();
    }

    private void bindViews(View view) {
        etStartDate = view.findViewById(R.id.etStartDate);
        etEndDate = view.findViewById(R.id.etEndDate);
        etTimeStart = view.findViewById(R.id.etTimeStart);
        etTimeEnd = view.findViewById(R.id.etTimeEnd);
        etRequestorName = view.findViewById(R.id.etRequestorName);
        etContactNumber = view.findViewById(R.id.etContactNumber);
        etCollegeDepartment = view.findViewById(R.id.etCollegeDepartment);
        etOfficeCourse = view.findViewById(R.id.etOfficeCourse);
        etParticipants = view.findViewById(R.id.etParticipants);
        etNumberOfParticipants = view.findViewById(R.id.etNumberOfParticipants);
        etPurpose = view.findViewById(R.id.etPurpose);
        etOtherFacility = view.findViewById(R.id.etOtherFacility);
        etConnectors = view.findViewById(R.id.etConnectors);
        etTablesCount = view.findViewById(R.id.etTablesCount);
        etChairsCount = view.findViewById(R.id.etChairsCount);
        etOtherAmenities = view.findViewById(R.id.etOtherAmenities);

        cbTables = view.findViewById(R.id.cbTables);
        cbChairs = view.findViewById(R.id.cbChairs);
        cbAgreement = view.findViewById(R.id.cbAgreement);

        cbNeedsTechnical = view.findViewById(R.id.cbNeedsTechnical);
        layoutTechnicalOptions = view.findViewById(R.id.layoutTechnicalOptions);

        cbSoundSystem = view.findViewById(R.id.cbSoundSystem);
        cbMicrophones = view.findViewById(R.id.cbMicrophones);
        cbPortableSpeaker = view.findViewById(R.id.cbPortableSpeaker);
        cbLights = view.findViewById(R.id.cbLights);
        cbLivestreaming = view.findViewById(R.id.cbLivestreaming);
        cbZoom = view.findViewById(R.id.cbZoom);
        cbGmeet = view.findViewById(R.id.cbGmeet);
        cbWebcam = view.findViewById(R.id.cbWebcam);
        cbTripod = view.findViewById(R.id.cbTripod);
        cbProjector = view.findViewById(R.id.cbProjector);

        chipGroupActivityType = view.findViewById(R.id.chipGroupActivityType);
        chipInstitutional = view.findViewById(R.id.chipInstitutional);
        chipLocal = view.findViewById(R.id.chipLocal);
        chipExternal = view.findViewById(R.id.chipExternal);

        chipGroupFacility = view.findViewById(R.id.chipGroupFacility);
        chipAmphitheater = view.findViewById(R.id.chipAmphitheater);
        chipCoveredCourt = view.findViewById(R.id.chipCoveredCourt);
        chipEntrancePavilion = view.findViewById(R.id.chipEntrancePavilion);
        chipStudentCenter = view.findViewById(R.id.chipStudentCenter);
        chipOthersFacility = view.findViewById(R.id.chipOthersFacility);

        btnUploadProposal = view.findViewById(R.id.btnUploadProposal);
        btnSubmitRequest = view.findViewById(R.id.btnSubmitRequest);
        tvSelectedFile = view.findViewById(R.id.tvSelectedFile);
    }

    private void loadRequestorInformation() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "No logged in user found.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;

                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        String contactNum = documentSnapshot.getString("contactNum");
                        String department = documentSnapshot.getString("department");
                        String course = documentSnapshot.getString("course");

                        etRequestorName.setText(fullName != null ? fullName : "");
                        etContactNumber.setText(contactNum != null ? contactNum : "");
                        etCollegeDepartment.setText(department != null ? department : "");
                        etOfficeCourse.setText(course != null ? course : "");

                        etRequestorName.setEnabled(false);
                        etContactNumber.setEnabled(false);
                        etCollegeDepartment.setEnabled(false);
                        etOfficeCourse.setEnabled(false);
                    } else {
                        Toast.makeText(requireContext(), "User info not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to load requestor info: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setupReadOnlyPickers() {
        setPickerField(etStartDate);
        setPickerField(etEndDate);
        setPickerField(etTimeStart);
        setPickerField(etTimeEnd);
    }

    private void setPickerField(TextInputEditText editText) {
        editText.setFocusable(false);
        editText.setClickable(true);
        editText.setLongClickable(false);
        editText.setCursorVisible(false);
        editText.setKeyListener(null);
    }

    private void setupActivityTypeChips() {
        updateChipStyles(chipInstitutional, chipLocal, chipExternal);

        chipGroupActivityType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            selectedActivityType = getSelectedChipText(group);
            updateChipStyles(chipInstitutional, chipLocal, chipExternal);
        });
    }

    private void setupFacilityChips() {
        toggleInput(etOtherFacility, false);

        updateChipStyles(
                chipAmphitheater,
                chipCoveredCourt,
                chipEntrancePavilion,
                chipStudentCenter,
                chipOthersFacility
        );

        chipGroupFacility.setOnCheckedStateChangeListener((group, checkedIds) -> {
            selectedFacility = getSelectedChipText(group);
            boolean isOthersSelected = chipOthersFacility.isChecked();

            toggleInput(etOtherFacility, isOthersSelected);
            if (isOthersSelected) etOtherFacility.requestFocus();

            updateChipStyles(
                    chipAmphitheater,
                    chipCoveredCourt,
                    chipEntrancePavilion,
                    chipStudentCenter,
                    chipOthersFacility
            );
        });
    }

    private void updateChipStyles(Chip... chips) {
        for (Chip chip : chips) {
            if (chip == null) continue;

            if (chip.isChecked()) {
                chip.setChipBackgroundColorResource(android.R.color.white);
                chip.setTextColor(requireContext().getColor(android.R.color.holo_red_dark));
                chip.setChipStrokeColorResource(android.R.color.holo_red_dark);
                chip.setChipStrokeWidth(2f);
            } else {
                chip.setChipBackgroundColorResource(android.R.color.white);
                chip.setTextColor(requireContext().getColor(android.R.color.black));
                chip.setChipStrokeColorResource(android.R.color.darker_gray);
                chip.setChipStrokeWidth(1f);
            }
        }
    }

    private String getSelectedChipText(ChipGroup chipGroup) {
        int checkedId = chipGroup.getCheckedChipId();
        if (checkedId == View.NO_ID) return "";

        Chip chip = chipGroup.findViewById(checkedId);
        return chip != null ? chip.getText().toString().trim() : "";
    }

    private void setupAmenitiesInputs() {
        toggleInput(etTablesCount, false);
        toggleInput(etChairsCount, false);

        cbTables.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleInput(etTablesCount, isChecked);
            if (isChecked) etTablesCount.requestFocus();
        });

        cbChairs.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleInput(etChairsCount, isChecked);
            if (isChecked) etChairsCount.requestFocus();
        });
    }

    private void setupTechnicalOptions() {
        layoutTechnicalOptions.setVisibility(View.GONE);
        clearTechnicalOptions();

        cbNeedsTechnical.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutTechnicalOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) clearTechnicalOptions();
        });
    }

    private void clearTechnicalOptions() {
        cbSoundSystem.setChecked(false);
        cbMicrophones.setChecked(false);
        cbPortableSpeaker.setChecked(false);
        cbLights.setChecked(false);
        cbLivestreaming.setChecked(false);
        cbZoom.setChecked(false);
        cbGmeet.setChecked(false);
        cbWebcam.setChecked(false);
        cbTripod.setChecked(false);
        cbProjector.setChecked(false);

        etConnectors.setText("");
        etConnectors.setError(null);
    }

    private boolean hasSelectedTechnicalOption() {
        return cbSoundSystem.isChecked()
                || cbMicrophones.isChecked()
                || cbPortableSpeaker.isChecked()
                || cbLights.isChecked()
                || cbLivestreaming.isChecked()
                || cbZoom.isChecked()
                || cbGmeet.isChecked()
                || cbWebcam.isChecked()
                || cbTripod.isChecked()
                || cbProjector.isChecked()
                || !getText(etConnectors).isEmpty();
    }

    private void toggleInput(TextInputEditText editText, boolean enabled) {
        editText.setEnabled(enabled);
        editText.setFocusable(enabled);
        editText.setFocusableInTouchMode(enabled);
        editText.setClickable(enabled);
        editText.setCursorVisible(enabled);

        if (!enabled) {
            editText.setText("");
            editText.setError(null);
        }
    }

    private void setupDatePickers() {
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
    }

    private void showDatePicker(TextInputEditText target) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (datePicker, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);

                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                    target.setText(sdf.format(selected.getTime()));
                    target.setError(null);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void setupTimePickers() {
        etTimeStart.setOnClickListener(v -> showTimePicker(etTimeStart));
        etTimeEnd.setOnClickListener(v -> showTimePicker(etTimeEnd));
    }

    private void showTimePicker(TextInputEditText target) {
        Calendar calendar = Calendar.getInstance();

        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (timePicker, hourOfDay, minute) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selected.set(Calendar.MINUTE, minute);

                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    target.setText(sdf.format(selected.getTime()));
                    target.setError(null);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        );

        dialog.show();
    }

    private void setupFilePicker() {
        btnUploadProposal.setOnClickListener(v -> {
            try {
                filePickerLauncher.launch(new String[]{
                        "application/pdf",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "image/*"
                });
            } catch (ActivityNotFoundException e) {
                Toast.makeText(requireContext(), "No file picker found.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSubmit() {
        btnSubmitRequest.setOnClickListener(v -> {
            if (!validateForm()) return;

            if (auth.getCurrentUser() == null) {
                Toast.makeText(requireContext(), "No logged in user found.", Toast.LENGTH_SHORT).show();
                return;
            }

            checkPendingOrApprovedFacilityDateTimeAndSubmit();
        });
    }

    private void saveRequestToFirestore() {
        String userId = auth.getCurrentUser().getUid();

        String activityType = getSafeText(selectedActivityType);
        String startDateText = getText(etStartDate);
        String endDateText = getText(etEndDate);
        String timeStartText = getText(etTimeStart);
        String timeEndText = getText(etTimeEnd);
        String participants = getText(etParticipants);
        String numberOfParticipantsText = getText(etNumberOfParticipants);
        String purpose = getText(etPurpose);
        String facility = selectedFacility;
        String otherFacility = getText(etOtherFacility);
        String finalFacilityName = getFinalFacilityName(facility, otherFacility);
        String facilityKey = normalizeFacilityName(finalFacilityName);
        String connectors = getText(etConnectors);
        String otherAmenities = getText(etOtherAmenities);

        boolean tablesRequested = cbTables.isChecked();
        boolean chairsRequested = cbChairs.isChecked();
        boolean agreementAccepted = cbAgreement.isChecked();

        boolean technicalNeeded = cbNeedsTechnical.isChecked();

        boolean soundSystemSetup = technicalNeeded && cbSoundSystem.isChecked();
        boolean microphones = technicalNeeded && cbMicrophones.isChecked();
        boolean portableSpeaker = technicalNeeded && cbPortableSpeaker.isChecked();
        boolean lights = technicalNeeded && cbLights.isChecked();
        boolean livestreamingServices = technicalNeeded && cbLivestreaming.isChecked();
        boolean zoomHosting = technicalNeeded && cbZoom.isChecked();
        boolean gmeetHosting = technicalNeeded && cbGmeet.isChecked();
        boolean webCamera = technicalNeeded && cbWebcam.isChecked();
        boolean tripod = technicalNeeded && cbTripod.isChecked();
        boolean multimediaProjector = technicalNeeded && cbProjector.isChecked();

        boolean needsITSO = technicalNeeded && (
                soundSystemSetup
                        || microphones
                        || portableSpeaker
                        || lights
                        || livestreamingServices
                        || zoomHosting
                        || gmeetHosting
                        || webCamera
                        || tripod
                        || multimediaProjector
                        || !connectors.isEmpty()
        );

        boolean isStudentCenter = "Student Center".equalsIgnoreCase(finalFacilityName);
        boolean needsSAC = isStudentCenter;

        String notificationTarget;
        String workflowStage;

        boolean sendToSAC = false;
        boolean sendToITSO = false;
        boolean sendToGSO = false;

        if (isStudentCenter) {
            notificationTarget = "SAC";
            workflowStage = "SAC_REVIEW";
            sendToSAC = true;
        } else if (needsITSO) {
            notificationTarget = "ITSO";
            workflowStage = "ITSO_REVIEW";
            sendToITSO = true;
        } else {
            notificationTarget = "GSO";
            workflowStage = "GSO_REVIEW";
            sendToGSO = true;
        }

        long numberOfParticipants = 0;
        long tablesCount = 0;
        long chairsCount = 0;

        if (!numberOfParticipantsText.isEmpty()) {
            numberOfParticipants = Long.parseLong(numberOfParticipantsText);
        }

        if (tablesRequested && !getText(etTablesCount).isEmpty()) {
            tablesCount = Long.parseLong(getText(etTablesCount));
        }

        if (chairsRequested && !getText(etChairsCount).isEmpty()) {
            chairsCount = Long.parseLong(getText(etChairsCount));
        }

        Timestamp startDateTimestamp = parseDateToTimestamp(startDateText);
        Timestamp endDateTimestamp = parseDateToTimestamp(endDateText);

        String proposalFileName = "";
        String proposalFileUrl = "";

        if (selectedFileUri != null) {
            String fileName = FileUtils.getFileName(requireContext(), selectedFileUri);
            proposalFileName = fileName != null ? fileName : "";
            proposalFileUrl = selectedFileUri.toString();
        }

        Map<String, Object> requestMap = new HashMap<>();

        requestMap.put("userId", userId);
        requestMap.put("activityType", activityType);

        requestMap.put("requestorName", getText(etRequestorName));
        requestMap.put("contactNumber", getText(etContactNumber));
        requestMap.put("collegeDepartment", getText(etCollegeDepartment));
        requestMap.put("officeCourse", getText(etOfficeCourse));

        requestMap.put("startDate", startDateTimestamp);
        requestMap.put("endDate", endDateTimestamp);
        requestMap.put("startDateText", startDateText);
        requestMap.put("endDateText", endDateText);
        requestMap.put("timeStartText", timeStartText);
        requestMap.put("timeEndText", timeEndText);
        requestMap.put("participants", participants);
        requestMap.put("numberOfParticipants", numberOfParticipants);
        requestMap.put("purpose", purpose);

        requestMap.put("facility", facility);
        requestMap.put("otherFacility", otherFacility);
        requestMap.put("finalFacilityName", finalFacilityName);
        requestMap.put("facilityKey", facilityKey);

        requestMap.put("tablesRequested", tablesRequested);
        requestMap.put("tablesCount", tablesCount);
        requestMap.put("chairsRequested", chairsRequested);
        requestMap.put("chairsCount", chairsCount);
        requestMap.put("otherAmenities", otherAmenities);

        requestMap.put("proposalFileUrl", proposalFileUrl);
        requestMap.put("proposalFileName", proposalFileName);
        requestMap.put("agreementAccepted", agreementAccepted);

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

        btnSubmitRequest.setEnabled(false);

        db.collection("requests")
                .add(requestMap)
                .addOnSuccessListener(documentReference -> {
                    if (!isAdded()) return;

                    btnSubmitRequest.setEnabled(true);

                    if (isStudentCenter && needsITSO) {
                        Toast.makeText(requireContext(),
                                "Request submitted successfully and sent to SAC. It will go to ITSO after SAC approval.",
                                Toast.LENGTH_LONG).show();
                    } else if (isStudentCenter) {
                        Toast.makeText(requireContext(),
                                "Request submitted successfully and sent to SAC.",
                                Toast.LENGTH_LONG).show();
                    } else if (needsITSO) {
                        Toast.makeText(requireContext(),
                                "Request submitted successfully and sent to ITSO.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(),
                                "Request submitted successfully and sent to GSO.",
                                Toast.LENGTH_LONG).show();
                    }

                    clearForm();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;

                    btnSubmitRequest.setEnabled(true);
                    Toast.makeText(requireContext(),
                            "Failed to submit request: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateForm() {
        if (TextUtils.isEmpty(selectedActivityType)) {
            Toast.makeText(requireContext(), "Please select an activity type.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (isEmpty(etRequestorName)) {
            etRequestorName.setError("Required");
            etRequestorName.requestFocus();
            return false;
        }

        if (isEmpty(etContactNumber)) {
            etContactNumber.setError("Required");
            etContactNumber.requestFocus();
            return false;
        }

        if (isEmpty(etCollegeDepartment)) {
            etCollegeDepartment.setError("Required");
            etCollegeDepartment.requestFocus();
            return false;
        }

        if (isEmpty(etOfficeCourse)) {
            etOfficeCourse.setError("Required");
            etOfficeCourse.requestFocus();
            return false;
        }

        if (isEmpty(etStartDate)) {
            etStartDate.setError("Required");
            etStartDate.requestFocus();
            return false;
        }

        if (isEmpty(etEndDate)) {
            etEndDate.setError("Required");
            etEndDate.requestFocus();
            return false;
        }

        if (isEmpty(etTimeStart)) {
            etTimeStart.setError("Required");
            etTimeStart.requestFocus();
            return false;
        }

        if (isEmpty(etTimeEnd)) {
            etTimeEnd.setError("Required");
            etTimeEnd.requestFocus();
            return false;
        }

        if (!isValidDateRange()) {
            Toast.makeText(requireContext(), "End date cannot be earlier than start date.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isValidTimeRange()) {
            Toast.makeText(requireContext(), "End time must be later than start time.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (isEmpty(etParticipants)) {
            etParticipants.setError("Required");
            etParticipants.requestFocus();
            return false;
        }

        if (isEmpty(etNumberOfParticipants)) {
            etNumberOfParticipants.setError("Required");
            etNumberOfParticipants.requestFocus();
            return false;
        }

        if (isEmpty(etPurpose)) {
            etPurpose.setError("Required");
            etPurpose.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(selectedFacility)) {
            Toast.makeText(requireContext(), "Please select a facility.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (chipOthersFacility.isChecked() && isEmpty(etOtherFacility)) {
            etOtherFacility.setError("Please specify other facility");
            etOtherFacility.requestFocus();
            return false;
        }

        if (cbNeedsTechnical.isChecked() && !hasSelectedTechnicalOption()) {
            Toast.makeText(requireContext(), "Please select at least one technical option.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (cbTables.isChecked() && isEmpty(etTablesCount)) {
            etTablesCount.setError("Required");
            etTablesCount.requestFocus();
            return false;
        }

        if (cbChairs.isChecked() && isEmpty(etChairsCount)) {
            etChairsCount.setError("Required");
            etChairsCount.requestFocus();
            return false;
        }

        if (selectedFileUri == null) {
            Toast.makeText(requireContext(), "Please upload the proposal file.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!cbAgreement.isChecked()) {
            Toast.makeText(requireContext(), "Please confirm the agreement checkbox.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean isValidDateRange() {
        long start = parseDateToMillis(getText(etStartDate));
        long end = parseDateToMillis(getText(etEndDate));

        if (start == -1 || end == -1) return false;
        return end >= start;
    }

    private boolean isValidTimeRange() {
        long start = parseTimeToMillis(getText(etTimeStart));
        long end = parseTimeToMillis(getText(etTimeEnd));

        if (start == -1 || end == -1) return false;
        return end > start;
    }

    private boolean isEmpty(TextInputEditText editText) {
        return editText.getText() == null || TextUtils.isEmpty(editText.getText().toString().trim());
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String getSafeText(String value) {
        return value != null ? value : "";
    }

    private Timestamp parseDateToTimestamp(String dateText) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            java.util.Date date = sdf.parse(dateText);
            if (date != null) return new Timestamp(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Timestamp.now();
    }

    private long parseDateToMillis(String dateText) {
        try {
            if (dateText == null || dateText.trim().isEmpty()) return -1;
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            java.util.Date date = sdf.parse(dateText.trim());
            if (date != null) return date.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private long parseTimeToMillis(String timeText) {
        try {
            if (timeText == null || timeText.trim().isEmpty()) return -1;
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            java.util.Date date = sdf.parse(timeText.trim());
            if (date != null) return date.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private boolean isDateRangeOverlapping(String newStartDate, String newEndDate, String existingStartDate, String existingEndDate) {
        long newStart = parseDateToMillis(newStartDate);
        long newEnd = parseDateToMillis(newEndDate);
        long existingStart = parseDateToMillis(existingStartDate);
        long existingEnd = parseDateToMillis(existingEndDate);

        if (newStart == -1 || newEnd == -1 || existingStart == -1) return false;
        if (existingEnd == -1) existingEnd = existingStart;

        return newStart <= existingEnd && newEnd >= existingStart;
    }

    private boolean isTimeOverlapping(String newStartTime, String newEndTime, String existingStartTime, String existingEndTime) {
        long newStart = parseTimeToMillis(newStartTime);
        long newEnd = parseTimeToMillis(newEndTime);
        long existingStart = parseTimeToMillis(existingStartTime);
        long existingEnd = parseTimeToMillis(existingEndTime);

        if (newStart == -1 || newEnd == -1 || existingStart == -1 || existingEnd == -1) return false;
        return newStart < existingEnd && newEnd > existingStart;
    }

    private String getFinalFacilityName(String facility, String otherFacility) {
        if ("Others".equalsIgnoreCase(facility) && otherFacility != null && !otherFacility.trim().isEmpty()) {
            return otherFacility.trim();
        }
        return facility != null ? facility.trim() : "";
    }

    private String getExistingFinalFacilityName(QueryDocumentSnapshot doc) {
        String finalFacilityName = doc.getString("finalFacilityName");

        if (finalFacilityName != null && !finalFacilityName.trim().isEmpty()) {
            return finalFacilityName.trim();
        }

        String facility = doc.getString("facility");
        String otherFacility = doc.getString("otherFacility");

        if ("Others".equalsIgnoreCase(facility) && otherFacility != null && !otherFacility.trim().isEmpty()) {
            return otherFacility.trim();
        }

        return facility != null ? facility.trim() : "";
    }

    private String normalizeFacilityName(String facilityName) {
        if (facilityName == null) return "";
        return facilityName.trim().toLowerCase(Locale.getDefault());
    }

    private String buildConflictMessage(String status, String facilityName, String startDate, String endDate, String startTime, String endTime) {
        StringBuilder message = new StringBuilder();
        message.append("This facility is taken ");

        if (startDate != null && !startDate.trim().isEmpty()) {
            if (endDate != null && !endDate.trim().isEmpty() && !startDate.trim().equalsIgnoreCase(endDate.trim())) {
                message.append("from ").append(startDate.trim()).append(" to ").append(endDate.trim());
            } else {
                message.append("on ").append(startDate.trim());
            }
        } else {
            message.append("on the selected date");
        }

        if (startTime != null && !startTime.trim().isEmpty() && endTime != null && !endTime.trim().isEmpty()) {
            message.append(" from ").append(startTime.trim()).append(" to ").append(endTime.trim());
        }

        if ("Pending".equalsIgnoreCase(status)) {
            message.append(". This schedule is still pending approval.");
        } else if ("Approved".equalsIgnoreCase(status)) {
            message.append(". This schedule is already approved.");
        } else {
            message.append(".");
        }

        return message.toString();
    }

    private void clearForm() {
        etStartDate.setText("");
        etEndDate.setText("");
        etTimeStart.setText("");
        etTimeEnd.setText("");
        etParticipants.setText("");
        etNumberOfParticipants.setText("");
        etPurpose.setText("");
        etOtherFacility.setText("");
        etConnectors.setText("");
        etTablesCount.setText("");
        etChairsCount.setText("");
        etOtherAmenities.setText("");

        cbTables.setChecked(false);
        cbChairs.setChecked(false);
        cbAgreement.setChecked(false);

        cbNeedsTechnical.setChecked(false);
        layoutTechnicalOptions.setVisibility(View.GONE);
        clearTechnicalOptions();

        chipInstitutional.setChecked(true);
        selectedActivityType = "Institutional";

        chipGroupFacility.clearCheck();
        selectedFacility = "";

        selectedFileUri = null;

        if (tvSelectedFile != null) {
            tvSelectedFile.setText("Accepted: PDF, DOCX, JPG, PNG");
        }

        toggleInput(etOtherFacility, false);
        toggleInput(etTablesCount, false);
        toggleInput(etChairsCount, false);

        loadRequestorInformation();
    }

    private void checkPendingOrApprovedFacilityDateTimeAndSubmit() {
        String requestedStartDate = getText(etStartDate);
        String requestedEndDate = getText(etEndDate);
        String requestedStartTime = getText(etTimeStart);
        String requestedEndTime = getText(etTimeEnd);

        String requestedFacility = getFinalFacilityName(selectedFacility, getText(etOtherFacility));
        String requestedFacilityKey = normalizeFacilityName(requestedFacility);

        btnSubmitRequest.setEnabled(false);

        db.collection("requests")
                .whereIn("status", java.util.Arrays.asList("Pending", "Approved"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    String conflictMessage = "";

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String existingFacility = getExistingFinalFacilityName(doc);
                        String existingFacilityKey = normalizeFacilityName(existingFacility);

                        if (!requestedFacilityKey.equals(existingFacilityKey)) continue;

                        String existingStatus = doc.getString("status");
                        String existingStartDate = doc.getString("startDateText");
                        String existingEndDate = doc.getString("endDateText");
                        String existingStartTime = doc.getString("timeStartText");
                        String existingEndTime = doc.getString("timeEndText");

                        boolean dateConflict = isDateRangeOverlapping(
                                requestedStartDate,
                                requestedEndDate,
                                existingStartDate,
                                existingEndDate
                        );

                        boolean timeConflict = isTimeOverlapping(
                                requestedStartTime,
                                requestedEndTime,
                                existingStartTime,
                                existingEndTime
                        );

                        if (dateConflict && timeConflict) {
                            conflictMessage = buildConflictMessage(
                                    existingStatus,
                                    existingFacility,
                                    existingStartDate,
                                    existingEndDate,
                                    existingStartTime,
                                    existingEndTime
                            );
                            break;
                        }
                    }

                    if (!conflictMessage.isEmpty()) {
                        btnSubmitRequest.setEnabled(true);
                        Toast.makeText(requireContext(), conflictMessage, Toast.LENGTH_LONG).show();
                    } else {
                        saveRequestToFirestore();
                    }
                })
                .addOnFailureListener(e -> {
                    btnSubmitRequest.setEnabled(true);
                    Toast.makeText(requireContext(),
                            "Failed to check facility availability: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
