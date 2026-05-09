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

import com.example.cnscfacilityhubproject.activities.requestorUI.RequestorNavBarActivity;
import com.example.cnscfacilityhubproject.activities.gsoUI.gsoNavBarActivity;
import com.example.cnscfacilityhubproject.activities.itsoUI.itsoNavBarActivity;
import com.example.cnscfacilityhubproject.activities.sacUI.sacNavBarActivity;
import com.example.cnscfacilityhubproject.models.Booking;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;


public class LoginActivity extends AppCompatActivity {



    private void loadBookingData(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        List<Booking> mockBookings = new ArrayList<>();

        mockBookings.add(new Booking(
                "Leadership Seminar",
                "Audio Visual Room",
                "2026-04-15",
                "08:00",
                "12:00",
                "approved"
        ));

        mockBookings.add(new Booking(
                "Faculty Meeting",
                "Conference Hall",
                "2026-04-15",
                "13:00",
                "15:00",
                "approved"
        ));

        mockBookings.add(new Booking(
                "Workshop",
                "Lab 1",
                "2026-04-16",
                "09:00",
                "11:00",
                "approved"
        ));

        for (Booking booking : mockBookings) {
            db.collection("bookings").add(booking);
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);


        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();





        EditText email = findViewById(R.id.etCampusId);
        EditText password = findViewById(R.id.etPassword);
        Button loginBtn = findViewById(R.id.btnLogin);
        TextView signUpTxt = findViewById(R.id.tvSignUp);


        loginBtn.setOnClickListener(v -> {

            String userEmail = email.getText().toString();
            String userPass = password.getText().toString();


            if (userEmail.isEmpty()){
                Toast.makeText(LoginActivity.this, "Please enter your email.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (userPass.isEmpty()){
                Toast.makeText(LoginActivity.this, "Please enter your password.", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(userEmail, userPass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {

                            db.collection("users")
                                    .document(auth.getCurrentUser().getUid())
                                            .get()
                                                    .addOnSuccessListener(documentSnapshot -> {
                                                        Toast.makeText(LoginActivity.this, "Login Success", Toast.LENGTH_SHORT).show();
                                                        if (documentSnapshot.exists()){
                                                            Intent intent;
                                                            String userType = documentSnapshot.getString("userType").toLowerCase();

                                                            if (userType.equals("requestor")) {

                                                                startActivity(new Intent(
                                                                        LoginActivity.this,
                                                                        RequestorNavBarActivity.class
                                                                ));
                                                                finish();

                                                            } else if (userType.equals("itso")) {

                                                                startActivity(new Intent(
                                                                        LoginActivity.this,
                                                                        itsoNavBarActivity.class
                                                                ));
                                                                finish();

                                                            } else if (userType.equals("sac")) {

                                                                startActivity(new Intent(
                                                                        LoginActivity.this,
                                                                        sacNavBarActivity.class
                                                                ));
                                                                finish();

                                                            } else if (userType.equals("gso")) {

                                                                startActivity(new Intent(
                                                                        LoginActivity.this,
                                                                        gsoNavBarActivity.class
                                                                ));
                                                                finish();

                                                            } else {

                                                                Toast.makeText(
                                                                        LoginActivity.this,
                                                                        "Unknown user role.",
                                                                        Toast.LENGTH_SHORT
                                                                ).show();
                                                            }
                                                        }
                                                    });

                        } else {
                            Toast.makeText(LoginActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        });



        signUpTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                email.setText("");
                password.setText("");

                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));

            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activityLogin), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}