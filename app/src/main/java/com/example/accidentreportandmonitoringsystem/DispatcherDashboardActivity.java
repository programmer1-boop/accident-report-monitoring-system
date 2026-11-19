package com.example.accidentreportandmonitoringsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalIncidentStore;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class DispatcherDashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private TextView tvQueuePendingCount, tvQueueAssignedCount, tvQueueResolvedCount;
    private LinearLayout containerDispatcherQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispatcher_dashboard);

        sessionManager = new SessionManager(this);
        tvQueuePendingCount = findViewById(R.id.tvQueuePendingCount);
        tvQueueAssignedCount = findViewById(R.id.tvQueueAssignedCount);
        tvQueueResolvedCount = findViewById(R.id.tvQueueResolvedCount);
        containerDispatcherQueue = findViewById(R.id.containerDispatcherQueue);

        ImageButton ibNotifications = findViewById(R.id.ibDispatcherNotifications);
        if (ibNotifications != null) {
            ibNotifications.setOnClickListener(v ->
                    Toast.makeText(this, "Monitoring live Valencia City incident queue.", Toast.LENGTH_SHORT).show());
        }

        ImageButton ibProfile = findViewById(R.id.ibDispatcherProfile);
        if (ibProfile != null) {
            ibProfile.setOnClickListener(v -> startActivity(new Intent(this, CitizenProfileActivity.class)));
        }

        loadIncidentQueue();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadIncidentQueue();
    }

    private void loadIncidentQueue() {
        if (containerDispatcherQueue == null) return;
        containerDispatcherQueue.removeAllViews();

        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
        progressBar.setPadding(0, 32, 0, 32);
        containerDispatcherQueue.addView(progressBar);

        String token = sessionManager.getAuthToken();
        if (token != null && !token.startsWith("arms_instant_token_") && !token.startsWith("arms_fallback_token_")) {
            com.example.accidentreportandmonitoringsystem.remote.ApiClient.get("/dispatcher/queue", token,
                    new com.example.accidentreportandmonitoringsystem.remote.ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            JSONArray queue = response.optJSONArray("queue");
                            if (queue == null) queue = response.optJSONArray("incidents");
                            if (queue != null && queue.length() > 0) {
                                renderQueueFromArrays(new JSONArray[]{queue});
                            } else {
                                renderQueueFromArrays(new JSONArray[]{
                                        ValenciaGlobalIncidentStore.getReports(),
                                        sessionManager.getLocalReports()
                                });
                            }
                        }

                        @Override
                        public void onError(int statusCode, String errorMessage) {
                            renderQueueFromArrays(new JSONArray[]{
                                    ValenciaGlobalIncidentStore.getReports(),
                                    sessionManager.getLocalReports()
                            });
                        }
                    });
        } else {
            renderQueueFromArrays(new JSONArray[]{
                    ValenciaGlobalIncidentStore.getReports(),
                    sessionManager.getLocalReports()
            });
        }
    }

    private void renderQueueFromArrays(JSONArray[] sources) {
        if (containerDispatcherQueue == null) return;
        containerDispatcherQueue.removeAllViews();

        int pending = 0, assigned = 0, resolved = 0;
        Set<String> seen = new HashSet<>();
        java.util.List<JSONObject> activeList = new java.util.ArrayList<>();

        for (JSONArray arr : sources) {
            if (arr == null) continue;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject rep = arr.optJSONObject(i);
                if (rep == null) continue;
                String ref = rep.optString("incident_reference", "");
                if (ref.isEmpty() || seen.contains(ref)) continue;
                seen.add(ref);

                String status = rep.optString("current_status", "SUBMITTED").toUpperCase();
                if (status.contains("RESOLVED")) {
                    resolved++;
                    continue;
                }
                if (status.contains("ASSIGNED") || status.contains("DISPATCHED") || status.contains("ACCEPTED")) {
                    assigned++;
                } else {
                    pending++;
                }
                activeList.add(rep);
            }
        }

        for (int idx = 0; idx < activeList.size(); idx++) {
            JSONObject rep = activeList.get(idx);
            String ref = rep.optString("incident_reference", "");
            String status = rep.optString("current_status", "SUBMITTED").toUpperCase();
            String type = rep.optString("incident_type", "EMERGENCY");
            String addr = rep.optString("landmark_address", "Valencia City, Bukidnon, Philippines");
            String time = rep.optString("created_at", "Just now");
            double lat = rep.optDouble("latitude", 7.9064);
            double lng = rep.optDouble("longitude", 125.0942);

            // Detect nearby duplicate incident (within 150m & same type)
            boolean isPossibleDup = rep.optBoolean("possible_duplicate", false);
            String dupOfRef = rep.optString("duplicate_of_ref", "");
            int dupDistM = rep.optInt("duplicate_distance_m", 0);

            if (!isPossibleDup) {
                for (int j = idx + 1; j < activeList.size(); j++) {
                    JSONObject other = activeList.get(j);
                    if (!type.equalsIgnoreCase(other.optString("incident_type", ""))) continue;
                    double oLat = other.optDouble("latitude", 0.0);
                    double oLng = other.optDouble("longitude", 0.0);
                    int dist = (int) Math.round(haversineMeters(lat, lng, oLat, oLng));
                    if (dist <= 150) {
                        isPossibleDup = true;
                        dupOfRef = other.optString("incident_reference", "");
                        dupDistM = dist;
                        break;
                    }
                }
            }

            View card = LayoutInflater.from(this).inflate(R.layout.item_dispatcher_queue_card, containerDispatcherQueue, false);
            TextView tvType = card.findViewById(R.id.tvQueueIncidentType);
            TextView tvStatus = card.findViewById(R.id.tvQueueStatusBadge);
            TextView tvRef = card.findViewById(R.id.tvQueueRefAndTime);
            TextView tvAddr = card.findViewById(R.id.tvQueueAddress);
            TextView tvDupWarning = card.findViewById(R.id.tvQueueDuplicateWarning);
            MaterialButton btnReview = card.findViewById(R.id.btnReviewAndAssign);

            if (tvType != null) tvType.setText(type.replace("_", " "));
            if (tvStatus != null) tvStatus.setText(status);
            if (tvRef != null) tvRef.setText("#" + ref + " • " + time);
            if (tvAddr != null) tvAddr.setText(addr);

            if (tvDupWarning != null && isPossibleDup) {
                tvDupWarning.setText("Possible Duplicate (~" + dupDistM + "m from #" + dupOfRef + ") — Check before dispatching");
                tvDupWarning.setVisibility(View.VISIBLE);
            }

            if (btnReview != null) {
                btnReview.setOnClickListener(v -> {
                    Intent intent = new Intent(this, DispatcherIncidentReviewActivity.class);
                    intent.putExtra("incident_id", rep.optString("id", ""));
                    intent.putExtra("incident_reference", ref);
                    intent.putExtra("incident_type", type);
                    intent.putExtra("landmark_address", addr);
                    intent.putExtra("description", rep.optString("description", ""));
                    intent.putExtra("latitude", lat);
                    intent.putExtra("longitude", lng);
                    startActivity(intent);
                });
            }
            containerDispatcherQueue.addView(card);
        }

        if (tvQueuePendingCount != null) tvQueuePendingCount.setText(String.format(Locale.getDefault(), "%02d", pending));
        if (tvQueueAssignedCount != null) tvQueueAssignedCount.setText(String.format(Locale.getDefault(), "%02d", assigned));
        if (tvQueueResolvedCount != null) tvQueueResolvedCount.setText(String.format(Locale.getDefault(), "%02d", resolved));

        if (containerDispatcherQueue.getChildCount() == 0) {
            View emptyCard = LayoutInflater.from(this).inflate(R.layout.layout_empty_state, containerDispatcherQueue, false);
            TextView tvEmptyTitle = emptyCard.findViewById(R.id.tvEmptyTitle);
            TextView tvEmptySub = emptyCard.findViewById(R.id.tvEmptySubtitle);
            if (tvEmptyTitle != null) tvEmptyTitle.setText("Dispatch Queue Clear");
            if (tvEmptySub != null) tvEmptySub.setText("No active incidents in the Valencia City dispatch queue.");
            containerDispatcherQueue.addView(emptyCard);
        }
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
