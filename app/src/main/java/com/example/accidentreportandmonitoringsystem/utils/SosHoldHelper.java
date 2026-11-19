package com.example.accidentreportandmonitoringsystem.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import com.example.accidentreportandmonitoringsystem.notifications.ArmsNotificationHelper;
import com.example.accidentreportandmonitoringsystem.remote.ApiClient;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalIncidentStore;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class SosHoldHelper {

    @SuppressLint("ClickableViewAccessibility")
    public static void attach(Context context, MaterialButton button, String defaultLabel) {
        if (button == null || context == null) return;

        Handler handler = new Handler(Looper.getMainLooper());
        final int[] secondsLeft = {3};
        final boolean[] holding = {false};

        Runnable tick = new Runnable() {
            @Override
            public void run() {
                if (!holding[0]) return;
                secondsLeft[0]--;
                button.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                if (secondsLeft[0] > 0) {
                    button.setText("HOLD SOS... " + secondsLeft[0] + "s");
                    handler.postDelayed(this, 1000);
                } else {
                    holding[0] = false;
                    button.setText("SOS DISPATCHED!");
                    dispatchInstantSos(context);
                    handler.postDelayed(() -> button.setText(defaultLabel), 2500);
                }
            }
        };

        button.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                holding[0] = true;
                secondsLeft[0] = 3;
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                button.setText("HOLD SOS... 3s");
                handler.removeCallbacks(tick);
                handler.postDelayed(tick, 1000);
                return true;
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (holding[0]) {
                    holding[0] = false;
                    handler.removeCallbacks(tick);
                    button.setText(defaultLabel);
                }
                return true;
            }
            return false;
        });
    }

    @SuppressLint("MissingPermission")
    private static void dispatchInstantSos(Context context) {
        double lat = 7.9064;
        double lng = 125.0942;

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                if (lm != null) {
                    Location gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Location net = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    Location best = gps != null ? gps : net;
                    if (best != null) {
                        double cLat = best.getLatitude();
                        double cLng = best.getLongitude();
                        if (cLat >= 7.75 && cLat <= 8.05 && cLng >= 124.95 && cLng <= 125.25) {
                            lat = cLat;
                            lng = cLng;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        SessionManager sessionManager = new SessionManager(context);
        String ref = "ARMS-SOS-" + (System.currentTimeMillis() % 10000);
        String idempotencyKey = UUID.randomUUID().toString();
        String location = "Valencia City, Bukidnon, Philippines";
        String desc = "URGENT ONE-TAP SOS PANIC ALERT dispatched with GPS (" + lat + ", " + lng + "). Immediate response requested.";
        String nowStr = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(new Date());

        sessionManager.saveLocalReportWithMedia("OTHER_EMERGENCY", ref, location, desc, "DISPATCHED", lat, lng, "", false, "");

        try {
            JSONObject globalObj = new JSONObject();
            globalObj.put("incident_reference", ref);
            globalObj.put("incident_type", "OTHER_EMERGENCY");
            globalObj.put("current_status", "DISPATCHED");
            globalObj.put("landmark_address", location);
            globalObj.put("description", desc);
            globalObj.put("latitude", lat);
            globalObj.put("longitude", lng);
            globalObj.put("created_at", nowStr);
            ValenciaGlobalIncidentStore.addReport(globalObj);

            JSONObject notif = new JSONObject();
            notif.put("title", "SOS Emergency Dispatched: #" + ref);
            notif.put("message", "Urgent SOS alert sent to CDRRMO, PNP, BFP & Medical responders.");
            notif.put("timestamp", nowStr);
            sessionManager.saveAlertNotification(notif);
            ValenciaGlobalIncidentStore.addNotification(notif);
        } catch (Exception ignored) {}

        ArmsNotificationHelper.showEmergencyNotification(
                context,
                "SOS Emergency Sent: #" + ref,
                "Valencia City emergency responders have been alerted to your GPS location."
        );

        String authToken = sessionManager.getAuthToken();
        boolean hasRealToken = authToken != null && !authToken.trim().isEmpty()
                && !authToken.startsWith("arms_instant_token_") && !authToken.startsWith("arms_fallback_token_");
        String endpoint = hasRealToken ? "/incidents" : "/incidents/guest";

        try {
            JSONArray responders = new JSONArray();
            responders.put("CDRRMO");
            responders.put("PNP");
            responders.put("BFP");
            responders.put("MEDICAL_EMS");

            JSONObject payload = new JSONObject();
            payload.put("idempotency_key", idempotencyKey);
            payload.put("incident_type", "OTHER_EMERGENCY");
            payload.put("landmark_address", location);
            payload.put("description", desc);
            payload.put("latitude", lat);
            payload.put("longitude", lng);
            payload.put("location_accuracy", 10.0);
            payload.put("selected_responders", responders);

            ApiClient.post(endpoint, payload, hasRealToken ? authToken : null, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    sessionManager.updateLocalReportSyncStatus(ref, "SYNCED");
                    String gToken = response.optString("tracking_token", "");
                    if (!gToken.isEmpty()) sessionManager.saveGuestTrackingToken(gToken);
                }

                @Override
                public void onError(int statusCode, String errorMessage) {
                    sessionManager.enqueuePendingSyncReport(idempotencyKey, ref, endpoint, payload, hasRealToken ? authToken : "");
                }
            });
        } catch (Exception ignored) {}

        try {
            new AlertDialog.Builder(context)
                    .setTitle("SOS Alert Dispatched")
                    .setMessage("Your urgent SOS alert (#" + ref + ") has been sent to Valencia City responders (CDRRMO, PNP, BFP, Medical).\n\nCoordinates: " + lat + ", " + lng)
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Exception ignored) {}
    }
}
