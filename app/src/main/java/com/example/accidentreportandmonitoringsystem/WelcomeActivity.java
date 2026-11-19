package com.example.accidentreportandmonitoringsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getWindow().getDecorView().setBackgroundColor(android.graphics.Color.parseColor("#F4F6F9"));
            setContentView(R.layout.activity_welcome);

            MaterialButton btnWelcomeSosHold = findViewById(R.id.btnWelcomeSosHold);
            MaterialButton btnContinueGuest = findViewById(R.id.btnContinueGuest);
            MaterialButton btnTrackGuestReport = findViewById(R.id.btnTrackGuestReport);
            MaterialButton btnSignIn = findViewById(R.id.btnSignIn);
            TextView tvSignUpLink = findViewById(R.id.tvSignUpLink);

            com.example.accidentreportandmonitoringsystem.utils.SosHoldHelper.attach(
                    this, btnWelcomeSosHold, "HOLD 3s FOR INSTANT SOS"
            );

            TextView tvPendingSync = findViewById(R.id.tvWelcomePendingSync);
            if (tvPendingSync != null) {
                tvPendingSync.setOnClickListener(v -> triggerPendingSync());
            }

            if (btnContinueGuest != null) {
                btnContinueGuest.setOnClickListener(v -> {
                    Intent intent = new Intent(WelcomeActivity.this, GuestReportActivity.class);
                    startActivity(intent);
                });
            }

            if (btnTrackGuestReport != null) {
                btnTrackGuestReport.setOnClickListener(v -> {
                    Intent intent = new Intent(WelcomeActivity.this, GuestReportTrackingActivity.class);
                    startActivity(intent);
                });
            }

            if (btnSignIn != null) {
                btnSignIn.setOnClickListener(v -> {
                    Intent intent = new Intent(WelcomeActivity.this, CitizenLoginActivity.class);
                    startActivity(intent);
                });
            }

            if (tvSignUpLink != null) {
                tvSignUpLink.setOnClickListener(v -> {
                    Intent intent = new Intent(WelcomeActivity.this, CitizenRegisterActivity.class);
                    startActivity(intent);
                });
            }
        } catch (Throwable t) {
            Toast.makeText(this, "Welcome Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            t.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        triggerPendingSync();
    }

    private void triggerPendingSync() {
        TextView tvPendingSync = findViewById(R.id.tvWelcomePendingSync);
        com.example.accidentreportandmonitoringsystem.remote.SessionManager sm =
                new com.example.accidentreportandmonitoringsystem.remote.SessionManager(this);
        int pendingCount = sm.getPendingSyncReports().length();
        if (tvPendingSync != null) {
            if (pendingCount > 0) {
                tvPendingSync.setText("Pending offline upload: " + pendingCount + " report(s) — Tap to retry sync");
                tvPendingSync.setVisibility(android.view.View.VISIBLE);
            } else {
                tvPendingSync.setVisibility(android.view.View.GONE);
            }
        }
        if (pendingCount > 0) {
            com.example.accidentreportandmonitoringsystem.remote.OfflineReportSyncManager.syncPendingReports(
                    this,
                    remaining -> {
                        if (tvPendingSync != null) {
                            if (remaining > 0) {
                                tvPendingSync.setText("Pending offline upload: " + remaining + " report(s) — Tap to retry sync");
                                tvPendingSync.setVisibility(android.view.View.VISIBLE);
                            } else {
                                tvPendingSync.setVisibility(android.view.View.GONE);
                            }
                        }
                    }
            );
        }
    }
}
