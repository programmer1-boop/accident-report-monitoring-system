package com.example.accidentreportandmonitoringsystem;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.json.JSONArray;
import org.json.JSONObject;

public class CitizenReportsActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private LinearLayout containerReports;
    private TextView tabAll, tabActive, tabResolved;
    private String currentFilter = "ALL"; // ALL, ACTIVE, RESOLVED

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_reports);

        sessionManager = new SessionManager(this);
        containerReports = findViewById(R.id.containerReports);
        tabAll = findViewById(R.id.tabAll);
        tabActive = findViewById(R.id.tabActive);
        tabResolved = findViewById(R.id.tabResolved);

        setupFilterTabs();
        setupBottomNavigation();
        loadPersonalReports(currentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPersonalReports(currentFilter);
    }

    private void setupFilterTabs() {
        if (tabAll != null) {
            tabAll.setOnClickListener(v -> {
                currentFilter = "ALL";
                updateTabStyles();
                loadPersonalReports(currentFilter);
            });
        }
        if (tabActive != null) {
            tabActive.setOnClickListener(v -> {
                currentFilter = "ACTIVE";
                updateTabStyles();
                loadPersonalReports(currentFilter);
            });
        }
        if (tabResolved != null) {
            tabResolved.setOnClickListener(v -> {
                currentFilter = "RESOLVED";
                updateTabStyles();
                loadPersonalReports(currentFilter);
            });
        }
    }

    private void updateTabStyles() {
        int activeBg = R.drawable.bg_primary_button;
        int activeTextColor = Color.parseColor("#FFFFFF");
        int inactiveTextColor = Color.parseColor("#64748B");

        if (tabAll != null) {
            tabAll.setBackgroundResource(currentFilter.equals("ALL") ? activeBg : 0);
            tabAll.setTextColor(currentFilter.equals("ALL") ? activeTextColor : inactiveTextColor);
        }
        if (tabActive != null) {
            tabActive.setBackgroundResource(currentFilter.equals("ACTIVE") ? activeBg : 0);
            tabActive.setTextColor(currentFilter.equals("ACTIVE") ? activeTextColor : inactiveTextColor);
        }
        if (tabResolved != null) {
            tabResolved.setBackgroundResource(currentFilter.equals("RESOLVED") ? activeBg : 0);
            tabResolved.setTextColor(currentFilter.equals("RESOLVED") ? activeTextColor : inactiveTextColor);
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_my_reports);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    Intent intent = new Intent(CitizenReportsActivity.this, CitizenDashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_my_reports) {
                    return true;
                } else if (itemId == R.id.nav_camera) {
                    Intent intent = new Intent(CitizenReportsActivity.this, GuestReportActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_notifications) {
                    Intent intent = new Intent(CitizenReportsActivity.this, CitizenAlertsActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    Intent intent = new Intent(CitizenReportsActivity.this, CitizenProfileActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }
    }

    private void loadPersonalReports(String filter) {
        if (containerReports == null) return;

        try {
            containerReports.removeAllViews();
            JSONArray reports = sessionManager.getLocalReports();

            JSONArray filteredReports = new JSONArray();
            if (reports != null) {
                for (int i = 0; i < reports.length(); i++) {
                    JSONObject rep = reports.getJSONObject(i);
                    String status = rep.optString("current_status", "SUBMITTED").toUpperCase();

                    boolean isResolved = status.contains("RESOLVED") || status.contains("CLOSED");
                    boolean isActive = !isResolved;

                    if (filter.equals("ALL")) {
                        filteredReports.put(rep);
                    } else if (filter.equals("ACTIVE") && isActive) {
                        filteredReports.put(rep);
                    } else if (filter.equals("RESOLVED") && isResolved) {
                        filteredReports.put(rep);
                    }
                }
            }

            if (filteredReports.length() == 0) {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("No personal reports found yet. Submit an emergency report to monitor its status here.");
                tvEmpty.setTextColor(getResources().getColor(R.color.arms_secondary_text, null));
                tvEmpty.setPadding(24, 48, 24, 48);
                containerReports.addView(tvEmpty);
                return;
            }

            for (int i = 0; i < filteredReports.length(); i++) {
                JSONObject rep = filteredReports.getJSONObject(i);
                String ref = rep.optString("incident_reference", "ARMS-VAL");
                String type = rep.optString("incident_type", "EMERGENCY");
                String status = rep.optString("current_status", "SUBMITTED");
                String address = rep.optString("landmark_address", "Valencia City");
                String desc = rep.optString("description", "");
                String date = rep.optString("created_at", "Just now");
                String photoBase64 = rep.optString("photo_base64", "");
                boolean hasVideo = rep.optBoolean("has_video", false);
                String videoUri = rep.optString("video_uri", "");

                View cardView = LayoutInflater.from(this).inflate(R.layout.item_recent_report, containerReports, false);

                TextView tvRef = cardView.findViewById(R.id.tvReportRef);
                TextView tvStatus = cardView.findViewById(R.id.tvReportStatus);
                TextView tvLocation = cardView.findViewById(R.id.tvReportLocation);
                TextView tvTime = cardView.findViewById(R.id.tvReportTime);
                ImageView ivThumb = cardView.findViewById(R.id.ivReportThumb);

                if (tvRef != null) tvRef.setText("#" + ref);
                if (tvStatus != null) {
                    String displayStatus = status;
                    if (displayStatus.toUpperCase().contains("DISPATCHED")) {
                        displayStatus = "DISPATCHED";
                    }
                    tvStatus.setText("● " + displayStatus);
                    if (status.equalsIgnoreCase("RESOLVED")) {
                        tvStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                        tvStatus.setTextColor(Color.parseColor("#2E9D68"));
                    } else {
                        tvStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                        tvStatus.setTextColor(Color.parseColor("#DC2626"));
                    }
                }
                if (tvLocation != null) tvLocation.setText("📍 " + address);
                if (tvTime != null) tvTime.setText("🕒 " + date);

                if (ivThumb != null) {
                    try {
                        if (photoBase64 != null && !photoBase64.trim().isEmpty()) {
                            byte[] decoded = Base64.decode(photoBase64, Base64.NO_WRAP);
                            Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                            if (bmp != null) {
                                ivThumb.setImageBitmap(bmp);
                            }
                        } else {
                            Bitmap bmp = Bitmap.createBitmap(200, 150, Bitmap.Config.ARGB_8888);
                            android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
                            canvas.drawColor(android.graphics.Color.parseColor("#142B4A"));
                            android.graphics.Paint paint = new android.graphics.Paint();
                            paint.setColor(android.graphics.Color.WHITE);
                            paint.setTextSize(24f);
                            canvas.drawText("ARMS", 20f, 85f, paint);
                            ivThumb.setImageBitmap(bmp);
                        }
                    } catch (Exception ignored) {}
                }

                cardView.setOnClickListener(v -> {
                    Intent intent = new Intent(this, CitizenReportDetailsActivity.class);
                    intent.putExtra("incident_type", type);
                    intent.putExtra("incident_ref", ref);
                    intent.putExtra("status", status);
                    intent.putExtra("location", address);
                    intent.putExtra("description", desc);
                    intent.putExtra("photo_base64", photoBase64);
                    intent.putExtra("has_video", hasVideo);
                    intent.putExtra("video_uri", videoUri);
                    startActivity(intent);
                });

                containerReports.addView(cardView);
            }
        } catch (Exception ignored) {}
    }
}
