package com.example.accidentreportandmonitoringsystem.data.repository;

import com.example.accidentreportandmonitoringsystem.remote.ApiClient;
import org.json.JSONObject;

public class IncidentRepository {

    public void submitGuestReport(JSONObject payload, ApiClient.ApiCallback callback) {
        ApiClient.post("/incidents/guest", payload, null, callback);
    }

    public void submitCitizenReport(JSONObject payload, String authToken, ApiClient.ApiCallback callback) {
        ApiClient.post("/incidents", payload, authToken, callback);
    }

    public void getResponderDispatches(String authToken, ApiClient.ApiCallback callback) {
        ApiClient.get("/responder/dispatches", authToken, callback);
    }

    public void updateResponderStatus(String assignmentId, String newStatus, String declineReason, String authToken, ApiClient.ApiCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("assignment_id", assignmentId);
            payload.put("new_status", newStatus);
            if (declineReason != null && !declineReason.trim().isEmpty()) {
                payload.put("decline_reason", declineReason.trim());
            }
            ApiClient.post("/responder/status", payload, authToken, callback);
        } catch (Exception e) {
            callback.onError(400, e.getMessage());
        }
    }
}
