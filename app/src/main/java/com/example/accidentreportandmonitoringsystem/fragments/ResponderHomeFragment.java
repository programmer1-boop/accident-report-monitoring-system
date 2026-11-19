package com.example.accidentreportandmonitoringsystem.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
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
import com.example.accidentreportandmonitoringsystem.utils.RotatingGradientDrawable;
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
    private TextView tvCountOngoing, tvCountResolved, tvOfficeName;
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
        tvOfficeName = view.findViewById(R.id.tvOfficeName);
        containerResponderActiveIncidents = view.findViewById(R.id.containerResponderActiveIncidents);

        ibResponderHeaderBell = view.findViewById(R.id.ibResponderHeaderBell);
        tvResponderHeaderAlertBadge = view.findViewById(R.id.tvResponderHeaderAlertBadge);

        String agency = sessionManager.getUserAgency() != null ? sessionManager.getUserAgency().toUpperCase() : "RESPONDER";
        if (tvOfficeName != null) {
            tvOfficeName.setText(agency + " Command Post • " + sessionManager.getUserName());
        }

        View rlHeader = view.findViewById(R.id.rlResponderHeader);
        TextView tvHeaderTitle = view.findViewById(R.id.tvResponderHeaderTitle);
        if (rlHeader != null && tvHeaderTitle != null) {
            if (agency.contains("BFP")) {
                rlHeader.setBackgroundColor(Color.parseColor("#B91C1C"));
                tvHeaderTitle.setText("BFP Fire Command Center");
            } else if (agency.contains("CDRRMO")) {
                rlHeader.setBackgroundColor(Color.parseColor("#B45309"));
                tvHeaderTitle.setText("CDRRMO Rescue Center");
            } else if (agency.contains("PNP")) {
                rlHeader.setBackgroundColor(Color.parseColor("#1D4ED8"));
                tvHeaderTitle.setText("PNP Police Command");
            } else if (agency.contains("MED") || agency.contains("HEMS")) {
                rlHeader.setBackgroundColor(Color.parseColor("#047857"));
                tvHeaderTitle.setText("Medical / EMS Command");
            }
        }

        View cardHotline = view.findViewById(R.id.cardResponderHotline);
        if (cardHotline != null) {
            startSpinningBorder(cardHotline);
        }

        MaterialButton btnCall = view.findViewById(R.id.btnCallResponderHotline);
        if (btnCall != null) {
            btnCall.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:0888281234"));
                startActivity(intent);
            });
        }

        setupResponderHeaderBell();
        loadResponderHeaderBadge();
        loadAllActiveReports();

        return view;
    }

    private void startSpinningBorder(View card) {
        if (card == null) return;
        card.post(() -> {
            RotatingGradientDrawable drawable = new RotatingGradientDrawable();
            if (card instanceof MaterialCardView) {
                ((MaterialCardView) card).setCardBackgroundColor(Color.TRANSPARENT);
                ((MaterialCardView) card).setRadius(16f);
                ((MaterialCardView) card).setCardElevation(3f);
            }
            card.setBackground(drawable);

            android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0f, 360f);
            animator.setDuration(3000);
            animator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            animator.setInterpolator(new android.view.animation.LinearInterpolator());
            animator.addUpdateListener(animation -> {
                float angle = (float) animation.getAnimatedValue();
                drawable.setRotationAngle(angle);
            });
            animator.start();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadResponderHeaderBadge();
        loadAllActiveReports();
    }

    private void loadResponderHeaderBadge() {
        if (tvResponderHeaderAlertBadge == null) return;
        try {
            JSONArray reports = ValenciaGlobalIncidentStore.getReports();
            int count = 0;
            String myAgency = sessionManager.getUserAgency();
            if (myAgency == null || myAgency.isEmpty()) myAgency = "BFP";

            if (reports != null) {
                for (int i = 0; i < reports.length(); i++) {
                    JSONObject r = reports.getJSONObject(i);
                    String status = r.optString("current_status", "").toUpperCase();
                    String assigned = r.optString("assigned_responders", "").toUpperCase();
                    if (!status.contains("RESOLVED") && (assigned.contains(myAgency.toUpperCase()) || assigned.isEmpty())) {
                        count++;
                    }
                }
            }
            tvResponderHeaderAlertBadge.setText(String.valueOf(count));
            tvResponderHeaderAlertBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        } catch (Exception ignored) {}
    }

    private void setupResponderHeaderBell() {
        if (ibResponderHeaderBell != null) {
            ibResponderHeaderBell.setOnClickListener(v -> {
                if (getActivity() != null) {
                    com.google.android.material.bottomnavigation.BottomNavigationView nav =
                            getActivity().findViewById(R.id.bottomNavigation);
                    if (nav != null) {
                        nav.setSelectedItemId(R.id.nav_notifications);
                        return;
                    }
                }
                Toast.makeText(requireContext(), "Opening " + sessionManager.getUserAgency() + " Alerts...", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadAllActiveReports() {
        if (containerResponderActiveIncidents == null || !isAdded()) return;
        containerResponderActiveIncidents.removeAllViews();

        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(requireContext());
        progressBar.setPadding(0, 32, 0, 32);
        containerResponderActiveIncidents.addView(progressBar);

        String token = sessionManager.getAuthToken();
        if (token != null && !token.startsWith("arms_instant_token_") && !token.startsWith("arms_fallback_token_")) {
            new com.example.accidentreportandmonitoringsystem.data.repository.IncidentRepository()
                    .getResponderDispatches(token, new com.example.accidentreportandmonitoringsystem.remote.ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            if (!isAdded()) return;
                            JSONArray dispatches = response.optJSONArray("dispatches");
                            renderActiveReports(dispatches);
                        }

                        @Override
                        public void onError(int statusCode, String errorMessage) {
                            if (!isAdded()) return;
                            renderActiveReports(null);
                        }
                    });
        } else {
            renderActiveReports(null);
        }
    }

    private void renderActiveReports(@Nullable JSONArray apiDispatches) {
        if (containerResponderActiveIncidents == null || !isAdded()) return;
        containerResponderActiveIncidents.removeAllViews();

        try {
            JSONArray globalReports = apiDispatches != null ? apiDispatches : ValenciaGlobalIncidentStore.getReports();
            JSONArray localReports = sessionManager.getLocalReports();
            JSONArray resolvedHistory = ValenciaGlobalIncidentStore.getResolvedHistory();

            JSONArray combinedActive = new JSONArray();
            Set<String> seenRefs = new HashSet<>();
            long now = System.currentTimeMillis();
            long oneMonthMillis = 30L * 24 * 60 * 60 * 1000L;

            if (globalReports != null) {
                for (int i = 0; i < globalReports.length(); i++) {
                    JSONObject rep = globalReports.getJSONObject(i);
                    String ref = rep.optString("incident_reference", "");
                    String st = rep.optString("current_status", "DISPATCHED").toUpperCase();
                    long t = rep.optLong("timestamp_millis", now);
                    if (!ref.isEmpty() && !seenRefs.contains(ref) && !st.contains("RESOLVED") && (now - t <= oneMonthMillis)) {
                        seenRefs.add(ref);
                        combinedActive.put(rep);
                    }
                }
            }
            if (localReports != null) {
                for (int i = 0; i < localReports.length(); i++) {
                    JSONObject rep = localReports.getJSONObject(i);
                    String ref = rep.optString("incident_reference", "");
                    String st = rep.optString("current_status", "DISPATCHED").toUpperCase();
                    long t = rep.optLong("timestamp_millis", now);
                    if (!ref.isEmpty() && !seenRefs.contains(ref) && !st.contains("RESOLVED") && (now - t <= oneMonthMillis)) {
                        seenRefs.add(ref);
                        combinedActive.put(rep);
                    }
                }
            }

            int ongoingCount = combinedActive.length();
            int resolvedCount = resolvedHistory != null ? resolvedHistory.length() : 0;

            if (tvCountOngoing != null) tvCountOngoing.setText(String.format(Locale.getDefault(), "%02d", ongoingCount));
            if (tvCountResolved != null) tvCountResolved.setText(String.format(Locale.getDefault(), "%02d", resolvedCount));

            if (combinedActive.length() == 0) {
                View emptyCard = LayoutInflater.from(requireContext()).inflate(R.layout.layout_empty_state, containerResponderActiveIncidents, false);
                TextView tvEmptyTitle = emptyCard.findViewById(R.id.tvEmptyTitle);
                TextView tvEmptySub = emptyCard.findViewById(R.id.tvEmptySubtitle);
                if (tvEmptyTitle != null) tvEmptyTitle.setText("No Active Incidents");
                if (tvEmptySub != null) tvEmptySub.setText("No active assigned incidents currently pending for your unit.");
                containerResponderActiveIncidents.addView(emptyCard);
                return;
            }

            for (int i = 0; i < combinedActive.length(); i++) {
                JSONObject rep = combinedActive.optJSONObject(i);
                if (rep == null) continue;
                String ref = rep.optString("incident_reference", "ARMS-VAL");
                String type = rep.optString("incident_type", "EMERGENCY");
                String address = rep.optString("landmark_address", "Valencia City, Bukidnon, Philippines");
                String status = rep.optString("current_status", "DISPATCHED");
                String desc = rep.optString("description", "");
                double lat = rep.optDouble("latitude", 7.9064);
                double lng = rep.optDouble("longitude", 125.0942);
                String photoBase64 = rep.optString("photo_base64", "");
                boolean hasVideo = rep.optBoolean("has_video", false);
                String videoUri = rep.optString("video_uri", "");
                String assignmentId = rep.optString("assignment_id", "");

                View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_responder_active_card, containerResponderActiveIncidents, false);
                TextView tvTitle = cardView.findViewById(R.id.tvIncidentTitle);
                TextView tvStatusBadge = cardView.findViewById(R.id.tvResponderStatusBadge);
                TextView tvLocation = cardView.findViewById(R.id.tvIncidentLocation);
                TextView tvAssigned = cardView.findViewById(R.id.tvAssignedResponders);
                TextView tvDesc = cardView.findViewById(R.id.tvIncidentDescription);

                if (tvTitle != null) tvTitle.setText(type.replace("_", " "));
                if (tvStatusBadge != null) tvStatusBadge.setText(status);
                if (tvLocation != null) tvLocation.setText(address + " • #" + ref);
                if (tvAssigned != null) tvAssigned.setText("Assigned Units: " + sessionManager.getUserAgency());
                if (tvDesc != null) tvDesc.setText("Details: " + desc);

                MaterialButton btnCardStartNav = cardView.findViewById(R.id.btnCardStartNavigation);
                if (btnCardStartNav != null) {
                    btnCardStartNav.setOnClickListener(v -> {
                        String ag = sessionManager.getUserAgency() != null ? sessionManager.getUserAgency().toUpperCase() : "CDRRMO";
                        double sLat = 7.9018, sLng = 125.0912; // CDRRMO Command Center (Sayre Hwy South)
                        if (ag.contains("BFP")) { sLat = 7.9118; sLng = 125.0926; } // BFP Fire Station (Sayre Hwy North)
                        else if (ag.contains("PNP")) { sLat = 7.9048; sLng = 125.0978; } // PNP Police Station (Poblacion East)
                        else if (ag.contains("MED") || ag.contains("HEMS")) { sLat = 7.9162; sLng = 125.0904; } // Medical / Hospital (Bagontaas Hwy)

                        Intent navIntent = new Intent(requireContext(), IncidentLocationActivity.class);
                        navIntent.putExtra("is_dispatcher_map", true);
                        navIntent.putExtra("auto_start_navigation", true);
                        navIntent.putExtra("incident_lat", lat);
                        navIntent.putExtra("incident_lng", lng);
                        navIntent.putExtra("station_lat", sLat);
                        navIntent.putExtra("station_lng", sLng);
                        navIntent.putExtra("incident_ref", ref);
                        navIntent.putExtra("incident_type", type);
                        navIntent.putExtra("address", address);
                        startActivity(navIntent);
                    });
                }

                cardView.setOnClickListener(v -> {
                    Intent intent = new Intent(requireContext(), com.example.accidentreportandmonitoringsystem.ResponderStatusUpdateActivity.class);
                    intent.putExtra("incident_lat", lat);
                    intent.putExtra("incident_lng", lng);
                    intent.putExtra("incident_ref", ref);
                    intent.putExtra("incident_type", type);
                    intent.putExtra("status", status);
                    intent.putExtra("address", address);
                    intent.putExtra("photo_base64", photoBase64);
                    intent.putExtra("has_video", hasVideo);
                    intent.putExtra("video_uri", videoUri);
                    intent.putExtra("assignment_id", assignmentId);
                    startActivity(intent);
                });

                containerResponderActiveIncidents.addView(cardView);
            }

        } catch (Exception ignored) {}
    }
}
