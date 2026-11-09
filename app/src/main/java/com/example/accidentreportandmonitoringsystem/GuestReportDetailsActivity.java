package com.example.accidentreportandmonitoringsystem;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.ApiClient;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;

public class GuestReportDetailsActivity extends AppCompatActivity {

    private AutoCompleteTextView actvIncidentType;
    private TextInputEditText etLocation, etDescription;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_report_details);

        sessionManager = new SessionManager(this);

        actvIncidentType = findViewById(R.id.actvIncidentType);
        etLocation = findViewById(R.id.etLocation);
        etDescription = findViewById(R.id.etDescription);

        MaterialButton btnSubmitGuestReport = findViewById(R.id.btnSubmitGuestReport);

        if (actvIncidentType != null) {
            String[] incidentTypes = new String[]{
                "Road Accident / Collision",
                "Fire Incident",
                "Medical Emergency",
                "Crime / Security Incident",
                "Disaster / Rescue Operation",
                "Other Emergency"
            };
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, incidentTypes);
            actvIncidentType.setAdapter(adapter);
        }

        if (btnSubmitGuestReport != null) {
            btnSubmitGuestReport.setOnClickListener(v -> submitGuestReport());
        }
    }

    private void submitGuestReport() {
        String typeDisplay = actvIncidentType != null ? actvIncidentType.getText().toString() : "Road Accident / Collision";
        String location = etLocation != null && etLocation.getText() != null ? etLocation.getText().toString().trim() : "Sayre Highway, Valencia City";
        String description = etDescription != null && etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        String incidentType = "ROAD_ACCIDENT";
        if (typeDisplay.contains("Fire")) incidentType = "FIRE";
        else if (typeDisplay.contains("Medical")) incidentType = "MEDICAL_EMERGENCY";
        else if (typeDisplay.contains("Crime") || typeDisplay.contains("Security")) incidentType = "CRIME_SECURITY";
        else if (typeDisplay.contains("Disaster") || typeDisplay.contains("Rescue")) incidentType = "DISASTER_RESCUE";
        else if (typeDisplay.contains("Other")) incidentType = "OTHER_EMERGENCY";

        try {
            JSONObject payload = new JSONObject();
            payload.put("incident_type", incidentType);
            payload.put("landmark_address", location);
            payload.put("description", description);
            payload.put("latitude", 7.9064);
            payload.put("longitude", 125.0942);

            Toast.makeText(this, "Submitting Guest Emergency Report...", Toast.LENGTH_SHORT).show();

            ApiClient.post("/incidents/guest", payload, null, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        String incidentRef = response.optString("incident_reference", "ARMS-2026-VAL");
                        String trackingToken = response.optString("tracking_token");

                        if (trackingToken != null && !trackingToken.isEmpty()) {
                            sessionManager.saveGuestTrackingToken(trackingToken);
                        }

                        Toast.makeText(GuestReportDetailsActivity.this, "🚨 REPORT SUBMITTED! Ref: " + incidentRef, Toast.LENGTH_LONG).show();
                        finish();
                    } catch (Exception e) {
                        Toast.makeText(GuestReportDetailsActivity.this, "Submission parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(int statusCode, String errorMessage) {
                    Toast.makeText(GuestReportDetailsActivity.this, "Submission Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error forming emergency report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
