package com.example.accidentreportandmonitoringsystem;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.accidentreportandmonitoringsystem.fragments.CitizenAlertsFragment;
import com.example.accidentreportandmonitoringsystem.fragments.CitizenHomeFragment;
import com.example.accidentreportandmonitoringsystem.fragments.CitizenReportsFragment;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalIncidentStore;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class CitizenDashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private LinearLayout navHome, navReports, navNotifications, navProfile;
    private FrameLayout fabCameraReport;
    private TextView tvHeaderAlertBadge;
    private TextView tvUserGreeting;
    private ImageButton ibHeaderBell;
    private FrameLayout bgHome, bgReports, bgNotifications, bgProfile;
    private ImageView ivHome, ivReports, ivNotifications, ivProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_dashboard);

        sessionManager = new SessionManager(this);

        navHome = findViewById(R.id.navHome);
        navReports = findViewById(R.id.navReports);
        navNotifications = findViewById(R.id.navNotifications);
        navProfile = findViewById(R.id.navProfile);
        fabCameraReport = findViewById(R.id.fabCameraReport);
        tvHeaderAlertBadge = findViewById(R.id.tvHeaderAlertBadge);
        tvUserGreeting = findViewById(R.id.tvUserGreeting);
        ibHeaderBell = findViewById(R.id.ibHeaderBell);

        bgHome = findViewById(R.id.bgHome);
        bgReports = findViewById(R.id.bgReports);
        bgNotifications = findViewById(R.id.bgNotifications);
        bgProfile = findViewById(R.id.bgProfile);

        ivHome = findViewById(R.id.ivHome);
        ivReports = findViewById(R.id.ivReports);
        ivNotifications = findViewById(R.id.ivNotifications);
        ivProfile = findViewById(R.id.ivProfile);

        if (tvUserGreeting != null) {
            tvUserGreeting.setText("Hello, " + sessionManager.getUserName());
        }

        setupBottomNavigation();
        setupHeaderBell();

        if (savedInstanceState == null) {
            loadFragment(new CitizenHomeFragment());
            updateActiveTab(0);
        }

        loadHeaderAlertBadge();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHeaderAlertBadge();
    }

    private void loadHeaderAlertBadge() {
        if (tvHeaderAlertBadge == null) return;
        boolean hasRead = sessionManager.hasReadAlerts();
        tvHeaderAlertBadge.setVisibility(hasRead ? View.GONE : View.VISIBLE);
    }

    private void setupHeaderBell() {
        if (ibHeaderBell != null) {
            ibHeaderBell.setImageResource(R.drawable.ic_bell);
            ibHeaderBell.setOnClickListener(v -> {
                sessionManager.saveHasReadAlerts(true);
                loadHeaderAlertBadge();
                showNotificationsPopover();
            });
        }
    }

    private void showNotificationsPopover() {
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
                JSONArray localArr = sessionManager.getAlertNotifications();
                JSONArray globalArr = ValenciaGlobalIncidentStore.getNotifications();

                JSONArray combinedToday = new JSONArray();
                JSONArray combinedYesterday = new JSONArray();
                Set<String> seenMsgs = new HashSet<>();

                String todayStr = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());

                if (globalArr != null) {
                    for (int i = 0; i < globalArr.length(); i++) {
                        JSONObject obj = globalArr.getJSONObject(i);
                        String msg = obj.optString("message", "");
                        String time = obj.optString("timestamp", todayStr);
                        if (!msg.isEmpty() && !seenMsgs.contains(msg)) {
                            seenMsgs.add(msg);
                            if (time.toLowerCase().contains("yesterday")) {
                                combinedYesterday.put(obj);
                            } else {
                                combinedToday.put(obj);
                            }
                        }
                    }
                }
                if (localArr != null) {
                    for (int i = 0; i < localArr.length(); i++) {
                        JSONObject obj = localArr.getJSONObject(i);
                        String msg = obj.optString("message", "");
                        String time = obj.optString("timestamp", todayStr);
                        if (!msg.isEmpty() && !seenMsgs.contains(msg)) {
                            seenMsgs.add(msg);
                            if (time.toLowerCase().contains("yesterday")) {
                                combinedYesterday.put(obj);
                            } else {
                                combinedToday.put(obj);
                            }
                        }
                    }
                }

                if (combinedToday.length() == 0 && combinedYesterday.length() == 0) {
                    TextView tvEmpty = new TextView(this);
                    tvEmpty.setText("All systems secure. No new emergency notifications from BFP, CDRRMO, PNP, or Medical at this time.");
                    tvEmpty.setTextColor(getResources().getColor(R.color.arms_secondary_text, null));
                    tvEmpty.setPadding(12, 16, 12, 16);
                    tvEmpty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                    containerPopover.addView(tvEmpty);
                } else {
                    if (combinedToday.length() > 0) {
                        TextView tvTodayHeader = new TextView(this);
                        tvTodayHeader.setText("── TODAY ──");
                        tvTodayHeader.setTextColor(Color.parseColor("#2563EB"));
                        tvTodayHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                        tvTodayHeader.setPadding(4, 6, 4, 4);
                        tvTodayHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                        containerPopover.addView(tvTodayHeader);

                        for (int i = 0; i < combinedToday.length(); i++) {
                            JSONObject obj = combinedToday.getJSONObject(i);
                            addNotificationCard(containerPopover, obj);
                        }
                    }

                    if (combinedYesterday.length() > 0) {
                        TextView tvYestHeader = new TextView(this);
                        tvYestHeader.setText("── YESTERDAY / EARLIER ──");
                        tvYestHeader.setTextColor(Color.parseColor("#64748B"));
                        tvYestHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                        tvYestHeader.setPadding(4, 12, 4, 4);
                        tvYestHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                        containerPopover.addView(tvYestHeader);

                        for (int i = 0; i < combinedYesterday.length(); i++) {
                            JSONObject obj = combinedYesterday.getJSONObject(i);
                            addNotificationCard(containerPopover, obj);
                        }
                    }
                }
            } catch (Exception e) {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("All systems secure.");
                containerPopover.addView(tvEmpty);
            }
        }

        if (tvClearAll != null) {
            tvClearAll.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void addNotificationCard(LinearLayout container, JSONObject obj) {
        try {
            String title = obj.optString("title", "Update");
            String msg = obj.optString("message", "");
            String time = obj.optString("timestamp", "Today");

            View cardView = LayoutInflater.from(this).inflate(R.layout.item_recent_report, container, false);
            TextView tvRef = cardView.findViewById(R.id.tvReportRef);
            TextView tvStatus = cardView.findViewById(R.id.tvReportStatus);
            TextView tvType = cardView.findViewById(R.id.tvReportType);
            TextView tvLocation = cardView.findViewById(R.id.tvReportLocation);

            if (tvRef != null) tvRef.setText(time);
            if (tvStatus != null) {
                tvStatus.setText("NOTIF");
                tvStatus.setBackgroundColor(Color.parseColor("#E3F2FD"));
                tvStatus.setTextColor(Color.parseColor("#2563EB"));
            }
            if (tvType != null) tvType.setText(title);
            if (tvLocation != null) tvLocation.setText(msg);

            container.addView(cardView);
        } catch (Exception ignored) {}
    }

    private void setupBottomNavigation() {
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                loadFragment(new CitizenHomeFragment());
                updateActiveTab(0);
            });
        }

        if (navReports != null) {
            navReports.setOnClickListener(v -> {
                loadFragment(new CitizenReportsFragment());
                updateActiveTab(1);
            });
        }

        if (fabCameraReport != null) {
            fabCameraReport.setOnClickListener(v -> {
                Intent intent = new Intent(CitizenDashboardActivity.this, GuestReportActivity.class);
                startActivity(intent);
            });
        }

        if (navNotifications != null) {
            navNotifications.setOnClickListener(v -> {
                sessionManager.saveHasReadAlerts(true);
                loadHeaderAlertBadge();
                loadFragment(new CitizenAlertsFragment());
                updateActiveTab(2);
            });
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                Intent intent = new Intent(CitizenDashboardActivity.this, CitizenProfileActivity.class);
                startActivity(intent);
            });
        }
    }

    private void updateActiveTab(int tabIndex) {
        int activeBg = R.drawable.bg_nav_active_circle;
        int transparentBg = android.R.color.transparent;
        int activeTint = Color.parseColor("#142B4A"); // arms_navy
        int whiteTint = Color.parseColor("#FFFFFF");

        if (bgHome != null) bgHome.setBackgroundResource(tabIndex == 0 ? activeBg : transparentBg);
        if (ivHome != null) ivHome.setColorFilter(tabIndex == 0 ? activeTint : whiteTint);

        if (bgReports != null) bgReports.setBackgroundResource(tabIndex == 1 ? activeBg : transparentBg);
        if (ivReports != null) ivReports.setColorFilter(tabIndex == 1 ? activeTint : whiteTint);

        if (bgNotifications != null) bgNotifications.setBackgroundResource(tabIndex == 2 ? activeBg : transparentBg);
        if (ivNotifications != null) ivNotifications.setColorFilter(tabIndex == 2 ? activeTint : whiteTint);

        if (bgProfile != null) bgProfile.setBackgroundResource(tabIndex == 3 ? activeBg : transparentBg);
        if (ivProfile != null) ivProfile.setColorFilter(tabIndex == 3 ? activeTint : whiteTint);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit();
    }
}
