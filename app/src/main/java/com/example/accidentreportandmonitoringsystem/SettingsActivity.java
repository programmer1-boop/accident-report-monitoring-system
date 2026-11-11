package com.example.accidentreportandmonitoringsystem;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.card.MaterialCardView;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton ibBack = findViewById(R.id.ibBack);
        SwitchCompat switchPushNotifications = findViewById(R.id.switchPushNotifications);
        SwitchCompat switchSoundAlerts = findViewById(R.id.switchSoundAlerts);
        SwitchCompat switchLocationSharing = findViewById(R.id.switchLocationSharing);
        MaterialCardView cardAboutArms = findViewById(R.id.cardAboutArms);

        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }

        if (switchPushNotifications != null) {
            switchPushNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Toast.makeText(this, "Push Notifications: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            });
        }

        if (switchSoundAlerts != null) {
            switchSoundAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Toast.makeText(this, "Dispatch Alarms: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            });
        }

        if (switchLocationSharing != null) {
            switchLocationSharing.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Toast.makeText(this, "GPS Location Sharing: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            });
        }

        if (cardAboutArms != null) {
            cardAboutArms.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                    .setTitle("ℹ️ About ARMS")
                    .setMessage("Accident Report & Monitoring System (ARMS)\nVersion 1.0.0\n\nValencia City Emergency Command Center\n\nEmpowering citizens and emergency responders (BFP, PNP, Medical EMS, CDRRMO) with real-time GPS dispatch and evidence tracking.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            });
        }
    }
}
