package com.example.accidentreportandmonitoringsystem.remote;

import org.json.JSONArray;
import org.json.JSONObject;

public class ValenciaGlobalIncidentStore {
    private static final JSONArray globalReports = new JSONArray();
    private static final JSONArray resolvedHistory = new JSONArray();
    private static final JSONArray globalNotifications = new JSONArray();

    public static synchronized void clearAll() {
        while (globalReports.length() > 0) globalReports.remove(0);
        while (resolvedHistory.length() > 0) resolvedHistory.remove(0);
        while (globalNotifications.length() > 0) globalNotifications.remove(0);
    }

    public static synchronized void addReport(JSONObject report) {
        try {
            String ref = report.optString("incident_reference", "");
            if (!ref.isEmpty()) {
                for (int i = 0; i < globalReports.length(); i++) {
                    if (globalReports.getJSONObject(i).optString("incident_reference", "").equals(ref)) {
                        return; // Already exists, prevent duplicate!
                    }
                }
            }

            JSONArray newArr = new JSONArray();
            newArr.put(report);
            for (int i = 0; i < globalReports.length(); i++) {
                newArr.put(globalReports.getJSONObject(i));
            }
            while (globalReports.length() > 0) {
                globalReports.remove(0);
            }
            for (int i = 0; i < newArr.length(); i++) {
                globalReports.put(newArr.getJSONObject(i));
            }
        } catch (Exception ignored) {}
    }

    public static synchronized JSONArray getReports() {
        return globalReports;
    }

    public static synchronized void addNotification(JSONObject notif) {
        try {
            JSONArray newArr = new JSONArray();
            newArr.put(notif);
            for (int i = 0; i < globalNotifications.length(); i++) {
                newArr.put(globalNotifications.getJSONObject(i));
            }
            while (globalNotifications.length() > 0) {
                globalNotifications.remove(0);
            }
            for (int i = 0; i < newArr.length(); i++) {
                globalNotifications.put(newArr.getJSONObject(i));
            }
        } catch (Exception ignored) {}
    }

    public static synchronized JSONArray getNotifications() {
        return globalNotifications;
    }

    public static synchronized void resolveReport(String ref) {
        for (int i = 0; i < globalReports.length(); i++) {
            try {
                JSONObject obj = globalReports.getJSONObject(i);
                if (obj.optString("incident_reference").equals(ref)) {
                    obj.put("current_status", "RESOLVED");
                    resolvedHistory.put(obj);
                }
            } catch (Exception ignored) {}
        }
    }

    public static synchronized JSONArray getResolvedHistory() {
        return resolvedHistory;
    }

    public static synchronized void updateStatus(String ref, String newStatus) {
        for (int i = 0; i < globalReports.length(); i++) {
            try {
                JSONObject obj = globalReports.getJSONObject(i);
                if (obj.optString("incident_reference").equals(ref)) {
                    obj.put("current_status", newStatus);
                    if (newStatus.equalsIgnoreCase("RESOLVED")) {
                        resolvedHistory.put(obj);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    public static synchronized void updateAssignedResponders(String ref, String assignedResponders) {
        for (int i = 0; i < globalReports.length(); i++) {
            try {
                JSONObject obj = globalReports.getJSONObject(i);
                if (obj.optString("incident_reference").equals(ref)) {
                    obj.put("assigned_responders", assignedResponders);
                }
            } catch (Exception ignored) {}
        }
    }
}
