package com.example.accidentreportandmonitoringsystem;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.accidentreportandmonitoringsystem.fragments.ResponderAlertsFragment;
import com.example.accidentreportandmonitoringsystem.fragments.ResponderHistoryFragment;
import com.example.accidentreportandmonitoringsystem.fragments.ResponderHomeFragment;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalIncidentStore;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.json.JSONArray;
import org.json.JSONObject;

public class ResponderDashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private ImageButton ibResponderHeaderBell;
    private TextView tvResponderHeaderAlertBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bfp_dashboard);

        sessionManager = new SessionManager(this);

        ibResponderHeaderBell = findViewById(R.id.ibResponderHeaderBell);
        tvResponderHeaderAlertBadge = findViewById(R.id.tvResponderHeaderAlertBadge);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        setupResponderHeaderBell();

        if (savedInstanceState == null) {
            loadFragment(new ResponderHomeFragment());
        }

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    loadFragment(new ResponderHomeFragment());
                    return true;
                } else if (itemId == R.id.nav_history) {
                    loadFragment(new ResponderHistoryFragment());
                    return true;
                } else if (itemId == R.id.nav_notifications) {
                    loadFragment(new ResponderAlertsFragment());
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    Intent intent = new Intent(this, CitizenProfileActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
        loadFragment(new ResponderHomeFragment());
        loadResponderHeaderBadge();
    }

    private void loadResponderHeaderBadge() {
        if (tvResponderHeaderAlertBadge == null) return;
        try {
            JSONArray reports = ValenciaGlobalIncidentStore.getReports();
            int count = reports != null ? reports.length() : 0;
            if (count > 0) {
                tvResponderHeaderAlertBadge.setText(String.valueOf(count));
                tvResponderHeaderAlertBadge.setVisibility(View.VISIBLE);
            } else {
                tvResponderHeaderAlertBadge.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            tvResponderHeaderAlertBadge.setVisibility(View.GONE);
        }
    }

    private void setupResponderHeaderBell() {
        if (ibResponderHeaderBell != null) {
            ibResponderHeaderBell.setOnClickListener(v -> {
                if (tvResponderHeaderAlertBadge != null) tvResponderHeaderAlertBadge.setVisibility(View.GONE);
                showResponderNotificationsPopover();
            });
        }
    }

    private void showResponderNotificationsPopover() {
        View popoverView = LayoutInflater.from(this).inflate(R.layout.dialog_citizen_notification_popover, null);
        LinearLayout containerPopover = popoverView.findViewById(R.id.containerPopoverNotifications);
        TextView tvClearAll = popoverView.findViewById(R.id.tvClearAll);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(popoverView)
            .setCancelable(true)
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setGravity(android.view.Gravity.TOP | android.view.Gravity.END);
            dialog.getWindow().getAttributes().y = 120;
            dialog.getWindow().getAttributes().x = 16;
        }

        if (containerPopover != null) {
            containerPopover.removeAllViews();
            try {
                JSONArray reports = ValenciaGlobalIncidentStore.getReports();
                if (reports == null || reports.length() == 0) {
                    TextView tvEmpty = new TextView(this);
                    tvEmpty.setText("No new citizen emergency reports received yet.");
                    tvEmpty.setTextColor(getResources().getColor(R.color.arms_secondary_text, null));
                    tvEmpty.setPadding(12, 16, 12, 16);
                    tvEmpty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                    containerPopover.addView(tvEmpty);
                } else {
                    for (int i = 0; i < reports.length(); i++) {
                        JSONObject obj = reports.getJSONObject(i);
                        String ref = obj.optString("incident_reference", "ARMS-VAL");
                        String type = obj.optString("incident_type", "EMERGENCY");
                        String address = obj.optString("landmark_address", "Valencia City");
                        String date = obj.optString("created_at", "Today");

                        View cardView = LayoutInflater.from(this).inflate(R.layout.item_recent_report, containerPopover, false);
                        TextView tvRef = cardView.findViewById(R.id.tvReportRef);
                        TextView tvStatus = cardView.findViewById(R.id.tvReportStatus);
                        TextView tvType = cardView.findViewById(R.id.tvReportType);
                        TextView tvLocation = cardView.findViewById(R.id.tvReportLocation);

                        if (tvRef != null) tvRef.setText(date);
                        if (tvStatus != null) {
                            tvStatus.setText("NEW DISPATCH");
                            tvStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                            tvStatus.setTextColor(Color.parseColor("#DC2626"));
                        }
                        if (tvType != null) tvType.setText("🚨 New " + type.replace("_", " ") + " Report (#" + ref + ")");
                        if (tvLocation != null) tvLocation.setText(address);

                        containerPopover.addView(cardView);
                    }
                }
            } catch (Exception e) {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("No new reports.");
                containerPopover.addView(tvEmpty);
            }
        }

        if (tvClearAll != null) {
            tvClearAll.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit();
    }
}
