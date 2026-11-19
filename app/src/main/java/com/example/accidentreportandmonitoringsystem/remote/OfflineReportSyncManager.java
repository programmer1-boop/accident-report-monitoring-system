package com.example.accidentreportandmonitoringsystem.remote;

import android.content.Context;
import android.widget.Toast;
import com.example.accidentreportandmonitoringsystem.notifications.ArmsNotificationHelper;
import org.json.JSONArray;
import org.json.JSONObject;

public class OfflineReportSyncManager {

    public interface SyncListener {
        void onSyncStatusChanged(int remainingCount);
    }

    public static void syncPendingReports(Context context, SyncListener listener) {
        SessionManager sessionManager = new SessionManager(context);
        JSONArray queue = sessionManager.getPendingSyncReports();
        if (queue == null || queue.length() == 0) {
            if (listener != null) listener.onSyncStatusChanged(0);
            return;
        }

        for (int i = 0; i < queue.length(); i++) {
            JSONObject item = queue.optJSONObject(i);
            if (item == null) continue;

            String key = item.optString("idempotency_key", "");
            String ref = item.optString("incident_reference", "");
            String endpoint = item.optString("endpoint", "/incidents/guest");
            JSONObject payload = item.optJSONObject("payload");
            String authToken = item.optString("auth_token", "");

            if (payload == null) {
                sessionManager.removePendingSyncReport(key);
                continue;
            }

            ApiClient.post(endpoint, payload, authToken.isEmpty() ? null : authToken, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    sessionManager.removePendingSyncReport(key);
                    sessionManager.updateLocalReportSyncStatus(ref, "SYNCED");
                    String guestToken = response.optString("tracking_token", "");
                    if (!guestToken.isEmpty()) {
                        sessionManager.saveGuestTrackingToken(guestToken);
                    }
                    ArmsNotificationHelper.showEmergencyNotification(
                            context,
                            "Offline Report Synced",
                            "Report #" + ref + " has been uploaded to Valencia City Command Center."
                    );
                    Toast.makeText(context, "Synced pending report #" + ref, Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onSyncStatusChanged(sessionManager.getPendingSyncReports().length());
                    }
                }

                @Override
                public void onError(int statusCode, String errorMessage) {
                    if (listener != null) {
                        listener.onSyncStatusChanged(sessionManager.getPendingSyncReports().length());
                    }
                }
            });
        }
    }
}
