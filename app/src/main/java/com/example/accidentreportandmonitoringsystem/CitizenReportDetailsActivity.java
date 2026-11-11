package com.example.accidentreportandmonitoringsystem;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalIncidentStore;
import org.json.JSONArray;
import org.json.JSONObject;

public class CitizenReportDetailsActivity extends AppCompatActivity {

    private TextView tvDetailStatus;
    private ImageView ivStepAccept, ivStepEnRoute, ivStepOnScene, ivStepResolved;
    private TextView tvStepAccept, tvStepEnRoute, tvStepOnScene, tvStepResolved;
    private View lineAcceptToEnRoute, lineEnRouteToOnScene, lineOnSceneToResolved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_report_details);

        ImageButton ibBack = findViewById(R.id.ibBack);
        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }

        String reference = getIntent().getStringExtra("incident_ref");
        String incidentType = getIntent().getStringExtra("incident_type");
        String status = getIntent().getStringExtra("status");
        String location = getIntent().getStringExtra("location");
        String description = getIntent().getStringExtra("description");

        if (incidentType == null || incidentType.isEmpty()) incidentType = "EMERGENCY";
        if (reference == null || reference.isEmpty()) reference = "ARMS-VAL";
        if (status == null || status.isEmpty()) status = "DISPATCHED";
        if (location == null || location.isEmpty()) location = "Valencia City";

        // Bind text details immediately on main thread for instant zero-lag response
        bindTextDetails(reference, incidentType, status, location, description);

        // Load photo evidence asynchronously on background thread
        loadEvidencePhotoAsync(reference);
    }

    private void bindTextDetails(String reference, String incidentType, String status, String location, String description) {
        TextView tvDetailTitle = findViewById(R.id.tvDetailTitle);
        TextView tvDetailTimestamp = findViewById(R.id.tvDetailTimestamp);
        tvDetailStatus = findViewById(R.id.tvDetailStatus);
        TextView tvDetailLocation = findViewById(R.id.tvDetailLocation);

        ivStepAccept = findViewById(R.id.ivStepAccept);
        tvStepAccept = findViewById(R.id.tvStepAccept);
        lineAcceptToEnRoute = findViewById(R.id.lineAcceptToEnRoute);

        ivStepEnRoute = findViewById(R.id.ivStepEnRoute);
        tvStepEnRoute = findViewById(R.id.tvStepEnRoute);
        lineEnRouteToOnScene = findViewById(R.id.lineEnRouteToOnScene);

        ivStepOnScene = findViewById(R.id.ivStepOnScene);
        tvStepOnScene = findViewById(R.id.tvStepOnScene);
        lineOnSceneToResolved = findViewById(R.id.lineOnSceneToResolved);

        ivStepResolved = findViewById(R.id.ivStepResolved);
        tvStepResolved = findViewById(R.id.tvStepResolved);

        if (tvDetailTitle != null) {
            tvDetailTitle.setText(incidentType.replace("_", " "));
        }
        if (reference != null && tvDetailTimestamp != null) {
            tvDetailTimestamp.setText("Ref: #" + reference);
        }

        if (tvDetailLocation != null) {
            String shortLocation = location;
            if (shortLocation.contains("(")) {
                shortLocation = shortLocation.substring(0, shortLocation.indexOf("(")).trim();
            }
            if (shortLocation.startsWith("Emergency Incident at ")) {
                shortLocation = shortLocation.replace("Emergency Incident at ", "").trim();
            }
            if (shortLocation.length() > 60) {
                shortLocation = shortLocation.substring(0, 57) + "...";
            }
            tvDetailLocation.setText(shortLocation);
        }

        updateSequentialTimeline(status);
    }

    private void loadEvidencePhotoAsync(String reference) {
        final ImageView ivEvidencePhoto = findViewById(R.id.ivEvidencePhoto);
        final View cardEvidenceCard = findViewById(R.id.cardEvidenceCard);

        if (ivEvidencePhoto != null) ivEvidencePhoto.setVisibility(View.GONE);
        if (cardEvidenceCard != null) cardEvidenceCard.setVisibility(View.GONE);

        if (reference == null || reference.trim().isEmpty()) return;

        new Thread(() -> {
            try {
                String photoBase64 = "";
                SessionManager sm = new SessionManager(this);
                JSONArray reports = sm.getLocalReports();
                for (int i = 0; i < reports.length(); i++) {
                    JSONObject rep = reports.getJSONObject(i);
                    if (rep.optString("incident_reference", "").equals(reference)) {
                        photoBase64 = rep.optString("photo_base64", "");
                        break;
                    }
                }

                if (photoBase64.isEmpty()) {
                    JSONArray globalReports = ValenciaGlobalIncidentStore.getReports();
                    for (int i = 0; i < globalReports.length(); i++) {
                        JSONObject rep = globalReports.getJSONObject(i);
                        if (rep.optString("incident_reference", "").equals(reference)) {
                            photoBase64 = rep.optString("photo_base64", "");
                            break;
                        }
                    }
                }

                if (!photoBase64.isEmpty()) {
                    byte[] decodedBytes = Base64.decode(photoBase64, Base64.DEFAULT | Base64.NO_WRAP);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    if (bitmap != null) {
                        runOnUiThread(() -> {
                            if (ivEvidencePhoto != null) {
                                ivEvidencePhoto.setImageBitmap(bitmap);
                                ivEvidencePhoto.setVisibility(View.VISIBLE);
                            }
                            if (cardEvidenceCard != null) {
                                cardEvidenceCard.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void updateSequentialTimeline(String status) {
        if (tvDetailStatus != null) {
            tvDetailStatus.setText(status);
        }
        String upper = status.toUpperCase();

        int green = Color.parseColor("#2E9D68");
        int blue = Color.parseColor("#2563EB");
        int red = Color.parseColor("#DC2626");
        int gray = Color.parseColor("#64748B");

        if (upper.contains("RESOLVED")) {
            if (tvDetailStatus != null) {
                tvDetailStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                tvDetailStatus.setTextColor(green);
            }
            activateStep(ivStepAccept, tvStepAccept, lineAcceptToEnRoute, green);
            activateStep(ivStepEnRoute, tvStepEnRoute, lineEnRouteToOnScene, green);
            activateStep(ivStepOnScene, tvStepOnScene, lineOnSceneToResolved, green);
            activateStep(ivStepResolved, tvStepResolved, null, green);
        } else if (upper.contains("ON SCENE")) {
            if (tvDetailStatus != null) {
                tvDetailStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                tvDetailStatus.setTextColor(red);
            }
            activateStep(ivStepAccept, tvStepAccept, lineAcceptToEnRoute, green);
            activateStep(ivStepEnRoute, tvStepEnRoute, lineEnRouteToOnScene, green);
            activateStep(ivStepOnScene, tvStepOnScene, lineOnSceneToResolved, green);
            deactivateStep(ivStepResolved, tvStepResolved, gray);
        } else if (upper.contains("EN ROUTE")) {
            if (tvDetailStatus != null) {
                tvDetailStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                tvDetailStatus.setTextColor(red);
            }
            activateStep(ivStepAccept, tvStepAccept, lineAcceptToEnRoute, green);
            activateStep(ivStepEnRoute, tvStepEnRoute, lineEnRouteToOnScene, green);
            deactivateStep(ivStepOnScene, tvStepOnScene, gray);
            deactivateStep(ivStepResolved, tvStepResolved, gray);
            if (lineOnSceneToResolved != null) lineOnSceneToResolved.setBackgroundColor(Color.parseColor("#E2E8F0"));
        } else if (upper.contains("ACCEPTED") || upper.contains("DISPATCHED")) {
            if (tvDetailStatus != null) {
                tvDetailStatus.setBackgroundColor(Color.parseColor("#E3F2FD"));
                tvDetailStatus.setTextColor(blue);
            }
            activateStep(ivStepAccept, tvStepAccept, lineAcceptToEnRoute, blue);
            deactivateStep(ivStepEnRoute, tvStepEnRoute, gray);
            deactivateStep(ivStepOnScene, tvStepOnScene, gray);
            deactivateStep(ivStepResolved, tvStepResolved, gray);
            if (lineEnRouteToOnScene != null) lineEnRouteToOnScene.setBackgroundColor(Color.parseColor("#E2E8F0"));
            if (lineOnSceneToResolved != null) lineOnSceneToResolved.setBackgroundColor(Color.parseColor("#E2E8F0"));
        }
    }

    private void activateStep(ImageView iv, TextView tv, View line, int color) {
        if (iv != null) iv.setColorFilter(color);
        if (tv != null) tv.setTextColor(color);
        if (line != null) line.setBackgroundColor(color);
    }

    private void deactivateStep(ImageView iv, TextView tv, int color) {
        if (iv != null) iv.setColorFilter(color);
        if (tv != null) tv.setTextColor(color);
    }
}
