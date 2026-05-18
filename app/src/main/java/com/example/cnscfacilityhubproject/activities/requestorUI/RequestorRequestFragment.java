package com.example.cnscfacilityhubproject.activities.requestorUI;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.models.FileUtils;
import com.example.cnscfacilityhubproject.models.LocalFileItem;
import com.example.cnscfacilityhubproject.models.ProposalFileItem;
import com.example.cnscfacilityhubproject.models.ScheduleDayItem;
import com.example.cnscfacilityhubproject.utils.RequestDataHelper;
import com.example.cnscfacilityhubproject.utils.RequestSubmissionHelper;
import com.example.cnscfacilityhubproject.utils.ScheduleConflictChecker;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
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

    private LinearLayout layoutSingleDayTimes;
    private LinearLayout layoutScheduleDays;
    private TextView tvScheduleHint;
    private LinearLayout layoutSelectedFiles;

    private MaterialButton btnUploadProposal;
    private MaterialButton btnSubmitRequest;
    private TextView tvSelectedFile;
    private ProgressBar progressSubmit;
    private TextView tvSubmitStatus;

    private String selectedActivityType = "Institutional";

    private final List<LocalFileItem> selectedFiles = new ArrayList<>();
    private final Map<String, TextInputEditText> perDayStartFields = new HashMap<>();
    private final Map<String, TextInputEditText> perDayEndFields = new HashMap<>();

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris == null || uris.isEmpty()) {
                    return;
                }

                for (Uri uri : uris) {
                    if (uri == null) continue;

                    try {
                        requireContext().getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (SecurityException ignored) {
                    }

                    String fileName = FileUtils.getFileName(requireContext(), uri);
                    String mimeType = requireContext().getContentResolver().getType(uri);
                    selectedFiles.add(new LocalFileItem(uri, fileName, mimeType));
                }

                refreshSelectedFilesUi();
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

        layoutSingleDayTimes = view.findViewById(R.id.layoutSingleDayTimes);
        layoutScheduleDays = view.findViewById(R.id.layoutScheduleDays);
        tvScheduleHint = view.findViewById(R.id.tvScheduleHint);
        layoutSelectedFiles = view.findViewById(R.id.layoutSelectedFiles);

        btnUploadProposal = view.findViewById(R.id.btnUploadProposal);
        btnSubmitRequest = view.findViewById(R.id.btnSubmitRequest);
        tvSelectedFile = view.findViewById(R.id.tvSelectedFile);
        progressSubmit = view.findViewById(R.id.progressSubmit);
        tvSubmitStatus = view.findViewById(R.id.tvSubmitStatus);
    }

    private void loadRequestorInformation() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "No logged in user found.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;

                    if (documentSnapshot.exists()) {
                        etRequestorName.setText(safe(documentSnapshot.getString("fullName")));
                        etContactNumber.setText(safe(documentSnapshot.getString("contactNum")));
                        etCollegeDepartment.setText(safe(documentSnapshot.getString("department")));
                        etOfficeCourse.setText(safe(documentSnapshot.getString("course")));

                        etRequestorName.setEnabled(false);
                        etContactNumber.setEnabled(false);
                        etCollegeDepartment.setEnabled(false);
                        etOfficeCourse.setEnabled(false);
                    }
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
        Chip[] facilityChips = {
                chipAmphitheater, chipCoveredCourt, chipEntrancePavilion,
                chipStudentCenter, chipOthersFacility
        };
        updateChipStyles(facilityChips);

        chipGroupFacility.setOnCheckedStateChangeListener((group, checkedIds) -> {
            toggleInput(etOtherFacility, chipOthersFacility.isChecked());
            if (chipOthersFacility.isChecked()) {
                etOtherFacility.requestFocus();
            }
            updateChipStyles(facilityChips);
        });
    }

    private void updateChipStyles(Chip... chips) {
        for (Chip chip : chips) {
            if (chip == null) continue;
            if (chip.isChecked()) {
                chip.setChipStrokeWidth(2f);
                chip.setChipStrokeColorResource(R.color.cnsc_primary);
                chip.setTextColor(requireContext().getColor(R.color.cnsc_primary));
            } else {
                chip.setChipStrokeWidth(1f);
                chip.setChipStrokeColorResource(R.color.cnsc_stroke);
                chip.setTextColor(requireContext().getColor(R.color.cnsc_text_primary));
            }
        }
    }

    private String getSelectedChipText(ChipGroup chipGroup) {
        int checkedId = chipGroup.getCheckedChipId();
        if (checkedId == View.NO_ID) return "";
        Chip chip = chipGroup.findViewById(checkedId);
        return chip != null ? chip.getText().toString().trim() : "";
    }

    private List<String> getSelectedFacilityNames() {
        List<String> names = new ArrayList<>();
        List<Integer> checkedIds = chipGroupFacility.getCheckedChipIds();

        for (int id : checkedIds) {
            Chip chip = chipGroupFacility.findViewById(id);
            if (chip == null) continue;

            String label = chip.getText().toString().trim();
            if ("Others".equalsIgnoreCase(label)) {
                String custom = getText(etOtherFacility);
                if (!custom.isEmpty()) {
                    names.add(custom);
                }
            } else if (!label.isEmpty()) {
                names.add(label);
            }
        }
        return names;
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
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate, true));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate, true));
    }

    private void showDatePicker(TextInputEditText target, boolean rebuildSchedule) {
        Calendar calendar = Calendar.getInstance();

        new DatePickerDialog(
                requireContext(),
                (datePicker, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    target.setText(formatDate(selected));
                    target.setError(null);
                    if (rebuildSchedule) {
                        rebuildScheduleDayCards();
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void setupTimePickers() {
        etTimeStart.setOnClickListener(v -> showTimePicker(etTimeStart));
        etTimeEnd.setOnClickListener(v -> showTimePicker(etTimeEnd));
    }

    private void showTimePicker(TextInputEditText target) {
        Calendar calendar = Calendar.getInstance();

        new TimePickerDialog(
                requireContext(),
                (timePicker, hourOfDay, minute) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selected.set(Calendar.MINUTE, minute);
                    target.setText(formatTime(selected));
                    target.setError(null);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        ).show();
    }

    private void showTimePickerForDay(TextInputEditText target) {
        showTimePicker(target);
    }

    private void rebuildScheduleDayCards() {
        perDayStartFields.clear();
        perDayEndFields.clear();
        layoutScheduleDays.removeAllViews();

        String startDateText = getText(etStartDate);
        String endDateText = getText(etEndDate);

        if (startDateText.isEmpty() || endDateText.isEmpty()) {
            layoutSingleDayTimes.setVisibility(View.VISIBLE);
            layoutScheduleDays.setVisibility(View.GONE);
            tvScheduleHint.setVisibility(View.GONE);
            return;
        }

        boolean multiDay = !startDateText.equalsIgnoreCase(endDateText);

        if (!multiDay) {
            layoutSingleDayTimes.setVisibility(View.VISIBLE);
            layoutScheduleDays.setVisibility(View.GONE);
            tvScheduleHint.setVisibility(View.GONE);
            return;
        }

        layoutSingleDayTimes.setVisibility(View.GONE);
        layoutScheduleDays.setVisibility(View.VISIBLE);
        tvScheduleHint.setVisibility(View.VISIBLE);

        long startMillis = RequestDataHelper.parseDateToMillis(startDateText);
        long endMillis = RequestDataHelper.parseDateToMillis(endDateText);
        if (startMillis == -1 || endMillis == -1) {
            return;
        }

        Calendar cursor = Calendar.getInstance();
        cursor.setTimeInMillis(startMillis);
        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(endMillis);

        while (!cursor.after(end)) {
            String dateText = formatDate(cursor);
            layoutScheduleDays.addView(createScheduleDayCard(dateText));
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private View createScheduleDayCard(String dateText) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(requireContext().getColor(R.color.cnsc_surface));
        card.setRadius(dp(16));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(requireContext().getColor(R.color.cnsc_stroke));

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(14), dp(14), dp(14), dp(14));

        TextView tvDate = new TextView(requireContext());
        tvDate.setText(dateText);
        tvDate.setTextColor(requireContext().getColor(R.color.cnsc_primary));
        tvDate.setTextSize(15f);
        tvDate.setTypeface(null, Typeface.BOLD);
        container.addView(tvDate);

        LinearLayout timeRow = new LinearLayout(requireContext());
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setPadding(0, dp(10), 0, 0);

        TextInputEditText etStart = new TextInputEditText(requireContext());
        etStart.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        etStart.setHint("Start time");
        setPickerField(etStart);
        etStart.setOnClickListener(v -> showTimePickerForDay(etStart));

        TextInputEditText etEnd = new TextInputEditText(requireContext());
        LinearLayout.LayoutParams endParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        endParams.setMarginStart(dp(10));
        etEnd.setLayoutParams(endParams);
        etEnd.setHint("End time");
        setPickerField(etEnd);
        etEnd.setOnClickListener(v -> showTimePickerForDay(etEnd));

        timeRow.addView(etStart);
        timeRow.addView(etEnd);
        container.addView(timeRow);
        card.addView(container);

        perDayStartFields.put(dateText, etStart);
        perDayEndFields.put(dateText, etEnd);

        return card;
    }

    private void setupFilePicker() {
        btnUploadProposal.setOnClickListener(v -> {
            try {
                filePickerLauncher.launch(new String[]{
                        "image/jpeg",
                        "image/png",
                        "application/pdf",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                });
            } catch (ActivityNotFoundException e) {
                Toast.makeText(requireContext(), "No file picker found.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshSelectedFilesUi() {
        layoutSelectedFiles.removeAllViews();

        if (selectedFiles.isEmpty()) {
            tvSelectedFile.setText("Accepted: PDF, DOCX, JPG, PNG (multiple files allowed)");
            return;
        }

        tvSelectedFile.setText(selectedFiles.size() + " file(s) selected");

        for (LocalFileItem file : new ArrayList<>(selectedFiles)) {

            MaterialCardView row = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = dp(6);
            row.setLayoutParams(params);
            row.setRadius(dp(12));
            row.setCardBackgroundColor(requireContext().getColor(R.color.white));
            row.setStrokeWidth(dp(1));
            row.setStrokeColor(requireContext().getColor(R.color.cnsc_stroke));

            LinearLayout inner = new LinearLayout(requireContext());
            inner.setOrientation(LinearLayout.HORIZONTAL);
            inner.setGravity(Gravity.CENTER_VERTICAL);
            inner.setPadding(dp(10), dp(10), dp(10), dp(10));

            TextView name = new TextView(requireContext());
            name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            name.setText(file.getFileName());
            name.setTextColor(requireContext().getColor(R.color.cnsc_text_primary));

            MaterialButton remove = new MaterialButton(requireContext());
            remove.setText("Remove");
            remove.setAllCaps(false);
            remove.setOnClickListener(v -> {
                selectedFiles.remove(file);
                refreshSelectedFilesUi();
            });

            inner.addView(name);
            inner.addView(remove);
            row.addView(inner);
            layoutSelectedFiles.addView(row);
        }
    }

    private void setupSubmit() {
        btnSubmitRequest.setOnClickListener(v -> {
            if (!validateForm()) return;
            if (auth.getCurrentUser() == null) {
                Toast.makeText(requireContext(), "No logged in user found.", Toast.LENGTH_SHORT).show();
                return;
            }
            checkConflictsAndSubmit();
        });
    }

    private List<ScheduleDayItem> buildScheduleDaysForSubmit() {
        String startDateText = getText(etStartDate);
        String endDateText = getText(etEndDate);
        boolean multiDay = !startDateText.equalsIgnoreCase(endDateText);

        Map<String, String> dayStarts = new HashMap<>();
        Map<String, String> dayEnds = new HashMap<>();

        if (multiDay) {
            for (String dateText : perDayStartFields.keySet()) {
                TextInputEditText startField = perDayStartFields.get(dateText);
                TextInputEditText endField = perDayEndFields.get(dateText);
                if (startField != null) {
                    dayStarts.put(dateText, getText(startField));
                }
                if (endField != null) {
                    dayEnds.put(dateText, getText(endField));
                }
            }
            return RequestDataHelper.buildScheduleDaysBetween(
                    startDateText, endDateText, "", "", dayEnds, dayStarts
            );
        }

        return RequestDataHelper.buildScheduleDaysBetween(
                startDateText,
                endDateText,
                getText(etTimeStart),
                getText(etTimeEnd),
                new HashMap<>(),
                new HashMap<>()
        );
    }

    private void checkConflictsAndSubmit() {
        List<String> facilityNames = getSelectedFacilityNames();
        List<String> facilityKeys = RequestDataHelper.buildFacilityKeys(facilityNames);
        List<ScheduleDayItem> scheduleDays = buildScheduleDaysForSubmit();

        setSubmitting(true, "Checking schedule availability...");

        db.collection("requests")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;

                    String conflict = (snapshot != null)
                            ? ScheduleConflictChecker.findConflictMessage(
                                facilityNames,
                                facilityKeys,
                                scheduleDays,
                                snapshot
                            ) : "";

                    if (!conflict.isEmpty()) {
                        setSubmitting(false, "");
                        Toast.makeText(requireContext(), conflict, Toast.LENGTH_LONG).show();
                        return;
                    }

                    createRequestAndUploadFiles(facilityNames, facilityKeys, scheduleDays);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    setSubmitting(false, "");
                    Toast.makeText(requireContext(),
                            "Failed to check availability: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void createRequestAndUploadFiles(
            List<String> facilityNames,
            List<String> facilityKeys,
            List<ScheduleDayItem> scheduleDays
    ) {
        setSubmitting(true, "Submitting request...");

        String userId = auth.getCurrentUser().getUid();
        String finalFacilityName = RequestDataHelper.buildFinalFacilityName(facilityNames);
        String facilityKeyLegacy = facilityKeys.isEmpty() ? "" : facilityKeys.get(0);
        String facilityLegacy = facilityNames.isEmpty() ? "" : facilityNames.get(0);
        String otherFacility = chipOthersFacility.isChecked() ? getText(etOtherFacility) : "";

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
                soundSystemSetup || microphones || portableSpeaker || lights
                        || livestreamingServices || zoomHosting || gmeetHosting
                        || webCamera || tripod || multimediaProjector
                        || !getText(etConnectors).isEmpty()
        );

        boolean needsSAC = RequestDataHelper.hasStudentCenter(facilityNames);

        boolean sendToSAC = false;
        boolean sendToITSO = false;
        boolean sendToGSO = false;
        String notificationTarget;
        String workflowStage;

        if (needsSAC) {
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

        String participantsCountText = getText(etNumberOfParticipants);
        if (!participantsCountText.isEmpty()) {
            numberOfParticipants = Long.parseLong(participantsCountText);
        }
        if (cbTables.isChecked() && !getText(etTablesCount).isEmpty()) {
            tablesCount = Long.parseLong(getText(etTablesCount));
        }
        if (cbChairs.isChecked() && !getText(etChairsCount).isEmpty()) {
            chairsCount = Long.parseLong(getText(etChairsCount));
        }

        Map<String, Object> requestMap = RequestSubmissionHelper.buildRequestMap(
                userId,
                selectedActivityType,
                getText(etRequestorName),
                getText(etContactNumber),
                getText(etCollegeDepartment),
                getText(etOfficeCourse),
                facilityNames,
                facilityKeys,
                facilityLegacy,
                otherFacility,
                finalFacilityName,
                facilityKeyLegacy,
                scheduleDays,
                getText(etParticipants),
                numberOfParticipants,
                getText(etPurpose),
                cbTables.isChecked(),
                tablesCount,
                cbChairs.isChecked(),
                chairsCount,
                getText(etOtherAmenities),
                cbAgreement.isChecked(),
                technicalNeeded,
                getText(etConnectors),
                soundSystemSetup,
                microphones,
                portableSpeaker,
                lights,
                livestreamingServices,
                zoomHosting,
                gmeetHosting,
                webCamera,
                tripod,
                multimediaProjector,
                needsSAC,
                needsITSO,
                sendToSAC,
                sendToITSO,
                sendToGSO,
                notificationTarget,
                workflowStage
        );

        db.collection("requests")
                .add(requestMap)
                .addOnSuccessListener(documentReference -> {
                    if (!isAdded()) return;

                    String requestId = documentReference.getId();
                    setSubmitting(true, "Uploading files...");

                    RequestSubmissionHelper.uploadProposalFiles(
                            requireContext(),
                            requestId,
                            selectedFiles,
                            new RequestSubmissionHelper.UploadCallback() {
                                @Override
                                public void onSuccess(List<ProposalFileItem> uploadedFiles) {
                                    RequestSubmissionHelper.attachUploadedFiles(
                                            requestId,
                                            uploadedFiles,
                                            () -> {
                                                if (!isAdded()) return;
                                                setSubmitting(false, "");
                                                showSuccessToast(needsSAC, needsITSO);
                                                clearForm();
                                            },
                                            message -> {
                                                if (!isAdded()) return;
                                                setSubmitting(false, "");
                                                Toast.makeText(requireContext(),
                                                        "Request saved but files failed: " + message,
                                                        Toast.LENGTH_LONG).show();
                                            }
                                    );
                                }

                                @Override
                                public void onFailure(String message) {
                                    if (!isAdded()) return;
                                    setSubmitting(false, "");
                                    Toast.makeText(requireContext(),
                                            "Request saved but upload failed: " + message,
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                    );
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    setSubmitting(false, "");
                    Toast.makeText(requireContext(),
                            "Failed to submit request: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showSuccessToast(boolean needsSAC, boolean needsITSO) {
        if (needsSAC && needsITSO) {
            Toast.makeText(requireContext(),
                    "Request submitted and sent to SAC. ITSO review follows SAC approval.",
                    Toast.LENGTH_LONG).show();
        } else if (needsSAC) {
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
    }

    private void setSubmitting(boolean submitting, String statusText) {
        btnSubmitRequest.setEnabled(!submitting);
        btnUploadProposal.setEnabled(!submitting);
        progressSubmit.setVisibility(submitting ? View.VISIBLE : View.GONE);

        if (statusText == null || statusText.isEmpty()) {
            tvSubmitStatus.setVisibility(View.GONE);
        } else {
            tvSubmitStatus.setVisibility(View.VISIBLE);
            tvSubmitStatus.setText(statusText);
        }
    }

    private boolean validateForm() {
        if (TextUtils.isEmpty(selectedActivityType)) {
            Toast.makeText(requireContext(), "Please select an activity type.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (isEmpty(etRequestorName) || isEmpty(etContactNumber)
                || isEmpty(etCollegeDepartment) || isEmpty(etOfficeCourse)) {
            Toast.makeText(requireContext(), "Requester information is incomplete.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (isEmpty(etStartDate) || isEmpty(etEndDate)) {
            Toast.makeText(requireContext(), "Please select start and end dates.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isValidDateRange()) {
            Toast.makeText(requireContext(), "End date cannot be earlier than start date.", Toast.LENGTH_SHORT).show();
            return false;
        }

        List<ScheduleDayItem> scheduleDays = buildScheduleDaysForSubmit();
        if (scheduleDays.isEmpty()) {
            Toast.makeText(requireContext(), "Please complete the schedule.", Toast.LENGTH_SHORT).show();
            return false;
        }

        for (ScheduleDayItem day : scheduleDays) {
            if (day.getStartTimeText().isEmpty() || day.getEndTimeText().isEmpty()) {
                Toast.makeText(requireContext(),
                        "Please set start and end time for " + day.getDateText() + ".",
                        Toast.LENGTH_SHORT).show();
                return false;
            }

            long start = RequestDataHelper.parseTimeToMillis(day.getStartTimeText());
            long end = RequestDataHelper.parseTimeToMillis(day.getEndTimeText());
            if (start == -1 || end == -1 || end <= start) {
                Toast.makeText(requireContext(),
                        "End time must be later than start time for " + day.getDateText() + ".",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        if (isEmpty(etParticipants) || isEmpty(etNumberOfParticipants) || isEmpty(etPurpose)) {
            Toast.makeText(requireContext(), "Please complete participants and purpose.", Toast.LENGTH_SHORT).show();
            return false;
        }

        List<String> facilities = getSelectedFacilityNames();
        if (facilities.isEmpty()) {
            Toast.makeText(requireContext(), "Please select at least one facility.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (chipOthersFacility.isChecked() && isEmpty(etOtherFacility)) {
            etOtherFacility.setError("Please specify the facility name");
            etOtherFacility.requestFocus();
            return false;
        }

        if (cbNeedsTechnical.isChecked() && !hasSelectedTechnicalOption()) {
            Toast.makeText(requireContext(), "Please select at least one technical option.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (cbTables.isChecked() && isEmpty(etTablesCount)) {
            etTablesCount.setError("Required");
            return false;
        }

        if (cbChairs.isChecked() && isEmpty(etChairsCount)) {
            etChairsCount.setError("Required");
            return false;
        }

        if (selectedFiles.isEmpty()) {
            Toast.makeText(requireContext(), "Please upload at least one proposal file.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!cbAgreement.isChecked()) {
            Toast.makeText(requireContext(), "Please confirm the agreement checkbox.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean isValidDateRange() {
        long start = RequestDataHelper.parseDateToMillis(getText(etStartDate));
        long end = RequestDataHelper.parseDateToMillis(getText(etEndDate));
        return start != -1 && end != -1 && end >= start;
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

        selectedFiles.clear();
        refreshSelectedFilesUi();
        rebuildScheduleDayCards();

        toggleInput(etOtherFacility, false);
        toggleInput(etTablesCount, false);
        toggleInput(etChairsCount, false);

        loadRequestorInformation();
    }

    private boolean isEmpty(TextInputEditText editText) {
        return editText.getText() == null || TextUtils.isEmpty(editText.getText().toString().trim());
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String formatDate(Calendar calendar) {
        return new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(calendar.getTime());
    }

    private String formatTime(Calendar calendar) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.getTime());
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
