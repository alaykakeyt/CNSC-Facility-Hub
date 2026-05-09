package com.example.cnscfacilityhubproject.activities.gsoUI;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cnscfacilityhubproject.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class gsoEditUserActivity extends AppCompatActivity {

    private ImageView ivBack;

    private AutoCompleteTextView actvUserType;
    private AutoCompleteTextView actvStatus;

    private LinearLayout layoutStaffFields;
    private LinearLayout layoutRequestorFields;

    private TextInputEditText etFullName;
    private TextInputEditText etEmail;
    private TextInputEditText etContact;
    private TextInputEditText etOfficeUnit;
    private TextInputEditText etDepartmentAgency;
    private TextInputEditText etOfficeCourse;

    private MaterialButton btnCancel;
    private MaterialButton btnSaveChanges;
    private MaterialButton btnDeleteUser;

    private FirebaseFirestore db;

    private String userId = "";
    private String originalEmail = "";
    private String currentFullName = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gso_edit_user);

        db = FirebaseFirestore.getInstance();

        userId = getIntent().getStringExtra("userId");

        bindViews();
        setupDropdowns();
        setupActions();

        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "User ID not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadUserDetails();
    }

    private void bindViews() {
        ivBack = findViewById(R.id.ivBack);

        actvUserType = findViewById(R.id.actvUserType);
        actvStatus = findViewById(R.id.actvStatus);

        layoutStaffFields = findViewById(R.id.layoutStaffFields);
        layoutRequestorFields = findViewById(R.id.layoutRequestorFields);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etContact = findViewById(R.id.etContact);
        etOfficeUnit = findViewById(R.id.etOfficeUnit);
        etDepartmentAgency = findViewById(R.id.etDepartmentAgency);
        etOfficeCourse = findViewById(R.id.etOfficeCourse);

        btnCancel = findViewById(R.id.btnCancel);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnDeleteUser = findViewById(R.id.btnDeleteUser);
    }

    private void setupDropdowns() {
        String[] userTypes = {"GSO", "ITSO", "SAC", "Requestor"};
        String[] statuses = {"Active", "Inactive", "Disabled"};

        ArrayAdapter<String> userTypeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                userTypes
        );

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                statuses
        );

        actvUserType.setAdapter(userTypeAdapter);
        actvStatus.setAdapter(statusAdapter);

        actvUserType.setOnItemClickListener((parent, view, position, id) ->
                updateFormByRole(userTypes[position])
        );
    }

    private void setupActions() {
        ivBack.setOnClickListener(v -> finish());

        btnCancel.setOnClickListener(v -> finish());

        btnSaveChanges.setOnClickListener(v -> {
            if (validateForm()) {
                updateUser();
            }
        });

        btnDeleteUser.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void loadUserDetails() {
        setLoading(true);

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    setLoading(false);

                    if (!doc.exists()) {
                        Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    displayUserDetails(doc);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to load user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void displayUserDetails(DocumentSnapshot doc) {
        String role = getUserRole(doc);
        String status = getStringValue(doc, "status");

        if (status.isEmpty()) {
            status = "Active";
        }

        originalEmail = getStringValue(doc, "email");
        currentFullName = getStringValue(doc, "fullName");

        String contact = firstNonEmpty(
                getStringValue(doc, "contactNum"),
                getStringValue(doc, "contactNumber")
        );

        String department = firstNonEmpty(
                getStringValue(doc, "department"),
                getStringValue(doc, "collegeDepartment")
        );

        String officeCourse = firstNonEmpty(
                getStringValue(doc, "officeCourse"),
                getStringValue(doc, "course")
        );

        String officeUnit = firstNonEmpty(
                getStringValue(doc, "officeUnit"),
                getStringValue(doc, "office")
        );

        etFullName.setText(currentFullName);
        etEmail.setText(originalEmail);
        etContact.setText(contact);

        actvUserType.setText(role, false);
        actvStatus.setText(status, false);

        if ("Requestor".equalsIgnoreCase(role)) {
            etDepartmentAgency.setText(department);
            etOfficeCourse.setText(officeCourse);
            etOfficeUnit.setText("");
        } else {
            etOfficeUnit.setText(officeUnit);
            etDepartmentAgency.setText("");
            etOfficeCourse.setText("");
        }

        updateFormByRole(role);
    }

    private void updateFormByRole(String role) {
        if ("GSO".equalsIgnoreCase(role)
                && getText(etOfficeUnit).isEmpty()) {

            etOfficeUnit.setText("General Services Office");

        } else if ("ITSO".equalsIgnoreCase(role)
                && getText(etOfficeUnit).isEmpty()) {

            etOfficeUnit.setText("Information Technology Services Office");

        } else if ("SAC".equalsIgnoreCase(role)
                && getText(etOfficeUnit).isEmpty()) {

            etOfficeUnit.setText("SAC");
        }
    }

    private boolean validateForm() {
        clearErrors();

        String role = getText(actvUserType);
        String status = getText(actvStatus);
        String email = getText(etEmail);

        if (role.isEmpty()) {
            actvUserType.setError("Required");
            actvUserType.requestFocus();
            return false;
        }

        if (status.isEmpty()) {
            actvStatus.setError("Required");
            actvStatus.requestFocus();
            return false;
        }

        if (isEmpty(etFullName)) {
            etFullName.setError("Required");
            etFullName.requestFocus();
            return false;
        }

        if (isEmpty(etEmail)) {
            etEmail.setError("Required");
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid email address");
            etEmail.requestFocus();
            return false;
        }

        if (isEmpty(etContact)) {
            etContact.setError("Required");
            etContact.requestFocus();
            return false;
        }

        if ("Requestor".equalsIgnoreCase(role)) {
            if (isEmpty(etDepartmentAgency)) {
                etDepartmentAgency.setError("Required");
                etDepartmentAgency.requestFocus();
                return false;
            }

            if (isEmpty(etOfficeCourse)) {
                etOfficeCourse.setError("Required");
                etOfficeCourse.requestFocus();
                return false;
            }
        } else {
            if (isEmpty(etOfficeUnit)) {
                etOfficeUnit.setError("Required");
                etOfficeUnit.requestFocus();
                return false;
            }
        }

        return true;
    }

    private void updateUser() {
        String role = getText(actvUserType);
        String status = getText(actvStatus);
        String fullName = getText(etFullName);
        String email = getText(etEmail).toLowerCase();
        String contact = getText(etContact);
        String officeUnit = getText(etOfficeUnit);
        String departmentAgency = getText(etDepartmentAgency);
        String officeCourse = getText(etOfficeCourse);

        setLoading(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("email", email);
        updates.put("contactNum", contact);
        updates.put("contactNumber", contact);
        updates.put("role", role);
        updates.put("userType", role);
        updates.put("accountType", role);
        updates.put("status", status);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        if ("Requestor".equalsIgnoreCase(role)) {
            updates.put("department", departmentAgency);
            updates.put("departmentAgency", departmentAgency);
            updates.put("collegeDepartment", departmentAgency);
            updates.put("course", officeCourse);
            updates.put("officeCourse", officeCourse);
            updates.put("officeUnit", "");
        } else {
            updates.put("officeUnit", officeUnit);
            updates.put("department", officeUnit);
            updates.put("departmentAgency", "");
            updates.put("collegeDepartment", "");
            updates.put("course", "");
            updates.put("officeCourse", "");
        }

        db.collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    setLoading(false);

                    if (!email.equalsIgnoreCase(originalEmail)) {
                        Toast.makeText(
                                this,
                                "User updated. Note: Firebase Auth login email was not changed.",
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        Toast.makeText(this, "User updated successfully.", Toast.LENGTH_SHORT).show();
                    }

                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to update user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showDeleteConfirmationDialog() {
        String name = getText(etFullName);

        if (name.isEmpty()) {
            name = currentFullName.isEmpty() ? "this user" : currentFullName;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + name + "?\n\nThis will remove the user profile from Firestore.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUser())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUser() {
        setLoading(true);
        btnDeleteUser.setEnabled(false);
        btnDeleteUser.setText("Deleting...");

        db.collection("users")
                .document(userId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "User deleted successfully.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    btnDeleteUser.setEnabled(true);
                    btnDeleteUser.setText("Delete User");

                    Toast.makeText(
                            this,
                            "Failed to delete user: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void setLoading(boolean loading) {
        btnSaveChanges.setEnabled(!loading);
        btnCancel.setEnabled(!loading);
        btnDeleteUser.setEnabled(!loading);
        actvUserType.setEnabled(!loading);
        actvStatus.setEnabled(!loading);

        btnSaveChanges.setText(loading ? "Saving..." : "Save Changes");
    }

    private void clearErrors() {
        actvUserType.setError(null);
        actvStatus.setError(null);
        etFullName.setError(null);
        etEmail.setError(null);
        etContact.setError(null);
        etOfficeUnit.setError(null);
        etDepartmentAgency.setError(null);
        etOfficeCourse.setError(null);
    }

    private String getUserRole(DocumentSnapshot doc) {
        String role = getStringValue(doc, "role");
        if (!role.isEmpty()) return role;

        role = getStringValue(doc, "userType");
        if (!role.isEmpty()) return role;

        role = getStringValue(doc, "accountType");
        if (!role.isEmpty()) return role;

        return "Requestor";
    }

    private boolean isEmpty(TextInputEditText editText) {
        return getText(editText).isEmpty();
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String getText(AutoCompleteTextView textView) {
        return textView.getText() == null ? "" : textView.getText().toString().trim();
    }

    private String getStringValue(DocumentSnapshot doc, String field) {
        String value = doc.getString(field);
        return value != null ? value.trim() : "";
    }

    private String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) return first.trim();
        if (second != null && !second.trim().isEmpty()) return second.trim();
        return "";
    }
}