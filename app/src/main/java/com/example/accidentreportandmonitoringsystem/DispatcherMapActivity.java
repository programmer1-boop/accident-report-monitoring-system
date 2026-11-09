package com.example.accidentreportandmonitoringsystem;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalIncidentStore;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class DispatcherMapActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private WebView mapWebViewDispatcherFull;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispatcher_map);

        sessionManager = new SessionManager(this);
        ImageButton ibBack = findViewById(R.id.ibBack);
        mapWebViewDispatcherFull = findViewById(R.id.mapWebViewDispatcherFull);

        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }

        if (mapWebViewDispatcherFull != null) {
            WebSettings settings = mapWebViewDispatcherFull.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
        }

        loadDispatcherFullMap();
    }

    private void loadDispatcherFullMap() {
        if (mapWebViewDispatcherFull == null) return;

        StringBuilder markersJs = new StringBuilder();
        markersJs.append("var map = L.map('map').setView([7.9064, 125.0942], 14);");
        markersJs.append("L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);");

        try {
            JSONArray globalReports = ValenciaGlobalIncidentStore.getReports();
            JSONArray localReports = sessionManager.getLocalReports();

            Set<String> seenRefs = new HashSet<>();
            long now = System.currentTimeMillis();
            long oneMonthMillis = 30L * 24 * 60 * 60 * 1000L; // 1 month retention rule

            if (globalReports != null) {
                for (int i = 0; i < globalReports.length(); i++) {
                    JSONObject rep = globalReports.getJSONObject(i);
                    String ref = rep.optString("incident_reference", "");
                    long t = rep.optLong("timestamp_millis", now);
                    if (!ref.isEmpty() && !seenRefs.contains(ref) && (now - t <= oneMonthMillis)) {
                        seenRefs.add(ref);
                        double lat = rep.optDouble("latitude", 7.9064);
                        double lng = rep.optDouble("longitude", 125.0942);
                        String type = rep.optString("incident_type", "EMERGENCY");
                        String address = rep.optString("landmark_address", "Valencia City");

                        markersJs.append("L.marker([").append(lat).append(", ").append(lng).append("]).addTo(map)")
                                 .append(".bindPopup('<b>🚨 ").append(type.replace("_", " ")).append("</b><br>")
                                 .append(address).append("<br>Ref: #").append(ref).append("');");
                    }
                }
            }
            if (localReports != null) {
                for (int i = 0; i < localReports.length(); i++) {
                    JSONObject rep = localReports.getJSONObject(i);
                    String ref = rep.optString("incident_reference", "");
                    long t = rep.optLong("timestamp_millis", now);
                    if (!ref.isEmpty() && !seenRefs.contains(ref) && (now - t <= oneMonthMillis)) {
                        seenRefs.add(ref);
                        double lat = rep.optDouble("latitude", 7.9064);
                        double lng = rep.optDouble("longitude", 125.0942);
                        String type = rep.optString("incident_type", "EMERGENCY");
                        String address = rep.optString("landmark_address", "Valencia City");

                        markersJs.append("L.marker([").append(lat).append(", ").append(lng).append("]).addTo(map)")
                                 .append(".bindPopup('<b>🚨 ").append(type.replace("_", " ")).append("</b><br>")
                                 .append(address).append("<br>Ref: #").append(ref).append("');");
                    }
                }
            }
        } catch (Exception ignored) {}

        String mapHtml = "<!DOCTYPE html><html><head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />" +
                "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>" +
                "<style>body { margin:0; padding:0; background:#f4f6f9; } #map { width:100%; height:100vh; }</style>" +
                "</head><body>" +
                "<div id=\"map\"></div>" +
                "<script>" +
                markersJs.toString() +
                "</script>" +
                "</body></html>";

        mapWebViewDispatcherFull.loadDataWithBaseURL("https://example.com", mapHtml, "text/html", "UTF-8", null);
    }
}
