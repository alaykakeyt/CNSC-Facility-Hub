package com.example.cnscfacilityhubproject.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cnscfacilityhubproject.R;
import com.google.android.material.button.MaterialButton;

public class LoginSplashActivity extends AppCompatActivity {

    private static final long AUTO_NAVIGATE_DELAY_MS = 1200L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable navigateToLogin = () -> {
        if (isFinishing()) {
            return;
        }
        startActivity(new Intent(LoginSplashActivity.this, LoginActivity.class));
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login_splash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        int[] buttonIds = {R.id.button, R.id.button2, R.id.button3, R.id.button4};
        for (int buttonId : buttonIds) {
            MaterialButton button = findViewById(buttonId);
            if (button != null) {
                button.setOnClickListener(v -> goToLogin());
            }
        }

        MaterialButton continueButton = findViewById(R.id.button4);
        if (continueButton != null) {
            continueButton.setText("Continue to Login");
        }

        handler.postDelayed(navigateToLogin, AUTO_NAVIGATE_DELAY_MS);
    }

    private void goToLogin() {
        handler.removeCallbacks(navigateToLogin);
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(navigateToLogin);
        super.onDestroy();
    }
}
