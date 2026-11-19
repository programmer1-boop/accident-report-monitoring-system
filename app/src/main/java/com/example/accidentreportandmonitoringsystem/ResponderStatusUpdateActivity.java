package com.example.accidentreportandmonitoringsystem;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalIncidentStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONObject;

public class ResponderStatusUpdateActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private String incidentRef;
    private TextView tvCurrentStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_responder_status_update);

        sessionManager = new SessionManager(this);
        incidentRef = getIntent().getStringExtra("incident_ref");
        String type = getIntent().getStringExtra("incident_type");
        String address = getIntent().getStringExtra("address");
        String status = getIntent().getStringExtra("status");
        double lat = getIntent().getDoubleExtra("incident_lat", 7.9064);
        double lng = getIntent().getDoubleExtra("incident_lng", 125.0942);

        ImageButton ibBack = findViewById(R.id.ibBack);
        if (ibBack != null) ibBack.setOnClickListener(v -> finish());

        TextView tvType = findViewById(R.id.tvResponderIncidentType);
        tvCurrentStatus = findViewById(R.id.tvResponderCurrentStatus);
        TextView tvRef = findViewById(R.id.tvResponderIncidentRef);
        TextView tvLoc = findViewById(R.id.tvResponderIncidentLocation);

        if (tvType != null && type != null) tvType.setText(type.replace("_", " "));
        if (tvCurrentStatus != null && status != null) tvCurrentStatus.setText(status);
        if (tvRef != null && incidentRef != null) tvRef.setText("Ref: #" + incidentRef);
        if (tvLoc != null) {
            tvLoc.setText(address != null && !address.isEmpty() ? address : "Valencia City, Bukidnon, Philippines");
        }

        bindMediaEvidence();

        MaterialButton btnOpenMap = findViewById(R.id.btnOpenIncidentMap);
        if (btnOpenMap != null) {
            btnOpenMap.setOnClickListener(v -> {
                String agency = sessionManager.getUserAgency() != null ? sessionManager.getUserAgency().toUpperCase() : "CDRRMO";
                double stationLat = 7.9018;
                double stationLng = 125.0912;
                if (agency.contains("BFP")) {
                    stationLat = 7.9118;
                    stationLng = 125.0926;
                } else if (agency.contains("PNP")) {
                    stationLat = 7.9048;
                    stationLng = 125.0978;
                } else if (agency.contains("MED") || agency.contains("HEMS")) {
                    stationLat = 7.9162;
                    stationLng = 125.0904;
                }

                Intent intent = new Intent(this, IncidentLocationActivity.class);
                intent.putExtra("is_dispatcher_map", true);
                intent.putExtra("auto_start_navigation", true);
                intent.putExtra("incident_lat", lat);
                intent.putExtra("incident_lng", lng);
                intent.putExtra("station_lat", stationLat);
                intent.putExtra("station_lng", stationLng);
                intent.putExtra("incident_ref", incidentRef);
                intent.putExtra("incident_type", type);
                intent.putExtra("address", address);
                startActivity(intent);
            });
        }

        MaterialButton btnAccept = findViewById(R.id.btnStatusAccept);
        MaterialButton btnEnRoute = findViewById(R.id.btnStatusEnRoute);
        MaterialButton btnOnScene = findViewById(R.id.btnStatusOnScene);
        MaterialButton btnResolve = findViewById(R.id.btnStatusResolve);
        MaterialButton btnDecline = findViewById(R.id.btnStatusDecline);
        TextInputEditText etDeclineReason = findViewById(R.id.etDeclineReason);

        if (btnAccept != null) btnAccept.setOnClickListener(v -> applyStatus("ACCEPTED"));
        if (btnEnRoute != null) btnEnRoute.setOnClickListener(v -> applyStatus("RESPONDING"));
        if (btnOnScene != null) btnOnScene.setOnClickListener(v -> applyStatus("ON_SCENE"));
        if (btnResolve != null) {
            btnResolve.setOnClickListener(v -> {
                if (incidentRef != null) {
                    sessionManager.markIncidentResolved(incidentRef);
                }
                applyStatus("RESOLVED");
                finish();
            });
        }

        if (btnDecline != null) {
            btnDecline.setOnClickListener(v -> {
                String reason = etDeclineReason != null && etDeclineReason.getText() != null
                        ? etDeclineReason.getText().toString().trim() : "";
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Please enter a decline reason.", Toast.LENGTH_SHORT).show();
                    return;
                }
                applyStatus("DECLINED");
                finish();
            });
        }
    }

    private void bindMediaEvidence() {
        ImageView ivPhoto = findViewById(R.id.ivResponderEvidencePhoto);
        TextView tvNoMedia = findViewById(R.id.tvNoMediaEvidence);
        MaterialButton btnPlayVideo = findViewById(R.id.btnPlayResponderVideo);

        String photoBase64 = getIntent().getStringExtra("photo_base64");
        boolean hasVideo = getIntent().getBooleanExtra("has_video", false);
        String videoUri = getIntent().getStringExtra("video_uri");

        if ((photoBase64 == null || photoBase64.isEmpty()) && incidentRef != null) {
            JSONArray[] sources = new JSONArray[]{
                    ValenciaGlobalIncidentStore.getReports(),
                    sessionManager.getLocalReports()
            };
            for (JSONArray arr : sources) {
                if (arr == null) continue;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.optJSONObject(i);
                    if (obj != null && incidentRef.equals(obj.optString("incident_reference", ""))) {
                        if (photoBase64 == null || photoBase64.isEmpty()) {
                            photoBase64 = obj.optString("photo_base64", "");
                        }
                        if (!hasVideo) {
                            hasVideo = obj.optBoolean("has_video", false);
                            videoUri = obj.optString("video_uri", "");
                        }
                        break;
                    }
                }
            }
        }

        boolean hasPhoto = false;
        if (ivPhoto != null && photoBase64 != null && !photoBase64.trim().isEmpty()) {
            try {
                byte[] decoded = Base64.decode(photoBase64, Base64.DEFAULT | Base64.NO_WRAP);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bmp != null) {
                    ivPhoto.setImageBitmap(bmp);
                    ivPhoto.setVisibility(View.VISIBLE);
                    hasPhoto = true;
                }
            } catch (Exception ignored) {}
        }

        if (!hasPhoto && ivPhoto != null) {
            ivPhoto.setVisibility(View.GONE);
        }

        if (btnPlayVideo != null && hasVideo && videoUri != null && !videoUri.trim().isEmpty()) {
            final String finalVideoUri = videoUri;
            btnPlayVideo.setVisibility(View.VISIBLE);
            btnPlayVideo.setOnClickListener(v -> {
                try {
                    android.widget.VideoView videoView = new android.widget.VideoView(this);
                    videoView.setVideoURI(Uri.parse(finalVideoUri));
                    android.widget.MediaController mc = new android.widget.MediaController(this);
                    mc.setAnchorView(videoView);
                    videoView.setMediaController(mc);
                    videoView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT, 650));

                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Captured Video Evidence")
                            .setView(videoView)
                            .setPositiveButton("Close", (d, w) -> {
                                videoView.stopPlayback();
                                d.dismiss();
                            })
                            .setOnDismissListener(d -> videoView.stopPlayback())
                            .show();
                    videoView.start();
                } catch (Exception e) {
                    try {
                        Intent videoIntent = new Intent(Intent.ACTION_VIEW);
                        videoIntent.setDataAndType(Uri.parse(finalVideoUri), "video/*");
                        videoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(videoIntent);
                    } catch (Exception ex) {
                        Toast.makeText(this, "Unable to play video: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (tvNoMedia != null && !hasPhoto && (!hasVideo || videoUri == null || videoUri.trim().isEmpty())) {
            tvNoMedia.setVisibility(View.VISIBLE);
        }
    }

    private void applyStatus(String newStatus) {
        if (incidentRef != null) {
            sessionManager.updateLocalReportStatus(incidentRef, newStatus);
            ValenciaGlobalIncidentStore.updateStatus(incidentRef, newStatus);
        }
        if (tvCurrentStatus != null) {
            tvCurrentStatus.setText(newStatus);
        }

        try {
            org.json.JSONObject notif = new org.json.JSONObject();
            notif.put("title", "Incident #" + incidentRef + " -> " + newStatus);
            notif.put("message", "Responder updated status to " + newStatus);
            notif.put("timestamp", new java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date()));
            sessionManager.saveAlertNotification(notif);
            ValenciaGlobalIncidentStore.addNotification(notif);
            com.example.accidentreportandmonitoringsystem.notifications.ArmsNotificationHelper.showEmergencyNotification(
                    this,
                    "Incident #" + incidentRef + " Updated",
                    "Status is now: " + newStatus
            );
        } catch (Exception ignored) {}

        String token = sessionManager.getAuthToken();
        String assignmentId = getIntent().getStringExtra("assignment_id");
        if (assignmentId != null && !assignmentId.isEmpty() && token != null
                && !token.startsWith("arms_instant_token_") && !token.startsWith("arms_fallback_token_")) {
            String apiStatus = "RESPONDING".equals(newStatus) ? "EN_ROUTE" : newStatus;
            new com.example.accidentreportandmonitoringsystem.data.repository.IncidentRepository()
                    .updateResponderStatus(assignmentId, apiStatus, null, token,
                            new com.example.accidentreportandmonitoringsystem.remote.ApiClient.ApiCallback() {
                                @Override
                                public void onSuccess(org.json.JSONObject response) {
                                    Toast.makeText(ResponderStatusUpdateActivity.this, "Synced status: " + newStatus, Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(int statusCode, String errorMessage) {
                                    Toast.makeText(ResponderStatusUpdateActivity.this, "Saved locally (API: " + errorMessage + ")", Toast.LENGTH_SHORT).show();
                                }
                            });
        } else {
            Toast.makeText(this, "Status updated to: " + newStatus, Toast.LENGTH_SHORT).show();
        }
    }
}
