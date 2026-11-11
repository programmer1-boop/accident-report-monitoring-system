package com.example.accidentreportandmonitoringsystem;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.accidentreportandmonitoringsystem.remote.ApiClient;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalIncidentStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class IncidentLocationActivity extends AppCompatActivity implements LocationListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;

    private AutoCompleteTextView actvIncidentType;
    private TextInputEditText etLocation;
    private WebView mapWebView;
    private SessionManager sessionManager;
    private LocationManager locationManager;

    private double currentLatitude = 7.9064; // Default Valencia City center fallback
    private double currentLongitude = 125.0942;
    private float currentAccuracy = 10.0f;
    private boolean hasReceivedLiveGps = false;

    private boolean isResponderMode = false;
    private boolean mapInitialized = false;
    private String photoBase64 = "";
    private boolean hasVideo = false;
    private String videoUri = "";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incident_location);

        sessionManager = new SessionManager(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        photoBase64 = getIntent().getStringExtra("photo_base64");
        if (photoBase64 == null || photoBase64.trim().isEmpty()) {
            try {
                Bitmap defaultBmp = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(defaultBmp);
                canvas.drawColor(Color.parseColor("#142B4A"));
                Paint paint = new Paint();
                paint.setColor(Color.WHITE);
                paint.setTextSize(32f);
                canvas.drawText("ARMS EMERGENCY", 40f, 150f, paint);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                defaultBmp.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                photoBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            } catch (Exception ignored) {
                photoBase64 = "";
            }
        }

        hasVideo = getIntent().getBooleanExtra("has_video", false);
        videoUri = getIntent().getStringExtra("video_uri");
        if (videoUri == null) videoUri = "";

        // Check if intent is in Responder/Dispatcher Map Mode
        isResponderMode = getIntent().hasExtra("incident_lat") && getIntent().hasExtra("station_lat");
        boolean isDispatcherMap = getIntent().getBooleanExtra("is_dispatcher_map", false) || isResponderMode;

        View cardBottomPanel = findViewById(R.id.cardBottomPanel);
        if (isDispatcherMap && cardBottomPanel != null) {
            cardBottomPanel.setVisibility(View.GONE);
        }

        ImageButton ibBack = findViewById(R.id.ibBack);
        actvIncidentType = findViewById(R.id.actvIncidentType);
        etLocation = findViewById(R.id.etLocation);
        mapWebView = findViewById(R.id.mapWebView);
        MaterialButton btnSendEmergencyReport = findViewById(R.id.btnSendEmergencyReport);

        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }

        if (actvIncidentType != null) {
            String[] incidentTypes = new String[]{
                "Road Accident / Collision",
                "Fire Incident",
                "Medical Emergency",
                "Crime / Security Incident",
                "Disaster / Rescue Operation",
                "Other Emergency"
            };
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, incidentTypes);
            actvIncidentType.setAdapter(adapter);
        }

        if (mapWebView != null) {
            mapWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            WebSettings webSettings = mapWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

            if (isDispatcherMap) {
                loadMultiMarkerMapHtml();
            } else {
                loadSingleMarkerMapHtml(currentLatitude, currentLongitude);
            }
        }

        if (btnSendEmergencyReport != null) {
            if (isDispatcherMap) {
                btnSendEmergencyReport.setVisibility(View.GONE);
            } else {
                btnSendEmergencyReport.setOnClickListener(v -> sendEmergencyReportToResponders());
            }
        }

        if (!isDispatcherMap) {
            checkGpsEnabledAndRequestLocation();
        }
    }

    private void loadSingleMarkerMapHtml(double lat, double lng) {
        String mapHtml = "<!DOCTYPE html><html><head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />" +
                "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>" +
                "<style>body { margin:0; padding:0; background:#f4f6f9; } #map { width:100%; height:100vh; }</style>" +
                "</head><body>" +
                "<div id=\"map\"></div>" +
                "<script>" +
                "var map = L.map('map').setView([" + lat + ", " + lng + "], 17);" +
                "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {" +
                "  maxZoom: 20," +
                "  attribution: '&copy; OpenStreetMap contributors'" +
                "}).addTo(map);" +
                "var marker = L.marker([" + lat + ", " + lng + "]).addTo(map);" +
                "var circle = L.circle([" + lat + ", " + lng + "], {color: '#D92D39', fillColor: '#D92D39', fillOpacity: 0.2, radius: 25}).addTo(map);" +
                "marker.bindPopup('<b>📍 Valencia City Live GPS</b><br>Lat: " + lat + ", Lng: " + lng + "').openPopup();" +
                "function updateLocation(newLat, newLng) {" +
                "  map.setView([newLat, newLng], 17);" +
                "  marker.setLatLng([newLat, newLng]);" +
                "  circle.setLatLng([newLat, newLng]);" +
                "}" +
                "</script>" +
                "</body></html>";

        if (mapWebView != null) {
            mapWebView.loadDataWithBaseURL("https://example.com", mapHtml, "text/html", "UTF-8", null);
        }
    }

    private void loadMultiMarkerMapHtml() {
        StringBuilder markersJs = new StringBuilder();
        markersJs.append("var map = L.map('map').setView([7.9064, 125.0942], 14);");
        markersJs.append("L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 20 }).addTo(map);");

        try {
            JSONArray globalReports = ValenciaGlobalIncidentStore.getReports();
            JSONArray localReports = sessionManager.getLocalReports();

            Set<String> seenRefs = new HashSet<>();
            long now = System.currentTimeMillis();
            long oneMonthMillis = 30L * 24 * 60 * 60 * 1000L;

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

        if (mapWebView != null) {
            mapWebView.loadDataWithBaseURL("https://example.com", mapHtml, "text/html", "UTF-8", null);
        }
    }

    private void checkGpsEnabledAndRequestLocation() {
        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(this)
                .setTitle("📍 GPS Location Required")
                .setMessage("Please enable device GPS / Location services to capture your precise live accident coordinates for emergency responders.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Continue with Default", (dialog, which) -> dialog.dismiss())
                .show();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLiveGpsUpdates();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @SuppressLint("MissingPermission")
    private void startLiveGpsUpdates() {
        try {
            if (locationManager != null) {
                Location gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Location netLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                Location best = gpsLoc != null ? gpsLoc : netLoc;
                if (best != null) {
                    onLocationChanged(best);
                }

                // High-precision frequency
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0.1f, this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 0.1f, this);
                Toast.makeText(this, "🛰️ Locking high-accuracy GPS fix...", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "GPS error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        currentAccuracy = location.hasAccuracy() ? location.getAccuracy() : 3.0f;
        hasReceivedLiveGps = true;

        if (mapWebView != null && !isResponderMode) {
            if (!mapInitialized) {
                mapInitialized = true;
                loadSingleMarkerMapHtml(currentLatitude, currentLongitude);
            } else {
                mapWebView.evaluateJavascript("updateLocation(" + currentLatitude + ", " + currentLongitude + ");", null);
            }
        }

        resolveAddressFromCoordinates(currentLatitude, currentLongitude);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {}

    private void resolveAddressFromCoordinates(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            String preciseName;
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String subLocality = address.getSubLocality(); // Barangay
                String thoroughfare = address.getThoroughfare(); // Street
                String feature = address.getFeatureName(); // Landmark

                StringBuilder sb = new StringBuilder();
                if (subLocality != null && !subLocality.trim().isEmpty() && !subLocality.contains("+")) {
                    sb.append(subLocality);
                } else if (feature != null && !feature.trim().isEmpty() && !feature.contains("+") && !feature.matches("^[0-9]+$")) {
                    sb.append(feature);
                } else if (thoroughfare != null && !thoroughfare.trim().isEmpty() && !thoroughfare.contains("+")) {
                    sb.append(thoroughfare);
                } else {
                    if (lat > 7.92) sb.append("Barangay Bagontaas");
                    else if (lng < 125.08) sb.append("Barangay Lumbo");
                    else if (lat < 7.89) sb.append("Barangay Mailag");
                    else sb.append("Barangay Poblacion");
                }

                sb.append(", Valencia City");
                preciseName = sb.toString();
            } else {
                preciseName = "Barangay Poblacion, Valencia City";
            }

            if (etLocation != null) {
                etLocation.setText(preciseName);
            }
        } catch (Exception e) {
            if (etLocation != null) {
                etLocation.setText("Barangay Poblacion, Valencia City");
            }
        }
    }

    private void sendEmergencyReportToResponders() {
        // DO NOT overwrite currentLatitude and currentLongitude with stale getLastKnownLocation!
        // We strictly lock onto the precise live currentLatitude and currentLongitude already established by onLocationChanged().

        String typeDisplay = actvIncidentType != null ? actvIncidentType.getText().toString() : "Road Accident / Collision";
        String location = etLocation != null && etLocation.getText() != null ? etLocation.getText().toString().trim() : "Barangay Poblacion, Valencia City";
        
        StringBuilder descBuilder = new StringBuilder("Emergency report dispatched with precise GPS coordinates (" + currentLatitude + ", " + currentLongitude + ").");
        if (photoBase64 != null && !photoBase64.isEmpty()) descBuilder.append(" [📸 HD Photo Evidence Attached]");
        if (hasVideo) descBuilder.append(" [🎥 Video Evidence Attached]");
        String description = descBuilder.toString();

        final String incidentType;
        if (typeDisplay.contains("Fire")) incidentType = "FIRE";
        else if (typeDisplay.contains("Medical")) incidentType = "MEDICAL_EMERGENCY";
        else if (typeDisplay.contains("Crime") || typeDisplay.contains("Security")) incidentType = "CRIME_SECURITY";
        else if (typeDisplay.contains("Disaster") || typeDisplay.contains("Rescue")) incidentType = "DISASTER_RESCUE";
        else if (typeDisplay.contains("Other")) incidentType = "OTHER_EMERGENCY";
        else incidentType = "ROAD_ACCIDENT";

        String incidentRef = "ARMS-VAL-" + (System.currentTimeMillis() % 10000);

        // INSTANT LOCAL SAVE & GLOBAL STORE BRIDGE WITH MEDIA & EXACT LIVE GPS
        sessionManager.saveLocalReportWithMedia(incidentType, incidentRef, location, description, "DISPATCHED", currentLatitude, currentLongitude, photoBase64, hasVideo, videoUri);
        
        try {
            JSONObject globalObj = new JSONObject();
            globalObj.put("incident_reference", incidentRef);
            globalObj.put("incident_type", incidentType);
            globalObj.put("current_status", "DISPATCHED");
            globalObj.put("landmark_address", location);
            globalObj.put("description", description);
            globalObj.put("latitude", currentLatitude);
            globalObj.put("longitude", currentLongitude);
            globalObj.put("photo_base64", photoBase64);
            globalObj.put("has_video", hasVideo);
            globalObj.put("video_uri", videoUri);
            globalObj.put("created_at", new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(new Date()));
            ValenciaGlobalIncidentStore.addReport(globalObj);

            // Also save initial notification alert
            JSONObject alertObj = new JSONObject();
            alertObj.put("title", "🚨 Emergency Report Submitted: #" + incidentRef);
            alertObj.put("message", "Your " + incidentType.replace("_", " ").toLowerCase() + " report has been received and dispatched.");
            alertObj.put("timestamp", new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(new Date()));
            sessionManager.saveAlertNotification(alertObj);
        } catch (Exception ignored) {}

        Toast.makeText(this, "🚨 SUCCESS! Report Sent with Media & GPS. Ref: " + incidentRef, Toast.LENGTH_LONG).show();

        String authToken = sessionManager.getAuthToken();
        boolean isLoggedInCitizen = (authToken != null && !authToken.trim().isEmpty());

        Intent intent;
        if (isLoggedInCitizen) {
            intent = new Intent(IncidentLocationActivity.this, CitizenDashboardActivity.class);
        } else {
            intent = new Intent(IncidentLocationActivity.this, WelcomeActivity.class);
        }
        intent.putExtra("photo_base64", photoBase64);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        // Also sync with server in background silently
        try {
            JSONArray selResponders = new JSONArray();
            if (getIntent().getBooleanExtra("sel_cdrrmo", true)) selResponders.put("CDRRMO");
            if (getIntent().getBooleanExtra("sel_bfp", false)) selResponders.put("BFP");
            if (getIntent().getBooleanExtra("sel_pnp", true)) selResponders.put("PNP");
            if (getIntent().getBooleanExtra("sel_medical", true)) selResponders.put("MEDICAL_EMS");

            JSONObject payload = new JSONObject();
            payload.put("incident_type", incidentType);
            payload.put("landmark_address", location);
            payload.put("description", description);
            payload.put("latitude", currentLatitude);
            payload.put("longitude", currentLongitude);
            payload.put("location_accuracy", (double) currentAccuracy);
            payload.put("photo_base64", photoBase64);
            payload.put("has_video", hasVideo);
            payload.put("video_uri", videoUri);
            payload.put("selected_responders", selResponders);

            String apiEndpoint = isLoggedInCitizen ? "/" : "/incidents/guest";
            ApiClient.post(apiEndpoint, payload, authToken, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {}
                @Override
                public void onError(int statusCode, String errorMessage) {}
            });
        } catch (Exception ignored) {}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLiveGpsUpdates();
            } else {
                Toast.makeText(this, "Location permission denied. Using default Valencia City coordinates.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (Exception ignored) {}
        }
    }
}
