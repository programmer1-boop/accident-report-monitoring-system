package com.example.accidentreportandmonitoringsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.ApiClient;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalIncidentStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONObject;

public class GuestReportTrackingActivity extends AppCompatActivity {

    private TextInputEditText etGuestRefLookup, etGuestTokenLookup;
    private MaterialButton btnCheckGuestStatus;
    private MaterialCardView cardGuestTrackingResult;
    private TextView tvTrackedRef, tvTrackedStatusBadge, tvTrackedType, tvTrackedLocation, tvTrackedAssignedTeam;
    private LinearLayout containerGuestTimeline;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_report_tracking);

        sessionManager = new SessionManager(this);

        ImageButton ibBack = findViewById(R.id.ibBack);
        etGuestRefLookup = findViewById(R.id.etGuestRefLookup);
        etGuestTokenLookup = findViewById(R.id.etGuestTokenLookup);
        btnCheckGuestStatus = findViewById(R.id.btnCheckGuestStatus);
        MaterialButton btnLinkGuestReportToAccount = findViewById(R.id.btnLinkGuestReportToAccount);

        cardGuestTrackingResult = findViewById(R.id.cardGuestTrackingResult);
        tvTrackedRef = findViewById(R.id.tvTrackedRef);
        tvTrackedStatusBadge = findViewById(R.id.tvTrackedStatusBadge);
        tvTrackedType = findViewById(R.id.tvTrackedType);
        tvTrackedLocation = findViewById(R.id.tvTrackedLocation);
        tvTrackedAssignedTeam = findViewById(R.id.tvTrackedAssignedTeam);
        containerGuestTimeline = findViewById(R.id.containerGuestTimeline);

        if (ibBack != null) ibBack.setOnClickListener(v -> finish());

        String savedToken = sessionManager.getGuestTrackingToken();
        if (savedToken != null && etGuestTokenLookup != null) {
            etGuestTokenLookup.setText(savedToken);
        }

        if (btnCheckGuestStatus != null) {
            btnCheckGuestStatus.setOnClickListener(v -> lookupGuestReport());
        }

        if (btnLinkGuestReportToAccount != null) {
            btnLinkGuestReportToAccount.setOnClickListener(v -> linkGuestReportToAccount());
        }
    }

    private void linkGuestReportToAccount() {
        String token = sessionManager.getAuthToken();
        if (token == null || token.trim().isEmpty() || token.startsWith("arms_instant_token_") || token.startsWith("arms_fallback_token_")) {
            Toast.makeText(this, "Please sign in as a Citizen first to link this report.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, CitizenLoginActivity.class));
            return;
        }

        String ref = etGuestRefLookup != null && etGuestRefLookup.getText() != null
                ? etGuestRefLookup.getText().toString().trim() : "";
        String trackingToken = etGuestTokenLookup != null && etGuestTokenLookup.getText() != null
                ? etGuestTokenLookup.getText().toString().trim() : "";

        if (ref.isEmpty() || trackingToken.isEmpty()) {
            Toast.makeText(this, "Enter both incident reference and tracking token to link.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("incident_reference", ref);
            payload.put("tracking_token", trackingToken);
            ApiClient.post("/incidents/guest/link", payload, token, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    Toast.makeText(GuestReportTrackingActivity.this, "Report linked to your Citizen account!", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(int statusCode, String errorMessage) {
                    Toast.makeText(GuestReportTrackingActivity.this, "Link failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error linking report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void lookupGuestReport() {
        String ref = etGuestRefLookup != null && etGuestRefLookup.getText() != null
                ? etGuestRefLookup.getText().toString().trim() : "";
        String token = etGuestTokenLookup != null && etGuestTokenLookup.getText() != null
                ? etGuestTokenLookup.getText().toString().trim() : "";

        if (ref.isEmpty() && token.isEmpty()) {
            Toast.makeText(this, "Enter an incident reference or tracking token.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnCheckGuestStatus != null) {
            btnCheckGuestStatus.setEnabled(false);
            btnCheckGuestStatus.setText("Checking...");
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("incident_reference", ref);
            payload.put("tracking_token", token);

            ApiClient.post("/incidents/guest/track", payload, null, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    if (btnCheckGuestStatus != null) {
                        btnCheckGuestStatus.setEnabled(true);
                        btnCheckGuestStatus.setText("Check Report Status");
                    }
                    JSONObject incident = response.optJSONObject("incident");
                    bindIncidentResult(incident != null ? incident : response);
                }

                @Override
                public void onError(int statusCode, String errorMessage) {
                    if (btnCheckGuestStatus != null) {
                        btnCheckGuestStatus.setEnabled(true);
                        btnCheckGuestStatus.setText("Check Report Status");
                    }
                    JSONObject localMatch = findInLocalOrGlobalStore(ref);
                    if (localMatch != null) {
                        bindIncidentResult(localMatch);
                    } else {
                        Toast.makeText(GuestReportTrackingActivity.this, "Report not found: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            });
        } catch (Exception e) {
            if (btnCheckGuestStatus != null) {
                btnCheckGuestStatus.setEnabled(true);
                btnCheckGuestStatus.setText("Check Report Status");
            }
            Toast.makeText(this, "Error checking status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private JSONObject findInLocalOrGlobalStore(String ref) {
        if (ref.isEmpty()) return null;
        JSONArray[] sources = new JSONArray[]{
                ValenciaGlobalIncidentStore.getReports(),
                sessionManager.getLocalReports()
        };
        for (JSONArray arr : sources) {
            if (arr == null) continue;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null && obj.optString("incident_reference", "").equalsIgnoreCase(ref)) {
                    return obj;
                }
            }
        }
        return null;
    }

    private void bindIncidentResult(JSONObject obj) {
        if (cardGuestTrackingResult != null) cardGuestTrackingResult.setVisibility(View.VISIBLE);
        if (tvTrackedRef != null) tvTrackedRef.setText("#" + obj.optString("incident_reference", "ARMS-VAL"));
        if (tvTrackedStatusBadge != null) tvTrackedStatusBadge.setText(obj.optString("current_status", "SUBMITTED"));
        if (tvTrackedType != null) tvTrackedType.setText(obj.optString("incident_type", "EMERGENCY").replace("_", " "));
        if (tvTrackedLocation != null) tvTrackedLocation.setText(obj.optString("landmark_address", "Valencia City"));
        if (tvTrackedAssignedTeam != null) {
            String assigned = obj.optString("assigned_responders", "Pending Assignment");
            tvTrackedAssignedTeam.setText("Assigned Team: " + assigned);
        }
        if (containerGuestTimeline != null) {
            containerGuestTimeline.removeAllViews();
            TextView tv = new TextView(this);
            tv.setText("● Current Status: " + obj.optString("current_status", "SUBMITTED"));
            tv.setTextColor(getResources().getColor(R.color.arms_navy, null));
            containerGuestTimeline.addView(tv);
        }
    }
}
