package com.example.accidentreportandmonitoringsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CitizenAlertsActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private LinearLayout containerAlerts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_alerts);

        sessionManager = new SessionManager(this);
        containerAlerts = findViewById(R.id.containerAlerts);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_notifications);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    Intent intent = new Intent(CitizenAlertsActivity.this, CitizenDashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_my_reports) {
                    Intent intent = new Intent(CitizenAlertsActivity.this, CitizenReportsActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_camera) {
                    Intent intent = new Intent(CitizenAlertsActivity.this, GuestReportActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_notifications) {
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    Intent intent = new Intent(CitizenAlertsActivity.this, CitizenProfileActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }
    }
}
