package com.example.accidentreportandmonitoringsystem;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setBackgroundColor(android.graphics.Color.parseColor("#142B4A"));
        setContentView(R.layout.activity_splash);

        SessionManager sessionManager = new SessionManager(this);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                Intent intent;
                if (sessionManager.isLoggedIn()) {
                    String role = sessionManager.getUserRole();
                    String email = sessionManager.getUserEmail() != null ? sessionManager.getUserEmail().toLowerCase() : "";
                    if ("ADMIN".equalsIgnoreCase(role) || email.contains("admin")) {
                        intent = new Intent(SplashActivity.this, AdminDashboardActivity.class);
                    } else if ("CITIZEN".equalsIgnoreCase(role)) {
                        intent = new Intent(SplashActivity.this, CitizenDashboardActivity.class);
                    } else {
                        intent = new Intent(SplashActivity.this, ResponderDashboardActivity.class);
                    }
                } else {
                    intent = new Intent(SplashActivity.this, WelcomeActivity.class);
                }
                startActivity(intent);
                finish();
            } catch (Exception e) {
                Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
                startActivity(intent);
                finish();
            }
        }, 1000);
    }
}
