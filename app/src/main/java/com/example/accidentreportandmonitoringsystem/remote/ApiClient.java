package com.example.accidentreportandmonitoringsystem.remote;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {

    // Use 10.0.2.2 for Android Emulator to connect to PC backend server instantly without Wi-Fi/firewall issues!
    // (Or use your PC's local IP like 192.168.x.x if testing on a physical phone).
    public static final String BASE_URL = "http://10.0.2.2:5000/api";

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(int statusCode, String errorMessage);
    }

    public static void post(String endpoint, JSONObject jsonBody, String authToken, ApiCallback callback) {
        executor.execute(() -> executeRequest("POST", endpoint, jsonBody, authToken, callback));
    }

    public static void get(String endpoint, String authToken, ApiCallback callback) {
        executor.execute(() -> executeRequest("GET", endpoint, null, authToken, callback));
    }

    private static void executeRequest(String method, String endpoint, JSONObject jsonBody, String authToken, ApiCallback callback) {
        HttpURLConnection conn = null;
        try {
            String normalizedEndpoint = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
            URL url = new URL(BASE_URL + normalizedEndpoint);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (authToken != null && !authToken.trim().isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            if ("POST".equalsIgnoreCase(method) && jsonBody != null) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    os.flush();
                }
            }

            int responseCode = conn.getResponseCode();
            java.io.InputStream stream = (responseCode >= 200 && responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder responseBuilder = new StringBuilder();
            if (stream != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                }
            }

            String responseText = responseBuilder.toString();
            JSONObject jsonResponse;
            try {
                jsonResponse = responseText.trim().startsWith("{") ? new JSONObject(responseText) : new JSONObject().put("message", responseText);
            } catch (Exception parseError) {
                jsonResponse = new JSONObject().put("message", responseText);
            }

            if (responseCode >= 200 && responseCode < 300) {
                JSONObject finalResponse = jsonResponse;
                mainHandler.post(() -> callback.onSuccess(finalResponse));
            } else {
                String errorMessage = jsonResponse.optString("error", jsonResponse.optString("message", "Server returned HTTP " + responseCode));
                int finalCode = responseCode;
                mainHandler.post(() -> callback.onError(finalCode, errorMessage));
            }

        } catch (Exception e) {
            String errorMsg = "Unable to connect to online server (" + e.getMessage() + "). Ensure backend server is running at " + BASE_URL;
            mainHandler.post(() -> callback.onError(503, errorMsg));
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
