package com.example.accidentreportandmonitoringsystem.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.accidentreportandmonitoringsystem.AdminCreateResponderActivity;
import com.example.accidentreportandmonitoringsystem.AdminDashboardActivity;
import com.example.accidentreportandmonitoringsystem.R;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalIncidentStore;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalUserStore;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import org.json.JSONArray;
import org.json.JSONObject;

public class AdminHomeFragment extends Fragment {

    private SessionManager sessionManager;
    private TextView tvAdminName;
    private TextView tvStatTotalUsers, tvStatStaffAccounts, tvStatTotalIncidents, tvStatResolvedIncidents;
    private MaterialCardView cardCreateResponder, cardManageUsers, cardIncidentRecords, cardAnalytics;
    private FrameLayout ibAdminNotification;
    private TextView tvViewAllActivities;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_home, container, false);
        sessionManager = new SessionManager(requireContext());

        tvAdminName = view.findViewById(R.id.tvAdminName);
        tvStatTotalUsers = view.findViewById(R.id.tvStatTotalUsers);
        tvStatStaffAccounts = view.findViewById(R.id.tvStatStaffAccounts);
        tvStatTotalIncidents = view.findViewById(R.id.tvStatTotalIncidents);
        tvStatResolvedIncidents = view.findViewById(R.id.tvStatResolvedIncidents);

        cardCreateResponder = view.findViewById(R.id.cardQuickCreateResponder);
        cardManageUsers = view.findViewById(R.id.cardQuickManageUsers);
        cardIncidentRecords = view.findViewById(R.id.cardQuickIncidentRecords);
        cardAnalytics = view.findViewById(R.id.cardQuickAnalytics);
        ibAdminNotification = view.findViewById(R.id.ibAdminNotification);
        tvViewAllActivities = view.findViewById(R.id.tvViewAllActivities);

        if (tvAdminName != null) {
            tvAdminName.setText(sessionManager.getUserName());
        }

        updateDynamicStats();

        if (cardCreateResponder != null) {
            cardCreateResponder.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), AdminCreateResponderActivity.class);
                startActivity(intent);
            });
        }

        if (cardManageUsers != null) {
            cardManageUsers.setOnClickListener(v -> {
                if (getActivity() instanceof AdminDashboardActivity) {
                    BottomNavigationView nav = getActivity().findViewById(R.id.adminBottomNavigation);
                    if (nav != null) nav.setSelectedItemId(R.id.nav_admin_users);
                }
            });
        }

        if (cardIncidentRecords != null) {
            cardIncidentRecords.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "📋 Opening All Incident Records...", Toast.LENGTH_SHORT).show();
            });
        }

        if (cardAnalytics != null) {
            cardAnalytics.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "📊 Opening Reports & Analytics...", Toast.LENGTH_SHORT).show();
            });
        }

        if (ibAdminNotification != null) {
            ibAdminNotification.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "🔔 No new system notifications.", Toast.LENGTH_SHORT).show();
            });
        }

        if (tvViewAllActivities != null) {
            tvViewAllActivities.setOnClickListener(v -> {
                if (getActivity() instanceof AdminDashboardActivity) {
                    BottomNavigationView nav = getActivity().findViewById(R.id.adminBottomNavigation);
                    if (nav != null) nav.setSelectedItemId(R.id.nav_admin_activity);
                }
            });
        }

        return view;
    }

    private void updateDynamicStats() {
        new Thread(() -> {
            try {
                JSONArray globalReports = ValenciaGlobalIncidentStore.getReports();
                JSONArray localReports = sessionManager.getLocalReports();
                JSONArray resolvedHistory = ValenciaGlobalIncidentStore.getResolvedHistory();
                JSONArray users = ValenciaGlobalUserStore.getUsers();

                int totalIncidents = 0;
                if (globalReports != null) totalIncidents += globalReports.length();
                if (localReports != null) totalIncidents += localReports.length();

                int resolvedCount = 0;
                if (resolvedHistory != null) resolvedCount += resolvedHistory.length();
                if (globalReports != null) {
                    for (int i = 0; i < globalReports.length(); i++) {
                        if (globalReports.getJSONObject(i).optString("current_status", "").equalsIgnoreCase("RESOLVED")) {
                            resolvedCount++;
                        }
                    }
                }
                if (localReports != null) {
                    for (int i = 0; i < localReports.length(); i++) {
                        if (localReports.getJSONObject(i).optString("current_status", "").equalsIgnoreCase("RESOLVED")) {
                            resolvedCount++;
                        }
                    }
                }

                int staffCount = 0;
                int citizenCount = 0;
                if (users != null) {
                    for (int i = 0; i < users.length(); i++) {
                        JSONObject u = users.getJSONObject(i);
                        if (u.optString("type").equals("STAFF")) {
                            staffCount++;
                        } else {
                            citizenCount++;
                        }
                    }
                }
                int totalUsers = users != null ? users.length() : 0;

                final int finalUsers = totalUsers;
                final int finalStaff = staffCount;
                final int finalIncidents = Math.max(0, totalIncidents);
                final int finalResolved = Math.max(0, resolvedCount);

                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (tvStatTotalUsers != null) tvStatTotalUsers.setText(String.valueOf(finalUsers));
                        if (tvStatStaffAccounts != null) tvStatStaffAccounts.setText(String.valueOf(finalStaff));
                        if (tvStatTotalIncidents != null) tvStatTotalIncidents.setText(String.valueOf(finalIncidents));
                        if (tvStatResolvedIncidents != null) tvStatResolvedIncidents.setText(String.valueOf(finalResolved));
                    });
                }
            } catch (Exception ignored) {}
        }).start();
    }
}
