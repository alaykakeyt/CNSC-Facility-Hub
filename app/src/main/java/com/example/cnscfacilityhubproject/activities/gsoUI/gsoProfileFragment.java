package com.example.cnscfacilityhubproject.activities.gsoUI;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.activities.LoginActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class gsoProfileFragment extends Fragment {

    private TextInputEditText etFullName, etEmail, etContact, etOffice;
    private MaterialButton btnSaveProfile;
    private LinearLayout layoutChangePassword, layoutLogout;

    private TextView profileName, initials;






    public gsoProfileFragment() {
        super(R.layout.fragment_gso_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);



        initials = view.findViewById(R.id.tvProfileInitials);
        etFullName = view.findViewById(R.id.etFullName);
        etEmail = view.findViewById(R.id.etEmail);
        etContact = view.findViewById(R.id.etContact);
        etOffice = view.findViewById(R.id.etOffice);

        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        layoutChangePassword = view.findViewById(R.id.layoutChangePassword);
        layoutLogout = view.findViewById(R.id.layoutLogout);


        initials = view.findViewById(R.id.tvProfileInitials);
        profileName = view.findViewById(R.id.tvProfileName);


        loadProfileData();
        setupActions();





    }




    private void loadProfileData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        DocumentSnapshot doc = task.getResult();

                        if (doc != null && doc.exists()){
                            String[] splitName = doc.getString("fullName").split(" ");

                            char char1 = splitName[0].toUpperCase().charAt(0);
                            char char2 = splitName[splitName.length - 1].toUpperCase().charAt(0);
                            String init = Character.toString(char1) + Character.toString(char2);
                            initials.setText(String.format(init));
                            profileName.setText(String.format("%s %s", splitName[0].toUpperCase(), splitName[splitName.length - 1].toUpperCase()));

                            etFullName.setText(doc.getString("fullName"));
                            etEmail.setText(doc.getString("email"));
                            etContact.setText(doc.getString("contactNum"));
                            etOffice.setText(doc.getString("department"));
                        }
                    }
                });
    }



    private boolean isValidInputs() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String contactNumber = etContact.getText().toString().trim();
        String officeUnit = etOffice.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return false;
        }

        if (fullName.split("\\s+").length < 2) {
            etFullName.setError("Enter your full name");
            etFullName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email address is required");
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(contactNumber)) {
            etContact.setError("Contact number is required");
            etContact.requestFocus();
            return false;
        }

        if (!contactNumber.matches("^09\\d{9}$")) {
            etContact.setError("Enter a valid 11-digit mobile number");
            etContact.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(officeUnit)) {
            etOffice.setError("Office / Unit is required");
            etOffice.requestFocus();
            return false;
        }

        return true;
    }


    private void setupActions() {
        btnSaveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isValidInputs()){
                    Map<String, Object> map = new HashMap<>();

                    map.put("fullName", etFullName.getText().toString().trim());
                    map.put("email", etEmail.getText().toString().trim());
                    map.put("contactNum", etContact.getText().toString().trim());
                    map.put("department", etOffice.getText().toString().trim());

                    DocumentReference docRef = FirebaseFirestore.getInstance().collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid());

                    docRef.update(map);
                }


            }
        });



        layoutChangePassword.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Open Change Password screen.", Toast.LENGTH_SHORT).show()
        );

        layoutLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();


                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

            }
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

        if (isEmpty(etOffice)) {
            etOffice.setError("Required");
            etOffice.requestFocus();
            return false;
        }

        return true;
    }

    private boolean isEmpty(TextInputEditText editText) {
        return editText.getText() == null || TextUtils.isEmpty(editText.getText().toString().trim());
    }
}