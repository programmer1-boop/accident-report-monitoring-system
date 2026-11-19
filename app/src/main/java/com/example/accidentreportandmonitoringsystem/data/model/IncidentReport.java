package com.example.accidentreportandmonitoringsystem.data.model;

import org.json.JSONObject;

public class IncidentReport {
    public String id;
    public String incidentReference;
    public String incidentType;
    public String description;
    public double latitude;
    public double longitude;
    public double locationAccuracy;
    public String landmarkAddress;
    public String currentStatus;
    public String createdAt;

    public static IncidentReport fromJson(JSONObject obj) {
        IncidentReport r = new IncidentReport();
        if (obj == null) return r;
        r.id = obj.optString("id", "");
        r.incidentReference = obj.optString("incident_reference", "");
        r.incidentType = obj.optString("incident_type", "OTHER_EMERGENCY");
        r.description = obj.optString("description", "");
        r.latitude = obj.optDouble("latitude", 7.9064);
        r.longitude = obj.optDouble("longitude", 125.0942);
        r.locationAccuracy = obj.optDouble("location_accuracy", 0.0);
        r.landmarkAddress = obj.optString("landmark_address", "Valencia City");
        r.currentStatus = obj.optString("current_status", "SUBMITTED");
        r.createdAt = obj.optString("created_at", "");
        return r;
    }
}
