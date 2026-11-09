package com.example.accidentreportandmonitoringsystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalIncidentStore;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class DispatcherDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispatcher_dashboard);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        if (savedInstanceState == null) {
            loadFragment(new DispatcherHomeFragment());
        }

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_disp_dashboard);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_disp_dashboard) {
                    loadFragment(new DispatcherHomeFragment());
                    return true;
                } else if (itemId == R.id.nav_disp_incidents) {
                    loadFragment(new DispatcherIncidentsFragment());
                    return true;
                } else if (itemId == R.id.nav_disp_map) {
                    Intent intent = new Intent(this, DispatcherMapActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_disp_history) {
                    loadFragment(new DispatcherHistoryFragment());
                    return true;
                }
                return false;
            });
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit();
    }

    // --- TAB 1: Dashboard Home ---
    public static class DispatcherHomeFragment extends Fragment {
        private SessionManager sessionManager;
        private LinearLayout containerDispatcherRecentReports;
        private WebView mapWebViewDispatcher;

        @SuppressLint("SetJavaScriptEnabled")
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_dispatcher_home, container, false);
            sessionManager = new SessionManager(requireContext());
            containerDispatcherRecentReports = view.findViewById(R.id.containerDispatcherRecentReports);
            mapWebViewDispatcher = view.findViewById(R.id.mapWebViewDispatcher);

            MaterialCardView cardMapRoot = view.findViewById(R.id.cardIncidentMapRoot);
            TextView tvSeeAll = view.findViewById(R.id.tvSeeAllReports);
            ImageButton ibBell = view.findViewById(R.id.ibDispatcherBell);

            if (tvSeeAll != null) tvSeeAll.setOnClickListener(v -> showViewAllReportsDialog());
            if (cardMapRoot != null) {
                cardMapRoot.setOnClickListener(v -> {
                    Intent intent = new Intent(requireContext(), DispatcherMapActivity.class);
                    startActivity(intent);
                });
            }
            if (ibBell != null) {
                ibBell.setOnClickListener(v -> Toast.makeText(requireContext(), "🔔 Dispatcher Notifications: All systems secure.", Toast.LENGTH_SHORT).show());
            }

            if (mapWebViewDispatcher != null) {
                WebSettings settings = mapWebViewDispatcher.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
            }

            loadDispatcherMapWithAllReports();
            loadRecentReports();
            return view;
        }

        private void loadDispatcherMapWithAllReports() {
            if (mapWebViewDispatcher == null) return;

            StringBuilder markersJs = new StringBuilder();
            markersJs.append("var map = L.map('map').setView([7.9064, 125.0942], 14);");
            markersJs.append("L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);");

            try {
                JSONArray globalReports = ValenciaGlobalIncidentStore.getReports();
                JSONArray localReports = sessionManager.getLocalReports();

                Set<String> seenRefs = new HashSet<>();
                long now = System.currentTimeMillis();
                long oneMonthMillis = 30L * 24 * 60 * 60 * 1000L; // 1 month retention rule

                if (globalReports != null) {
                    for (int i = 0; i < globalReports.length(); i++) {
                        JSONObject rep = globalReports.getJSONObject(i);
                        String ref = rep.optString("incident_reference", "");
                        long t = rep.optLong("timestamp_millis", now);
                        if (!ref.isEmpty() && !seenRefs.contains(ref) && (now - t <= oneMonthMillis)) {
                            seenRefs.add(ref);
                            double lat = rep.optDouble("latitude", 7.9064);
                            double lng = rep.optDouble("longitude", 125.0942);
                            String type = rep.optString("incident_type", "EMERGENCY");
                            String address = rep.optString("landmark_address", "Valencia City");

                            markersJs.append("L.marker([").append(lat).append(", ").append(lng).append("]).addTo(map)")
                                     .append(".bindPopup('<b>🚨 ").append(type.replace("_", " ")).append("</b><br>")
                                     .append(address).append("<br>Ref: #").append(ref).append("');");
                        }
                    }
                }
                if (localReports != null) {
                    for (int i = 0; i < localReports.length(); i++) {
                        JSONObject rep = localReports.getJSONObject(i);
                        String ref = rep.optString("incident_reference", "");
                        long t = rep.optLong("timestamp_millis", now);
                        if (!ref.isEmpty() && !seenRefs.contains(ref) && (now - t <= oneMonthMillis)) {
                            seenRefs.add(ref);
                            double lat = rep.optDouble("latitude", 7.9064);
                            double lng = rep.optDouble("longitude", 125.0942);
                            String type = rep.optString("incident_type", "EMERGENCY");
                            String address = rep.optString("landmark_address", "Valencia City");

                            markersJs.append("L.marker([").append(lat).append(", ").append(lng).append("]).addTo(map)")
                                     .append(".bindPopup('<b>🚨 ").append(type.replace("_", " ")).append("</b><br>")
                                     .append(address).append("<br>Ref: #").append(ref).append("');");
                        }
                    }
                }
            } catch (Exception ignored) {}

            String mapHtml = "<!DOCTYPE html><html><head>" +
                    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                    "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />" +
                    "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>" +
                    "<style>body { margin:0; padding:0; background:#f4f6f9; } #map { width:100%; height:100vh; }</style>" +
                    "</head><body>" +
                    "<div id=\"map\"></div>" +
                    "<script>" +
                    markersJs +
                    "</script>" +
                    "</body></html>";

            mapWebViewDispatcher.loadDataWithBaseURL("https://example.com", mapHtml, "text/html", "UTF-8", null);
        }

        private void loadRecentReports() {
            if (containerDispatcherRecentReports == null) return;
            try {
                containerDispatcherRecentReports.removeAllViews();
                JSONArray globalReports = ValenciaGlobalIncidentStore.getReports();
                JSONArray localReports = sessionManager.getLocalReports();

                JSONArray combined = new JSONArray();
                Set<String> seenRefs = new HashSet<>();
                long now = System.currentTimeMillis();
                long oneMonthMillis = 30L * 24 * 60 * 60 * 1000L;

                if (globalReports != null) {
                    for (int i = 0; i < globalReports.length(); i++) {
                        JSONObject rep = globalReports.getJSONObject(i);
                        String ref = rep.optString("incident_reference", "");
                        long t = rep.optLong("timestamp_millis", now);
                        if (!ref.isEmpty() && !seenRefs.contains(ref) && (now - t <= oneMonthMillis)) {
                            seenRefs.add(ref);
                            combined.put(rep);
                        }
                    }
                }
                if (localReports != null) {
                    for (int i = 0; i < localReports.length(); i++) {
                        JSONObject rep = localReports.getJSONObject(i);
                        String ref = rep.optString("incident_reference", "");
                        long t = rep.optLong("timestamp_millis", now);
                        if (!ref.isEmpty() && !seenRefs.contains(ref) && (now - t <= oneMonthMillis)) {
                            seenRefs.add(ref);
                            combined.put(rep);
                        }
                    }
                }

                if (combined.length() == 0) {
                    TextView tvEmpty = new TextView(requireContext());
                    tvEmpty.setText("No recent incident reports found.");
                    tvEmpty.setTextColor(getResources().getColor(R.color.arms_secondary_text, null));
                    tvEmpty.setPadding(16, 24, 16, 24);
                    containerDispatcherRecentReports.addView(tvEmpty);
                    return;
                }

                for (int i = 0; i < Math.min(combined.length(), 5); i++) {
                    JSONObject rep = combined.getJSONObject(i);
                    String ref = rep.optString("incident_reference", "ARMS-VAL");
                    String status = rep.optString("current_status", "DISPATCHED");
                    String address = rep.optString("landmark_address", "Valencia City");
                    String photoBase64 = rep.optString("photo_base64", "");

                    View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_recent_report, containerDispatcherRecentReports, false);
                    TextView tvRef = cardView.findViewById(R.id.tvReportRef);
                    TextView tvStatus = cardView.findViewById(R.id.tvReportStatus);
                    TextView tvLocation = cardView.findViewById(R.id.tvReportLocation);
                    ImageView ivThumb = cardView.findViewById(R.id.ivReportThumb);

                    if (tvRef != null) tvRef.setText(String.format("#%s", ref));
                    if (tvLocation != null) tvLocation.setText("📍 " + address);
                    if (tvStatus != null) {
                        tvStatus.setText("● " + status);
                        tvStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                        tvStatus.setTextColor(Color.parseColor("#DC2626"));
                    }
                    if (ivThumb != null) {
                        try {
                            if (photoBase64 != null && !photoBase64.trim().isEmpty()) {
                                byte[] decoded = Base64.decode(photoBase64, Base64.NO_WRAP);
                                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                                if (bmp != null) {
                                    ivThumb.setImageBitmap(bmp);
                                }
                            } else {
                                Bitmap bmp = Bitmap.createBitmap(200, 150, Bitmap.Config.ARGB_8888);
                                android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
                                canvas.drawColor(android.graphics.Color.parseColor("#142B4A"));
                                android.graphics.Paint paint = new android.graphics.Paint();
                                paint.setColor(android.graphics.Color.WHITE);
                                paint.setTextSize(24f);
                                canvas.drawText("ARMS", 20f, 85f, paint);
                                ivThumb.setImageBitmap(bmp);
                            }
                        } catch (Exception ignored) {}
                    }

                    cardView.setOnClickListener(v -> openReportReview(rep));
                    containerDispatcherRecentReports.addView(cardView);
                }
            } catch (Exception ignored) {}
        }

        private void openReportReview(JSONObject rep) {
            String ref = rep.optString("incident_reference", "ARMS-VAL");
            String type = rep.optString("incident_type", "EMERGENCY");
            String address = rep.optString("landmark_address", "Valencia City");
            String date = rep.optString("created_at", "Just now");
            String photoBase64 = rep.optString("photo_base64", "");
            boolean hasVideo = rep.optBoolean("has_video", false);
            String videoUri = rep.optString("video_uri", "");
            double lat = rep.optDouble("latitude", 7.9064);
            double lng = rep.optDouble("longitude", 125.0942);

            Intent intent = new Intent(requireContext(), DispatcherReportReviewActivity.class);
            intent.putExtra("incident_ref", ref);
            intent.putExtra("incident_type", type);
            intent.putExtra("location", address);
            intent.putExtra("created_at", date);
            intent.putExtra("photo_base64", photoBase64);
            intent.putExtra("has_video", hasVideo);
            intent.putExtra("video_uri", videoUri);
            intent.putExtra("latitude", lat);
            intent.putExtra("longitude", lng);
            startActivity(intent);
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
                    long now = System.currentTimeMillis();
                    long oneMonthMillis = 30L * 24 * 60 * 60 * 1000L;

                    if (globalReports != null) {
                        for (int i = 0; i < globalReports.length(); i++) {
                            JSONObject rep = globalReports.getJSONObject(i);
                            String ref = rep.optString("incident_reference", "");
                            long t = rep.optLong("timestamp_millis", now);
                            if (!ref.isEmpty() && !seenRefs.contains(ref) && (now - t <= oneMonthMillis)) {
                                seenRefs.add(ref);
                                combined.put(rep);
                            }
                        }
                    }
                    if (localReports != null) {
                        for (int i = 0; i < localReports.length(); i++) {
                            JSONObject rep = localReports.getJSONObject(i);
                            String ref = rep.optString("incident_reference", "");
                            long t = rep.optLong("timestamp_millis", now);
                            if (!ref.isEmpty() && !seenRefs.contains(ref) && (now - t <= oneMonthMillis)) {
                                seenRefs.add(ref);
                                combined.put(rep);
                            }
                        }
                    }

                    for (int i = 0; i < combined.length(); i++) {
                        JSONObject rep = combined.getJSONObject(i);
                        String ref = rep.optString("incident_reference", "ARMS-VAL");
                        String status = rep.optString("current_status", "DISPATCHED");
                        String address = rep.optString("landmark_address", "Valencia City");
                        String photoBase64 = rep.optString("photo_base64", "");

                        View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_recent_report, container, false);
                        TextView tvRef = cardView.findViewById(R.id.tvReportRef);
                        TextView tvStatus = cardView.findViewById(R.id.tvReportStatus);
                        TextView tvLocation = cardView.findViewById(R.id.tvReportLocation);
                        ImageView ivThumb = cardView.findViewById(R.id.ivReportThumb);

                        if (tvRef != null) tvRef.setText(String.format("#%s", ref));
                        if (tvLocation != null) tvLocation.setText("📍 " + address);
                        if (tvStatus != null) {
                            tvStatus.setText("● " + status);
                            tvStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                            tvStatus.setTextColor(Color.parseColor("#DC2626"));
                        }
                        if (ivThumb != null) {
                            try {
                                if (photoBase64 != null && !photoBase64.trim().isEmpty()) {
                                    byte[] decoded = Base64.decode(photoBase64, Base64.NO_WRAP);
                                    Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                                    if (bmp != null) {
                                        ivThumb.setImageBitmap(bmp);
                                    }
                                } else {
                                    Bitmap bmp = Bitmap.createBitmap(200, 150, Bitmap.Config.ARGB_8888);
                                    android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
                                    canvas.drawColor(android.graphics.Color.parseColor("#142B4A"));
                                    android.graphics.Paint paint = new android.graphics.Paint();
                                    paint.setColor(android.graphics.Color.WHITE);
                                    paint.setTextSize(24f);
                                    canvas.drawText("ARMS", 20f, 85f, paint);
                                    ivThumb.setImageBitmap(bmp);
                                }
                            } catch (Exception ignored) {}
                        }

                        cardView.setOnClickListener(v -> {
                            dialog.dismiss();
                            openReportReview(rep);
                        });

                        container.addView(cardView);
                    }
                } catch (Exception ignored) {}
            }

            if (btnClose != null) {
                btnClose.setOnClickListener(v -> dialog.dismiss());
            }

            dialog.show();
        }
    }

    // --- TAB 2: Incidents Management ---
    public static class DispatcherIncidentsFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            LinearLayout layout = new LinearLayout(requireContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(32, 32, 32, 32);

            TextView tv = new TextView(requireContext());
            tv.setText("🛡️ Incident Management & Verification\n\n• Verify Incident Validity\n• Reject Invalid/False Reports\n• Initiate Team Dispatch");
            tv.setTextColor(getResources().getColor(R.color.arms_navy, null));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            layout.addView(tv);

            return layout;
        }
    }

    // --- TAB 4: History ---
    public static class DispatcherHistoryFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            LinearLayout layout = new LinearLayout(requireContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(32, 32, 32, 32);

            TextView tv = new TextView(requireContext());
            tv.setText("Archive & Response History");
            tv.setTextColor(getResources().getColor(R.color.arms_navy, null));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            layout.addView(tv);

            return layout;
        }
    }
}
