package com.example.cnscfacilityhubproject.activities.gsoUI;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cnscfacilityhubproject.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class gsoUsersFragment extends Fragment {

    private AutoCompleteTextView actvUserFilter;
    private MaterialButton btnAddUser;

    private TextView tvTotalUsers;
    private TextView tvActiveUsers;

    private LinearLayout layoutUserList;
    private LinearLayout layoutEmptyState;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration usersListener;

    private String selectedFilter = "All";

    private final List<DocumentSnapshot> userList = new ArrayList<>();

    public gsoUsersFragment() {
        super(R.layout.fragment_gso_users);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        bindViews(view);
        setupFilter();
        setupActions();
        listenForUsers();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }
    }

    private void bindViews(View view) {
        actvUserFilter = view.findViewById(R.id.actvUserFilter);
        btnAddUser = view.findViewById(R.id.btnAddUser);

        tvTotalUsers = view.findViewById(R.id.tvTotalUsers);
        tvActiveUsers = view.findViewById(R.id.tvActiveUsers);

        layoutUserList = view.findViewById(R.id.layoutUserList);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
    }

    private void setupFilter() {
        String[] filters = {"All", "GSO", "ITSO", "Requestor"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                filters
        );

        actvUserFilter.setAdapter(adapter);
        actvUserFilter.setText(selectedFilter, false);

        actvUserFilter.setOnItemClickListener((parent, view, position, id) -> {
            selectedFilter = filters[position];
            renderUsers();
        });
    }

    private void setupActions() {
        btnAddUser.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), gsoAddUserActivity.class);
            startActivity(intent);
        });
    }

    private void listenForUsers() {
        if (usersListener != null) {
            usersListener.remove();
        }

        usersListener = db.collection("users")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null || snapshot == null) {
                        Toast.makeText(requireContext(), "Failed to load users.", Toast.LENGTH_SHORT).show();
                        showEmptyState();
                        return;
                    }

                    userList.clear();

                    int totalUsers = 0;
                    int activeUsers = 0;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        totalUsers++;

                        String status = getStringValue(doc, "status");
                        if (status.isEmpty() || "Active".equalsIgnoreCase(status)) {
                            activeUsers++;
                        }

                        userList.add(doc);
                    }

                    tvTotalUsers.setText(String.valueOf(totalUsers));
                    tvActiveUsers.setText(String.valueOf(activeUsers));

                    renderUsers();
                });
    }

    private void renderUsers() {
        if (!isAdded()) return;

        layoutUserList.removeAllViews();

        List<DocumentSnapshot> filteredUsers = new ArrayList<>();

        for (DocumentSnapshot doc : userList) {
            String role = getUserRole(doc);

            if ("All".equalsIgnoreCase(selectedFilter)) {
                filteredUsers.add(doc);
            } else if (selectedFilter.equalsIgnoreCase(role)) {
                filteredUsers.add(doc);
            }
        }

        if (filteredUsers.isEmpty()) {
            showEmptyState();
            return;
        }

        layoutEmptyState.setVisibility(View.GONE);
        layoutUserList.setVisibility(View.VISIBLE);

        for (DocumentSnapshot doc : filteredUsers) {
            layoutUserList.addView(createUserCard(doc));
        }
    }

    private View createUserCard(DocumentSnapshot doc) {
        String userId = doc.getId();

        String fullName = getStringValue(doc, "fullName");
        String email = getStringValue(doc, "email");
        String role = getUserRole(doc);
        String status = getStringValue(doc, "status");

        if (status.isEmpty()) {
            status = "Active";
        }

        String department = getStringValue(doc, "department");
        String officeUnit = getStringValue(doc, "officeUnit");
        String course = getStringValue(doc, "course");
        String officeCourse = getStringValue(doc, "officeCourse");

        MaterialCardView card = new MaterialCardView(requireContext());

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(12));

        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(24));
        card.setCardElevation(dp(6));
        card.setStrokeColor(Color.parseColor("#313131"));
        card.setStrokeWidth(dp(1));

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(18));

        LinearLayout headerRow = new LinearLayout(requireContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        MaterialCardView avatarCard = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(46), dp(46));
        avatarCard.setLayoutParams(avatarParams);
        avatarCard.setRadius(dp(15));
        avatarCard.setCardElevation(0);
        avatarCard.setCardBackgroundColor(getRoleColor(role));

        TextView avatarText = new TextView(requireContext());
        avatarText.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        avatarText.setGravity(Gravity.CENTER);
        avatarText.setText(getInitials(fullName));
        avatarText.setTextColor(Color.WHITE);
        avatarText.setTextSize(14f);
        avatarText.setTypeface(null, android.graphics.Typeface.BOLD);

        avatarCard.addView(avatarText);

        LinearLayout titleLayout = new LinearLayout(requireContext());
        titleLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        titleParams.setMargins(dp(12), 0, dp(8), 0);
        titleLayout.setLayoutParams(titleParams);

        TextView tvName = new TextView(requireContext());
        tvName.setText(!fullName.isEmpty() ? fullName : "Unnamed User");
        tvName.setTextColor(Color.parseColor("#313131"));
        tvName.setTextSize(16f);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvSubtitle = new TextView(requireContext());
        tvSubtitle.setText(buildSubtitle(role, department, officeUnit, course, officeCourse));
        tvSubtitle.setTextColor(Color.parseColor("#313131"));
        tvSubtitle.setTextSize(12f);
        tvSubtitle.setAlpha(0.65f);

        titleLayout.addView(tvName);
        titleLayout.addView(tvSubtitle);

        Chip chipStatus = new Chip(requireContext());
        chipStatus.setText(status);
        chipStatus.setTextColor(getStatusTextColor(status));
        chipStatus.setChipBackgroundColor(ColorStateList.valueOf(getStatusBackgroundColor(status)));
        chipStatus.setCheckable(false);
        chipStatus.setClickable(false);

        headerRow.addView(avatarCard);
        headerRow.addView(titleLayout);
        headerRow.addView(chipStatus);

        TextView tvDescription = new TextView(requireContext());
        tvDescription.setText(buildDescription(role, email, department, officeUnit));
        tvDescription.setTextColor(Color.parseColor("#313131"));
        tvDescription.setTextSize(14f);
        tvDescription.setLineSpacing(2f, 1f);

        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        descriptionParams.setMargins(0, dp(12), 0, 0);
        tvDescription.setLayoutParams(descriptionParams);

        LinearLayout buttonRow = new LinearLayout(requireContext());
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams buttonRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        buttonRowParams.setMargins(0, dp(14), 0, 0);
        buttonRow.setLayoutParams(buttonRowParams);

        MaterialButton btnView = createFilledButton("View", getRoleColor(role));
        btnView.setOnClickListener(v -> openViewUserActivity(userId));

        MaterialButton btnEdit = createOutlinedButton("Edit");
        btnEdit.setOnClickListener(v -> openEditUserActivity(userId));

        MaterialButton btnDelete = createOutlinedButton("Delete");
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog(userId, fullName, email));

        buttonRow.addView(btnView);
        buttonRow.addView(btnEdit);
        buttonRow.addView(btnDelete);

        container.addView(headerRow);
        container.addView(tvDescription);
        container.addView(buttonRow);

        card.addView(container);

        return card;
    }

    private MaterialButton createFilledButton(String text, int color) {
        MaterialButton button = new MaterialButton(requireContext());

        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
        button.setBackgroundTintList(ColorStateList.valueOf(color));
        button.setCornerRadius(dp(16));
        button.setElevation(0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                dp(46),
                1f
        );
        params.setMargins(0, 0, dp(8), 0);
        button.setLayoutParams(params);

        return button;
    }

    private MaterialButton createOutlinedButton(String text) {
        MaterialButton button = new MaterialButton(requireContext());

        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.parseColor("#970705"));
        button.setTypeface(null, android.graphics.Typeface.BOLD);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        button.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#970705")));
        button.setStrokeWidth(dp(1));
        button.setCornerRadius(dp(16));
        button.setElevation(0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                dp(46),
                1f
        );
        params.setMargins(0, 0, dp(8), 0);
        button.setLayoutParams(params);

        return button;
    }

    private void showDeleteConfirmationDialog(String userId, String fullName, String email) {
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "User ID not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() != null && userId.equals(auth.getCurrentUser().getUid())) {
            Toast.makeText(
                    requireContext(),
                    "You cannot delete your own account while logged in.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String displayName = fullName != null && !fullName.trim().isEmpty()
                ? fullName.trim()
                : "this user";

        String displayEmail = email != null && !email.trim().isEmpty()
                ? "\n\nEmail: " + email.trim()
                : "";

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete User")
                .setMessage(
                        "Are you sure you want to delete " + displayName + "?"
                                + displayEmail
                                + "\n\nThis will remove the user profile from Firestore."
                )
                .setPositiveButton("Delete", (dialog, which) -> deleteUser(userId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUser(String userId) {
        db.collection("users")
                .document(userId)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;

                    Toast.makeText(
                            requireContext(),
                            "User deleted successfully.",
                            Toast.LENGTH_SHORT
                    ).show();

                    /*
                     * The Firestore listener will automatically refresh the list.
                     * This manual cleanup makes the UI update immediately.
                     */
                    removeUserFromLocalList(userId);
                    renderUsers();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;

                    Toast.makeText(
                            requireContext(),
                            "Failed to delete user: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void removeUserFromLocalList(String userId) {
        for (int i = userList.size() - 1; i >= 0; i--) {
            DocumentSnapshot doc = userList.get(i);

            if (doc.getId().equals(userId)) {
                userList.remove(i);
            }
        }

        int totalUsers = userList.size();
        int activeUsers = 0;

        for (DocumentSnapshot doc : userList) {
            String status = getStringValue(doc, "status");

            if (status.isEmpty() || "Active".equalsIgnoreCase(status)) {
                activeUsers++;
            }
        }

        tvTotalUsers.setText(String.valueOf(totalUsers));
        tvActiveUsers.setText(String.valueOf(activeUsers));
    }

    private void openViewUserActivity(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "User ID not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(requireContext(), gsoViewUserActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }

    private void openEditUserActivity(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "User ID not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(requireContext(), gsoEditUserActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }

    private void showEmptyState() {
        layoutUserList.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
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

    private String buildSubtitle(String role, String department, String officeUnit, String course, String officeCourse) {
        String secondary = "";

        if ("Requestor".equalsIgnoreCase(role)) {
            secondary = !department.isEmpty() ? department : course;
            if (secondary.isEmpty()) secondary = officeCourse;
        } else {
            secondary = !officeUnit.isEmpty() ? officeUnit : department;
        }

        if (secondary.isEmpty()) {
            return role;
        }

        return role + " • " + secondary;
    }

    private String buildDescription(String role, String email, String department, String officeUnit) {
        StringBuilder builder = new StringBuilder();

        if (!email.isEmpty()) {
            builder.append("Email: ").append(email);
        }

        if ("Requestor".equalsIgnoreCase(role)) {
            if (!department.isEmpty()) {
                if (builder.length() > 0) builder.append("\n");
                builder.append("Department: ").append(department);
            }
        } else {
            if (!officeUnit.isEmpty()) {
                if (builder.length() > 0) builder.append("\n");
                builder.append("Office / Unit: ").append(officeUnit);
            }
        }

        return builder.length() == 0 ? "User account information available." : builder.toString();
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "U";
        }

        String[] parts = fullName.trim().split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }

        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private int getRoleColor(String role) {
        if ("GSO".equalsIgnoreCase(role)) {
            return Color.parseColor("#313131");
        }

        return Color.parseColor("#970705");
    }

    private int getStatusTextColor(String status) {
        if ("Inactive".equalsIgnoreCase(status) || "Disabled".equalsIgnoreCase(status)) {
            return Color.parseColor("#970705");
        }

        return Color.parseColor("#2E7D32");
    }

    private int getStatusBackgroundColor(String status) {
        if ("Inactive".equalsIgnoreCase(status) || "Disabled".equalsIgnoreCase(status)) {
            return Color.parseColor("#F3D9D9");
        }

        return Color.parseColor("#E7F4E8");
    }

    private String getStringValue(DocumentSnapshot doc, String field) {
        String value = doc.getString(field);
        return value != null ? value.trim() : "";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}