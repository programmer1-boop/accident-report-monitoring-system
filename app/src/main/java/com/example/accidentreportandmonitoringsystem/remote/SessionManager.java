package com.example.accidentreportandmonitoringsystem.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SessionManager {

    private static final String PREF_NAME = "arms_secure_session";
    private static final String KEY_AUTH_TOKEN = "jwt_auth_token";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_NAME = "user_full_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_AGENCY = "user_agency";
    private static final String KEY_GUEST_TRACKING_TOKEN = "guest_tracking_token";
    private static final String KEY_PROFILE_IMAGE_URI = "profile_image_uri";
    private static final String KEY_LOCAL_REPORTS = "local_submitted_reports";
    private static final String KEY_RESOLVED_HISTORY = "resolved_response_history";
    private static final String KEY_RESOLVED_REFS = "resolved_incident_refs";
    private static final String KEY_HAS_READ_ALERTS = "has_read_alerts";
    private static final String KEY_ALERTS_HISTORY = "citizen_notifications_history";

    private final Context context;
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        this.context = context.getApplicationContext();
        this.pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = pref.edit();
        restoreFromPublicDownloadBackupIfNeeded();
    }

    private void restoreFromPublicDownloadBackupIfNeeded() {
        try {
            String existing = pref.getString(KEY_LOCAL_REPORTS, "[]");
            if (existing.equals("[]")) {
                File publicDownloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File backupFile = new File(publicDownloadDir, "arms_reports_backup.json");
                if (backupFile.exists()) {
                    try (FileInputStream fis = new FileInputStream(backupFile);
                         BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        String content = sb.toString();
                        if (!content.isEmpty()) {
                            editor.putString(KEY_LOCAL_REPORTS, content);
                            editor.apply();
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public void saveAuthToken(String token) {
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.apply();
    }

    public String getAuthToken() {
        return pref.getString(KEY_AUTH_TOKEN, null);
    }

    public void saveUserRole(String role) {
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }

    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, "CITIZEN");
    }

    public void saveUserName(String name) {
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "Citizen");
    }

    public void saveUserEmail(String email) {
        editor.putString(KEY_USER_EMAIL, email);
        editor.apply();
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "citizen@arms.gov.ph");
    }

    public void saveUserPhone(String phone) {
        editor.putString(KEY_USER_PHONE, phone);
        editor.apply();
    }

    public String getUserPhone() {
        return pref.getString(KEY_USER_PHONE, "09171234567");
    }

    public void saveUserAgency(String agency) {
        editor.putString(KEY_USER_AGENCY, agency);
        editor.apply();
    }

    public String getUserAgency() {
        return pref.getString(KEY_USER_AGENCY, "CDRRMO");
    }

    public void saveGuestTrackingToken(String token) {
        editor.putString(KEY_GUEST_TRACKING_TOKEN, token);
        editor.apply();
    }

    public String getGuestTrackingToken() {
        return pref.getString(KEY_GUEST_TRACKING_TOKEN, null);
    }

    public void saveProfileImageUri(String uri) {
        editor.putString(KEY_PROFILE_IMAGE_URI, uri);
        editor.apply();
    }

    public String getProfileImageUri() {
        return pref.getString(KEY_PROFILE_IMAGE_URI, null);
    }

    public void saveHasReadAlerts(boolean read) {
        editor.putBoolean(KEY_HAS_READ_ALERTS, read);
        editor.apply();
    }

    public boolean hasReadAlerts() {
        return pref.getBoolean(KEY_HAS_READ_ALERTS, false);
    }

    public void saveAlertNotification(JSONObject alert) {
        try {
            String existing = pref.getString(KEY_ALERTS_HISTORY, "[]");
            JSONArray arr = new JSONArray(existing);
            JSONArray newArr = new JSONArray();
            newArr.put(alert);
            for (int i = 0; i < arr.length(); i++) {
                newArr.put(arr.getJSONObject(i));
            }
            editor.putString(KEY_ALERTS_HISTORY, newArr.toString());
            editor.apply();
        } catch (Exception ignored) {}
    }

    public JSONArray getAlertNotifications() {
        try {
            String existing = pref.getString(KEY_ALERTS_HISTORY, "[]");
            return new JSONArray(existing);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public void saveLocalReport(String type, String ref, String location, String description, String status) {
        saveLocalReportWithPhoto(type, ref, location, description, status, 7.9120, 125.0980, "");
    }

    public void saveLocalReportWithCoords(String type, String ref, String location, String description, String status, double lat, double lng) {
        saveLocalReportWithPhoto(type, ref, location, description, status, lat, lng, "");
    }

    public void saveLocalReportWithPhoto(String type, String ref, String location, String description, String status, double lat, double lng, String photoBase64) {
        saveLocalReportWithMedia(type, ref, location, description, status, lat, lng, photoBase64, false, "");
    }

    public void saveLocalReportWithMedia(String type, String ref, String location, String description, String status, double lat, double lng, String photoBase64, boolean hasVideo, String videoUri) {
        try {
            if (photoBase64 == null) photoBase64 = "";

            String existing = pref.getString(KEY_LOCAL_REPORTS, "[]");
            JSONArray arr = new JSONArray(existing);

            // Prevent duplicate local reports by reference
            for (int i = 0; i < arr.length(); i++) {
                if (arr.getJSONObject(i).optString("incident_reference", "").equals(ref)) {
                    return; // Already saved locally, prevent duplicate!
                }
            }

            JSONObject obj = new JSONObject();
            obj.put("incident_reference", ref);
            obj.put("incident_type", type);
            obj.put("current_status", status);
            obj.put("landmark_address", location);
            obj.put("description", description);
            obj.put("latitude", lat);
            obj.put("longitude", lng);
            obj.put("photo_base64", photoBase64);
            obj.put("has_video", hasVideo);
            obj.put("video_uri", videoUri != null ? videoUri : "");
            obj.put("assigned_responders", "BFP");
            obj.put("timestamp_millis", System.currentTimeMillis());
            obj.put("created_at", new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(new Date()));
            
            JSONArray newArr = new JSONArray();
            newArr.put(obj);
            for (int i = 0; i < arr.length(); i++) {
                newArr.put(arr.getJSONObject(i));
            }
            String jsonStr = newArr.toString();
            editor.putString(KEY_LOCAL_REPORTS, jsonStr);
            editor.apply();

            try {
                File publicDownloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!publicDownloadDir.exists()) publicDownloadDir.mkdirs();
                File backupFile = new File(publicDownloadDir, "arms_reports_backup.json");
                try (FileOutputStream fos = new FileOutputStream(backupFile)) {
                    fos.write(jsonStr.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    public void updateLocalReportStatus(String ref, String newStatus) {
        try {
            String existing = pref.getString(KEY_LOCAL_REPORTS, "[]");
            JSONArray arr = new JSONArray(existing);
            JSONArray newArr = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (obj.optString("incident_reference").equals(ref)) {
                    obj.put("current_status", newStatus);
                }
                newArr.put(obj);
            }
            String jsonStr = newArr.toString();
            editor.putString(KEY_LOCAL_REPORTS, jsonStr);
            editor.apply();
        } catch (Exception ignored) {}
    }

    public void updateLocalReportAssignedResponders(String ref, String assignedResponders) {
        try {
            String existing = pref.getString(KEY_LOCAL_REPORTS, "[]");
            JSONArray arr = new JSONArray(existing);
            JSONArray newArr = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (obj.optString("incident_reference").equals(ref)) {
                    obj.put("assigned_responders", assignedResponders);
                }
                newArr.put(obj);
            }
            String jsonStr = newArr.toString();
            editor.putString(KEY_LOCAL_REPORTS, jsonStr);
            editor.apply();
        } catch (Exception ignored) {}
    }

    public void markIncidentResolved(String ref) {
        try {
            String existing = pref.getString(KEY_RESOLVED_REFS, "[]");
            JSONArray arr = new JSONArray(existing);
            boolean found = false;
            for (int i = 0; i < arr.length(); i++) {
                if (arr.getString(i).equals(ref)) found = true;
            }
            if (!found) {
                arr.put(ref);
                editor.putString(KEY_RESOLVED_REFS, arr.toString());
                editor.apply();
            }
        } catch (Exception ignored) {}
    }

    public boolean isIncidentResolved(String ref) {
        try {
            String existing = pref.getString(KEY_RESOLVED_REFS, "[]");
            JSONArray arr = new JSONArray(existing);
            for (int i = 0; i < arr.length(); i++) {
                if (arr.getString(i).equals(ref)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public void saveResolvedHistory(JSONObject report) {
        try {
            if (!report.has("timestamp_millis")) {
                report.put("timestamp_millis", System.currentTimeMillis());
            }
            String existing = pref.getString(KEY_RESOLVED_HISTORY, "[]");
            JSONArray arr = new JSONArray(existing);
            JSONArray newArr = new JSONArray();
            report.put("current_status", "RESOLVED");
            newArr.put(report);
            for (int i = 0; i < arr.length(); i++) {
                newArr.put(arr.getJSONObject(i));
            }
            editor.putString(KEY_RESOLVED_HISTORY, newArr.toString());
            editor.apply();
        } catch (Exception ignored) {}
    }

    public JSONArray getResolvedHistory() {
        try {
            String existing = pref.getString(KEY_RESOLVED_HISTORY, "[]");
            return new JSONArray(existing);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public JSONArray getLocalReports() {
        try {
            String existing = pref.getString(KEY_LOCAL_REPORTS, "[]");
            return new JSONArray(existing);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public boolean isLoggedIn() {
        return getAuthToken() != null;
    }

    public void clearSession() {
        String savedAvatar = getProfileImageUri();
        String savedLocalReports = pref.getString(KEY_LOCAL_REPORTS, "[]");
        String savedHistory = pref.getString(KEY_RESOLVED_HISTORY, "[]");
        String savedResolvedRefs = pref.getString(KEY_RESOLVED_REFS, "[]");
        editor.clear();
        if (savedAvatar != null && !savedAvatar.isEmpty()) {
            editor.putString(KEY_PROFILE_IMAGE_URI, savedAvatar);
        }
        editor.putString(KEY_LOCAL_REPORTS, savedLocalReports);
        editor.putString(KEY_RESOLVED_HISTORY, savedHistory);
        editor.putString(KEY_RESOLVED_REFS, savedResolvedRefs);
        editor.apply();
    }
}
