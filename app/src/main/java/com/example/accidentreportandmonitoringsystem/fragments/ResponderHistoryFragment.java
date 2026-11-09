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
import org.json.JSONArray;
import org.json.JSONObject;

public class ResponderHistoryFragment extends Fragment {

    private SessionManager sessionManager;
    private LinearLayout containerHistory;
    private TextView tabAll, tabResolved;
    private String currentFilter = "ALL"; // ALL, RESOLVED

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_responder_history, container, false);

        sessionManager = new SessionManager(requireContext());
        containerHistory = view.findViewById(R.id.containerHistory);
        tabAll = view.findViewById(R.id.tabAll);
        tabResolved = view.findViewById(R.id.tabResolved);

        setupFilterTabs();
        loadResolvedHistory(currentFilter);

        return view;
    }

    private void setupFilterTabs() {
        if (tabAll != null) {
            tabAll.setOnClickListener(v -> {
                currentFilter = "ALL";
                updateTabStyles();
                loadResolvedHistory(currentFilter);
            });
        }
        if (tabResolved != null) {
            tabResolved.setOnClickListener(v -> {
                currentFilter = "RESOLVED";
                updateTabStyles();
                loadResolvedHistory(currentFilter);
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
        if (tabResolved != null) {
            tabResolved.setBackgroundResource(currentFilter.equals("RESOLVED") ? activeBg : 0);
            tabResolved.setTextColor(currentFilter.equals("RESOLVED") ? activeTextColor : inactiveTextColor);
        }
    }

    private void loadResolvedHistory(String filter) {
        if (containerHistory == null) return;

        try {
            containerHistory.removeAllViews();
            JSONArray historyArr = sessionManager.getResolvedHistory();

            JSONArray filteredArr = new JSONArray();
            if (historyArr != null) {
                for (int i = 0; i < historyArr.length(); i++) {
                    JSONObject rep = historyArr.getJSONObject(i);
                    String status = rep.optString("current_status", "RESOLVED").toUpperCase();

                    boolean matchesFilter = filter.equals("ALL") || (filter.equals("RESOLVED") && status.contains("RESOLVED"));

                    if (matchesFilter) {
                        filteredArr.put(rep);
                    }
                }
            }

            if (filteredArr.length() == 0) {
                TextView tvEmpty = new TextView(requireContext());
                tvEmpty.setText("No resolved response history found.");
                tvEmpty.setTextColor(getResources().getColor(R.color.arms_secondary_text, null));
                tvEmpty.setPadding(24, 48, 24, 48);
                containerHistory.addView(tvEmpty);
                return;
            }

            for (int i = 0; i < filteredArr.length(); i++) {
                JSONObject rep = filteredArr.getJSONObject(i);
                String ref = rep.optString("incident_reference", "ARMS-VAL");
                String type = rep.optString("incident_type", "EMERGENCY");
                String address = rep.optString("landmark_address", "Valencia City");
                String date = rep.optString("created_at", "Today");

                View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_recent_report, containerHistory, false);

                TextView tvRef = cardView.findViewById(R.id.tvReportRef);
                TextView tvStatus = cardView.findViewById(R.id.tvReportStatus);
                TextView tvType = cardView.findViewById(R.id.tvReportType);
                TextView tvLocation = cardView.findViewById(R.id.tvReportLocation);

                if (tvRef != null) tvRef.setText("#" + ref);
                if (tvStatus != null) {
                    tvStatus.setText("RESOLVED");
                    tvStatus.setBackgroundColor(getResources().getColor(R.color.arms_green, null));
                    tvStatus.setTextColor(getResources().getColor(R.color.arms_white, null));
                }
                if (tvType != null) tvType.setText(type.replace("_", " "));
                if (tvLocation != null) tvLocation.setText(address + " • " + date);

                containerHistory.addView(cardView);
            }
        } catch (Exception ignored) {}
    }
}
