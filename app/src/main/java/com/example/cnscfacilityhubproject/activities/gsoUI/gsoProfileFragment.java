package com.example.cnscfacilityhubproject.activities.gsoUI;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.activities.LoginActivity;
import com.example.cnscfacilityhubproject.utils.ChangePasswordBottomSheetHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class gsoProfileFragment extends Fragment {

    private static final int MAX_PROFILE_IMAGE_SIZE = 512;

    private TextInputEditText etFullName;
    private TextInputEditText etEmail;
    private TextInputEditText etContact;
    private TextInputEditText etOffice;

    private MaterialButton btnSaveProfile;
    private LinearLayout layoutChangePassword;
    private LinearLayout layoutLogout;

    private MaterialCardView cardProfileAvatar;
    private ImageView ivProfilePhoto;
    private TextView tvProfileInitials;
    private TextView tvAvatarActionLabel;
    private TextView tvProfileName;
    private TextView tvProfileRole;
    private TextView tvProfileUnit;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration profileListener;
    private ActivityResultLauncher<String> profileImagePickerLauncher;

    private String currentFullName = "";
    private String currentUserType = "";
    private String currentOfficeOrDepartment = "";
    private String currentProfileImageBase64 = "";

    private boolean isLoggingOut = false;
    public gsoProfileFragment() {
        super(R.layout.fragment_gso_profile);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        profileImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        saveProfileImage(uri);
                    }
                }
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();

        bindViews(view);
        clearProfileFields();
        setupListeners();
        loadGSOProfileData();
    }

    private void bindViews(View view) {
        etFullName = view.findViewById(R.id.etFullName);
        etEmail = view.findViewById(R.id.etEmail);
        etContact = view.findViewById(R.id.etContact);
        etOffice = view.findViewById(R.id.etOffice);

        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        layoutChangePassword = view.findViewById(R.id.layoutChangePassword);
        layoutLogout = view.findViewById(R.id.layoutLogout);

        cardProfileAvatar = view.findViewById(R.id.cardProfileAvatar);
        ivProfilePhoto = view.findViewById(R.id.ivProfilePhoto);
        tvProfileInitials = view.findViewById(R.id.tvProfileInitials);
        tvAvatarActionLabel = view.findViewById(R.id.tvAvatarActionLabel);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileRole = view.findViewById(R.id.tvProfileRole);
        tvProfileUnit = view.findViewById(R.id.tvProfileUnit);
    }

    private void clearProfileFields() {
        currentFullName = "";
        currentUserType = "";
        currentOfficeOrDepartment = "";
        currentProfileImageBase64 = "";

        clearProfileImage();
        setAvatarActionLabel(false);

        setProfileText(tvProfileInitials, "");
        setProfileText(tvProfileName, "");
        setProfileText(tvProfileRole, "");
        setProfileText(tvProfileUnit, "");

        setEditTextValue(etFullName, "");
        setEditTextValue(etEmail, "");
        setEditTextValue(etContact, "");
        setEditTextValue(etOffice, "");
    }

    private void loadGSOProfileData() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "No logged in user found", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        DocumentReference userRef = db.collection("users").document(userId);

        if (currentUser.getEmail() != null) {
            setEditTextValue(etEmail, currentUser.getEmail());
        }

        userRef.get(Source.CACHE)
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;

                    if (documentSnapshot.exists()) {
                        bindGSOProfile(documentSnapshot);
                    }
                });

        profileListener = userRef.addSnapshotListener((documentSnapshot, error) -> {
            if (!isAdded() || isLoggingOut || auth.getCurrentUser() == null) return;

            if (error != null) {
                Toast.makeText(
                        requireContext(),
                        "Failed to load GSO profile: " + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                bindGSOProfile(documentSnapshot);
            } else if (!isLoggingOut) {
                Toast.makeText(requireContext(), "User profile not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindGSOProfile(DocumentSnapshot documentSnapshot) {
        String fullName = safeText(documentSnapshot.getString("fullName"));
        String email = safeText(documentSnapshot.getString("email"));
        String contactNum = safeText(documentSnapshot.getString("contactNum"));
        String department = safeText(documentSnapshot.getString("department"));
        String office = safeText(documentSnapshot.getString("office"));
        String userType = safeText(documentSnapshot.getString("userType"));
        String profileImageBase64 = firstNonEmpty(
                documentSnapshot.getString("profileImageBase64"),
                documentSnapshot.getString("profilePicBase64")
        );

        currentFullName = fullName;
        currentUserType = userType;
        currentOfficeOrDepartment = firstNonEmpty(office, department);

        bindProfileHeader(
                currentFullName,
                currentUserType,
                currentOfficeOrDepartment,
                profileImageBase64
        );

        setEditTextValue(etFullName, currentFullName);
        setEditTextValue(etEmail, firstNonEmpty(email, currentUser != null ? currentUser.getEmail() : ""));
        setEditTextValue(etContact, contactNum);
        setEditTextValue(etOffice, currentOfficeOrDepartment);
    }

    private void bindProfileHeader(String fullName, String userType, String officeOrDepartment, String profileImageBase64) {
        currentFullName = safeText(fullName);
        currentUserType = safeText(userType);
        currentOfficeOrDepartment = safeText(officeOrDepartment);

        setProfileText(tvProfileName, currentFullName);
        setProfileText(tvProfileRole, currentUserType);
        setProfileText(tvProfileUnit, currentOfficeOrDepartment);

        if (!showProfileImage(profileImageBase64)) {
            setProfileText(tvProfileInitials, getInitials(currentFullName));
        }
    }

    private void setupListeners() {
        if (cardProfileAvatar != null) {
            cardProfileAvatar.setOnClickListener(v -> showProfilePhotoOptions());
        }

        if (ivProfilePhoto != null) {
            ivProfilePhoto.setOnClickListener(v -> showProfilePhotoOptions());
        }

        if (tvProfileInitials != null) {
            tvProfileInitials.setOnClickListener(v -> showProfilePhotoOptions());
        }

        if (btnSaveProfile != null) {
            btnSaveProfile.setOnClickListener(v -> saveProfileChanges());
        }

        if (layoutChangePassword != null) {
            layoutChangePassword.setOnClickListener(v -> ChangePasswordBottomSheetHelper.show(requireActivity()));
        }

        if (layoutLogout != null) {
            layoutLogout.setOnClickListener(v -> logoutUser());
        }
    }

    private void showProfilePhotoOptions() {
        if (!isAdded()) return;

        boolean hasPhoto = !safeText(currentProfileImageBase64).isEmpty();

        String[] options = hasPhoto
                ? new String[]{"Change Photo", "Remove Photo"}
                : new String[]{"Add Photo"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Profile Photo")
                .setItems(options, (dialog, which) -> {
                    if (!hasPhoto || which == 0) {
                        openProfileImagePicker();
                    } else {
                        removeProfileImage();
                    }
                })
                .show();
    }

    private void openProfileImagePicker() {
        if (profileImagePickerLauncher != null) {
            profileImagePickerLauncher.launch("image/*");
        }
    }

    private void saveProfileImage(Uri imageUri) {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "No logged in user found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String imageBase64 = encodeImageToBase64(imageUri);

            if (imageBase64.isEmpty()) {
                Toast.makeText(requireContext(), "Unable to read selected image", Toast.LENGTH_SHORT).show();
                return;
            }

            showProfileImage(imageBase64);

            Map<String, Object> updates = new HashMap<>();
            updates.put("profileImageBase64", imageBase64);
            updates.put("profilePicBase64", imageBase64);
            updates.put("profileImageMimeType", "image/jpeg");
            updates.put("profileImageUpdatedAt", FieldValue.serverTimestamp());

            db.collection("users")
                    .document(currentUser.getUid())
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Profile photo updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Failed to update photo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } catch (Exception e) {
            if (!isAdded()) return;
            Toast.makeText(requireContext(), "Failed to load photo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void removeProfileImage() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "No logged in user found", Toast.LENGTH_SHORT).show();
            return;
        }

        clearProfileImage();
        setProfileText(tvProfileInitials, getInitials(getText(etFullName)));
        setAvatarActionLabel(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("profileImageBase64", FieldValue.delete());
        updates.put("profilePicBase64", FieldValue.delete());
        updates.put("profileImageMimeType", FieldValue.delete());
        updates.put("profileImageUpdatedAt", FieldValue.serverTimestamp());

        db.collection("users")
                .document(currentUser.getUid())
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Profile photo removed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to remove photo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveProfileChanges() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "No logged in user found", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = getText(etFullName);
        String email = getText(etEmail);
        String contact = getText(etContact);
        String officeOrUnit = getText(etOffice);

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

        if (officeOrUnit.isEmpty()) {
            etOffice.setError("Office / Unit is required");
            etOffice.requestFocus();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("email", email);
        updates.put("contactNum", contact);
        updates.put("office", officeOrUnit);
        updates.put("department", officeOrUnit);

        db.collection("users")
                .document(currentUser.getUid())
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;

                    bindProfileHeader(
                            fullName,
                            currentUserType,
                            officeOrUnit,
                            currentProfileImageBase64
                    );

                    Toast.makeText(requireContext(), "GSO profile updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void removeProfileListener() {
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
    }
    private void logoutUser() {
        isLoggingOut = true;
        removeProfileListener();

        if (auth != null) {
            auth.signOut();
        }

        currentUser = null;

        if (!isAdded()) return;

        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        removeProfileListener();
        super.onDestroyView();
    }

    private boolean showProfileImage(String base64Value) {
        String cleanBase64 = extractBase64(base64Value);

        if (cleanBase64.isEmpty()) {
            clearProfileImage();
            return false;
        }

        Bitmap bitmap = decodeBase64ToBitmap(cleanBase64);

        if (bitmap == null) {
            clearProfileImage();
            return false;
        }

        currentProfileImageBase64 = cleanBase64;
        setAvatarActionLabel(true);

        if (ivProfilePhoto != null) {
            ivProfilePhoto.setImageBitmap(bitmap);
            ivProfilePhoto.setVisibility(View.VISIBLE);
        }

        if (tvProfileInitials != null) {
            tvProfileInitials.setVisibility(View.GONE);
        }

        return true;
    }

    private void clearProfileImage() {
        currentProfileImageBase64 = "";
        setAvatarActionLabel(false);

        if (ivProfilePhoto != null) {
            ivProfilePhoto.setImageDrawable(null);
            ivProfilePhoto.setVisibility(View.GONE);
        }
    }

    private String encodeImageToBase64(Uri imageUri) throws IOException {
        Bitmap originalBitmap;

        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri)) {
            originalBitmap = BitmapFactory.decodeStream(inputStream);
        }

        if (originalBitmap == null) {
            return "";
        }

        Bitmap resizedBitmap = resizeBitmap(originalBitmap, MAX_PROFILE_IMAGE_SIZE);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);

        byte[] imageBytes = outputStream.toByteArray();

        if (imageBytes.length > 850_000) {
            outputStream.reset();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);
            imageBytes = outputStream.toByteArray();
        }

        if (imageBytes.length > 850_000) {
            Bitmap smallerBitmap = resizeBitmap(originalBitmap, 360);
            outputStream.reset();
            smallerBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);
            imageBytes = outputStream.toByteArray();
        }

        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private Bitmap decodeBase64ToBitmap(String base64Value) {
        try {
            byte[] imageBytes = Base64.decode(extractBase64(base64Value), Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractBase64(String value) {
        String cleanValue = safeText(value);

        if (cleanValue.startsWith("data:") && cleanValue.contains(",")) {
            return cleanValue.substring(cleanValue.indexOf(',') + 1).trim();
        }

        return cleanValue;
    }

    private void setAvatarActionLabel(boolean hasPhoto) {
        if (tvAvatarActionLabel == null) return;
        tvAvatarActionLabel.setText(hasPhoto ? "Edit Photo" : "Add Photo");
    }

    private void setProfileText(TextView textView, String value) {
        if (textView == null) return;

        String cleanValue = safeText(value);
        textView.setText(cleanValue);
        textView.setVisibility(cleanValue.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setEditTextValue(TextInputEditText editText, String value) {
        if (editText == null) return;
        editText.setText(safeText(value));
    }

    private String getText(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) return "";
        return editText.getText().toString().trim();
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }

    private String firstNonEmpty(String first, String second) {
        String cleanFirst = safeText(first);

        if (!cleanFirst.isEmpty()) {
            return cleanFirst;
        }

        return safeText(second);
    }

    private String getInitials(String fullName) {
        String cleanName = safeText(fullName);

        if (TextUtils.isEmpty(cleanName)) {
            return "";
        }

        String[] parts = cleanName.split("\\s+");
        StringBuilder initials = new StringBuilder();

        for (int i = 0; i < parts.length && i < 2; i++) {
            if (!parts[i].isEmpty()) {
                initials.append(Character.toUpperCase(parts[i].charAt(0)));
            }
        }

        return initials.toString();
    }
}