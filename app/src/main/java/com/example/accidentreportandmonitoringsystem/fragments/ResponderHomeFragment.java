package com.example.accidentreportandmonitoringsystem.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.accidentreportandmonitoringsystem.IncidentLocationActivity;
import com.example.accidentreportandmonitoringsystem.R;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalIncidentStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ResponderHomeFragment extends Fragment {

    private SessionManager sessionManager;
    private TextView tvCountOngoing, tvCountResolved, tvViewAll;
    private LinearLayout containerResponderActiveIncidents;
    private ImageButton ibResponderHeaderBell;
    private TextView tvResponderHeaderAlertBadge;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_responder_home, container, false);

        sessionManager = new SessionManager(requireContext());
        tvCountOngoing = view.findViewById(R.id.tvCountOngoing);
        tvCountResolved = view.findViewById(R.id.tvCountResolved);
        containerResponderActiveIncidents = view.findViewById(R.id.containerResponderActiveIncidents);
        tvViewAll = view.findViewById(R.id.tvViewAll);

        ibResponderHeaderBell = view.findViewById(R.id.ibResponderHeaderBell);
        tvResponderHeaderAlertBadge = view.findViewById(R.id.tvResponderHeaderAlertBadge);

        setupResponderHeaderBell();
        loadResponderHeaderBadge();
        loadAllActiveReports();

        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> showViewAllReportsDialog());
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadResponderHeaderBadge();
        loadAllActiveReports();
    }

    private void showViewAllReportsDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_responder_view_all_reports, null);
        LinearLayout container = dialogView.findViewById(R.id.containerViewAllReports);
        MaterialButton btnClose = dialogView.findViewById(R.id.btnCloseViewAll);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create();

        if (container != null) {
            container.removeAllViews();
            try {
                JSONArray globalReports = ValenciaGlobalIncidentStore.getReports();
                JSONArray localReports = sessionManager.getLocalReports();

                JSONArray combined = new JSONArray();
                Set<String> seenRefs = new HashSet<>();

                if (globalReports != null) {
                    for (int i = 0; i < globalReports.length(); i++) {
                        JSONObject rep = globalReports.getJSONObject(i);
                        String ref = rep.optString("incident_reference", "");
                        if (!ref.isEmpty() && !seenRefs.contains(ref)) {
                            seenRefs.add(ref);
                            combined.put(rep);
                        }
                    }
                }
                if (localReports != null) {
                    for (int i = 0; i < localReports.length(); i++) {
                        JSONObject rep = localReports.getJSONObject(i);
                        String ref = rep.optString("incident_reference", "");
                        if (!ref.isEmpty() && !seenRefs.contains(ref)) {
                            seenRefs.add(ref);
                            combined.put(rep);
                        }
                    }
                }

                if (combined.length() == 0) {
                    TextView tvEmpty = new TextView(requireContext());
                    tvEmpty.setText("No citizen reports submitted yet.");
                    tvEmpty.setTextColor(getResources().getColor(R.color.arms_secondary_text, null));
                    tvEmpty.setPadding(16, 24, 16, 24);
                    container.addView(tvEmpty);
                } else {
                    for (int i = 0; i < combined.length(); i++) {
                        JSONObject rep = combined.getJSONObject(i);
                        String ref = rep.optString("incident_reference", "ARMS-VAL");
                        String type = rep.optString("incident_type", "EMERGENCY");
                        String status = rep.optString("current_status", "DISPATCHED");
                        String address = rep.optString("landmark_address", "Valencia City");
                        String date = rep.optString("created_at", "Today");

                        View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_recent_report, container, false);
                        TextView tvRef = cardView.findViewById(R.id.tvReportRef);
                        TextView tvStatus = cardView.findViewById(R.id.tvReportStatus);
                        TextView tvType = cardView.findViewById(R.id.tvReportType);
                        TextView tvLocation = cardView.findViewById(R.id.tvReportLocation);

                        if (tvRef != null) tvRef.setText("#" + ref);
                        if (tvStatus != null) {
                            tvStatus.setText(status);
                            tvStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                            tvStatus.setTextColor(Color.parseColor("#DC2626"));
                        }
                        if (tvType != null) tvType.setText(type.replace("_", " "));
                        if (tvLocation != null) tvLocation.setText(address + " • " + date);

                        container.addView(cardView);
                    }
                }
            } catch (Exception e) {
                TextView tvEmpty = new TextView(requireContext());
                tvEmpty.setText("Error loading reports.");
                container.addView(tvEmpty);
            }
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void loadResponderHeaderBadge() {
        if (tvResponderHeaderAlertBadge == null) return;
        try {
            JSONArray reports = ValenciaGlobalIncidentStore.getReports();
            int count = reports != null ? reports.length() : 0;
            if (count > 0) {
                tvResponderHeaderAlertBadge.setText(String.valueOf(count));
                tvResponderHeaderAlertBadge.setVisibility(View.VISIBLE);
            } else {
                tvResponderHeaderAlertBadge.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            tvResponderHeaderAlertBadge.setVisibility(View.GONE);
        }
    }

    private void setupResponderHeaderBell() {
        if (ibResponderHeaderBell != null) {
            ibResponderHeaderBell.setOnClickListener(v -> {
                if (tvResponderHeaderAlertBadge != null) tvResponderHeaderAlertBadge.setVisibility(View.GONE);
                showResponderNotificationsPopover();
            });
        }
    }

    private void showResponderNotificationsPopover() {
        View popoverView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_citizen_notification_popover, null);
        LinearLayout containerPopover = popoverView.findViewById(R.id.containerPopoverNotifications);
        TextView tvClearAll = popoverView.findViewById(R.id.tvClearAll);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setView(popoverView)
            .setCancelable(true)
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setGravity(android.view.Gravity.TOP | android.view.Gravity.END);
            dialog.getWindow().getAttributes().y = 120;
            dialog.getWindow().getAttributes().x = 16;
        }

        if (containerPopover != null) {
            containerPopover.removeAllViews();
            try {
                JSONArray reports = ValenciaGlobalIncidentStore.getReports();
                if (reports == null || reports.length() == 0) {
                    TextView tvEmpty = new TextView(requireContext());
                    tvEmpty.setText("No new citizen emergency reports received yet.");
                    tvEmpty.setTextColor(getResources().getColor(R.color.arms_secondary_text, null));
                    tvEmpty.setPadding(12, 16, 12, 16);
                    tvEmpty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                    containerPopover.addView(tvEmpty);
                } else {
                    for (int i = 0; i < reports.length(); i++) {
                        JSONObject obj = reports.getJSONObject(i);
                        String ref = obj.optString("incident_reference", "ARMS-VAL");
                        String type = obj.optString("incident_type", "EMERGENCY");
                        String address = obj.optString("landmark_address", "Valencia City");
                        String date = obj.optString("created_at", "Today");

                        View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_recent_report, containerPopover, false);
                        TextView tvRef = cardView.findViewById(R.id.tvReportRef);
                        TextView tvStatus = cardView.findViewById(R.id.tvReportStatus);
                        TextView tvType = cardView.findViewById(R.id.tvReportType);
                        TextView tvLocation = cardView.findViewById(R.id.tvReportLocation);

                        if (tvRef != null) tvRef.setText(date);
                        if (tvStatus != null) {
                            tvStatus.setText("NEW DISPATCH");
                            tvStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                            tvStatus.setTextColor(Color.parseColor("#DC2626"));
                        }
                        if (tvType != null) tvType.setText("🚨 New " + type.replace("_", " ") + " Report (#" + ref + ")");
                        if (tvLocation != null) tvLocation.setText(address);

                        containerPopover.addView(cardView);
                    }
                }
            } catch (Exception e) {
                TextView tvEmpty = new TextView(requireContext());
                tvEmpty.setText("No new reports.");
                containerPopover.addView(tvEmpty);
            }
        }

        if (tvClearAll != null) {
            tvClearAll.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void saveNotificationAlert(String ref, String status, String type) {
        try {
            String agency = sessionManager.getUserAgency();
            if (agency == null || agency.trim().isEmpty()) agency = "BFP";
            String agencyFullName = "Emergency Responders";
            if (agency.equalsIgnoreCase("BFP")) agencyFullName = "Bureau of Fire Protection (BFP)";
            else if (agency.equalsIgnoreCase("PNP")) agencyFullName = "Philippine National Police (PNP)";
            else if (agency.equalsIgnoreCase("CDRRMO")) agencyFullName = "Valencia City CDRRMO";
            else if (agency.equalsIgnoreCase("MEDICAL")) agencyFullName = "Medical Emergency Team";

            JSONObject alertObj = new JSONObject();
            alertObj.put("title", "🚨 " + agency.toUpperCase() + " Update: " + status);
            alertObj.put("message", agencyFullName + " has updated your " + type.replace("_", " ").toLowerCase() + " incident (#" + ref + ") status to: " + status);
            alertObj.put("timestamp", new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(new Date()));
            
            // Save to both local session and global incident store bridge
            sessionManager.saveAlertNotification(alertObj);
            ValenciaGlobalIncidentStore.addNotification(alertObj);
        } catch (Exception ignored) {}
    }

    private void loadAllActiveReports() {
        if (containerResponderActiveIncidents == null) return;

        try {
            containerResponderActiveIncidents.removeAllViews();
            JSONArray globalReports = ValenciaGlobalIncidentStore.getReports();
            JSONArray localReports = sessionManager.getLocalReports();

            // Deduplicate reports by incident_reference
            JSONArray combined = new JSONArray();
            Set<String> seenRefs = new HashSet<>();

            if (globalReports != null) {
                for (int i = 0; i < globalReports.length(); i++) {
                    JSONObject rep = globalReports.getJSONObject(i);
                    String ref = rep.optString("incident_reference", "");
                    if (!ref.isEmpty() && !seenRefs.contains(ref)) {
                        seenRefs.add(ref);
                        combined.put(rep);
                    }
                }
            }
            if (localReports != null) {
                for (int i = 0; i < localReports.length(); i++) {
                    JSONObject rep = localReports.getJSONObject(i);
                    String ref = rep.optString("incident_reference", "");
                    if (!ref.isEmpty() && !seenRefs.contains(ref)) {
                        seenRefs.add(ref);
                        combined.put(rep);
                    }
                }
            }

            int resolvedCount = sessionManager.getResolvedHistory().length();
            if (tvCountResolved != null) tvCountResolved.setText(String.format(Locale.getDefault(), "%02d", resolvedCount));

            int activeCount = 0;
            for (int i = 0; i < combined.length(); i++) {
                JSONObject rep = combined.getJSONObject(i);
                String ref = rep.optString("incident_reference");
                String status = rep.optString("current_status", "SUBMITTED");
                if (!sessionManager.isIncidentResolved(ref) && !status.equalsIgnoreCase("RESOLVED")) {
                    activeCount++;
                }
            }
            if (tvCountOngoing != null) tvCountOngoing.setText(String.format(Locale.getDefault(), "%02d", activeCount));

            if (combined.length() == 0 || activeCount == 0) {
                TextView tvEmpty = new TextView(requireContext());
                tvEmpty.setText("No pending active dispatches assigned.");
                tvEmpty.setTextColor(getResources().getColor(R.color.arms_secondary_text, null));
                tvEmpty.setPadding(16, 24, 16, 24);
                containerResponderActiveIncidents.addView(tvEmpty);
                return;
            }

            for (int i = 0; i < combined.length(); i++) {
                JSONObject rep = combined.getJSONObject(i);
                String ref = rep.optString("incident_reference", "ARMS-VAL");
                String status = rep.optString("current_status", "DISPATCHED");
                if (sessionManager.isIncidentResolved(ref) || status.equalsIgnoreCase("RESOLVED")) {
                    continue; // Skip resolved reports on active dashboard
                }

                String type = rep.optString("incident_type", "FIRE");
                String address = rep.optString("landmark_address", "Valencia City");
                String desc = rep.optString("description", "Emergency report dispatched.");
                double lat = rep.optDouble("latitude", 7.9092);
                double lng = rep.optDouble("longitude", 125.0935);
                String photoBase64 = rep.optString("photo_base64", "");
                boolean hasVideo = rep.optBoolean("has_video", false);
                String videoUriStr = rep.optString("video_uri", "");

                View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_responder_active_card, containerResponderActiveIncidents, false);

                TextView tvTitle = cardView.findViewById(R.id.tvIncidentTitle);
                TextView tvBadge = cardView.findViewById(R.id.tvResponderStatusBadge);
                TextView tvLoc = cardView.findViewById(R.id.tvIncidentLocation);
                TextView tvDesc = cardView.findViewById(R.id.tvIncidentDescription);
                ImageView ivPhoto = cardView.findViewById(R.id.ivResponderEvidencePhoto);
                TextView tvNoPhotoHint = cardView.findViewById(R.id.tvNoPhotoHint);
                MaterialButton btnPlayVideo = cardView.findViewById(R.id.btnPlayVideoEvidence);
                LinearLayout layoutDetails = cardView.findViewById(R.id.layoutExpandableDetails);
                MaterialCardView cardRoot = cardView.findViewById(R.id.cardIncidentRoot);

                MaterialButton btnAccept = cardView.findViewById(R.id.btnAcceptDispatch);
                MaterialButton btnEnRoute = cardView.findViewById(R.id.btnEnRoute);
                MaterialButton btnOnScene = cardView.findViewById(R.id.btnOnScene);
                MaterialButton btnResolve = cardView.findViewById(R.id.btnResolve);
                MaterialButton btnMap = cardView.findViewById(R.id.btnNavigateMap);

                if (tvTitle != null) tvTitle.setText(type.replace("_", " "));
                if (tvBadge != null) tvBadge.setText(status);
                if (tvLoc != null) tvLoc.setText(address + " • Incident #" + ref);
                if (tvDesc != null) tvDesc.setText("Details: " + desc);

                if (ivPhoto != null && tvNoPhotoHint != null) {
                    if (photoBase64 != null && !photoBase64.trim().isEmpty()) {
                        try {
                            byte[] decodedBytes = Base64.decode(photoBase64, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            ivPhoto.setImageBitmap(bitmap);
                            ivPhoto.setVisibility(View.VISIBLE);
                            tvNoPhotoHint.setVisibility(View.GONE);
                        } catch (Exception e) {
                            ivPhoto.setVisibility(View.GONE);
                            tvNoPhotoHint.setVisibility(View.GONE);
                        }
                    } else {
                        View photoCardParent = (View) ivPhoto.getParent().getParent();
                        if (photoCardParent != null) {
                            photoCardParent.setVisibility(View.GONE);
                        }
                    }
                }

                if (btnPlayVideo != null) {
                    if (hasVideo || (videoUriStr != null && !videoUriStr.trim().isEmpty())) {
                        btnPlayVideo.setVisibility(View.VISIBLE);
                        btnPlayVideo.setOnClickListener(v -> {
                            try {
                                Intent playIntent = new Intent(Intent.ACTION_VIEW);
                                if (videoUriStr != null && !videoUriStr.trim().isEmpty()) {
                                    playIntent.setDataAndType(Uri.parse(videoUriStr), "video/*");
                                } else {
                                    Toast.makeText(requireContext(), "🎥 Playing citizen recorded video evidence...", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                playIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(playIntent);
                            } catch (Exception e) {
                                Toast.makeText(requireContext(), "Playing video evidence...", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        btnPlayVideo.setVisibility(View.GONE);
                    }
                }

                // Expandable toggle
                final boolean[] isCardExpanded = {false};
                if (cardRoot != null) {
                    cardRoot.setOnClickListener(v -> {
                        isCardExpanded[0] = !isCardExpanded[0];
                        if (layoutDetails != null) {
                            layoutDetails.setVisibility(isCardExpanded[0] ? View.VISIBLE : View.GONE);
                        }
                    });
                }

                if (btnMap != null) {
                    btnMap.setOnClickListener(v -> {
                        Toast.makeText(requireContext(), "🗺️ Opening GPS Map & Tactical Route...", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(requireContext(), IncidentLocationActivity.class);
                        intent.putExtra("station_lat", 7.9092);
                        intent.putExtra("station_lng", 125.0935);
                        intent.putExtra("incident_lat", lat);
                        intent.putExtra("incident_lng", lng);
                        intent.putExtra("incident_address", address);
                        intent.putExtra("incident_ref", ref);
                        startActivity(intent);
                    });
                }

                if (btnAccept != null) {
                    btnAccept.setOnClickListener(v -> {
                        sessionManager.updateLocalReportStatus(ref, "ACCEPTED");
                        ValenciaGlobalIncidentStore.updateStatus(ref, "ACCEPTED");
                        saveNotificationAlert(ref, "ACCEPTED", type);
                        if (tvBadge != null) tvBadge.setText("ACCEPTED");
                        Toast.makeText(requireContext(), "✅ Status updated to ACCEPTED & notification sent!", Toast.LENGTH_SHORT).show();
                    });
                }
                if (btnEnRoute != null) {
                    btnEnRoute.setOnClickListener(v -> {
                        sessionManager.updateLocalReportStatus(ref, "EN ROUTE");
                        ValenciaGlobalIncidentStore.updateStatus(ref, "EN ROUTE");
                        saveNotificationAlert(ref, "EN ROUTE", type);
                        if (tvBadge != null) tvBadge.setText("EN ROUTE");
                        Toast.makeText(requireContext(), "✅ Status updated to EN ROUTE & notification sent!", Toast.LENGTH_SHORT).show();
                    });
                }
                if (btnOnScene != null) {
                    btnOnScene.setOnClickListener(v -> {
                        sessionManager.updateLocalReportStatus(ref, "ON SCENE");
                        ValenciaGlobalIncidentStore.updateStatus(ref, "ON SCENE");
                        saveNotificationAlert(ref, "ON SCENE", type);
                        if (tvBadge != null) tvBadge.setText("ON SCENE");
                        Toast.makeText(requireContext(), "✅ Status updated to ON SCENE & notification sent!", Toast.LENGTH_SHORT).show();
                    });
                }
                if (btnResolve != null) {
                    btnResolve.setOnClickListener(v -> {
                        sessionManager.markIncidentResolved(ref);
                        sessionManager.updateLocalReportStatus(ref, "RESOLVED");
                        ValenciaGlobalIncidentStore.updateStatus(ref, "RESOLVED");
                        saveNotificationAlert(ref, "RESOLVED", type);

                        try {
                            JSONObject resolvedObj = new JSONObject();
                            resolvedObj.put("incident_reference", ref);
                            resolvedObj.put("incident_type", type);
                            resolvedObj.put("landmark_address", address);
                            resolvedObj.put("description", desc);
                            resolvedObj.put("photo_base64", photoBase64);
                            resolvedObj.put("has_video", hasVideo);
                            resolvedObj.put("video_uri", videoUriStr);
                            resolvedObj.put("current_status", "RESOLVED");
                            sessionManager.saveResolvedHistory(resolvedObj);
                        } catch (Exception ignored) {}

                        Toast.makeText(requireContext(), "✅ Incident Resolved! Moved to History & notification sent.", Toast.LENGTH_LONG).show();
                        loadAllActiveReports();
                    });
                }

                containerResponderActiveIncidents.addView(cardView);
            }
        } catch (Exception ignored) {}
    }
}
