
package com.example.cnscfacilityhubproject.activities.requestorUI;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RequestorProfileFragment extends Fragment {

    private TextView tvProfileInitials, tvProfileName, tvProfileRole, tvProfileDepartment;
    private TextInputEditText etFullName, etEmail, etContact, etDepartment, etOfficeCourse;
    private MaterialButton btnSaveProfile;
    private LinearLayout layoutChangePassword, layoutLogout;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    public RequestorProfileFragment() {
        // Required empty public constructor
    }

    public static RequestorProfileFragment newInstance() {
        return new RequestorProfileFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_requestor_profile, container, false);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();

        initViews(view);
        loadUserProfile();
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        tvProfileInitials = view.findViewById(R.id.tvProfileInitials);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileRole = view.findViewById(R.id.tvProfileRole);
        tvProfileDepartment = view.findViewById(R.id.tvProfileDepartment);

        etFullName = view.findViewById(R.id.etFullName);
        etEmail = view.findViewById(R.id.etEmail);
        etContact = view.findViewById(R.id.etContact);
        etDepartment = view.findViewById(R.id.etDepartment);
        etOfficeCourse = view.findViewById(R.id.etOfficeCourse);

        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        layoutChangePassword = view.findViewById(R.id.layoutChangePassword);
        layoutLogout = view.findViewById(R.id.layoutLogout);
    }

    private void loadUserProfile() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "No user is logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

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
                        String email = documentSnapshot.getString("email");
                        String userType = documentSnapshot.getString("userType");

                        tvProfileName.setText(notNull(fullName));
                        tvProfileRole.setText(notNull(userType));
                        tvProfileDepartment.setText(notNull(department));
                        tvProfileInitials.setText(getInitials(fullName));

                        etFullName.setText(notNull(fullName));
                        etEmail.setText(notNull(email));
                        etContact.setText(notNull(contactNum));
                        etDepartment.setText(notNull(department));
                        etOfficeCourse.setText(notNull(course));
                    } else {
                        Toast.makeText(getContext(), "Profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Failed to load profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setupListeners() {
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());

        layoutLogout.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        layoutChangePassword.setOnClickListener(v -> {
            if (currentUser != null && currentUser.getEmail() != null) {
                auth.sendPasswordResetEmail(currentUser.getEmail())
                        .addOnSuccessListener(unused -> {
                            if (!isAdded()) return;
                            Toast.makeText(getContext(), "Password reset email sent", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            if (!isAdded()) return;
                            Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }
        });
    }

    private void saveProfileChanges() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "No user is logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = getText(etFullName);
        String email = getText(etEmail);
        String contact = getText(etContact);
        String department = getText(etDepartment);
        String officeCourse = getText(etOfficeCourse);

        if (fullName.isEmpty()) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (contact.isEmpty()) {
            etContact.setError("Contact number is required");
            etContact.requestFocus();
            return;
        }

        if (!contact.matches("^[0-9]{11}$")) {
            etContact.setError("Enter a valid 11-digit contact number");
            etContact.requestFocus();
            return;
        }

        if (department.isEmpty()) {
            etDepartment.setError("Department is required");
            etDepartment.requestFocus();
            return;
        }

        if (officeCourse.isEmpty()) {
            etOfficeCourse.setError("Office/Course is required");
            etOfficeCourse.requestFocus();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("email", email);
        updates.put("contactNum", contact);
        updates.put("department", department);
        updates.put("course", officeCourse);

        db.collection("users")
                .document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;

                    tvProfileName.setText(fullName);
                    tvProfileDepartment.setText(department);
                    tvProfileInitials.setText(getInitials(fullName));

                    Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String notNull(String value) {
        return value != null ? value : "";
    }

    private String getInitials(String fullName) {
        if (TextUtils.isEmpty(fullName)) return "NA";

        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();

        for (int i = 0; i < parts.length && i < 2; i++) {
            if (!parts[i].isEmpty()) {
                initials.append(Character.toUpperCase(parts[i].charAt(0)));
            }
        }

        return initials.length() > 0 ? initials.toString() : "NA";
    }
}