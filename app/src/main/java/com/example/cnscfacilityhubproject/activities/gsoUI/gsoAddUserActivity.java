package com.example.cnscfacilityhubproject.activities.gsoUI;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cnscfacilityhubproject.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class gsoAddUserActivity extends AppCompatActivity {

    private ImageView ivBack;
    private AutoCompleteTextView actvUserType;
    private TextView tvSectionTitle;

    private LinearLayout layoutStaffFields;
    private LinearLayout layoutRequestorFields;

    private TextInputEditText etFullName;
    private TextInputEditText etEmail;
    private TextInputEditText etContact;
    private TextInputEditText etOfficeUnit;
    private TextInputEditText etDepartmentAgency;
    private TextInputEditText etOfficeCourse;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;

    private MaterialCheckBox cbSendCredentials;
    private MaterialButton btnCancel;
    private MaterialButton btnCreateUser;

    private FirebaseAuth mainAuth;
    private FirebaseAuth secondaryAuth;
    private FirebaseFirestore db;

    private static final String SECONDARY_APP_NAME = "secondary_user_creation_app";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gso_add_user);

        mainAuth = FirebaseAuth.getInstance();
        secondaryAuth = getSecondaryFirebaseAuth();
        db = FirebaseFirestore.getInstance();

        bindViews();
        setupUserTypeDropdown();
        setupActions();
        updateFormByRole("GSO");
    }

    private void bindViews() {
        ivBack = findViewById(R.id.ivBack);
        actvUserType = findViewById(R.id.actvUserType);
        tvSectionTitle = findViewById(R.id.tvSectionTitle);

        layoutStaffFields = findViewById(R.id.layoutStaffFields);
        layoutRequestorFields = findViewById(R.id.layoutRequestorFields);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etContact = findViewById(R.id.etContact);
        etOfficeUnit = findViewById(R.id.etOfficeUnit);
        etDepartmentAgency = findViewById(R.id.etDepartmentAgency);
        etOfficeCourse = findViewById(R.id.etOfficeCourse);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        cbSendCredentials = findViewById(R.id.cbSendCredentials);
        btnCancel = findViewById(R.id.btnCancel);
        btnCreateUser = findViewById(R.id.btnCreateUser);
    }

    private FirebaseAuth getSecondaryFirebaseAuth() {
        try {
            FirebaseApp secondaryApp = FirebaseApp.getInstance(SECONDARY_APP_NAME);
            return FirebaseAuth.getInstance(secondaryApp);
        } catch (IllegalStateException ignored) {
            FirebaseApp defaultApp = FirebaseApp.getInstance();
            FirebaseOptions defaultOptions = defaultApp.getOptions();

            FirebaseOptions.Builder builder = new FirebaseOptions.Builder()
                    .setApplicationId(defaultOptions.getApplicationId())
                    .setApiKey(defaultOptions.getApiKey())
                    .setProjectId(defaultOptions.getProjectId());

            if (defaultOptions.getDatabaseUrl() != null) {
                builder.setDatabaseUrl(defaultOptions.getDatabaseUrl());
            }

            if (defaultOptions.getStorageBucket() != null) {
                builder.setStorageBucket(defaultOptions.getStorageBucket());
            }

            FirebaseApp secondaryApp = FirebaseApp.initializeApp(
                    this,
                    builder.build(),
                    SECONDARY_APP_NAME
            );

            return FirebaseAuth.getInstance(secondaryApp);
        }
    }

    private void setupUserTypeDropdown() {
        String[] userTypes = {"GSO", "ITSO", "SAC", "Requestor"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                userTypes
        );

        actvUserType.setAdapter(adapter);
        actvUserType.setText(userTypes[0], false);

        actvUserType.setOnItemClickListener((parent, view, position, id) ->
                updateFormByRole(userTypes[position])
        );
    }

    private void updateFormByRole(String role) {
        clearErrors();

        if ("Requestor".equalsIgnoreCase(role)) {
            tvSectionTitle.setText("Personal Information");
            layoutStaffFields.setVisibility(View.GONE);
            layoutRequestorFields.setVisibility(View.VISIBLE);
        } else {
            tvSectionTitle.setText("Account Information");
            layoutStaffFields.setVisibility(View.VISIBLE);
            layoutRequestorFields.setVisibility(View.GONE);

            if ("GSO".equalsIgnoreCase(role)) {

                etOfficeUnit.setHint("General Services Office");
                etOfficeUnit.setText("General Services Office");

            } else if ("ITSO".equalsIgnoreCase(role)) {

                etOfficeUnit.setHint("Information Technology Services Office");
                etOfficeUnit.setText("Information Technology Services Office");

            } else if ("SAC".equalsIgnoreCase(role)) {

                etOfficeUnit.setHint("SAC");
                etOfficeUnit.setText("SAC");

            } else {
            }
        }
    }

    private void setupActions() {
        ivBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());

        btnCreateUser.setOnClickListener(v -> {
            if (validateForm()) {
                createUserAccount();
            }
        });
    }

    private void createUserAccount() {
        String selectedRole = getText(actvUserType);
        String fullName = getText(etFullName);
        String email = getText(etEmail).toLowerCase();
        String contact = getText(etContact);
        String officeUnit = getText(etOfficeUnit);
        String departmentAgency = getText(etDepartmentAgency);
        String officeCourse = getText(etOfficeCourse);
        String password = getText(etPassword);

        setLoading(true);

        secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() == null) {
                        setLoading(false);
                        Toast.makeText(this, "Failed to create user account.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String newUserId = authResult.getUser().getUid();

                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("uid", newUserId);
                    userMap.put("fullName", fullName);
                    userMap.put("email", email);
                    userMap.put("contactNum", contact);
                    userMap.put("contactNumber", contact);
                    userMap.put("role", selectedRole);
                    userMap.put("userType", selectedRole);
                    userMap.put("accountType", selectedRole);
                    userMap.put("status", "Active");
                    userMap.put("createdAt", FieldValue.serverTimestamp());
                    userMap.put("updatedAt", FieldValue.serverTimestamp());

                    if (mainAuth.getCurrentUser() != null) {
                        userMap.put("createdBy", mainAuth.getCurrentUser().getUid());
                    }

                    if ("Requestor".equalsIgnoreCase(selectedRole)) {
                        userMap.put("department", departmentAgency);
                        userMap.put("departmentAgency", departmentAgency);
                        userMap.put("collegeDepartment", departmentAgency);
                        userMap.put("course", officeCourse);
                        userMap.put("officeCourse", officeCourse);
                    } else {
                        userMap.put("officeUnit", officeUnit);
                        userMap.put("department", officeUnit);
                    }

                    db.collection("users")
                            .document(newUserId)
                            .set(userMap)
                            .addOnSuccessListener(unused -> {
                                secondaryAuth.signOut();
                                setLoading(false);

                                Toast.makeText(
                                        this,
                                        selectedRole + " account created successfully.",
                                        Toast.LENGTH_LONG
                                ).show();

                                if (cbSendCredentials.isChecked()) {
                                    openEmailAppWithCredentials(email, fullName, selectedRole, password);
                                }

                                clearForm();
                            })
                            .addOnFailureListener(e -> {
                                secondaryAuth.signOut();
                                setLoading(false);

                                Toast.makeText(
                                        this,
                                        "Auth created but failed to save user data: " + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {
                    secondaryAuth.signOut();
                    setLoading(false);

                    Toast.makeText(
                            this,
                            "Failed to create user: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void openEmailAppWithCredentials(
            String email,
            String fullName,
            String role,
            String temporaryPassword
    ) {
        String subject = "CNSC FacilityHub Account Credentials";

        String body =
                "Hello " + fullName + ",\n\n" +
                        "Your CNSC FacilityHub account has been created.\n\n" +
                        "Account Type: " + role + "\n" +
                        "Email: " + email + "\n" +
                        "Temporary Password: " + temporaryPassword + "\n\n" +
                        "Please log in and change your password immediately if required.\n\n" +
                        "Thank you.";

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + email));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            startActivity(Intent.createChooser(intent, "Send credentials"));
        } catch (Exception e) {
            Toast.makeText(this, "No email app found to send credentials.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateForm() {
        String selectedType = getText(actvUserType);

        if (TextUtils.isEmpty(selectedType)) {
            actvUserType.setError("Required");
            actvUserType.requestFocus();
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

        String email = getText(etEmail);

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

        if ("Requestor".equalsIgnoreCase(selectedType)) {
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

        if (isEmpty(etPassword)) {
            etPassword.setError("Required");
            etPassword.requestFocus();
            return false;
        }

        if (isEmpty(etConfirmPassword)) {
            etConfirmPassword.setError("Required");
            etConfirmPassword.requestFocus();
            return false;
        }

        String password = getText(etPassword);
        String confirmPassword = getText(etConfirmPassword);

        if (password.length() < 6) {
            etPassword.setError("Minimum 6 characters");
            etPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void setLoading(boolean loading) {
        btnCreateUser.setEnabled(!loading);
        btnCancel.setEnabled(!loading);
        actvUserType.setEnabled(!loading);
        cbSendCredentials.setEnabled(!loading);

        if (loading) {
            btnCreateUser.setText("Creating...");
        } else {
            btnCreateUser.setText("Create User");
        }
    }

    private void clearForm() {
        String currentRole = getText(actvUserType);

        etFullName.setText("");
        etEmail.setText("");
        etContact.setText("");
        etPassword.setText("");
        etConfirmPassword.setText("");
        etDepartmentAgency.setText("");
        etOfficeCourse.setText("");
        cbSendCredentials.setChecked(false);

        if ("GSO".equalsIgnoreCase(currentRole)) {
            etOfficeUnit.setText("General Services Office");
        } else if ("ITSO".equalsIgnoreCase(currentRole)) {
            etOfficeUnit.setText("Information Technology Support Office");
        } else {
            etOfficeUnit.setText("");
        }

        clearErrors();
    }

    private void clearErrors() {
        actvUserType.setError(null);
        etFullName.setError(null);
        etEmail.setError(null);
        etContact.setError(null);
        etOfficeUnit.setError(null);
        etDepartmentAgency.setError(null);
        etOfficeCourse.setError(null);
        etPassword.setError(null);
        etConfirmPassword.setError(null);
    }

    private boolean isEmpty(TextInputEditText editText) {
        return TextUtils.isEmpty(getText(editText));
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String getText(AutoCompleteTextView textView) {
        return textView.getText() == null ? "" : textView.getText().toString().trim();
    }
}