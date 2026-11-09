package com.example.accidentreportandmonitoringsystem.fragments;

import android.content.Intent;
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
import com.example.accidentreportandmonitoringsystem.CitizenReportDetailsActivity;
import com.example.accidentreportandmonitoringsystem.R;
import com.example.accidentreportandmonitoringsystem.remote.ApiClient;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class CitizenReportsFragment extends Fragment {

    private SessionManager sessionManager;
    private LinearLayout containerReports;
    private TextView tabAll, tabActive, tabResolved;
    private JSONArray allReportsCache = new JSONArray();
    private String currentFilter = "ALL"; // ALL, ACTIVE, RESOLVED

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_citizen_reports, container, false);

        sessionManager = new SessionManager(requireContext());
        containerReports = view.findViewById(R.id.containerReports);
        tabAll = view.findViewById(R.id.tabAll);
        tabActive = view.findViewById(R.id.tabActive);
        tabResolved = view.findViewById(R.id.tabResolved);

        setupFilterTabs();

        // Load local persistent reports immediately
        loadAndDisplayReports();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAndDisplayReports();
    }

    private void loadAndDisplayReports() {
        try {
            allReportsCache = sessionManager.getLocalReports();
            displayReports(allReportsCache, currentFilter);
        } catch (Exception e) {
            allReportsCache = new JSONArray();
            displayReports(allReportsCache, currentFilter);
        }
    }

    private void setupFilterTabs() {
        if (tabAll != null) {
            tabAll.setOnClickListener(v -> {
                currentFilter = "ALL";
                updateTabStyles();
                displayReports(allReportsCache, currentFilter);
            });
        }
        if (tabActive != null) {
            tabActive.setOnClickListener(v -> {
                currentFilter = "ACTIVE";
                updateTabStyles();
                displayReports(allReportsCache, currentFilter);
            });
        }
        if (tabResolved != null) {
            tabResolved.setOnClickListener(v -> {
                currentFilter = "RESOLVED";
                updateTabStyles();
                displayReports(allReportsCache, currentFilter);
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

    private void displayReports(JSONArray reports, String filter) {
        if (!isAdded() || containerReports == null) return;

        try {
            containerReports.removeAllViews();

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
                TextView tvEmpty = new TextView(requireContext());
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
                String date = rep.optString("created_at", "");

                View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_recent_report, containerReports, false);

                TextView tvRef = cardView.findViewById(R.id.tvReportRef);
                TextView tvStatus = cardView.findViewById(R.id.tvReportStatus);
                TextView tvType = cardView.findViewById(R.id.tvReportType);
                TextView tvLocation = cardView.findViewById(R.id.tvReportLocation);

                if (tvRef != null) tvRef.setText("#" + ref);
                if (tvStatus != null) {
                    tvStatus.setText(status);
                    if (status.equalsIgnoreCase("RESOLVED")) {
                        tvStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                        tvStatus.setTextColor(Color.parseColor("#2E9D68"));
                    } else {
                        tvStatus.setBackgroundColor(Color.parseColor("#FFF3E0"));
                        tvStatus.setTextColor(Color.parseColor("#D97706"));
                    }
                }
                if (tvType != null) tvType.setText(type.replace("_", " "));
                if (tvLocation != null) tvLocation.setText(address + " • " + date);

                cardView.setOnClickListener(v -> {
                    Intent intent = new Intent(requireContext(), CitizenReportDetailsActivity.class);
                    intent.putExtra("incident_type", type);
                    intent.putExtra("incident_ref", ref);
                    intent.putExtra("status", status);
                    intent.putExtra("location", address);
                    intent.putExtra("description", desc);
                    startActivity(intent);
                });

                if (containerReports != null) {
                    containerReports.addView(cardView);
                }
            }
        } catch (Exception ignored) {}
    }
}
