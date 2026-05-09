package com.example.cnscfacilityhubproject.activities.gsoUI;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.activities.LoginActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class gsoNavBarActivity extends AppCompatActivity {

    private LinearLayout navHome, navRequests, navReports, navUsers, navProfile;
    private TextView textHome, textRequests, textReports, textUsers, textProfile;
    private ImageView iconHome, iconRequests, iconReports, iconUsers, iconProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gso_nav_bar);

        if (FirebaseAuth.getInstance().getCurrentUser() == null){
            Toast.makeText(this, "No user is logged in", Toast.LENGTH_LONG).show();

            startActivity(new Intent(gsoNavBarActivity.this, LoginActivity.class));
            finish();
        }

        navHome = findViewById(R.id.navHome);
        navRequests = findViewById(R.id.navRequests);
        navReports = findViewById(R.id.navReports);
        navUsers = findViewById(R.id.navUsers);
        navProfile = findViewById(R.id.navProfile);

        textHome = findViewById(R.id.textHome);
        textRequests = findViewById(R.id.textRequests);
        textReports = findViewById(R.id.textReports);
        textUsers = findViewById(R.id.textUsers);
        textProfile = findViewById(R.id.textProfile);

        iconHome = findViewById(R.id.iconHome);
        iconRequests = findViewById(R.id.iconRequests);
        iconReports = findViewById(R.id.iconReports);
        iconUsers = findViewById(R.id.iconUsers);
        iconProfile = findViewById(R.id.iconProfile);

        if (savedInstanceState == null) {
            loadFragment(new gsoHomeFragment());
            setSelectedTab(Tab.HOME);
        }

        navHome.setOnClickListener(v -> {
            loadFragment(new gsoHomeFragment());
            setSelectedTab(Tab.HOME);
        });

        navRequests.setOnClickListener(v -> {
            loadFragment(new gsoRequestsFragment());
            setSelectedTab(Tab.REQUESTS);
        });

        navReports.setOnClickListener(v -> {
            loadFragment(new gsoReportsFragment());
            setSelectedTab(Tab.REPORTS);
        });

        navUsers.setOnClickListener(v -> {
            loadFragment(new gsoUsersFragment());
            setSelectedTab(Tab.USERS);
        });

        navProfile.setOnClickListener(v -> {
            loadFragment(new gsoProfileFragment());
            Bundle bundle = new Bundle();

            setSelectedTab(Tab.PROFILE);
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.gso_fragment_container, fragment)
                .commit();
    }

    private void setSelectedTab(Tab selectedTab) {
        resetTabs();

        switch (selectedTab) {
            case HOME:
                textHome.setTextColor(Color.parseColor("#970705"));
                iconHome.setImageTintList(ColorStateList.valueOf(Color.parseColor("#970705")));
                break;

            case REQUESTS:
                textRequests.setTextColor(Color.parseColor("#970705"));
                iconRequests.setImageTintList(ColorStateList.valueOf(Color.parseColor("#970705")));
                break;

            case REPORTS:
                textReports.setTextColor(Color.parseColor("#970705"));
                iconReports.setImageTintList(ColorStateList.valueOf(Color.parseColor("#970705")));
                break;

            case USERS:
                textUsers.setTextColor(Color.parseColor("#970705"));
                iconUsers.setImageTintList(ColorStateList.valueOf(Color.parseColor("#970705")));
                break;

            case PROFILE:
                textProfile.setTextColor(Color.parseColor("#970705"));
                iconProfile.setImageTintList(ColorStateList.valueOf(Color.parseColor("#970705")));
                break;
        }
    }

    private void resetTabs() {
        int dark = Color.parseColor("#313131");

        textHome.setTextColor(dark);
        textRequests.setTextColor(dark);
        textReports.setTextColor(dark);
        textUsers.setTextColor(dark);
        textProfile.setTextColor(dark);

        iconHome.setImageTintList(ColorStateList.valueOf(dark));
        iconRequests.setImageTintList(ColorStateList.valueOf(dark));
        iconReports.setImageTintList(ColorStateList.valueOf(dark));
        iconUsers.setImageTintList(ColorStateList.valueOf(dark));
        iconProfile.setImageTintList(ColorStateList.valueOf(dark));
    }

    private enum Tab {
        HOME, REQUESTS, REPORTS, USERS, PROFILE
    }


    }
