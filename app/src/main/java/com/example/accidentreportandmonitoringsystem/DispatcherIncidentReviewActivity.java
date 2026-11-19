package com.example.accidentreportandmonitoringsystem;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalIncidentStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class DispatcherIncidentReviewActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private String incidentRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispatcher_incident_review);

        sessionManager = new SessionManager(this);
        incidentRef = getIntent().getStringExtra("incident_reference");
        String type = getIntent().getStringExtra("incident_type");
        String address = getIntent().getStringExtra("landmark_address");
        String desc = getIntent().getStringExtra("description");
        double lat = getIntent().getDoubleExtra("latitude", 7.9064);
        double lng = getIntent().getDoubleExtra("longitude", 125.0942);

        ImageButton ibBack = findViewById(R.id.ibBack);
        if (ibBack != null) ibBack.setOnClickListener(v -> finish());

        TextView tvType = findViewById(R.id.tvDispatchIncidentType);
        TextView tvRef = findViewById(R.id.tvDispatchIncidentRef);
        TextView tvLocation = findViewById(R.id.tvDispatchLocation);
        TextView tvDesc = findViewById(R.id.tvDispatchDescription);

        if (tvType != null && type != null) tvType.setText(type.replace("_", " "));
        if (tvRef != null && incidentRef != null) tvRef.setText("Ref: #" + incidentRef);
        if (tvLocation != null) tvLocation.setText((address != null ? address : "Valencia City") + " (" + lat + ", " + lng + ")");
        if (tvDesc != null && desc != null) tvDesc.setText(desc);

        MaterialCheckBox cbCdrrmo = findViewById(R.id.cbAssignCdrrmo);
        MaterialCheckBox cbBfp = findViewById(R.id.cbAssignBfp);
        MaterialCheckBox cbPnp = findViewById(R.id.cbAssignPnp);
        MaterialCheckBox cbMedical = findViewById(R.id.cbAssignMedical);
        TextInputEditText etRejectReason = findViewById(R.id.etRejectReason);

        MaterialButton btnVerifyAndDispatch = findViewById(R.id.btnVerifyAndDispatch);
        MaterialButton btnRejectIncident = findViewById(R.id.btnRejectIncident);

        if (btnVerifyAndDispatch != null) {
            btnVerifyAndDispatch.setOnClickListener(v -> {
                List<String> teams = new ArrayList<>();
                if (cbCdrrmo != null && cbCdrrmo.isChecked()) teams.add("CDRRMO");
                if (cbBfp != null && cbBfp.isChecked()) teams.add("BFP");
                if (cbPnp != null && cbPnp.isChecked()) teams.add("PNP");
                if (cbMedical != null && cbMedical.isChecked()) teams.add("MEDICAL_EMS");

                if (teams.isEmpty()) {
                    Toast.makeText(this, "Select at least one responder team.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String joined = String.join(", ", teams);
                if (incidentRef != null) {
                    sessionManager.updateLocalReportAssignedResponders(incidentRef, joined);
                    sessionManager.updateLocalReportStatus(incidentRef, "ASSIGNED");
                    ValenciaGlobalIncidentStore.updateAssignedResponders(incidentRef, joined);
                    ValenciaGlobalIncidentStore.updateStatus(incidentRef, "ASSIGNED");
                }

                String token = sessionManager.getAuthToken();
                String incidentId = getIntent().getStringExtra("incident_id");
                if (incidentId != null && !incidentId.isEmpty() && token != null
                        && !token.startsWith("arms_instant_token_") && !token.startsWith("arms_fallback_token_")) {
                    try {
                        org.json.JSONObject verifyPayload = new org.json.JSONObject();
                        verifyPayload.put("incident_id", incidentId);
                        verifyPayload.put("verified", true);
                        verifyPayload.put("reason", "Verified and dispatched to " + joined);
                        com.example.accidentreportandmonitoringsystem.remote.ApiClient.post("/dispatcher/verify", verifyPayload, token,
                                new com.example.accidentreportandmonitoringsystem.remote.ApiClient.ApiCallback() {
                                    @Override
                                    public void onSuccess(org.json.JSONObject response) {}
                                    @Override
                                    public void onError(int statusCode, String errorMessage) {}
                                });
                    } catch (Exception ignored) {}
                }

                Toast.makeText(this, "Verified & assigned to: " + joined, Toast.LENGTH_SHORT).show();
                com.example.accidentreportandmonitoringsystem.notifications.ArmsNotificationHelper.showEmergencyNotification(
                        this,
                        "Dispatched #" + (incidentRef != null ? incidentRef : "INCIDENT"),
                        "Assigned teams: " + joined
                );
                finish();
            });
        }

        if (btnRejectIncident != null) {
            btnRejectIncident.setOnClickListener(v -> {
                String reason = etRejectReason != null && etRejectReason.getText() != null
                        ? etRejectReason.getText().toString().trim() : "";
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Please provide a rejection reason.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (incidentRef != null) {
                    sessionManager.updateLocalReportStatus(incidentRef, "REJECTED");
                    ValenciaGlobalIncidentStore.updateStatus(incidentRef, "REJECTED");
                }

                String token = sessionManager.getAuthToken();
                String incidentId = getIntent().getStringExtra("incident_id");
                if (incidentId != null && !incidentId.isEmpty() && token != null
                        && !token.startsWith("arms_instant_token_") && !token.startsWith("arms_fallback_token_")) {
                    try {
                        org.json.JSONObject rejectPayload = new org.json.JSONObject();
                        rejectPayload.put("incident_id", incidentId);
                        rejectPayload.put("verified", false);
                        rejectPayload.put("reason", reason);
                        com.example.accidentreportandmonitoringsystem.remote.ApiClient.post("/dispatcher/verify", rejectPayload, token,
                                new com.example.accidentreportandmonitoringsystem.remote.ApiClient.ApiCallback() {
                                    @Override
                                    public void onSuccess(org.json.JSONObject response) {}
                                    @Override
                                    public void onError(int statusCode, String errorMessage) {}
                                });
                    } catch (Exception ignored) {}
                }

                Toast.makeText(this, "Report marked REJECTED.", Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }
}
