package com.example.cnscfacilityhubproject.activities.gsoUI;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cnscfacilityhubproject.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class gsoViewUserActivity extends AppCompatActivity {

    private ImageView ivBack;

    private TextView tvInitials;
    private TextView tvFullName;
    private TextView tvRole;
    private Chip chipStatus;

    private TextView tvEmail;
    private TextView tvContact;
    private TextView tvDepartment;
    private TextView tvOfficeCourse;
    private TextView tvOfficeUnit;


    private MaterialButton btnEmailUser;
    private MaterialButton btnCallUser;

    private FirebaseFirestore db;
    private String userId = "";

    private String email = "";
    private String contact = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gso_view_user);

        db = FirebaseFirestore.getInstance();

        userId = getIntent().getStringExtra("userId");

        bindViews();
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

        tvInitials = findViewById(R.id.tvInitials);
        tvFullName = findViewById(R.id.tvFullName);
        tvRole = findViewById(R.id.tvRole);
        chipStatus = findViewById(R.id.chipStatus);

        tvEmail = findViewById(R.id.tvEmail);
        tvContact = findViewById(R.id.tvContact);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvOfficeCourse = findViewById(R.id.tvOfficeCourse);
        tvOfficeUnit = findViewById(R.id.tvOfficeUnit);

        btnEmailUser = findViewById(R.id.btnEmailUser);
        btnCallUser = findViewById(R.id.btnCallUser);
    }

    private void setupActions() {
        ivBack.setOnClickListener(v -> finish());

        btnEmailUser.setOnClickListener(v -> openEmailApp());

        btnCallUser.setOnClickListener(v -> openDialer());
    }

    private void loadUserDetails() {
        setLoadingState();

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    displayUserDetails(doc);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            this,
                            "Failed to load user details: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                });
    }

    private void setLoadingState() {
        tvInitials.setText("...");
        tvFullName.setText("Loading user...");
        tvRole.setText("Please wait");
        chipStatus.setText("Loading");

        tvEmail.setText("—");
        tvContact.setText("—");
        tvDepartment.setText("—");
        tvOfficeCourse.setText("—");
        tvOfficeUnit.setText("—");

        btnEmailUser.setEnabled(false);
        btnCallUser.setEnabled(false);
    }

    private void displayUserDetails(DocumentSnapshot doc) {
        String fullName = getStringValue(doc, "fullName");
        String role = getUserRole(doc);
        String status = getStringValue(doc, "status");

        if (status.isEmpty()) {
            status = "Active";
        }

        email = getStringValue(doc, "email");

        contact = firstNonEmpty(
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

        tvInitials.setText(getInitials(fullName));
        tvFullName.setText(fallback(fullName));
        tvRole.setText(role);
        chipStatus.setText(status);

        styleStatusChip(status);

        tvEmail.setText(fallback(email));
        tvContact.setText(fallback(contact));


        if ("Requestor".equalsIgnoreCase(role)) {
            tvDepartment.setText(fallback(department));
            tvOfficeCourse.setText(fallback(officeCourse));
            tvOfficeUnit.setText("—");
        } else {
            tvDepartment.setText("—");
            tvOfficeCourse.setText("—");

            if (officeUnit.isEmpty()) {
                if ("GSO".equalsIgnoreCase(role)) {
                    tvOfficeUnit.setText("General Services Office");
                } else if ("ITSO".equalsIgnoreCase(role)) {
                    tvOfficeUnit.setText("Information Technology Support Office");
                } else {
                    tvOfficeUnit.setText("—");
                }
            } else {
                tvOfficeUnit.setText(officeUnit);
            }
        }

        btnEmailUser.setEnabled(!email.isEmpty());
        btnCallUser.setEnabled(!contact.isEmpty());
    }

    private void openEmailApp() {
        if (email.isEmpty()) {
            Toast.makeText(this, "No email available.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + email));

        try {
            startActivity(Intent.createChooser(intent, "Email user"));
        } catch (Exception e) {
            Toast.makeText(this, "No email app found.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openDialer() {
        if (contact.isEmpty()) {
            Toast.makeText(this, "No contact number available.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + contact));

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open dialer.", Toast.LENGTH_SHORT).show();
        }
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

    private void styleStatusChip(String status) {
        if ("Inactive".equalsIgnoreCase(status)
                || "Disabled".equalsIgnoreCase(status)) {

            chipStatus.setTextColor(Color.parseColor("#970705"));
            chipStatus.setChipBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#F3D9D9"))
            );

        } else {
            chipStatus.setTextColor(Color.parseColor("#2E7D32"));
            chipStatus.setChipBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#E7F4E8"))
            );
        }
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "—";
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "MMMM dd, yyyy • hh:mm a",
                    Locale.getDefault()
            );

            return sdf.format(timestamp.toDate());
        } catch (Exception e) {
            return "—";
        }
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "U";
        }

        String[] parts = fullName.trim().split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }

        return (
                parts[0].substring(0, 1)
                        + parts[parts.length - 1].substring(0, 1)
        ).toUpperCase();
    }

    private String getStringValue(DocumentSnapshot doc, String field) {
        String value = doc.getString(field);
        return value != null ? value.trim() : "";
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
}