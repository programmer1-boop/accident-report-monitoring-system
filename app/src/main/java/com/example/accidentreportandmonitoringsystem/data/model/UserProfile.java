package com.example.accidentreportandmonitoringsystem.data.model;

import org.json.JSONObject;

public class UserProfile {
    public String id;
    public String fullName;
    public String email;
    public String phoneNumber;
    public String role;
    public String accountStatus;
    public String agencyType;

    public static UserProfile fromJson(JSONObject obj) {
        UserProfile u = new UserProfile();
        if (obj == null) return u;
        u.id = obj.optString("id", "");
        u.fullName = obj.optString("full_name", "");
        u.email = obj.optString("email", "");
        u.phoneNumber = obj.optString("phone_number", "");
        u.role = obj.optString("role", "CITIZEN");
        u.accountStatus = obj.optString("account_status", "ACTIVE");
        u.agencyType = obj.optString("agency_type", obj.optString("team_type", ""));
        return u;
    }
}
