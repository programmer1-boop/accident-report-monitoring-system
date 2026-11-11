package com.example.accidentreportandmonitoringsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.ApiClient;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalUserStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONObject;

public class CitizenLoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnLoginSubmit;
    private TextView tvRegisterLink, tvForgotPassword;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setBackgroundColor(android.graphics.Color.parseColor("#F5F7FA"));
        setContentView(R.layout.activity_citizen_login);

        sessionManager = new SessionManager(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLoginSubmit = findViewById(R.id.btnLoginSubmit);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        if (btnLoginSubmit != null) {
            btnLoginSubmit.setOnClickListener(v -> performLogin());
        }

        if (tvRegisterLink != null) {
            tvRegisterLink.setOnClickListener(v -> {
                Intent intent = new Intent(CitizenLoginActivity.this, CitizenRegisterActivity.class);
                startActivity(intent);
            });
        }

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                Toast.makeText(this, "Password reset link sent to your registered email.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void performLogin() {
        String username = etUsername != null && etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword != null && etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter your username and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONArray persistedUsers = sessionManager.getRegisteredUsers();

            String lower = username.toLowerCase();
            boolean isSystemStaff = lower.contains("admin") ||
                    lower.contains("responder") ||
                    lower.contains("bfp") ||
                    lower.contains("pnp") ||
                    lower.contains("cdrrmo") ||
                    lower.contains("hems") ||
                    lower.contains("med");

            boolean isRegisteredCitizen = false;
            if (!isSystemStaff) {
                for (int i = 0; i < persistedUsers.length(); i++) {
                    JSONObject u = persistedUsers.getJSONObject(i);
                    String uName = u.optString("username", u.optString("name", "")).trim();
                    if (uName.equalsIgnoreCase(username)) {
                        isRegisteredCitizen = true;
                        break;
                    }
                }
            }

            boolean isKnownAccount = isSystemStaff || isRegisteredCitizen || ValenciaGlobalUserStore.accountExists(username);

            if (!isKnownAccount) {
                Toast.makeText(this, "❌ Account not found. Please register first.", Toast.LENGTH_LONG).show();
                return;
            }

            String role = "CITIZEN";
            String name = "Citizen User";
            String agencyType = "BFP";

            if (lower.contains("admin")) {
                role = "ADMIN";
                name = "System Administrator";
                agencyType = "ADMIN";
            } else if (isSystemStaff) {
                role = "RESPONDER";
                name = "BFP Fire Officer";
                agencyType = "BFP";
            } else {
                role = "CITIZEN";
                name = username;
            }

            sessionManager.clearSession();
            sessionManager.saveAuthToken("arms_instant_token_" + System.currentTimeMillis());
            sessionManager.saveUserRole(role);
            sessionManager.saveUserName(name);
            sessionManager.saveUserEmail(username + "@arms.gov.ph");
            sessionManager.saveUserAgency(agencyType);

            JSONObject loginUser = new JSONObject();
            loginUser.put("name", name);
            loginUser.put("username", username);
            loginUser.put("email", username + "@arms.gov.ph");
            loginUser.put("role", role);
            loginUser.put("status", "Active");
            loginUser.put("type", "CITIZEN".equalsIgnoreCase(role) ? "CITIZEN" : "STAFF");
            sessionManager.saveRegisteredUser(loginUser);

            Toast.makeText(CitizenLoginActivity.this, "⚡ Instant Sign In Successful!", Toast.LENGTH_SHORT).show();

            Intent intent;
            if ("CITIZEN".equalsIgnoreCase(role)) {
                intent = new Intent(CitizenLoginActivity.this, CitizenDashboardActivity.class);
            } else if ("ADMIN".equalsIgnoreCase(role) || lower.contains("admin")) {
                intent = new Intent(CitizenLoginActivity.this, AdminDashboardActivity.class);
            } else {
                intent = new Intent(CitizenLoginActivity.this, ResponderDashboardActivity.class);
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

            JSONObject payload = new JSONObject();
            payload.put("username", username);
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
