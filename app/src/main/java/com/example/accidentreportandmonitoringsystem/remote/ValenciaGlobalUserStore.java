package com.example.accidentreportandmonitoringsystem.remote;

import org.json.JSONArray;
import org.json.JSONObject;

public class ValenciaGlobalUserStore {
    private static final JSONArray registeredUsers = new JSONArray();

    public static synchronized void clearAll() {
        while (registeredUsers.length() > 0) {
            registeredUsers.remove(0);
        }
    }

    public static synchronized void addUser(JSONObject user) {
        try {
            String email = user.optString("email", "").toLowerCase();
            if (!email.isEmpty()) {
                for (int i = 0; i < registeredUsers.length(); i++) {
                    if (registeredUsers.getJSONObject(i).optString("email", "").toLowerCase().equals(email)) {
                        return; // Already exists
                    }
                }
            }
            registeredUsers.put(user);
        } catch (Exception ignored) {}
    }

    public static synchronized boolean accountExists(String email) {
        try {
            String lowerEmail = email.trim().toLowerCase();
            for (int i = 0; i < registeredUsers.length(); i++) {
                if (registeredUsers.getJSONObject(i).optString("email", "").toLowerCase().equals(lowerEmail)) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static synchronized JSONArray getUsers() {
        return registeredUsers;
    }
}
