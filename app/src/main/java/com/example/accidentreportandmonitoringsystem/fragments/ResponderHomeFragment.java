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

        if (tvOfficeName != null) {
            tvOfficeName.setText(sessionManager.getUserAgency() + " Command Post • " + sessionManager.getUserName());
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
                Toast.makeText(requireContext(), "🔔 Responder Command Center: All assigned units active.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadAllActiveReports() {
        if (containerResponderActiveIncidents == null) return;
        containerResponderActiveIncidents.removeAllViews();

        try {
            JSONArray globalReports = ValenciaGlobalIncidentStore.getReports();
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
                TextView tvEmpty = new TextView(requireContext());
                tvEmpty.setText("No active assigned incidents currently pending.");
                tvEmpty.setTextColor(getResources().getColor(R.color.arms_secondary_text, null));
                tvEmpty.setPadding(12, 24, 12, 24);
                containerResponderActiveIncidents.addView(tvEmpty);
                return;
            }

            for (int i = 0; i < combinedActive.length(); i++) {
                JSONObject rep = combinedActive.optJSONObject(i);
                if (rep == null) continue;
                String ref = rep.optString("incident_reference", "ARMS-VAL");
                String type = rep.optString("incident_type", "EMERGENCY");
                String address = rep.optString("landmark_address", "Valencia City");
                String status = rep.optString("current_status", "DISPATCHED");
                String desc = rep.optString("description", "");
                double lat = rep.optDouble("latitude", 7.9064);
                double lng = rep.optDouble("longitude", 125.0942);

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

                cardView.setOnClickListener(v -> {
                    Intent intent = new Intent(requireContext(), IncidentLocationActivity.class);
                    intent.putExtra("is_dispatcher_map", true);
                    intent.putExtra("incident_lat", lat);
                    intent.putExtra("incident_lng", lng);
                    intent.putExtra("station_lat", 7.9064);
                    intent.putExtra("station_lng", 125.0942);
                    intent.putExtra("incident_ref", ref);
                    intent.putExtra("incident_type", type);
                    intent.putExtra("address", address);
                    startActivity(intent);
                });

                containerResponderActiveIncidents.addView(cardView);
            }

        } catch (Exception ignored) {}
    }
}
