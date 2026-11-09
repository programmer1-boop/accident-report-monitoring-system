package com.example.accidentreportandmonitoringsystem.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.accidentreportandmonitoringsystem.R;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class CitizenAlertsFragment extends Fragment {

    private SessionManager sessionManager;
    private LinearLayout containerAlerts;
    private TextView tabAllAlerts, tabSystemAlerts, tabUpdatesAlerts;
    private String currentAlertFilter = "ALL"; // ALL, SYSTEM, UPDATES
    private final List<AlertItem> allAlertsList = new ArrayList<>();

    private static class AlertItem {
        String title;
        String message;
        String time;
        boolean isNew;
        boolean isSystem;
        boolean isRead;

        AlertItem(String title, String message, String time, boolean isNew, boolean isSystem, boolean isRead) {
            this.title = title;
            this.message = message;
            this.time = time;
            this.isNew = isNew;
            this.isSystem = isSystem;
            this.isRead = isRead;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_citizen_alerts, container, false);

        sessionManager = new SessionManager(requireContext());
        containerAlerts = view.findViewById(R.id.containerAlerts);
        tabAllAlerts = view.findViewById(R.id.tabAllAlerts);
        tabSystemAlerts = view.findViewById(R.id.tabSystemAlerts);
        tabUpdatesAlerts = view.findViewById(R.id.tabUpdatesAlerts);

        setupFilterTabs();
        loadLiveAlerts();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLiveAlerts();
    }

    private void setupFilterTabs() {
        if (tabAllAlerts != null) {
            tabAllAlerts.setOnClickListener(v -> {
                currentAlertFilter = "ALL";
                updateTabStyles();
                renderAlerts();
            });
        }
        if (tabSystemAlerts != null) {
            tabSystemAlerts.setOnClickListener(v -> {
                currentAlertFilter = "SYSTEM";
                updateTabStyles();
                renderAlerts();
            });
        }
        if (tabUpdatesAlerts != null) {
            tabUpdatesAlerts.setOnClickListener(v -> {
                currentAlertFilter = "UPDATES";
                updateTabStyles();
                renderAlerts();
            });
        }
    }

    private void updateTabStyles() {
        int activeBg = R.drawable.bg_primary_button;
        int activeTextColor = Color.parseColor("#FFFFFF");
        int inactiveTextColor = Color.parseColor("#64748B");

        if (tabAllAlerts != null) {
            tabAllAlerts.setBackgroundResource(currentAlertFilter.equals("ALL") ? activeBg : 0);
            tabAllAlerts.setTextColor(currentAlertFilter.equals("ALL") ? activeTextColor : inactiveTextColor);
        }
        if (tabSystemAlerts != null) {
            tabSystemAlerts.setBackgroundResource(currentAlertFilter.equals("SYSTEM") ? activeBg : 0);
            tabSystemAlerts.setTextColor(currentAlertFilter.equals("SYSTEM") ? activeTextColor : inactiveTextColor);
        }
        if (tabUpdatesAlerts != null) {
            tabUpdatesAlerts.setBackgroundResource(currentAlertFilter.equals("UPDATES") ? activeBg : 0);
            tabUpdatesAlerts.setTextColor(currentAlertFilter.equals("UPDATES") ? activeTextColor : inactiveTextColor);
        }
    }

    private void loadLiveAlerts() {
        allAlertsList.clear();

        // ALERTS TAB: Strictly system-wide CDRRMO advisories & disaster warnings (NO personal report notifications)
        allAlertsList.add(new AlertItem(
            "🚨 Valencia City CDRRMO Advisory",
            "All emergency responder units are on high alert due to weather conditions. Report incidents immediately using the camera button.",
            "10 min ago",
            true,
            true,
            sessionManager.hasReadAlerts()
        ));

        allAlertsList.add(new AlertItem(
            "📢 Valencia City Safety Notice",
            "Please ensure clear pathways for emergency vehicles along Sayre Highway during peak hours.",
            "1 hour ago",
            false,
            true,
            true
        ));

        renderAlerts();
    }

    private void renderAlerts() {
        if (containerAlerts == null) return;
        containerAlerts.removeAllViews();

        List<AlertItem> displayList = new ArrayList<>();
        for (AlertItem item : allAlertsList) {
            if (currentAlertFilter.equals("ALL")) {
                displayList.add(item);
            } else if (currentAlertFilter.equals("SYSTEM") && item.isSystem) {
                displayList.add(item);
            } else if (currentAlertFilter.equals("UPDATES") && !item.isSystem) {
                displayList.add(item);
            }
        }

        if (displayList.isEmpty()) {
            TextView tvEmpty = new TextView(requireContext());
            tvEmpty.setText("No alerts or advisories available.");
            tvEmpty.setTextColor(getResources().getColor(R.color.arms_secondary_text, null));
            tvEmpty.setPadding(24, 48, 24, 48);
            containerAlerts.addView(tvEmpty);
            return;
        }

        for (AlertItem item : displayList) {
            View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_recent_report, containerAlerts, false);
            TextView tvRef = cardView.findViewById(R.id.tvReportRef);
            TextView tvStatus = cardView.findViewById(R.id.tvReportStatus);
            TextView tvType = cardView.findViewById(R.id.tvReportType);
            TextView tvLocation = cardView.findViewById(R.id.tvReportLocation);

            if (tvRef != null) tvRef.setText(item.time);
            if (tvStatus != null) {
                tvStatus.setText(item.isSystem ? "ADVISORY" : "NOTICE");
                tvStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                tvStatus.setTextColor(Color.parseColor("#DC2626"));
            }
            if (tvType != null) tvType.setText(item.title);
            if (tvLocation != null) tvLocation.setText(item.message);

            containerAlerts.addView(cardView);
        }
    }
}
