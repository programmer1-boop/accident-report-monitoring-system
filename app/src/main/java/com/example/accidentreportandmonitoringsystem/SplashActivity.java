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

        // Smart Persistent Routing: If logged in -> go straight to dashboard. If not -> WelcomeActivity.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;
            if (sessionManager.isLoggedIn()) {
                String role = sessionManager.getUserRole();
                String email = sessionManager.getUserEmail().toLowerCase();
                if ("CITIZEN".equalsIgnoreCase(role)) {
                    intent = new Intent(SplashActivity.this, CitizenDashboardActivity.class);
                } else if (email.contains("dispatcher")) {
                    intent = new Intent(SplashActivity.this, DispatcherDashboardActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, ResponderDashboardActivity.class);
                }
            } else {
                intent = new Intent(SplashActivity.this, WelcomeActivity.class);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 1800);
    }
}
