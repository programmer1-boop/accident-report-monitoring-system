package com.example.accidentreportandmonitoringsystem.remote;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

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
    private static final String KEY_REGISTERED_USERS_JSON = "registered_users_json";

    private final Context context;
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        this.context = context.getApplicationContext();
        this.pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = pref.edit();
    }

    public void saveRegisteredUser(JSONObject userObj) {
        try {
            JSONArray users = getRegisteredUsers();
            String email = userObj.optString("email", "").toLowerCase();
            boolean exists = false;
            for (int i = 0; i < users.length(); i++) {
                if (users.getJSONObject(i).optString("email", "").toLowerCase().equals(email)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                users.put(userObj);
                editor.putString(KEY_REGISTERED_USERS_JSON, users.toString());
                editor.apply();
                ValenciaGlobalUserStore.addUser(userObj);
            }
        } catch (Exception ignored) {}
    }

    public JSONArray getRegisteredUsers() {
        try {
            String json = pref.getString(KEY_REGISTERED_USERS_JSON, "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                ValenciaGlobalUserStore.addUser(arr.getJSONObject(i));
            }
            return arr;
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public boolean isLoggedIn() {
        String token = getAuthToken();
        return token != null && !token.trim().isEmpty();
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
        return pref.getString(KEY_USER_NAME, "ARMS User");
    }

    public void saveUserEmail(String email) {
        editor.putString(KEY_USER_EMAIL, email);
        editor.apply();
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "user@arms.gov.ph");
    }

    public void saveUserPhone(String phone) {
        editor.putString(KEY_USER_PHONE, phone);
        editor.apply();
    }

    public String getUserPhone() {
        return pref.getString(KEY_USER_PHONE, "09123456789");
    }

    public void saveUserAgency(String agency) {
        editor.putString(KEY_USER_AGENCY, agency);
        editor.apply();
    }

    public String getUserAgency() {
        return pref.getString(KEY_USER_AGENCY, "General");
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

    public void saveLocalReport(JSONObject report) {
        try {
            JSONArray reports = getLocalReports();
            JSONArray newReports = new JSONArray();
            newReports.put(report);
            for (int i = 0; i < reports.length(); i++) {
                newReports.put(reports.getJSONObject(i));
            }
            editor.putString(KEY_LOCAL_REPORTS, newReports.toString());
            editor.apply();
        } catch (Exception ignored) {}
    }

    public void saveLocalReportWithMedia(String type, String ref, String location, String desc, String status, double lat, double lng, String photoB64, boolean hasVideo, String videoUri) {
        try {
            JSONObject rep = new JSONObject();
            rep.put("incident_type", type);
            rep.put("incident_reference", ref);
            rep.put("landmark_address", location);
            rep.put("description", desc);
            rep.put("current_status", status);
            rep.put("latitude", lat);
            rep.put("longitude", lng);
            rep.put("photo_base64", photoB64);
            rep.put("has_video", hasVideo);
            rep.put("video_uri", videoUri);
            rep.put("timestamp_millis", System.currentTimeMillis());
            rep.put("created_at", new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date()));
            saveLocalReport(rep);
            ValenciaGlobalIncidentStore.addReport(rep);
        } catch (Exception ignored) {}
    }

    public JSONArray getLocalReports() {
        try {
            String json = pref.getString(KEY_LOCAL_REPORTS, "[]");
            return new JSONArray(json);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public void updateLocalReportStatus(String ref, String newStatus) {
        try {
            JSONArray reports = getLocalReports();
            JSONArray updated = new JSONArray();
            for (int i = 0; i < reports.length(); i++) {
                JSONObject obj = reports.getJSONObject(i);
                if (obj.optString("incident_reference").equals(ref)) {
                    obj.put("current_status", newStatus);
                }
                updated.put(obj);
            }
            editor.putString(KEY_LOCAL_REPORTS, updated.toString());
            editor.apply();
        } catch (Exception ignored) {}
    }

    public void updateLocalReportAssignedResponders(String ref, String assignedResponders) {
        try {
            JSONArray reports = getLocalReports();
            JSONArray updated = new JSONArray();
            for (int i = 0; i < reports.length(); i++) {
                JSONObject obj = reports.getJSONObject(i);
                if (obj.optString("incident_reference").equals(ref)) {
                    obj.put("assigned_responders", assignedResponders);
                }
                updated.put(obj);
            }
            editor.putString(KEY_LOCAL_REPORTS, updated.toString());
            editor.apply();
        } catch (Exception ignored) {}
    }

    public void saveResolvedReportToHistory(JSONObject report) {
        try {
            JSONArray history = getResolvedHistory();
            String ref = report.optString("incident_reference", "");
            JSONArray newHistory = new JSONArray();
            newHistory.put(report);
            for (int i = 0; i < history.length(); i++) {
                JSONObject item = history.getJSONObject(i);
                if (!item.optString("incident_reference", "").equals(ref)) {
                    newHistory.put(item);
                }
            }
            editor.putString(KEY_RESOLVED_HISTORY, newHistory.toString());
            editor.apply();
        } catch (Exception ignored) {}
    }

    public void saveResolvedHistory(JSONObject report) {
        saveResolvedReportToHistory(report);
    }

    public JSONArray getResolvedHistory() {
        try {
            String json = pref.getString(KEY_RESOLVED_HISTORY, "[]");
            return new JSONArray(json);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public boolean isIncidentResolved(String ref) {
        try {
            JSONArray history = getResolvedHistory();
            for (int i = 0; i < history.length(); i++) {
                if (history.getJSONObject(i).optString("incident_reference", "").equals(ref)) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public void markIncidentResolved(String ref) {
        try {
            JSONArray reports = getLocalReports();
            for (int i = 0; i < reports.length(); i++) {
                JSONObject obj = reports.getJSONObject(i);
                if (obj.optString("incident_reference", "").equals(ref)) {
                    obj.put("current_status", "RESOLVED");
                    saveResolvedHistory(obj);
                }
            }
            updateLocalReportStatus(ref, "RESOLVED");
            ValenciaGlobalIncidentStore.resolveReport(ref);
        } catch (Exception ignored) {}
    }

    public void saveHasReadAlerts(boolean read) {
        editor.putBoolean(KEY_HAS_READ_ALERTS, read);
        editor.apply();
    }

    public boolean getHasReadAlerts() {
        return pref.getBoolean(KEY_HAS_READ_ALERTS, false);
    }

    public boolean hasReadAlerts() {
        return getHasReadAlerts();
    }

    public void saveNotificationHistory(JSONArray alerts) {
        editor.putString(KEY_ALERTS_HISTORY, alerts.toString());
        editor.apply();
    }

    public void saveAlertNotification(JSONObject notif) {
        try {
            JSONArray history = getNotificationHistory();
            JSONArray newHistory = new JSONArray();
            newHistory.put(notif);
            for (int i = 0; i < history.length(); i++) {
                newHistory.put(history.getJSONObject(i));
            }
            editor.putString(KEY_ALERTS_HISTORY, newHistory.toString());
            editor.apply();
        } catch (Exception ignored) {}
    }

    public JSONArray getNotificationHistory() {
        try {
            String json = pref.getString(KEY_ALERTS_HISTORY, "[]");
            return new JSONArray(json);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public JSONArray getAlertNotifications() {
        return getNotificationHistory();
    }

    public void clearSession() {
        String registeredUsersJson = pref.getString(KEY_REGISTERED_USERS_JSON, "[]");
        editor.clear();
        editor.putString(KEY_REGISTERED_USERS_JSON, registeredUsersJson);
        editor.apply();
    }
}
