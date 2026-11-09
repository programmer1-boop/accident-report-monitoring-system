package com.example.accidentreportandmonitoringsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.google.android.material.card.MaterialCardView;

public class DispatcherProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispatcher_profile);

        sessionManager = new SessionManager(this);

        ImageButton ibBack = findViewById(R.id.ibBack);
        TextView tvDispatcherName = findViewById(R.id.tvDispatcherName);
        TextView tvDispatcherEmail = findViewById(R.id.tvDispatcherEmail);
        MaterialCardView cardDispatcherLogout = findViewById(R.id.cardDispatcherLogout);

        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }

        if (tvDispatcherName != null) {
            tvDispatcherName.setText(sessionManager.getUserName());
        }

        if (tvDispatcherEmail != null) {
            tvDispatcherEmail.setText(sessionManager.getUserEmail());
        }

        if (cardDispatcherLogout != null) {
            cardDispatcherLogout.setOnClickListener(v -> {
                sessionManager.clearSession();
                Toast.makeText(this, "🔒 Successfully signed out.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(DispatcherProfileActivity.this, WelcomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
}
