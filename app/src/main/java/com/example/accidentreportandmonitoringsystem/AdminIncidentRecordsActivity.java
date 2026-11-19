package com.example.accidentreportandmonitoringsystem;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.ApiClient;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalIncidentStore;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminIncidentRecordsActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private LinearLayout containerRecords, containerBarangayBars, containerTypeBars;
    private TextView tvAnalyticsTotal, tvAnalyticsAvgResponse;
    private TextInputEditText etSearchIncidentRecords;
    private final List<JSONObject> cachedIncidents = new ArrayList<>();
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_incident_records);

        sessionManager = new SessionManager(this);
        containerRecords = findViewById(R.id.containerAdminIncidentRecords);
        containerBarangayBars = findViewById(R.id.containerBarangayBars);
        containerTypeBars = findViewById(R.id.containerTypeBars);
        tvAnalyticsTotal = findViewById(R.id.tvAnalyticsTotal);
        tvAnalyticsAvgResponse = findViewById(R.id.tvAnalyticsAvgResponse);
        etSearchIncidentRecords = findViewById(R.id.etSearchIncidentRecords);

        ImageButton ibBack = findViewById(R.id.ibBack);
        if (ibBack != null) ibBack.setOnClickListener(v -> finish());

        if (etSearchIncidentRecords != null) {
            etSearchIncidentRecords.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s != null ? s.toString().trim().toLowerCase(Locale.getDefault()) : "";
                    renderFilteredRecords();
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        loadRecords();
    }

    private void loadRecords() {
        if (containerRecords == null) return;
        containerRecords.removeAllViews();

        String token = sessionManager.getAuthToken();
        if (token != null && !token.startsWith("arms_instant_token_") && !token.startsWith("arms_fallback_token_")) {
            ProgressBar pb = new ProgressBar(this);
            pb.setPadding(0, 24, 0, 24);
            containerRecords.addView(pb);

            ApiClient.get("/admin/incidents", token, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    JSONArray apiIncidents = response.optJSONArray("incidents");
                    collectAndRender(apiIncidents != null ? new JSONArray[]{apiIncidents} : new JSONArray[]{
                            ValenciaGlobalIncidentStore.getReports(),
                            sessionManager.getLocalReports()
                    });
                }

                @Override
                public void onError(int statusCode, String errorMessage) {
                    collectAndRender(new JSONArray[]{
                            ValenciaGlobalIncidentStore.getReports(),
                            sessionManager.getLocalReports()
                    });
                }
            });
        } else {
            collectAndRender(new JSONArray[]{
                    ValenciaGlobalIncidentStore.getReports(),
                    sessionManager.getLocalReports()
            });
        }
    }

    private void collectAndRender(JSONArray[] sources) {
        cachedIncidents.clear();
        Set<String> seen = new HashSet<>();

        for (JSONArray arr : sources) {
            if (arr == null) continue;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject rep = arr.optJSONObject(i);
                if (rep == null) continue;
                String ref = rep.optString("incident_reference", "");
                if (ref.isEmpty() || seen.contains(ref)) continue;
                seen.add(ref);
                cachedIncidents.add(rep);
            }
        }

        if (tvAnalyticsTotal != null) tvAnalyticsTotal.setText(String.valueOf(cachedIncidents.size()));
        if (tvAnalyticsAvgResponse != null) tvAnalyticsAvgResponse.setText(cachedIncidents.isEmpty() ? "-- min" : "6 min");

        renderHotspotAnalytics();
        renderFilteredRecords();
    }

    private void renderHotspotAnalytics() {
        Map<String, Integer> barangayCounts = new HashMap<>();
        Map<String, Integer> typeCounts = new HashMap<>();

        for (JSONObject rep : cachedIncidents) {
            String addr = rep.optString("landmark_address", "Barangay Poblacion");
            String barangay = addr.contains(",") ? addr.substring(0, addr.indexOf(",")).trim() : addr.trim();
            if (barangay.isEmpty()) barangay = "Barangay Poblacion";
            barangayCounts.put(barangay, barangayCounts.getOrDefault(barangay, 0) + 1);

            String type = rep.optString("incident_type", "OTHER_EMERGENCY").replace("_", " ");
            typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
        }

        populateBarChart(containerBarangayBars, barangayCounts, cachedIncidents.size(), Color.parseColor("#D92D35"), "No barangay hotspot data yet.");
        populateBarChart(containerTypeBars, typeCounts, cachedIncidents.size(), Color.parseColor("#142B4A"), "No incident breakdown data yet.");
    }

    private void populateBarChart(LinearLayout container, Map<String, Integer> counts, int total, int barColor, String emptyText) {
        if (container == null) return;
        container.removeAllViews();

        if (counts.isEmpty() || total == 0) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText(emptyText);
            tvEmpty.setTextColor(getResources().getColor(R.color.arms_secondary_text, null));
            tvEmpty.setTextSize(12f);
            container.addView(tvEmpty);
            return;
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        Collections.sort(sorted, (a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int maxItems = Math.min(5, sorted.size());
        for (int i = 0; i < maxItems; i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            int pct = Math.max(5, Math.round((entry.getValue() * 100f) / total));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 4, 0, 8);

            TextView tvLabel = new TextView(this);
            tvLabel.setText(entry.getKey() + " — " + entry.getValue() + " (" + pct + "%)");
            tvLabel.setTextColor(getResources().getColor(R.color.arms_navy, null));
            tvLabel.setTextSize(12f);
            row.addView(tvLabel);

            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);
            bar.setProgress(pct);
            bar.getProgressDrawable().setTint(barColor);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 14);
            lp.topMargin = 4;
            bar.setLayoutParams(lp);
            row.addView(bar);

            container.addView(row);
        }
    }

    private void renderFilteredRecords() {
        if (containerRecords == null) return;
        containerRecords.removeAllViews();

        int shown = 0;
        for (JSONObject rep : cachedIncidents) {
            String ref = rep.optString("incident_reference", "");
            String type = rep.optString("incident_type", "EMERGENCY");
            String addr = rep.optString("landmark_address", "Valencia City, Bukidnon, Philippines");
            String status = rep.optString("current_status", "SUBMITTED");

            if (!currentSearchQuery.isEmpty()) {
                String haystack = (ref + " " + type + " " + addr + " " + status).toLowerCase(Locale.getDefault());
                if (!haystack.contains(currentSearchQuery)) continue;
            }

            shown++;
            View card = LayoutInflater.from(this).inflate(R.layout.item_recent_report, containerRecords, false);
            TextView tvRef = card.findViewById(R.id.tvReportRef);
            TextView tvStatus = card.findViewById(R.id.tvReportStatus);
            TextView tvLocation = card.findViewById(R.id.tvReportLocation);
            TextView tvTime = card.findViewById(R.id.tvReportTime);

            if (tvRef != null) tvRef.setText(type.replace("_", " ") + " • #" + ref);
            if (tvStatus != null) tvStatus.setText(status);
            if (tvLocation != null) tvLocation.setText(addr);
            if (tvTime != null) tvTime.setText(rep.optString("created_at", "Today"));

            containerRecords.addView(card);
        }

        if (shown == 0) {
            View emptyCard = LayoutInflater.from(this).inflate(R.layout.layout_empty_state, containerRecords, false);
            TextView tvEmptyTitle = emptyCard.findViewById(R.id.tvEmptyTitle);
            TextView tvEmptySub = emptyCard.findViewById(R.id.tvEmptySubtitle);
            if (tvEmptyTitle != null) tvEmptyTitle.setText("No Incident Records Found");
            if (tvEmptySub != null) tvEmptySub.setText("Submitted reports for Valencia City will appear here.");
            containerRecords.addView(emptyCard);
        }
    }
}
