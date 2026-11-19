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
    private boolean isCitizenTrackingMode = false;
    private boolean mapInitialized = false;
    private long lastTelemetryPushMs = 0L;
    private long lastRouteFetchMs = 0L;
    private double targetIncidentLat = 7.9064;
    private double targetIncidentLng = 125.0942;
    private double activeStartLat = 7.9096;
    private double activeStartLng = 125.0928;
    private final java.util.concurrent.ExecutorService routeExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.os.Handler trackingPollHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable trackingPollRunnable;
    private String photoBase64 = "";

    // Google Maps-Like Turn-by-Turn Navigation State
    private static class NavStep {
        String instruction;
        String turnIcon;
        double distanceMeters;
        double durationSeconds;
        double lat;
        double lng;
    }

    private final java.util.List<NavStep> navSteps = new java.util.ArrayList<>();
    private final java.util.List<double[]> routePolylinePoints = new java.util.ArrayList<>();
    private double totalRouteDistanceMeters = 0.0;
    private double totalRouteDurationSeconds = 0.0;
    private boolean isNavigating = false;
    private boolean isVoiceMuted = false;
    private int currentStepIndex = 0;
    private boolean isSimulatingDrive = false;
    private int simPointIndex = 0;
    private final android.os.Handler navSimHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable navSimRunnable;
    private android.speech.tts.TextToSpeech navTts;

    private View rlMapTopBar, cardNavBanner, cardNavPanel, layoutMapNavControls;
    private android.widget.TextView tvNavTurnIcon, tvNavStepDistance, tvNavInstruction, tvNavNextStep, btnToggleNavVoice;
    private android.widget.TextView tvNavEta, tvNavDistanceArrival, tvNavDestination;
    private MaterialButton btnStartNavigation, btnViewAllSteps, btnRecenterNav, btnSimulateNav;
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

        // Check if intent is in Responder/Dispatcher/Citizen-Tracking Map Mode
        isResponderMode = getIntent().hasExtra("incident_lat") && getIntent().hasExtra("station_lat");
        isCitizenTrackingMode = getIntent().getBooleanExtra("is_citizen_tracking", false);
        boolean isDispatcherMap = getIntent().getBooleanExtra("is_dispatcher_map", false) || isResponderMode;

        View cardBottomPanel = findViewById(R.id.cardBottomPanel);
        rlMapTopBar = findViewById(R.id.rlMapTopBar);
        cardNavBanner = findViewById(R.id.cardNavBanner);
        cardNavPanel = findViewById(R.id.cardNavPanel);
        layoutMapNavControls = findViewById(R.id.layoutMapNavControls);
        tvNavTurnIcon = findViewById(R.id.tvNavTurnIcon);
        tvNavStepDistance = findViewById(R.id.tvNavStepDistance);
        tvNavInstruction = findViewById(R.id.tvNavInstruction);
        tvNavNextStep = findViewById(R.id.tvNavNextStep);
        btnToggleNavVoice = findViewById(R.id.btnToggleNavVoice);
        tvNavEta = findViewById(R.id.tvNavEta);
        tvNavDistanceArrival = findViewById(R.id.tvNavDistanceArrival);
        tvNavDestination = findViewById(R.id.tvNavDestination);
        btnStartNavigation = findViewById(R.id.btnStartNavigation);
        btnViewAllSteps = findViewById(R.id.btnViewAllSteps);
        btnRecenterNav = findViewById(R.id.btnRecenterNav);
        btnSimulateNav = findViewById(R.id.btnSimulateNav);

        if (isDispatcherMap && cardBottomPanel != null) {
            cardBottomPanel.setVisibility(View.GONE);
        }
        if (isResponderMode && cardNavPanel != null) {
            cardNavPanel.setVisibility(View.VISIBLE);
            String destAddr = getIntent().getStringExtra("address");
            if (tvNavDestination != null) {
                tvNavDestination.setText("Destination: " + (destAddr != null && !destAddr.isEmpty()
                        ? destAddr : "Valencia City, Bukidnon, Philippines"));
            }
            initGoogleMapsNavigationUi();
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
                btnSendEmergencyReport.setOnClickListener(v -> confirmAndSendReport());
            }
        }

        if (isCitizenTrackingMode) {
            startCitizenResponderLocationPolling();
        } else {
            checkGpsEnabledAndRequestLocation();
        }
    }

    private void startCitizenResponderLocationPolling() {
        String ref = getIntent().getStringExtra("incident_ref");
        if (ref == null || ref.trim().isEmpty()) return;
        trackingPollRunnable = new Runnable() {
            @Override
            public void run() {
                JSONObject cachedLoc = ValenciaGlobalIncidentStore.getResponderLocation(ref);
                if (cachedLoc != null && mapWebView != null) {
                    double rLat = cachedLoc.optDouble("latitude", 7.9064);
                    double rLng = cachedLoc.optDouble("longitude", 125.0942);
                    mapWebView.evaluateJavascript(
                            "if (typeof updateResponderTracker === 'function') { updateResponderTracker(" + rLat + ", " + rLng + "); }",
                            null
                    );
                    fetchRoadRouteNative(rLat, rLng, targetIncidentLat, targetIncidentLng, false);
                }
                ApiClient.get("/incidents/" + ref + "/responder-location", sessionManager.getAuthToken(), new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        if (response.optBoolean("live", false) && mapWebView != null) {
                            double rLat = response.optDouble("latitude", 7.9064);
                            double rLng = response.optDouble("longitude", 125.0942);
                            mapWebView.evaluateJavascript(
                                    "if (typeof updateResponderTracker === 'function') { updateResponderTracker(" + rLat + ", " + rLng + "); }",
                                    null
                            );
                        }
                    }
                    @Override
                    public void onError(int statusCode, String errorMessage) {}
                });
                trackingPollHandler.postDelayed(this, 8000);
            }
        };
        trackingPollHandler.postDelayed(trackingPollRunnable, 2000);
    }

    private void loadSingleMarkerMapHtml(double lat, double lng) {
        String mapHtml = "<!DOCTYPE html><html><head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />" +
                "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>" +
                "<style>body { margin:0; padding:0; background:#f4f6f9; } #map { width:100%; height:100vh; }</style>" +
                "</head><body>" +
                "<div id=\"routeInfoBadge\" style=\"display:none;position:fixed;top:12px;left:12px;z-index:9999;background:#142B4A;color:#fff;padding:8px 14px;border-radius:12px;font-family:sans-serif;font-size:13px;font-weight:bold;box-shadow:0 2px 8px rgba(0,0,0,0.25);\"></div>" +
                "<div id=\"map\"></div>" +
                "<script>" +
                "var valenciaBounds = L.latLngBounds([7.75, 124.95], [8.05, 125.25]);" +
                "var map = L.map('map', { maxBounds: valenciaBounds, maxBoundsViscosity: 1.0, minZoom: 12 }).setView([" + lat + ", " + lng + "], 17);" +
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

    private static double[] getAgencyStationCoords(String agency) {
        String ag = agency != null ? agency.toUpperCase() : "CDRRMO";
        if (ag.contains("BFP")) return new double[]{7.9118, 125.0926}; // BFP Fire Station (Sayre Hwy North)
        if (ag.contains("PNP")) return new double[]{7.9048, 125.0978}; // PNP Police Station (Poblacion East)
        if (ag.contains("MED") || ag.contains("HEMS")) return new double[]{7.9162, 125.0904}; // Medical / Hospital (Bagontaas Hwy)
        return new double[]{7.9018, 125.0912}; // CDRRMO Command Center (Sayre Hwy South)
    }

    private void loadMultiMarkerMapHtml() {
        double centerLat = getIntent().getDoubleExtra("incident_lat", 7.9064);
        double centerLng = getIntent().getDoubleExtra("incident_lng", 125.0942);
        if (centerLat < 7.75 || centerLat > 8.05 || centerLng < 124.95 || centerLng > 125.25) {
            centerLat = 7.9064;
            centerLng = 125.0942;
        }
        targetIncidentLat = centerLat;
        targetIncidentLng = centerLng;

        double[] defaultStation = getAgencyStationCoords(sessionManager.getUserAgency());
        double initialViewLat = centerLat;
        double initialViewLng = centerLng;
        int zoomLevel = getIntent().hasExtra("incident_lat") ? 16 : 14;

        if (getIntent().hasExtra("station_lat") && getIntent().hasExtra("station_lng")) {
            double sLat = getIntent().getDoubleExtra("station_lat", defaultStation[0]);
            double sLng = getIntent().getDoubleExtra("station_lng", defaultStation[1]);
            if (sLat < 7.75 || sLat > 8.05 || sLng < 124.95 || sLng > 125.25
                    || (Math.abs(sLat - centerLat) < 0.0004 && Math.abs(sLng - centerLng) < 0.0004)) {
                sLat = defaultStation[0];
                sLng = defaultStation[1];
                if (Math.abs(sLat - centerLat) < 0.0004 && Math.abs(sLng - centerLng) < 0.0004) {
                    sLat = 7.9118;
                    sLng = 125.0926;
                }
            }
            activeStartLat = sLat;
            activeStartLng = sLng;
            // Center initial camera on the RESPONDER'S location (not the accident report)
            initialViewLat = sLat;
            initialViewLng = sLng;
            zoomLevel = 17;
        }

        StringBuilder markersJs = new StringBuilder();
        markersJs.append("var valenciaBounds = L.latLngBounds([7.75, 124.95], [8.05, 125.25]);");
        markersJs.append("var map = L.map('map', { maxBounds: valenciaBounds, maxBoundsViscosity: 1.0, minZoom: 12 }).setView([").append(initialViewLat).append(", ").append(initialViewLng).append("], ").append(zoomLevel).append(");");
        markersJs.append("L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 20 }).addTo(map);");

        Set<String> seenRefs = new HashSet<>();
        boolean shouldFetchRoadRoute = false;

        // Always plot the selected incident passed via Intent extras (works for both API & local reports)
        if (getIntent().hasExtra("incident_lat") && getIntent().hasExtra("incident_lng")) {
            String intentRef = getIntent().getStringExtra("incident_ref");
            String intentType = getIntent().getStringExtra("incident_type");
            String intentAddr = getIntent().getStringExtra("address");
            if (intentRef == null) intentRef = "ARMS-INCIDENT";
            if (intentType == null) intentType = "EMERGENCY";
            if (intentAddr == null) intentAddr = "Valencia City, Bukidnon, Philippines";

            seenRefs.add(intentRef);
            String safeType = intentType.replace("_", " ").replace("'", "\\'");
            String safeAddr = intentAddr.replace("'", "\\'");
            String safeRef = intentRef.replace("'", "\\'");

            // Do not call .openPopup() on the accident marker when in responder mode so the camera stays on the responder!
            markersJs.append("var incidentMarker = L.marker([").append(centerLat).append(", ").append(centerLng).append("]).addTo(map)")
                     .append(".bindPopup('<b>").append(safeType).append("</b><br>")
                     .append(safeAddr).append("<br>Ref: #").append(safeRef).append("');");

            if (getIntent().hasExtra("station_lat") && getIntent().hasExtra("station_lng")) {
                double stationLat = activeStartLat;
                double stationLng = activeStartLng;
                shouldFetchRoadRoute = true;
                String agencyLabel = (sessionManager.getUserAgency() != null ? sessionManager.getUserAgency() : "Responder").replace("'", "\\'");

                markersJs.append("var stationMarker = L.circleMarker([").append(stationLat).append(", ").append(stationLng)
                         .append("], {radius: 11, color: '#FFFFFF', weight: 3, fillColor: '#2563EB', fillOpacity: 1.0}).addTo(map)")
                         .append(".bindPopup('<b>").append(agencyLabel).append(" Unit (Your Location)</b><br>Valencia City, Bukidnon').openPopup();");
                markersJs.append("var activeRouteCasing = null;");
                markersJs.append("var activeRouteLine = null;");
                markersJs.append("var trackerTrailCoords = [[").append(stationLat).append(", ").append(stationLng).append("]];");
                markersJs.append("var trackerTrailLine = L.polyline(trackerTrailCoords, {color: '#38BDF8', weight: 4, opacity: 0.75, dashArray: '6, 6'}).addTo(map);");

                // Google Maps style layered blue road navigation renderer called natively from Java
                markersJs.append("function renderNativeRoadRoute(coords, infoHtml, focusResponder) {")
                         .append("  if (activeRouteCasing) { map.removeLayer(activeRouteCasing); }")
                         .append("  if (activeRouteLine) { map.removeLayer(activeRouteLine); }")
                         .append("  var badge = document.getElementById('routeInfoBadge');")
                         .append("  if (badge && infoHtml) { badge.innerHTML = infoHtml; badge.style.display = 'block'; }")
                         .append("  if (coords && coords.length >= 2) {")
                         .append("    activeRouteCasing = L.polyline(coords, {color: '#1E3A8A', weight: 10, opacity: 0.9, lineCap: 'round', lineJoin: 'round'}).addTo(map);")
                         .append("    activeRouteLine = L.polyline(coords, {color: '#2563EB', weight: 6, opacity: 1.0, lineCap: 'round', lineJoin: 'round'}).addTo(map);")
                         .append("    if (focusResponder) {")
                         .append("      map.setView(stationMarker.getLatLng(), 18, {animate: true});")
                         .append("    } else {")
                         .append("      map.setView(stationMarker.getLatLng(), 17, {animate: true});")
                         .append("    }")
                         .append("  }")
                         .append("}");

                markersJs.append("function updateResponderTracker(newLat, newLng) {")
                         .append("  stationMarker.setLatLng([newLat, newLng]);")
                         .append("  trackerTrailCoords.push([newLat, newLng]);")
                         .append("  trackerTrailLine.setLatLngs(trackerTrailCoords);")
                         .append("}");
                markersJs.append("function focusNavigationCamera(lat, lng, zoom) {")
                         .append("  map.setView([lat, lng], zoom || 18, {animate: true});")
                         .append("}");
                markersJs.append("function fitFullRouteOverview() {")
                         .append("  if (activeRouteLine) { map.fitBounds(activeRouteLine.getBounds(), {padding: [48, 48]}); }")
                         .append("}");
            }
        }

        try {
            JSONArray globalReports = ValenciaGlobalIncidentStore.getReports();
            JSONArray localReports = sessionManager.getLocalReports();

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
                        String type = rep.optString("incident_type", "EMERGENCY").replace("_", " ").replace("'", "\\'");
                        String address = rep.optString("landmark_address", "Valencia City").replace("'", "\\'");

                        markersJs.append("L.marker([").append(lat).append(", ").append(lng).append("]).addTo(map)")
                                 .append(".bindPopup('<b>").append(type).append("</b><br>")
                                 .append(address).append("<br>Ref: #").append(ref.replace("'", "\\'")).append("');");
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
                        String type = rep.optString("incident_type", "EMERGENCY").replace("_", " ").replace("'", "\\'");
                        String address = rep.optString("landmark_address", "Valencia City").replace("'", "\\'");

                        markersJs.append("L.marker([").append(lat).append(", ").append(lng).append("]).addTo(map)")
                                 .append(".bindPopup('<b>").append(type).append("</b><br>")
                                 .append(address).append("<br>Ref: #").append(ref.replace("'", "\\'")).append("');");
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
                "<div id=\"routeInfoBadge\" style=\"display:none;position:fixed;top:12px;left:12px;z-index:9999;background:#142B4A;color:#fff;padding:8px 14px;border-radius:12px;font-family:sans-serif;font-size:13px;font-weight:bold;box-shadow:0 2px 8px rgba(0,0,0,0.25);\"></div>" +
                "<div id=\"map\"></div>" +
                "<script>" +
                markersJs.toString() +
                "</script>" +
                "</body></html>";

        if (mapWebView != null) {
            final boolean fetchAfterLoad = shouldFetchRoadRoute;
            mapWebView.setWebViewClient(new android.webkit.WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (fetchAfterLoad) {
                        fetchRoadRouteNative(activeStartLat, activeStartLng, targetIncidentLat, targetIncidentLng, true);
                    }
                }
            });
            mapWebView.loadDataWithBaseURL("https://example.com", mapHtml, "text/html", "UTF-8", null);
        }
    }

    private void fetchRoadRouteNative(double sLat, double sLng, double eLat, double eLng, boolean force) {
        long now = System.currentTimeMillis();
        if (!force && (now - lastRouteFetchMs < 4000)) return;
        lastRouteFetchMs = now;

        routeExecutor.execute(() -> {
            String[] urls = new String[]{
                    "https://router.project-osrm.org/route/v1/driving/" + sLng + "," + sLat + ";" + eLng + "," + eLat + "?overview=full&geometries=geojson&steps=true",
                    "https://routing.openstreetmap.de/routed-car/route/v1/driving/" + sLng + "," + sLat + ";" + eLng + "," + eLat + "?overview=full&geometries=geojson&steps=true"
            };

            String coordsJsArray = null;
            String infoHtml = null;
            final java.util.List<NavStep> parsedSteps = new java.util.ArrayList<>();
            final java.util.List<double[]> parsedPoints = new java.util.ArrayList<>();
            double parsedTotalDist = 0.0;
            double parsedTotalDur = 0.0;

            for (String routeUrl : urls) {
                java.net.HttpURLConnection conn = null;
                try {
                    java.net.URL url = new java.net.URL(routeUrl);
                    conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "ARMS-ValenciaCity/1.0");
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);

                    if (conn.getResponseCode() == 200) {
                        java.io.BufferedReader br = new java.io.BufferedReader(
                                new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        br.close();

                        JSONObject root = new JSONObject(sb.toString());
                        JSONArray routes = root.optJSONArray("routes");
                        if (routes != null && routes.length() > 0) {
                            JSONObject route = routes.getJSONObject(0);
                            parsedTotalDist = route.optDouble("distance", 0.0);
                            parsedTotalDur = route.optDouble("duration", 0.0);
                            String kmStr = String.format(Locale.US, "%.2f", parsedTotalDist / 1000.0);
                            int mins = Math.max(1, (int) Math.round(parsedTotalDur / 60.0));

                            parsedSteps.clear();
                            JSONArray legs = route.optJSONArray("legs");
                            if (legs != null && legs.length() > 0) {
                                JSONArray steps = legs.getJSONObject(0).optJSONArray("steps");
                                if (steps != null) {
                                    for (int s = 0; s < steps.length(); s++) {
                                        JSONObject st = steps.getJSONObject(s);
                                        NavStep ns = buildNavStepFromOsrm(st);
                                        parsedSteps.add(ns);
                                    }
                                }
                            }

                            JSONObject geom = route.optJSONObject("geometry");
                            JSONArray rawCoords = geom != null ? geom.optJSONArray("coordinates") : null;
                            if (rawCoords != null && rawCoords.length() >= 2) {
                                parsedPoints.clear();
                                StringBuilder arr = new StringBuilder("[");
                                for (int i = 0; i < rawCoords.length(); i++) {
                                    JSONArray pt = rawCoords.getJSONArray(i);
                                    double pLat = pt.getDouble(1);
                                    double pLng = pt.getDouble(0);
                                    parsedPoints.add(new double[]{pLat, pLng});
                                    if (i > 0) arr.append(",");
                                    arr.append("[").append(pLat).append(",").append(pLng).append("]");
                                }
                                arr.append("]");
                                coordsJsArray = arr.toString();
                                String firstInstr = !parsedSteps.isEmpty() ? parsedSteps.get(0).instruction : "Follow blue road route";
                                String safeStep = firstInstr.replace("'", "\\'").replace("\"", "");
                                infoHtml = kmStr + " km &bull; " + mins + " min<br><span style=\\'font-weight:normal;font-size:11px;\\'>" + safeStep + "</span>";
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }

            if (coordsJsArray == null) {
                double directDist = haversineMeters(sLat, sLng, eLat, eLng);
                parsedTotalDist = directDist;
                parsedTotalDur = Math.max(60.0, (directDist / 1000.0) / 35.0 * 3600.0);
                parsedPoints.clear();
                parsedPoints.add(new double[]{sLat, sLng});
                parsedPoints.add(new double[]{eLat, eLng});
                NavStep directStep = new NavStep();
                directStep.instruction = "Head toward accident location in Valencia City";
                directStep.turnIcon = "↑";
                directStep.distanceMeters = directDist;
                directStep.durationSeconds = parsedTotalDur;
                directStep.lat = eLat;
                directStep.lng = eLng;
                parsedSteps.clear();
                parsedSteps.add(directStep);
                coordsJsArray = "[[" + sLat + "," + sLng + "],[" + eLat + "," + eLng + "]]";
                infoHtml = String.format(Locale.US, "%.2f km", directDist / 1000.0) + " (Offline Direct Route)";
            }

            final String finalCoords = coordsJsArray;
            final String finalInfo = infoHtml;
            final double finalDist = parsedTotalDist;
            final double finalDur = parsedTotalDur;

            runOnUiThread(() -> {
                totalRouteDistanceMeters = finalDist;
                totalRouteDurationSeconds = finalDur;
                navSteps.clear();
                navSteps.addAll(parsedSteps);
                routePolylinePoints.clear();
                routePolylinePoints.addAll(parsedPoints);

                updateBottomNavSummary(finalDist, finalDur);
                if (getIntent().getBooleanExtra("auto_start_navigation", false) && !isNavigating) {
                    startTurnByTurnNavigation();
                } else if (isNavigating) {
                    updateActiveTurnBanner();
                }

                if (mapWebView != null) {
                    mapWebView.evaluateJavascript(
                            "if (typeof renderNativeRoadRoute === 'function') { renderNativeRoadRoute(" + finalCoords + ", '" + (isNavigating ? "" : finalInfo) + "', " + isNavigating + "); }",
                            null
                    );
                    if (isNavigating) {
                        mapWebView.evaluateJavascript(
                                "var b=document.getElementById('routeInfoBadge');if(b){b.style.display='none';}",
                                null
                        );
                    }
                }
            });
        });
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
        if (isSimulatingDrive) return;

        double lat = location.getLatitude();
        double lng = location.getLongitude();
        boolean insideValencia = (lat >= 7.75 && lat <= 8.05 && lng >= 124.95 && lng <= 125.25);

        // In Responder Mode, never let out-of-bounds emulator GPS snap the responder onto the accident report (7.9064, 125.0942)!
        if (isResponderMode && !insideValencia) {
            return;
        }

        if (insideValencia) {
            currentLatitude = lat;
            currentLongitude = lng;
        } else {
            currentLatitude = 7.9064;
            currentLongitude = 125.0942;
        }
        currentAccuracy = location.hasAccuracy() ? location.getAccuracy() : 3.0f;
        hasReceivedLiveGps = true;

        if (mapWebView != null) {
            if (isResponderMode && !isCitizenTrackingMode) {
                activeStartLat = currentLatitude;
                activeStartLng = currentLongitude;
                mapWebView.evaluateJavascript(
                        "if (typeof updateResponderTracker === 'function') { updateResponderTracker(" + currentLatitude + ", " + currentLongitude + "); }",
                        null
                );
                if (isNavigating && !isSimulatingDrive) {
                    updateNavigationProgress(currentLatitude, currentLongitude);
                }
                fetchRoadRouteNative(currentLatitude, currentLongitude, targetIncidentLat, targetIncidentLng, false);
                pushLiveResponderLocation(currentLatitude, currentLongitude);
            } else if (!isResponderMode) {
                if (!mapInitialized) {
                    mapInitialized = true;
                    loadSingleMarkerMapHtml(currentLatitude, currentLongitude);
                } else {
                    mapWebView.evaluateJavascript("updateLocation(" + currentLatitude + ", " + currentLongitude + ");", null);
                }
            }
        }

        resolveAddressFromCoordinates(currentLatitude, currentLongitude);
    }

    private void pushLiveResponderLocation(double lat, double lng) {
        long now = System.currentTimeMillis();
        if (now - lastTelemetryPushMs < 8000) return;
        lastTelemetryPushMs = now;

        String ref = getIntent().getStringExtra("incident_ref");
        if (ref == null || ref.trim().isEmpty()) return;

        String agency = sessionManager.getUserAgency();
        ValenciaGlobalIncidentStore.updateResponderLocation(ref, lat, lng, agency);

        String token = sessionManager.getAuthToken();
        if (token != null && !token.startsWith("arms_instant_token_") && !token.startsWith("arms_fallback_token_")) {
            try {
                JSONObject locPayload = new JSONObject();
                locPayload.put("incident_reference", ref);
                locPayload.put("latitude", lat);
                locPayload.put("longitude", lng);
                locPayload.put("agency_type", agency != null ? agency : "RESPONDER");
                ApiClient.post("/responder/location", locPayload, token, new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {}
                    @Override
                    public void onError(int statusCode, String errorMessage) {}
                });
            } catch (Exception ignored) {}
        }
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

                sb.append(", Valencia City, Bukidnon, Philippines");
                preciseName = sb.toString();
            } else {
                preciseName = "Barangay Poblacion, Valencia City, Bukidnon, Philippines";
            }

            if (etLocation != null) {
                etLocation.setText(preciseName);
            }
        } catch (Exception e) {
            if (etLocation != null) {
                etLocation.setText("Barangay Poblacion, Valencia City, Bukidnon, Philippines");
            }
        }
    }

    private void confirmAndSendReport() {
        String typeDisplay = actvIncidentType != null ? actvIncidentType.getText().toString().trim() : "Road Accident / Collision";
        String location = etLocation != null && etLocation.getText() != null
                ? etLocation.getText().toString().trim()
                : "Barangay Poblacion, Valencia City, Bukidnon, Philippines";
        String mediaSummary = (hasVideo ? "Video attached" : "Photo attached");

        new AlertDialog.Builder(this)
                .setTitle("Review Emergency Report")
                .setMessage("Incident Type: " + typeDisplay
                        + "\nLocation: " + location
                        + "\nCoordinates: " + String.format(Locale.getDefault(), "%.5f, %.5f", currentLatitude, currentLongitude)
                        + "\nEvidence: " + mediaSummary)
                .setPositiveButton("Confirm & Send", (dialog, which) -> sendEmergencyReportToResponders())
                .setNegativeButton("Edit", (dialog, which) -> dialog.dismiss())
                .show();
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
            ValenciaGlobalIncidentStore.addNotification(alertObj);
            com.example.accidentreportandmonitoringsystem.notifications.ArmsNotificationHelper.showEmergencyNotification(
                    this,
                    "Emergency Report Submitted: #" + incidentRef,
                    incidentType.replace("_", " ") + " at " + location
            );
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

            final String idempotencyKey = java.util.UUID.randomUUID().toString();
            JSONObject payload = new JSONObject();
            payload.put("idempotency_key", idempotencyKey);
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

            final String apiEndpoint = isLoggedInCitizen ? "/incidents" : "/incidents/guest";
            ApiClient.post(apiEndpoint, payload, authToken, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    sessionManager.updateLocalReportSyncStatus(incidentRef, "SYNCED");
                    String guestToken = response.optString("tracking_token", "");
                    if (!guestToken.isEmpty()) {
                        sessionManager.saveGuestTrackingToken(guestToken);
                    }
                }
                @Override
                public void onError(int statusCode, String errorMessage) {
                    sessionManager.enqueuePendingSyncReport(idempotencyKey, incidentRef, apiEndpoint, payload, authToken);
                }
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

    private void initGoogleMapsNavigationUi() {
        navTts = new android.speech.tts.TextToSpeech(this, status -> {
            if (status == android.speech.tts.TextToSpeech.SUCCESS && navTts != null) {
                navTts.setLanguage(Locale.US);
            }
        });

        if (btnStartNavigation != null) {
            btnStartNavigation.setOnClickListener(v -> {
                if (isNavigating) {
                    stopTurnByTurnNavigation();
                } else {
                    startTurnByTurnNavigation();
                }
            });
        }

        if (btnViewAllSteps != null) {
            btnViewAllSteps.setOnClickListener(v -> showAllTurnStepsDialog());
        }

        if (btnRecenterNav != null) {
            btnRecenterNav.setOnClickListener(v -> {
                if (mapWebView != null) {
                    mapWebView.evaluateJavascript(
                            "if (typeof focusNavigationCamera === 'function') { focusNavigationCamera(" + activeStartLat + ", " + activeStartLng + ", 18); }",
                            null
                    );
                }
            });
        }

        if (btnSimulateNav != null) {
            btnSimulateNav.setOnClickListener(v -> toggleSimulatedDrive());
        }

        if (btnToggleNavVoice != null) {
            btnToggleNavVoice.setOnClickListener(v -> {
                isVoiceMuted = !isVoiceMuted;
                btnToggleNavVoice.setText(isVoiceMuted ? "🔇" : "🔊");
                if (isVoiceMuted && navTts != null) {
                    navTts.stop();
                }
            });
        }
    }

    private void startTurnByTurnNavigation() {
        isNavigating = true;
        currentStepIndex = 0;

        if (rlMapTopBar != null) rlMapTopBar.setVisibility(View.GONE);
        if (cardNavBanner != null) cardNavBanner.setVisibility(View.VISIBLE);
        if (layoutMapNavControls != null) layoutMapNavControls.setVisibility(View.VISIBLE);

        if (btnStartNavigation != null) {
            btnStartNavigation.setText("Exit Navigation");
            btnStartNavigation.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DC2626")));
        }

        if (mapWebView != null) {
            mapWebView.evaluateJavascript("var b=document.getElementById('routeInfoBadge');if(b){b.style.display='none';}", null);
            mapWebView.evaluateJavascript(
                    "if (typeof focusNavigationCamera === 'function') { focusNavigationCamera(" + activeStartLat + ", " + activeStartLng + ", 18); }",
                    null
            );
        }

        updateActiveTurnBanner();
        if (!navSteps.isEmpty()) {
            speakInstruction("Starting navigation. " + navSteps.get(0).instruction);
        } else {
            speakInstruction("Starting navigation toward the accident location in Valencia City.");
        }

        // Automatically begin real-time step-by-step road guidance along the route
        if (!isSimulatingDrive && routePolylinePoints.size() >= 2) {
            toggleSimulatedDrive();
        }
    }

    private void stopTurnByTurnNavigation() {
        isNavigating = false;
        stopSimulatedDrive();
        if (navTts != null) navTts.stop();

        if (rlMapTopBar != null) rlMapTopBar.setVisibility(View.VISIBLE);
        if (cardNavBanner != null) cardNavBanner.setVisibility(View.GONE);
        if (layoutMapNavControls != null) layoutMapNavControls.setVisibility(View.GONE);

        if (btnStartNavigation != null) {
            btnStartNavigation.setText("Start Navigation");
            btnStartNavigation.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1D4ED8")));
        }

        updateBottomNavSummary(totalRouteDistanceMeters, totalRouteDurationSeconds);
        if (mapWebView != null) {
            mapWebView.evaluateJavascript("if (typeof fitFullRouteOverview === 'function') { fitFullRouteOverview(); }", null);
        }
    }

    private void updateBottomNavSummary(double distMeters, double durSeconds) {
        int mins = Math.max(1, (int) Math.round(durSeconds / 60.0));
        String distText = distMeters >= 1000.0
                ? String.format(Locale.US, "%.1f km", distMeters / 1000.0)
                : String.format(Locale.US, "%d m", Math.max(10, (int) Math.round(distMeters)));
        String arrivalTime = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(new Date(System.currentTimeMillis() + (long) (durSeconds * 1000)));

        if (tvNavEta != null) {
            tvNavEta.setText(mins + (mins == 1 ? " min" : " mins"));
        }
        if (tvNavDistanceArrival != null) {
            tvNavDistanceArrival.setText(distText + " • Arrive " + arrivalTime);
        }
    }

    private void updateActiveTurnBanner() {
        if (navSteps.isEmpty()) {
            if (tvNavTurnIcon != null) tvNavTurnIcon.setText("↑");
            if (tvNavStepDistance != null) tvNavStepDistance.setText("Navigating");
            if (tvNavInstruction != null) tvNavInstruction.setText("Follow blue route to accident");
            if (tvNavNextStep != null) tvNavNextStep.setText("Valencia City, Bukidnon");
            return;
        }

        int idx = Math.min(currentStepIndex, navSteps.size() - 1);
        NavStep cur = navSteps.get(idx);
        if (tvNavTurnIcon != null) tvNavTurnIcon.setText(cur.turnIcon);
        if (tvNavStepDistance != null) tvNavStepDistance.setText(formatDistanceShort(cur.distanceMeters));
        if (tvNavInstruction != null) tvNavInstruction.setText(cur.instruction);

        if (tvNavNextStep != null) {
            if (idx + 1 < navSteps.size()) {
                NavStep next = navSteps.get(idx + 1);
                tvNavNextStep.setText("Then: " + next.instruction);
            } else {
                tvNavNextStep.setText("Destination ahead — Valencia City");
            }
        }
    }

    private void updateNavigationProgress(double curLat, double curLng) {
        activeStartLat = curLat;
        activeStartLng = curLng;

        if (mapWebView != null) {
            mapWebView.evaluateJavascript(
                    "if (typeof updateResponderTracker === 'function') { updateResponderTracker(" + curLat + ", " + curLng + "); }" +
                    "if (typeof focusNavigationCamera === 'function') { focusNavigationCamera(" + curLat + ", " + curLng + ", 18); }",
                    null
            );
        }

        double distToDest = haversineMeters(curLat, curLng, targetIncidentLat, targetIncidentLng);
        if (distToDest <= 30.0) {
            if (tvNavTurnIcon != null) tvNavTurnIcon.setText("🏁");
            if (tvNavStepDistance != null) tvNavStepDistance.setText("0 m");
            if (tvNavInstruction != null) tvNavInstruction.setText("You have arrived at the accident location");
            if (tvNavNextStep != null) tvNavNextStep.setText("Mark yourself On Scene when ready");
            if (tvNavEta != null) tvNavEta.setText("Arrived");
            if (tvNavDistanceArrival != null) tvNavDistanceArrival.setText("0 m remaining");
            speakInstruction("You have arrived at the accident location.");
            stopSimulatedDrive();
            return;
        }

        // Advance current turn step if within 35 meters of the next maneuver point
        if (currentStepIndex + 1 < navSteps.size()) {
            NavStep nextStep = navSteps.get(currentStepIndex + 1);
            double distToNext = haversineMeters(curLat, curLng, nextStep.lat, nextStep.lng);
            if (tvNavStepDistance != null) {
                tvNavStepDistance.setText("In " + formatDistanceShort(distToNext));
            }
            if (distToNext <= 35.0) {
                currentStepIndex++;
                updateActiveTurnBanner();
                speakInstruction(navSteps.get(currentStepIndex).instruction);
            }
        }

        // Update live remaining distance & estimated arrival time
        double estRemainingDur = totalRouteDistanceMeters > 1.0
                ? totalRouteDurationSeconds * Math.min(1.0, distToDest / totalRouteDistanceMeters)
                : Math.max(60.0, (distToDest / 1000.0) / 35.0 * 3600.0);
        updateBottomNavSummary(distToDest, Math.max(30.0, estRemainingDur));
    }

    private void toggleSimulatedDrive() {
        if (isSimulatingDrive) {
            stopSimulatedDrive();
            return;
        }
        if (routePolylinePoints.size() < 2) {
            Toast.makeText(this, "Waiting for road route to finish loading...", Toast.LENGTH_SHORT).show();
            return;
        }

        isSimulatingDrive = true;
        simPointIndex = 0;
        if (btnSimulateNav != null) btnSimulateNav.setText("Stop Drive");

        navSimRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isSimulatingDrive || !isNavigating || simPointIndex >= routePolylinePoints.size()) {
                    stopSimulatedDrive();
                    return;
                }
                double[] pt = routePolylinePoints.get(simPointIndex);
                updateNavigationProgress(pt[0], pt[1]);
                pushLiveResponderLocation(pt[0], pt[1]);
                simPointIndex++;
                navSimHandler.postDelayed(this, 700);
            }
        };
        navSimHandler.post(navSimRunnable);
    }

    private void stopSimulatedDrive() {
        isSimulatingDrive = false;
        if (navSimRunnable != null) {
            navSimHandler.removeCallbacks(navSimRunnable);
        }
        if (btnSimulateNav != null) btnSimulateNav.setText("Preview Drive");
    }

    private void showAllTurnStepsDialog() {
        if (navSteps.isEmpty()) {
            Toast.makeText(this, "Route directions are still loading...", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = new String[navSteps.size()];
        for (int i = 0; i < navSteps.size(); i++) {
            NavStep s = navSteps.get(i);
            items[i] = s.turnIcon + "  " + s.instruction + "  (" + formatDistanceShort(s.distanceMeters) + ")";
        }
        new AlertDialog.Builder(this)
                .setTitle("Turn-by-Turn Directions")
                .setItems(items, null)
                .setPositiveButton("Close", null)
                .show();
    }

    private void speakInstruction(String text) {
        if (isVoiceMuted || navTts == null || text == null || text.isEmpty()) return;
        try {
            navTts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "arms_nav");
        } catch (Exception ignored) {}
    }

    private NavStep buildNavStepFromOsrm(JSONObject st) {
        NavStep ns = new NavStep();
        ns.distanceMeters = st.optDouble("distance", 0.0);
        ns.durationSeconds = st.optDouble("duration", 0.0);
        String roadName = st.optString("name", "").trim();
        JSONObject maneuver = st.optJSONObject("maneuver");
        String type = maneuver != null ? maneuver.optString("type", "continue") : "continue";
        String modifier = maneuver != null ? maneuver.optString("modifier", "") : "";
        JSONArray loc = maneuver != null ? maneuver.optJSONArray("location") : null;
        if (loc != null && loc.length() >= 2) {
            ns.lng = loc.optDouble(0, targetIncidentLng);
            ns.lat = loc.optDouble(1, targetIncidentLat);
        } else {
            ns.lat = targetIncidentLat;
            ns.lng = targetIncidentLng;
        }

        String streetSuffix = roadName.isEmpty() ? "" : " onto " + roadName;
        if ("arrive".equalsIgnoreCase(type)) {
            ns.turnIcon = "🏁";
            ns.instruction = "Arrive at accident location";
        } else if ("depart".equalsIgnoreCase(type)) {
            ns.turnIcon = "↑";
            ns.instruction = "Head " + (modifier.isEmpty() ? "forward" : modifier) + (roadName.isEmpty() ? "" : " on " + roadName);
        } else if ("roundabout".equalsIgnoreCase(type) || "rotary".equalsIgnoreCase(type)) {
            ns.turnIcon = "↻";
            ns.instruction = "Enter roundabout" + streetSuffix;
        } else if (modifier.contains("right")) {
            ns.turnIcon = modifier.contains("slight") ? "↗" : "↱";
            ns.instruction = (modifier.contains("slight") ? "Keep right" : "Turn right") + streetSuffix;
        } else if (modifier.contains("left")) {
            ns.turnIcon = modifier.contains("slight") ? "↖" : "↰";
            ns.instruction = (modifier.contains("slight") ? "Keep left" : "Turn left") + streetSuffix;
        } else if (modifier.contains("uturn")) {
            ns.turnIcon = "↺";
            ns.instruction = "Make a U-turn" + streetSuffix;
        } else {
            ns.turnIcon = "↑";
            ns.instruction = "Continue straight" + (roadName.isEmpty() ? "" : " on " + roadName);
        }
        return ns;
    }

    private String formatDistanceShort(double meters) {
        if (meters >= 1000.0) {
            return String.format(Locale.US, "%.1f km", meters / 1000.0);
        }
        return Math.max(10, (int) Math.round(meters)) + " m";
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSimulatedDrive();
        if (navTts != null) {
            try {
                navTts.stop();
                navTts.shutdown();
            } catch (Exception ignored) {}
        }
        routeExecutor.shutdownNow();
        if (trackingPollRunnable != null) {
            trackingPollHandler.removeCallbacks(trackingPollRunnable);
        }
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (Exception ignored) {}
        }
    }
}
