package com.example.accidentreportandmonitoringsystem;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalIncidentStore;
import com.google.android.material.button.MaterialButton;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DispatcherReportReviewActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispatcher_report_review);

        sessionManager = new SessionManager(this);

        ImageButton ibBack = findViewById(R.id.ibBack);
        TextView tvReviewRefTitle = findViewById(R.id.tvReviewRefTitle);
        TextView tvReviewType = findViewById(R.id.tvReviewType);
        TextView tvReviewLocation = findViewById(R.id.tvReviewLocation);
        TextView tvReviewTime = findViewById(R.id.tvReviewTime);
        ImageView ivReviewEvidence = findViewById(R.id.ivReviewEvidence);
        MaterialButton btnReviewPlayVideo = findViewById(R.id.btnReviewPlayVideo);
        MaterialButton btnReviewMap = findViewById(R.id.btnReviewMap);
        MaterialButton btnReviewDispatch = findViewById(R.id.btnReviewDispatch);

        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }

        String ref = getIntent().getStringExtra("incident_ref");
        String type = getIntent().getStringExtra("incident_type");
        String address = getIntent().getStringExtra("location");
        String date = getIntent().getStringExtra("created_at");
        String photoBase64 = getIntent().getStringExtra("photo_base64");
        boolean hasVideo = getIntent().getBooleanExtra("has_video", false);
        String videoUri = getIntent().getStringExtra("video_uri");
        double lat = getIntent().getDoubleExtra("latitude", 7.9064);
        double lng = getIntent().getDoubleExtra("longitude", 125.0942);

        if (tvReviewRefTitle != null) tvReviewRefTitle.setText("🚨 INCIDENT REVIEW #" + ref);
        if (tvReviewType != null && type != null) tvReviewType.setText(type.replace("_", " "));
        if (tvReviewLocation != null && address != null) tvReviewLocation.setText("📍 " + address);
        if (tvReviewTime != null && date != null) tvReviewTime.setText("🕒 Reported " + date);

        if (ivReviewEvidence != null) {
            try {
                if (photoBase64 != null && !photoBase64.trim().isEmpty()) {
                    byte[] decoded = Base64.decode(photoBase64, Base64.NO_WRAP);
                    Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (bmp != null) ivReviewEvidence.setImageBitmap(bmp);
                } else {
                    Bitmap bmp = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888);
                    android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
                    canvas.drawColor(android.graphics.Color.parseColor("#142B4A"));
                    android.graphics.Paint paint = new android.graphics.Paint();
                    paint.setColor(android.graphics.Color.WHITE);
                    paint.setTextSize(32f);
                    canvas.drawText("ARMS EVIDENCE", 40f, 150f, paint);
                    ivReviewEvidence.setImageBitmap(bmp);
                }
            } catch (Exception ignored) {}
        }

        if (btnReviewPlayVideo != null) {
            if (hasVideo || (videoUri != null && !videoUri.isEmpty())) {
                btnReviewPlayVideo.setVisibility(View.VISIBLE);
                btnReviewPlayVideo.setOnClickListener(v -> {
                    try {
                        Intent playIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUri));
                        playIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(playIntent);
                    } catch (Exception e) {
                        Toast.makeText(this, "Playing video evidence...", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                btnReviewPlayVideo.setVisibility(View.GONE);
            }
        }

        if (btnReviewMap != null) {
            btnReviewMap.setOnClickListener(v -> {
                Intent intent = new Intent(DispatcherReportReviewActivity.this, IncidentLocationActivity.class);
                intent.putExtra("incident_lat", lat);
                intent.putExtra("incident_lng", lng);
                intent.putExtra("incident_address", address);
                intent.putExtra("incident_ref", ref);
                startActivity(intent);
            });
        }

        if (btnReviewDispatch != null) {
            btnReviewDispatch.setOnClickListener(v -> showSelectRespondersDialog(ref, type));
        }
    }

    private void showSelectRespondersDialog(String ref, String type) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_dispatcher_select_responders, null);
        TextView btnClose = dialogView.findViewById(R.id.btnCloseResponderDialog);
        CheckBox cbBfp = dialogView.findViewById(R.id.cbBfp);
        CheckBox cbPnp = dialogView.findViewById(R.id.cbPnp);
        CheckBox cbMedical = dialogView.findViewById(R.id.cbMedical);
        CheckBox cbCdrrmo = dialogView.findViewById(R.id.cbCdrrmo);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirmDispatchTeam);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create();

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                StringBuilder dispatchedTeams = new StringBuilder();
                if (cbBfp != null && cbBfp.isChecked()) dispatchedTeams.append("BFP, ");
                if (cbPnp != null && cbPnp.isChecked()) dispatchedTeams.append("PNP, ");
                if (cbMedical != null && cbMedical.isChecked()) dispatchedTeams.append("Medical EMS, ");
                if (cbCdrrmo != null && cbCdrrmo.isChecked()) dispatchedTeams.append("CDRRMO, ");

                String teams = dispatchedTeams.toString().trim();
                if (teams.endsWith(",")) teams = teams.substring(0, teams.length() - 1);
                if (teams.isEmpty()) teams = "BFP";

                sessionManager.updateLocalReportStatus(ref, "DISPATCHED TO " + teams.toUpperCase());
                sessionManager.updateLocalReportAssignedResponders(ref, teams);
                ValenciaGlobalIncidentStore.updateStatus(ref, "DISPATCHED TO " + teams.toUpperCase());
                ValenciaGlobalIncidentStore.updateAssignedResponders(ref, teams);

                try {
                    JSONObject alertObj = new JSONObject();
                    alertObj.put("title", "🚨 DISPATCH CENTER UPDATE: Units Dispatched");
                    alertObj.put("message", "Valencia City Dispatcher has assigned and dispatched " + teams + " to your " + (type != null ? type.replace("_", " ").toLowerCase() : "emergency") + " report (#" + ref + ").");
                    alertObj.put("timestamp", new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(new Date()));
                    sessionManager.saveAlertNotification(alertObj);
                    ValenciaGlobalIncidentStore.addNotification(alertObj);
                } catch (Exception ignored) {}

                Toast.makeText(this, "🚀 Successfully dispatched Incident #" + ref + " to: " + teams, Toast.LENGTH_LONG).show();
                dialog.dismiss();
                finish();
            });
        }

        dialog.show();
    }
}
