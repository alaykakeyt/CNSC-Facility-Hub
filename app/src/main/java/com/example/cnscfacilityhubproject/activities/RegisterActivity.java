package com.example.cnscfacilityhubproject.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cnscfacilityhubproject.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;



public class RegisterActivity extends AppCompatActivity {


    EditText fullName ;

    EditText contactNum ;

    EditText colDeptAgency;

    EditText officeCourse;

    EditText email;

    EditText password;

    EditText confirm ;

    Button registerBtn;

    TextView signIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);



        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        fullName = findViewById(R.id.etFullName);

        contactNum = findViewById(R.id.etContactNumber);

        colDeptAgency = findViewById(R.id.etCollegeDepartmentAgency);

        officeCourse = findViewById(R.id.etOfficeCourse);

        email = findViewById(R.id.etEmail);

        password = findViewById(R.id.etPassword);

        confirm = findViewById(R.id.etConfirmPassword);

        registerBtn = findViewById(R.id.btnRegister);

        signIn = findViewById(R.id.tvSignIn);



        registerBtn.setOnClickListener(v -> {

            if (validFields()){
                String fullNameText = fullName.getText().toString().trim();
                String contactNumText = contactNum.getText().toString().trim();
                String colDeptAgencyText = colDeptAgency.getText().toString().trim();
                String officeCourseText = officeCourse.getText().toString().trim();
                String emailText = email.getText().toString().trim();
                String passwordText = password.getText().toString().trim();
                String confirmText = confirm.getText().toString().trim();


                auth.createUserWithEmailAndPassword(emailText, passwordText)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {


                                String userId = auth.getCurrentUser().getUid();

                                // 🔥 Create user data map
                                Map<String, Object> userMap = new HashMap<>();
                                userMap.put("uid", userId);
                                userMap.put("fullName", fullNameText);
                                userMap.put("contactNum", contactNumText);
                                userMap.put("department", colDeptAgencyText);
                                userMap.put("course", officeCourseText);
                                userMap.put("email", emailText);
                                userMap.put("userType", "Requestor");


                                // 🔥 Save to Firestore
                                db.collection("users")
                                        .document(userId)
                                        .set(userMap)
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(this, "Registered Successfully!", Toast.LENGTH_SHORT).show();

                                            // Go to main screen
                                            startActivity(new Intent(this, LoginActivity.class));
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });

                            } else {
                                Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }






        });


        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activityRegister), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }


    // Call this when user clicks Register button
    private boolean validFields() {
        String fullNameText = fullName.getText().toString().trim();
        String contactNumText = contactNum.getText().toString().trim();
        String colDeptAgencyText = colDeptAgency.getText().toString().trim();
        String officeCourseText = officeCourse.getText().toString().trim();
        String emailText = email.getText().toString().trim();
        String passwordText = password.getText().toString().trim();
        String confirmText = confirm.getText().toString().trim();

        if (fullNameText.isEmpty()) {
            fullName.setError("Full name is required");
            fullName.requestFocus();
            return false;
        }

        if (contactNumText.isEmpty()) {
            contactNum.setError("Contact number is required");
            contactNum.requestFocus();
            return false;
        }

        if (!contactNumText.matches("^[0-9]{11}$")) {
            contactNum.setError("Enter a valid 11-digit contact number");
            contactNum.requestFocus();
            return false;
        }

        if (colDeptAgencyText.isEmpty()) {
            colDeptAgency.setError("College/Department/Agency is required");
            colDeptAgency.requestFocus();
            return false;
        }

        if (officeCourseText.isEmpty()) {
            officeCourse.setError("Office/Course is required");
            officeCourse.requestFocus();
            return false;
        }

        if (emailText.isEmpty()) {
            email.setError("Email is required");
            email.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            email.setError("Enter a valid email address");
            email.requestFocus();
            return false;
        }

        if (passwordText.isEmpty()) {
            password.setError("Password is required");
            password.requestFocus();
            return false;
        }

        if (passwordText.length() < 8) {
            password.setError("Password must be at least 8 characters");
            password.requestFocus();
            return false;
        }

        if (confirmText.isEmpty()) {
            confirm.setError("Please confirm your password");
            confirm.requestFocus();
            return false;
        }

        if (!passwordText.equals(confirmText)) {
            confirm.setError("Passwords do not match");
            confirm.requestFocus();
            return false;
        }

        return true;
    }

}