package com.example.cnscfacilityhubproject.activities.sacUI;

import android.content.Intent;
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
import com.example.cnscfacilityhubproject.activities.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class sacProfileFragment extends Fragment {

    private TextInputEditText etFullName, etEmail, etContact, etDepartment;
    private MaterialButton btnSaveProfile;
    private LinearLayout layoutChangePassword, layoutLogout;
    private TextView tvProfileInitials, tvProfileName, tvProfileRole, tvProfileUnit;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    public sacProfileFragment() {
        super(R.layout.fragment_sac_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews(view);
        loadSACProfileData();
        setupActions();
    }

    private void bindViews(View view) {
        etFullName = view.findViewById(R.id.etFullName);
        etEmail = view.findViewById(R.id.etEmail);
        etContact = view.findViewById(R.id.etContact);
        etDepartment = view.findViewById(R.id.etDepartment);

        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        layoutChangePassword = view.findViewById(R.id.layoutChangePassword);
        layoutLogout = view.findViewById(R.id.layoutLogout);

        tvProfileInitials = view.findViewById(R.id.tvProfileInitials);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileRole = view.findViewById(R.id.tvProfileRole);
        tvProfileUnit = view.findViewById(R.id.tvProfileUnit);
    }

    private void loadSACProfileData() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "No logged in user found.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;

                    if (!doc.exists()) {
                        Toast.makeText(requireContext(), "User profile not found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String fullName = safe(doc.getString("fullName"));
                    String email = safe(doc.getString("email"));
                    String contactNum = safe(doc.getString("contactNum"));
                    String department = safe(doc.getString("department"));
                    String office = safe(doc.getString("office"));
                    String userType = safe(doc.getString("userType"));

                    etFullName.setText(fullName);
                    etEmail.setText(email);
                    etContact.setText(contactNum);
                    etDepartment.setText(!office.isEmpty() ? office : department);

                    tvProfileName.setText(!fullName.isEmpty() ? fullName : "SAC Officer");
                    tvProfileRole.setText(!userType.isEmpty() ? userType : "Student Affairs Coordinator");
                    tvProfileUnit.setText(!office.isEmpty() ? office : (!department.isEmpty() ? department : "Student Center Approval Office"));
                    tvProfileInitials.setText(getInitials(fullName));
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to load SAC profile.", Toast.LENGTH_LONG).show();
                });
    }

    private void setupActions() {
        btnSaveProfile.setOnClickListener(v -> {
            if (!validateInputs()) return;

            if (auth.getCurrentUser() == null) {
                Toast.makeText(requireContext(), "No logged in user found.", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = auth.getCurrentUser().getUid();
            String fullName = getText(etFullName);
            String email = getText(etEmail);
            String contact = getText(etContact);
            String department = getText(etDepartment);

            db.collection("users")
                    .document(userId)
                    .update(
                            "fullName", fullName,
                            "email", email,
                            "contactNum", contact,
                            "office", department
                    )
                    .addOnSuccessListener(unused -> {
                        if (!isAdded()) return;

                        tvProfileName.setText(fullName);
                        tvProfileUnit.setText(department);
                        tvProfileInitials.setText(getInitials(fullName));

                        Toast.makeText(requireContext(), "SAC profile updated successfully.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Failed to update profile.", Toast.LENGTH_LONG).show();
                    });
        });

        layoutChangePassword.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Open Change Password screen.", Toast.LENGTH_SHORT).show()
        );

        layoutLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(requireContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    private boolean validateInputs() {
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

        if (isEmpty(etContact)) {
            etContact.setError("Required");
            etContact.requestFocus();
            return false;
        }

        if (isEmpty(etDepartment)) {
            etDepartment.setError("Required");
            etDepartment.requestFocus();
            return false;
        }

        return true;
    }

    private boolean isEmpty(TextInputEditText editText) {
        return editText.getText() == null || TextUtils.isEmpty(editText.getText().toString().trim());
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "SA";

        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.getDefault());
        }

        String first = parts[0].substring(0, 1);
        String last = parts[parts.length - 1].substring(0, 1);
        return (first + last).toUpperCase(Locale.getDefault());
    }
}
