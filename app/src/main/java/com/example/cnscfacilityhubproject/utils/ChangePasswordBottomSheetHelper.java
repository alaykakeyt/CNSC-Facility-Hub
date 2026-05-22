package com.example.cnscfacilityhubproject.utils;

import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.example.cnscfacilityhubproject.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordBottomSheetHelper {

    public static void show(Activity activity) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null || user.getEmail() == null) {
            Toast.makeText(activity, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(activity, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_change_password, null);
        dialog.setContentView(view);

        TextInputLayout tilCurrent = view.findViewById(R.id.tilCurrentPassword);
        TextInputLayout tilNew = view.findViewById(R.id.tilNewPassword);
        TextInputLayout tilConfirm = view.findViewById(R.id.tilConfirmPassword);

        TextInputEditText etCurrent = view.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNew = view.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = view.findViewById(R.id.etConfirmPassword);

        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        MaterialButton btnUpdate = view.findViewById(R.id.btnUpdatePassword);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnUpdate.setOnClickListener(v -> {
            String currentPwd = etCurrent.getText().toString().trim();
            String newPwd = etNew.getText().toString().trim();
            String confirmPwd = etConfirm.getText().toString().trim();

            tilCurrent.setError(null);
            tilNew.setError(null);
            tilConfirm.setError(null);

            if (TextUtils.isEmpty(currentPwd)) {
                tilCurrent.setError("Current password is required");
                return;
            }
            if (TextUtils.isEmpty(newPwd)) {
                tilNew.setError("New password is required");
                return;
            }
            if (newPwd.length() < 8) {
                tilNew.setError("Password must be at least 8 characters");
                return;
            }
            if (TextUtils.isEmpty(confirmPwd)) {
                tilConfirm.setError("Confirm password is required");
                return;
            }
            if (!newPwd.equals(confirmPwd)) {
                tilConfirm.setError("Passwords do not match");
                return;
            }
            if (newPwd.equals(currentPwd)) {
                tilNew.setError("New password must be different from current password");
                return;
            }

            setLoading(true, btnUpdate, etCurrent, etNew, etConfirm, btnCancel);

            // Re-authenticate
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPwd);
            user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
                if (reauthTask.isSuccessful()) {
                    // Update password
                    user.updatePassword(newPwd).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(activity, "Password updated successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            setLoading(false, btnUpdate, etCurrent, etNew, etConfirm, btnCancel);
                            String error = updateTask.getException() != null ? updateTask.getException().getMessage() : "Update failed";
                            Toast.makeText(activity, "Error: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    setLoading(false, btnUpdate, etCurrent, etNew, etConfirm, btnCancel);
                    if (reauthTask.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                        tilCurrent.setError("Current password is incorrect");
                    } else {
                        String error = reauthTask.getException() != null ? reauthTask.getException().getMessage() : "Authentication failed";
                        Toast.makeText(activity, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                }
            });
        });

        dialog.show();
    }

    private static void setLoading(boolean loading, MaterialButton btn, TextInputEditText... fields) {
        btn.setEnabled(!loading);
        btn.setText(loading ? "Updating..." : "Update Password");
        for (TextInputEditText f : fields) {
            f.setEnabled(!loading);
        }
    }

    private static void setLoading(boolean loading, MaterialButton btn, TextInputEditText f1, TextInputEditText f2, TextInputEditText f3, MaterialButton btn2) {
        btn.setEnabled(!loading);
        btn2.setEnabled(!loading);
        btn.setText(loading ? "Updating..." : "Update Password");
        f1.setEnabled(!loading);
        f2.setEnabled(!loading);
        f3.setEnabled(!loading);
    }
}
