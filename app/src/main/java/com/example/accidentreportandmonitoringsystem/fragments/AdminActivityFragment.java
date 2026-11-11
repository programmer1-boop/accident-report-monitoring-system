package com.example.accidentreportandmonitoringsystem.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.accidentreportandmonitoringsystem.R;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalIncidentStore;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONObject;

public class AdminActivityFragment extends Fragment {

    private SessionManager sessionManager;
    private LinearLayout containerAdminActivityList;
    private EditText etSearchActivity;
    private TextView tabActivityAll, tabActivityUser, tabActivityIncident, tabActivityTeam, tabActivitySystem, tvActivityCountBadge;
    private TextView tvStatTotalActivities, tvStatTodayActivities, tvStatFailedActions, tvStatSecurityEvents;
    private String currentCategory = "ALL"; // ALL, USER, INCIDENT, TEAM, SYSTEM
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_activity, container, false);
        sessionManager = new SessionManager(requireContext());

        containerAdminActivityList = view.findViewById(R.id.containerAdminActivityList);
        etSearchActivity = view.findViewById(R.id.etSearchActivity);
        tabActivityAll = view.findViewById(R.id.tabActivityAll);
        tabActivityUser = view.findViewById(R.id.tabActivityUser);
        tabActivityIncident = view.findViewById(R.id.tabActivityIncident);
        tabActivityTeam = view.findViewById(R.id.tabActivityTeam);
        tabActivitySystem = view.findViewById(R.id.tabActivitySystem);
        tvActivityCountBadge = view.findViewById(R.id.tvActivityCountBadge);

        tvStatTotalActivities = view.findViewById(R.id.tvStatTotalActivities);
        tvStatTodayActivities = view.findViewById(R.id.tvStatTodayActivities);
        tvStatFailedActions = view.findViewById(R.id.tvStatFailedActions);
        tvStatSecurityEvents = view.findViewById(R.id.tvStatSecurityEvents);

        setupTabs();
        setupSearch();
        loadActivities();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadActivities();
    }

    private void setupTabs() {
        TextView[] tabs = {tabActivityAll, tabActivityUser, tabActivityIncident, tabActivityTeam, tabActivitySystem};
        String[] categories = {"ALL", "USER", "INCIDENT", "TEAM", "SYSTEM"};

        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            if (tabs[i] != null) {
                tabs[i].setOnClickListener(v -> {
                    currentCategory = categories[idx];
                    updateTabStyles(tabs);
                    loadActivities();
                });
            }
        }
    }

    private void updateTabStyles(TextView[] tabs) {
        int primaryBg = R.drawable.bg_primary_button;
        int secondaryBg = R.drawable.bg_secondary_button;
        int white = Color.parseColor("#FFFFFF");
        int navy = Color.parseColor("#142B4A");

        String[] categories = {"ALL", "USER", "INCIDENT", "TEAM", "SYSTEM"};
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] != null) {
                boolean selected = currentCategory.equals(categories[i]);
                tabs[i].setBackgroundResource(selected ? primaryBg : secondaryBg);
                tabs[i].setTextColor(selected ? white : navy);
            }
        }
    }

    private void setupSearch() {
        if (etSearchActivity != null) {
            etSearchActivity.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().trim().toLowerCase();
                    loadActivities();
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void loadActivities() {
        if (containerAdminActivityList == null) return;
        containerAdminActivityList.removeAllViews();

        try {
            JSONArray activities = new JSONArray();

            // Inject real citizen reports as incident activities
            JSONArray globalReports = ValenciaGlobalIncidentStore.getReports();
            JSONArray localReports = sessionManager.getLocalReports();
            if (globalReports != null) {
                for (int i = 0; i < globalReports.length(); i++) {
                    JSONObject r = globalReports.getJSONObject(i);
                    String ref = r.optString("incident_reference", "ARMS-VAL");
                    String type = r.optString("incident_type", "EMERGENCY");
                    String address = r.optString("landmark_address", "Valencia City");
                    activities.put(new JSONObject().put("module", "INCIDENTS").put("action", "Emergency incident report #" + ref + " submitted (" + type + ")").put("user", "Citizen Reporter · Citizen").put("time", r.optString("created_at", "Today")).put("category", "INCIDENT").put("target", "Report #" + ref).put("ip", "192.168.1.10").put("desc", "Live emergency report submitted at " + address));
                }
            }
            if (localReports != null) {
                for (int i = 0; i < localReports.length(); i++) {
                    JSONObject r = localReports.getJSONObject(i);
                    String ref = r.optString("incident_reference", "ARMS-VAL");
                    String type = r.optString("incident_type", "EMERGENCY");
                    activities.put(new JSONObject().put("module", "INCIDENTS").put("action", "Local emergency incident #" + ref + " (" + type + ")").put("user", sessionManager.getUserName() + " · User").put("time", r.optString("created_at", "Today")).put("category", "INCIDENT").put("target", "Report #" + ref).put("ip", "127.0.0.1").put("desc", "Local report entry created in session."));
                }
            }

            // Standard audit logs
            activities.put(new JSONObject().put("module", "USERS").put("action", "New responder account created (BFP)").put("user", "System Administrator · Admin").put("time", "Today, 08:15 AM").put("category", "USER").put("target", "Responder User #42").put("ip", "192.168.1.2").put("desc", "Admin provisioned a new BFP firefighter responder account."));
            activities.put(new JSONObject().put("module", "TEAMS").put("action", "Responder assigned to CDRRMO rescue team").put("user", "Dispatcher Officer · Dispatcher").put("time", "Yesterday, 04:30 PM").put("category", "TEAM").put("target", "Team CDRRMO").put("ip", "192.168.1.10").put("desc", "Assigned certified emergency responder to active rescue team."));
            activities.put(new JSONObject().put("module", "SYSTEM").put("action", "System security parameters updated").put("user", "System Administrator · Admin").put("time", "May 12, 10:00 AM").put("category", "SYSTEM").put("target", "Security Policy").put("ip", "192.168.1.1").put("desc", "Updated JWT session timeout and PostgreSQL connection pool limits."));

            int totalCount = activities.length();
            int todayCount = 2 + (globalReports != null ? globalReports.length() : 0);
            int failedCount = 3;
            int securityCount = 1;

            if (tvStatTotalActivities != null) tvStatTotalActivities.setText(String.valueOf(totalCount));
            if (tvStatTodayActivities != null) tvStatTodayActivities.setText(String.valueOf(todayCount));
            if (tvStatFailedActions != null) tvStatFailedActions.setText(String.valueOf(failedCount));
            if (tvStatSecurityEvents != null) tvStatSecurityEvents.setText(String.valueOf(securityCount));

            int displayedCount = 0;
            for (int i = 0; i < activities.length(); i++) {
                JSONObject a = activities.getJSONObject(i);
                String module = a.optString("module", "");
                String action = a.optString("action", "");
                String user = a.optString("user", "");
                String time = a.optString("time", "");
                String category = a.optString("category", "");
                String target = a.optString("target", "");
                String ip = a.optString("ip", "");
                String desc = a.optString("desc", "");

                boolean matchesCategory = currentCategory.equals("ALL") || currentCategory.equalsIgnoreCase(category);
                boolean matchesSearch = searchQuery.isEmpty() ||
                        action.toLowerCase().contains(searchQuery) ||
                        user.toLowerCase().contains(searchQuery) ||
                        module.toLowerCase().contains(searchQuery);

                if (matchesCategory && matchesSearch) {
                    displayedCount++;
                    View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_admin_activity, containerAdminActivityList, false);
                    TextView tvModule = cardView.findViewById(R.id.tvActivityModuleBadge);
                    TextView tvTimestamp = cardView.findViewById(R.id.tvActivityTimestamp);
                    TextView tvAction = cardView.findViewById(R.id.tvActivityAction);
                    TextView tvUser = cardView.findViewById(R.id.tvActivityUser);
                    MaterialButton btnDetails = cardView.findViewById(R.id.btnActivityDetails);

                    if (tvModule != null) tvModule.setText(module);
                    if (tvTimestamp != null) tvTimestamp.setText(time);
                    if (tvAction != null) tvAction.setText(action);
                    if (tvUser != null) tvUser.setText(user);

                    if (btnDetails != null) {
                        btnDetails.setOnClickListener(v -> {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Audit Log Details")
                                    .setMessage("• Action: " + action + "\n\n• Module: " + module + "\n• Actor: " + user + "\n• Target: " + target + "\n• IP Address: " + ip + "\n• Timestamp: " + time + "\n\n• Description:\n" + desc)
                                    .setPositiveButton("Close", null)
                                    .show();
                        });
                    }

                    containerAdminActivityList.addView(cardView);
                }
            }

            if (tvActivityCountBadge != null) {
                tvActivityCountBadge.setText("Showing " + displayedCount + " logs");
            }
        } catch (Exception ignored) {}
    }
}
