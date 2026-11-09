package com.example.accidentreportandmonitoringsystem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class CitizenNotificationsActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private LinearLayout containerNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_notifications);

        sessionManager = new SessionManager(this);
        containerNotifications = findViewById(R.id.containerNotifications);

        ImageButton ibBack = findViewById(R.id.ibBack);
        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }

        loadNotifications();
    }

    private void loadNotifications() {
        if (containerNotifications == null) return;
        containerNotifications.removeAllViews();

        try {
            JSONArray arr = sessionManager.getAlertNotifications();
            if (arr.length() == 0) {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("No personal status update notifications yet. Notifications will appear here when responders accept, en route, on scene, or resolve your emergency reports.");
                tvEmpty.setTextColor(getResources().getColor(R.color.arms_secondary_text, null));
                tvEmpty.setPadding(24, 48, 24, 48);
                containerNotifications.addView(tvEmpty);
                return;
            }

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String title = obj.optString("title", "Emergency Update");
                String message = obj.optString("message", "Status updated");
                String time = obj.optString("timestamp", "Today");

                View cardView = LayoutInflater.from(this).inflate(R.layout.item_recent_report, containerNotifications, false);
                TextView tvRef = cardView.findViewById(R.id.tvReportRef);
                TextView tvStatus = cardView.findViewById(R.id.tvReportStatus);
                TextView tvType = cardView.findViewById(R.id.tvReportType);
                TextView tvLocation = cardView.findViewById(R.id.tvReportLocation);

                if (tvRef != null) tvRef.setText(time);
                if (tvStatus != null) {
                    tvStatus.setText("NOTIF");
                    tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#E3F2FD"));
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#2563EB"));
                }
                if (tvType != null) tvType.setText(title);
                if (tvLocation != null) tvLocation.setText(message);

                containerNotifications.addView(cardView);
            }
        } catch (Exception ignored) {}
    }
}
