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
import com.example.accidentreportandmonitoringsystem.R;
import com.example.accidentreportandmonitoringsystem.ResponderStatusUpdateActivity;
import com.example.accidentreportandmonitoringsystem.data.repository.IncidentRepository;
import com.example.accidentreportandmonitoringsystem.remote.ApiClient;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalIncidentStore;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class ResponderAlertsFragment extends Fragment {

    private SessionManager sessionManager;
    private LinearLayout containerAlerts;
    private TextView tvTitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_responder_alerts, container, false);
        sessionManager = new SessionManager(requireContext());
        containerAlerts = view.findViewById(R.id.containerResponderAlertsList);
        tvTitle = view.findViewById(R.id.tvResponderAlertsTitle);

        String agency = sessionManager.getUserAgency();
        if (agency == null || agency.trim().isEmpty()) agency = "Responder";
        if (tvTitle != null) {
            tvTitle.setText(agency + " Emergency Alerts");
        }

        loadResponderAlerts();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadResponderAlerts();
    }

    private void loadResponderAlerts() {
        if (containerAlerts == null || !isAdded()) return;
        String token = sessionManager.getAuthToken();
        if (token != null && !token.startsWith("arms_instant_token_") && !token.startsWith("arms_fallback_token_")) {
            new IncidentRepository().getResponderDispatches(token, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    if (!isAdded()) return;
                    renderAlerts(response.optJSONArray("dispatches"));
                }

                @Override
                public void onError(int statusCode, String errorMessage) {
                    if (!isAdded()) return;
                    renderAlerts(null);
                }
            });
        } else {
            renderAlerts(null);
        }
    }

    private void renderAlerts(@Nullable JSONArray apiDispatches) {
        if (containerAlerts == null || !isAdded()) return;
        containerAlerts.removeAllViews();

        Set<String> seenRefs = new HashSet<>();
        JSONArray[] sources = new JSONArray[]{
                apiDispatches,
                ValenciaGlobalIncidentStore.getReports(),
                sessionManager.getLocalReports()
        };

        for (JSONArray arr : sources) {
            if (arr == null) continue;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject rep = arr.optJSONObject(i);
                if (rep == null) continue;
                String ref = rep.optString("incident_reference", "");
                if (ref.isEmpty() || seenRefs.contains(ref)) continue;
                seenRefs.add(ref);

                String type = rep.optString("incident_type", "EMERGENCY");
                String status = rep.optString("current_status", "DISPATCHED");
                String address = rep.optString("landmark_address", "Valencia City, Bukidnon, Philippines");
                String createdAt = rep.optString("created_at", "Just now");
                double lat = rep.optDouble("latitude", 7.9064);
                double lng = rep.optDouble("longitude", 125.0942);

                View card = LayoutInflater.from(requireContext()).inflate(R.layout.item_recent_report, containerAlerts, false);
                TextView tvRef = card.findViewById(R.id.tvReportRef);
                TextView tvStatus = card.findViewById(R.id.tvReportStatus);
                TextView tvLoc = card.findViewById(R.id.tvReportLocation);
                TextView tvTime = card.findViewById(R.id.tvReportTime);

                if (tvRef != null) tvRef.setText(type.replace("_", " ") + " • #" + ref);
                if (tvStatus != null) {
                    tvStatus.setText(status);
                    if (status.equalsIgnoreCase("RESOLVED")) {
                        tvStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                        tvStatus.setTextColor(Color.parseColor("#2E9D68"));
                    } else {
                        tvStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                        tvStatus.setTextColor(Color.parseColor("#DC2626"));
                    }
                }
                if (tvLoc != null) tvLoc.setText(address);
                if (tvTime != null) tvTime.setText(createdAt);

                card.setOnClickListener(v -> {
                    Intent intent = new Intent(requireContext(), ResponderStatusUpdateActivity.class);
                    intent.putExtra("incident_lat", lat);
                    intent.putExtra("incident_lng", lng);
                    intent.putExtra("incident_ref", ref);
                    intent.putExtra("incident_type", type);
                    intent.putExtra("status", status);
                    intent.putExtra("address", address);
                    intent.putExtra("photo_base64", rep.optString("photo_base64", ""));
                    intent.putExtra("has_video", rep.optBoolean("has_video", false));
                    intent.putExtra("video_uri", rep.optString("video_uri", ""));
                    intent.putExtra("assignment_id", rep.optString("assignment_id", ""));
                    startActivity(intent);
                });

                containerAlerts.addView(card);
            }
        }

        // Also include saved notification history items
        JSONArray history = sessionManager.getNotificationHistory();
        if (history != null) {
            for (int i = 0; i < history.length(); i++) {
                JSONObject notif = history.optJSONObject(i);
                if (notif == null) continue;
                View card = LayoutInflater.from(requireContext()).inflate(R.layout.item_recent_report, containerAlerts, false);
                TextView tvRef = card.findViewById(R.id.tvReportRef);
                TextView tvStatus = card.findViewById(R.id.tvReportStatus);
                TextView tvLoc = card.findViewById(R.id.tvReportLocation);
                TextView tvTime = card.findViewById(R.id.tvReportTime);

                if (tvRef != null) tvRef.setText(notif.optString("title", "System Alert"));
                if (tvStatus != null) tvStatus.setText("ALERT");
                if (tvLoc != null) tvLoc.setText(notif.optString("message", ""));
                if (tvTime != null) tvTime.setText(notif.optString("timestamp", "Today"));
                containerAlerts.addView(card);
            }
        }

        if (containerAlerts.getChildCount() == 0) {
            View emptyCard = LayoutInflater.from(requireContext()).inflate(R.layout.layout_empty_state, containerAlerts, false);
            TextView tvEmptyTitle = emptyCard.findViewById(R.id.tvEmptyTitle);
            TextView tvEmptySub = emptyCard.findViewById(R.id.tvEmptySubtitle);
            if (tvEmptyTitle != null) tvEmptyTitle.setText("No Emergency Alerts");
            if (tvEmptySub != null) tvEmptySub.setText("No emergency alerts for " + sessionManager.getUserAgency() + " yet.");
            containerAlerts.addView(emptyCard);
        }
    }
}
