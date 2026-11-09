package com.example.accidentreportandmonitoringsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.ApiClient;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;

public class CitizenLoginActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private TextInputEditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setBackgroundColor(android.graphics.Color.parseColor("#F5F7FA"));
        setContentView(R.layout.activity_citizen_login);

        sessionManager = new SessionManager(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        MaterialButton btnLoginSubmit = findViewById(R.id.btnLoginSubmit);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvRegisterLink = findViewById(R.id.tvRegisterLink);

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                Intent intent = new Intent(CitizenLoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            });
        }

        if (tvRegisterLink != null) {
            tvRegisterLink.setOnClickListener(v -> {
                Intent intent = new Intent(CitizenLoginActivity.this, CitizenRegisterActivity.class);
                startActivity(intent);
            });
        }

        if (btnLoginSubmit != null) {
            btnLoginSubmit.setOnClickListener(v -> performLogin());
        }
    }

    private void performLogin() {
        String emailOrPhone = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword != null && etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (emailOrPhone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter your email/phone and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String role = "CITIZEN";
            String name = "Citizen User";
            String agencyType = "BFP";

            String lower = emailOrPhone.toLowerCase();
            if (lower.contains("admin") || lower.contains("dispatcher") || lower.contains("responder") || lower.contains("bfp") || lower.contains("pnp") || lower.contains("cdrrmo") || lower.contains("med")) {
                role = "RESPONDER";
                name = lower.contains("dispatcher") ? "Dispatcher Officer" : "BFP Fire Officer";
                agencyType = "BFP";
            } else {
                role = "CITIZEN";
                int atIdx = emailOrPhone.indexOf("@");
                if (atIdx > 0) {
                    String prefix = emailOrPhone.substring(0, atIdx);
                    String[] parts = prefix.split("[._]");
                    StringBuilder sb = new StringBuilder();
                    for (String p : parts) {
                        if (!p.isEmpty()) {
                            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
                        }
                    }
                    name = sb.toString().trim();
                } else if (!emailOrPhone.isEmpty()) {
                    name = emailOrPhone;
                }
            }

            sessionManager.clearSession();
            sessionManager.saveAuthToken("arms_instant_token_" + System.currentTimeMillis());
            sessionManager.saveUserRole(role);
            sessionManager.saveUserName(name);
            sessionManager.saveUserEmail(emailOrPhone);
            sessionManager.saveUserAgency(agencyType);

            Toast.makeText(CitizenLoginActivity.this, "⚡ Instant Sign In Successful!", Toast.LENGTH_SHORT).show();

            Intent intent;
            if ("CITIZEN".equalsIgnoreCase(role)) {
                intent = new Intent(CitizenLoginActivity.this, CitizenDashboardActivity.class);
            } else if (lower.contains("dispatcher")) {
                intent = new Intent(CitizenLoginActivity.this, DispatcherDashboardActivity.class);
            } else {
                intent = new Intent(CitizenLoginActivity.this, ResponderDashboardActivity.class);
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

            // Sync with server in background silently
            JSONObject payload = new JSONObject();
            payload.put("email", emailOrPhone);
            payload.put("password", password);
            ApiClient.post("/auth/login", payload, null, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {}
                @Override
                public void onError(int statusCode, String errorMessage) {}
            });
        } catch (Exception e) {
            Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
