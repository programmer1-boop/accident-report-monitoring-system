package com.example.accidentreportandmonitoringsystem;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.example.accidentreportandmonitoringsystem.fragments.CitizenAlertsFragment;
import com.example.accidentreportandmonitoringsystem.fragments.CitizenHomeFragment;
import com.example.accidentreportandmonitoringsystem.fragments.CitizenProfileFragment;
import com.example.accidentreportandmonitoringsystem.fragments.CitizenReportsFragment;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;

public class CitizenDashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private LinearLayout navHome, navReports, navNotifications, navProfile;
    private FrameLayout fabCameraReport;
    private TextView tvUserGreeting, tvHeaderSubtitle;

    private FrameLayout bgHome, bgReports, bgNotifications, bgProfile;
    private ImageView ivHome, ivReports, ivNotifications, ivProfile;

    private final Fragment homeFragment = new CitizenHomeFragment();
    private final Fragment reportsFragment = new CitizenReportsFragment();
    private final Fragment alertsFragment = new CitizenAlertsFragment();
    private final Fragment profileFragment = new CitizenProfileFragment();
    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment activeFragment = homeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.parseColor("#F8FAFC")));
        setContentView(R.layout.activity_citizen_dashboard);

        sessionManager = new SessionManager(this);

        navHome = findViewById(R.id.navHome);
        navReports = findViewById(R.id.navReports);
        navNotifications = findViewById(R.id.navNotifications);
        navProfile = findViewById(R.id.navProfile);
        fabCameraReport = findViewById(R.id.fabCameraReport);
        tvUserGreeting = findViewById(R.id.tvUserGreeting);
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);

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

        if (savedInstanceState == null) {
            fm.beginTransaction().add(R.id.fragmentContainer, profileFragment, "4").hide(profileFragment).commit();
            fm.beginTransaction().add(R.id.fragmentContainer, alertsFragment, "3").hide(alertsFragment).commit();
            fm.beginTransaction().add(R.id.fragmentContainer, reportsFragment, "2").hide(reportsFragment).commit();
            fm.beginTransaction().add(R.id.fragmentContainer, homeFragment, "1").commit();
            activeFragment = homeFragment;
            updateActiveTab(0);
        }
    }

    private void setupBottomNavigation() {
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                switchFragment(homeFragment);
                updateActiveTab(0);
            });
        }

        if (navReports != null) {
            navReports.setOnClickListener(v -> {
                switchFragment(reportsFragment);
                updateActiveTab(1);
            });
        }

        if (navNotifications != null) {
            navNotifications.setOnClickListener(v -> {
                switchFragment(alertsFragment);
                updateActiveTab(2);
            });
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                switchFragment(profileFragment);
                updateActiveTab(3);
            });
        }

        if (fabCameraReport != null) {
            fabCameraReport.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(CitizenDashboardActivity.this, CameraCaptureActivity.class);
                startActivity(intent);
            });
        }
    }

    private void switchFragment(Fragment target) {
        if (activeFragment != target) {
            fm.beginTransaction().hide(activeFragment).show(target).commit();
            activeFragment = target;
        }
    }

    private void updateActiveTab(int tabIndex) {
        int activeBg = R.drawable.bg_nav_active_circle;
        int transparentBg = android.R.color.transparent;
        int whiteTint = Color.parseColor("#FFFFFF");

        if (bgHome != null) bgHome.setBackgroundResource(tabIndex == 0 ? activeBg : transparentBg);
        if (ivHome != null) ivHome.setColorFilter(tabIndex == 0 ? Color.parseColor("#142B4A") : whiteTint);

        if (bgReports != null) bgReports.setBackgroundResource(tabIndex == 1 ? activeBg : transparentBg);
        if (ivReports != null) ivReports.setColorFilter(tabIndex == 1 ? Color.parseColor("#142B4A") : whiteTint);

        if (bgNotifications != null) bgNotifications.setBackgroundResource(tabIndex == 2 ? activeBg : transparentBg);
        if (ivNotifications != null) ivNotifications.setColorFilter(tabIndex == 2 ? Color.parseColor("#142B4A") : whiteTint);

        if (bgProfile != null) bgProfile.setBackgroundResource(tabIndex == 3 ? activeBg : transparentBg);
        if (ivProfile != null) ivProfile.setColorFilter(tabIndex == 3 ? Color.parseColor("#142B4A") : whiteTint);

        View headerView = findViewById(R.id.cardDashboardHeader);
        if (headerView != null) {
            headerView.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        }

        if (tvUserGreeting != null && tvHeaderSubtitle != null) {
            if (tabIndex == 0) {
                tvUserGreeting.setText("Hello, " + sessionManager.getUserName());
                tvHeaderSubtitle.setText("Stay safe and report when needed.");
            }
        }
    }
}
