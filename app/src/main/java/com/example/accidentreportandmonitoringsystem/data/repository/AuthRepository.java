package com.example.accidentreportandmonitoringsystem.data.repository;

import com.example.accidentreportandmonitoringsystem.remote.ApiClient;
import org.json.JSONObject;

public class AuthRepository {

    public void login(String emailOrUsername, String password, ApiClient.ApiCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("email", emailOrUsername);
            payload.put("password", password);
            ApiClient.post("/auth/login", payload, null, callback);
        } catch (Exception e) {
            callback.onError(400, e.getMessage());
        }
    }

    public void registerCitizen(String fullName, String email, String phone, String password, ApiClient.ApiCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("full_name", fullName);
            payload.put("email", email);
            if (phone != null && !phone.trim().isEmpty()) {
                payload.put("phone_number", phone.trim());
            }
            payload.put("password", password);
            ApiClient.post("/auth/register", payload, null, callback);
        } catch (Exception e) {
            callback.onError(400, e.getMessage());
        }
    }
}
