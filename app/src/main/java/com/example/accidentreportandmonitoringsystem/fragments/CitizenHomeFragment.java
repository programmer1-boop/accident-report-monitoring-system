package com.example.accidentreportandmonitoringsystem.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.accidentreportandmonitoringsystem.CitizenReportDetailsActivity;
import com.example.accidentreportandmonitoringsystem.R;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class CitizenHomeFragment extends Fragment {

    private SessionManager sessionManager;
    private LinearLayout containerHomeRecentReports;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_citizen_home, container, false);

        sessionManager = new SessionManager(requireContext());
        containerHomeRecentReports = view.findViewById(R.id.containerHomeRecentReports);

        setupHotlineCopyButtons(view);
        loadRecentReportsInstantly();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecentReportsInstantly();
    }

    private void setupHotlineCopyButtons(View view) {
        setupCopyButton(view, R.id.btnCopyCdrrmo, R.id.tvCdrrmoNum, "(088) 828-1234");
        setupCopyButton(view, R.id.btnCopyBfp, R.id.tvBfpNum, "(088) 828-1119");
        setupCopyButton(view, R.id.btnCopyPnp, R.id.tvPnpNum, "(088) 828-1221");
        setupCopyButton(view, R.id.btnCopyMed, R.id.tvMedNum, "(088) 828-5555");
    }

    private void setupCopyButton(View rootView, int buttonId, int textId, String defaultNumber) {
        View btn = rootView.findViewById(buttonId);
        TextView tv = rootView.findViewById(textId);
        String numberToCopy = tv != null ? tv.getText().toString() : defaultNumber;

        if (btn != null) {
            btn.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Emergency Hotline", numberToCopy);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(requireContext(), "📋 Hotline " + numberToCopy + " copied to clipboard!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadRecentReportsInstantly() {
        try {
            JSONArray localReports = sessionManager.getLocalReports();
            if (containerHomeRecentReports != null) {
                containerHomeRecentReports.removeAllViews();
            }

            int addedCount = 0;
            if (localReports != null) {
                for (int i = 0; i < localReports.length() && addedCount < 3; i++) {
                    JSONObject rep = localReports.getJSONObject(i);
                    addedCount++;
                    String ref = rep.optString("incident_reference", "ARMS-VAL");
                    String type = rep.optString("incident_type", "EMERGENCY");
                    String status = rep.optString("current_status", "SUBMITTED");
                    String address = rep.optString("landmark_address", "Valencia City");
                    String desc = rep.optString("description", "");
                    String dateStr = rep.optString("created_at", "Just now");
                    String photoBase64 = rep.optString("photo_base64", "");
                    boolean hasVideo = rep.optBoolean("has_video", false);
                    String videoUri = rep.optString("video_uri", "");

                    View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_recent_report, containerHomeRecentReports, false);

                    TextView tvRef = cardView.findViewById(R.id.tvReportRef);
                    TextView tvStatus = cardView.findViewById(R.id.tvReportStatus);
                    TextView tvLocation = cardView.findViewById(R.id.tvReportLocation);
                    TextView tvTime = cardView.findViewById(R.id.tvReportTime);
                    ImageView ivThumb = cardView.findViewById(R.id.ivReportThumb);

                    if (tvRef != null) tvRef.setText("#" + ref);
                    if (tvStatus != null) {
                        String displayStatus = status;
                        if (displayStatus.toUpperCase().contains("DISPATCHED")) {
                            displayStatus = "DISPATCHED";
                        }
                        tvStatus.setText("● " + displayStatus);
                        if (status.equalsIgnoreCase("RESOLVED")) {
                            tvStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                            tvStatus.setTextColor(Color.parseColor("#2E9D68"));
                        } else {
                            tvStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                            tvStatus.setTextColor(Color.parseColor("#DC2626"));
                        }
                    }
                    if (tvLocation != null) tvLocation.setText("📍 " + address);
                    if (tvTime != null) tvTime.setText("🕒 " + dateStr);

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
                        Intent intent = new Intent(requireContext(), CitizenReportDetailsActivity.class);
                        intent.putExtra("incident_type", type);
                        intent.putExtra("incident_ref", ref);
                        intent.putExtra("status", status);
                        intent.putExtra("location", address);
                        intent.putExtra("description", desc);
                        intent.putExtra("photo_base64", photoBase64);
                        intent.putExtra("has_video", hasVideo);
                        intent.putExtra("video_uri", videoUri);
                        startActivity(intent);
                    });

                    if (containerHomeRecentReports != null) {
                        containerHomeRecentReports.addView(cardView);
                    }
                }
            }

            if (addedCount == 0 && containerHomeRecentReports != null) {
                TextView tvEmpty = new TextView(requireContext());
                tvEmpty.setText("No recent reports. Submit an emergency report to see it here.");
                tvEmpty.setTextColor(getResources().getColor(R.color.arms_secondary_text, null));
                tvEmpty.setPadding(16, 24, 16, 24);
                containerHomeRecentReports.addView(tvEmpty);
            }
        } catch (Exception ignored) {}
    }
}
