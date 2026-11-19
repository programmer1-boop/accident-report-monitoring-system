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

        MaterialButton btnDemoCitizen = findViewById(R.id.btnDemoCitizen);
        MaterialButton btnDemoDispatcher = findViewById(R.id.btnDemoDispatcher);
        MaterialButton btnDemoAdmin = findViewById(R.id.btnDemoAdmin);
        MaterialButton btnDemoCdrrmo = findViewById(R.id.btnDemoCdrrmo);
        MaterialButton btnDemoBfp = findViewById(R.id.btnDemoBfp);
        MaterialButton btnDemoPnp = findViewById(R.id.btnDemoPnp);
        MaterialButton btnDemoMedical = findViewById(R.id.btnDemoMedical);

        if (btnDemoCitizen != null) {
            btnDemoCitizen.setOnClickListener(v -> loginAsDemo("citizen", "Juan Citizen", "CITIZEN", "CITIZEN"));
        }
        if (btnDemoDispatcher != null) {
            btnDemoDispatcher.setOnClickListener(v -> loginAsDemo("dispatcher", "Command Dispatcher", "DISPATCHER", "DISPATCHER"));
        }
        if (btnDemoAdmin != null) {
            btnDemoAdmin.setOnClickListener(v -> loginAsDemo("admin", "System Administrator", "SUPER_ADMIN", "ADMIN"));
        }
        if (btnDemoCdrrmo != null) {
            btnDemoCdrrmo.setOnClickListener(v -> loginAsDemo("cdrrmo", "CDRRMO Disaster Officer", "RESPONDER", "CDRRMO"));
        }
        if (btnDemoBfp != null) {
            btnDemoBfp.setOnClickListener(v -> loginAsDemo("bfp", "BFP Fire Officer", "RESPONDER", "BFP"));
        }
        if (btnDemoPnp != null) {
            btnDemoPnp.setOnClickListener(v -> loginAsDemo("pnp", "PNP Police Officer", "RESPONDER", "PNP"));
        }
        if (btnDemoMedical != null) {
            btnDemoMedical.setOnClickListener(v -> loginAsDemo("medical", "Medical ER Officer", "RESPONDER", "MEDICAL_EMS"));
        }

        if (tvRegisterLink != null) {
            tvRegisterLink.setOnClickListener(v -> {
                Intent intent = new Intent(CitizenLoginActivity.this, CitizenRegisterActivity.class);
                startActivity(intent);
            });
        }

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                Intent intent = new Intent(CitizenLoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            });
        }
    }

    private void loginAsDemo(String username, String name, String role, String agencyType) {
        try {
            completeLoginSession("arms_instant_token_" + System.currentTimeMillis(), username, name, role, agencyType, "⚡ Demo Sign In: " + name);
        } catch (Exception e) {
            Toast.makeText(this, "Demo login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void completeLoginSession(String token, String username, String name, String role, String agencyType, String toastMsg) throws Exception {
        sessionManager.clearSession();
        sessionManager.saveAuthToken(token);
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

        Toast.makeText(CitizenLoginActivity.this, toastMsg, Toast.LENGTH_SHORT).show();

        Intent intent;
        if ("CITIZEN".equalsIgnoreCase(role)) {
            intent = new Intent(CitizenLoginActivity.this, CitizenDashboardActivity.class);
        } else if ("DISPATCHER".equalsIgnoreCase(role) || username.toLowerCase().contains("dispatcher")) {
            intent = new Intent(CitizenLoginActivity.this, DispatcherDashboardActivity.class);
        } else if ("ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role) || username.toLowerCase().contains("admin")) {
            intent = new Intent(CitizenLoginActivity.this, AdminDashboardActivity.class);
        } else {
            String agencyUpper = agencyType != null ? agencyType.toUpperCase() : "";
            if (agencyUpper.contains("CDRRMO")) {
                intent = new Intent(CitizenLoginActivity.this, CdrrmoDashboardActivity.class);
            } else if (agencyUpper.contains("BFP")) {
                intent = new Intent(CitizenLoginActivity.this, BfpDashboardActivity.class);
            } else if (agencyUpper.contains("PNP")) {
                intent = new Intent(CitizenLoginActivity.this, PnpDashboardActivity.class);
            } else if (agencyUpper.contains("MED") || agencyUpper.contains("HEMS")) {
                intent = new Intent(CitizenLoginActivity.this, MedicalDashboardActivity.class);
            } else {
                intent = new Intent(CitizenLoginActivity.this, ResponderDashboardActivity.class);
            }
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void performLogin() {
        String username = etUsername != null && etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword != null && etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter your username and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnLoginSubmit != null) {
            btnLoginSubmit.setEnabled(false);
            btnLoginSubmit.setText("Signing in...");
        }

        String loginIdentifier = username.contains("@") ? username : username + "@arms.gov.ph";
        new com.example.accidentreportandmonitoringsystem.data.repository.AuthRepository().login(
                loginIdentifier,
                password,
                new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        if (btnLoginSubmit != null) {
                            btnLoginSubmit.setEnabled(true);
                            btnLoginSubmit.setText("Sign In");
                        }
                        try {
                            String token = response.optString("token", "");
                            JSONObject userObj = response.optJSONObject("user");
                            String role = userObj != null ? userObj.optString("role", "CITIZEN") : "CITIZEN";
                            String fullName = userObj != null ? userObj.optString("full_name", username) : username;
                            String email = userObj != null ? userObj.optString("email", loginIdentifier) : loginIdentifier;
                            String agency = userObj != null ? userObj.optString("agency_type", role) : role;
                            completeLoginSession(token, email, fullName, role, agency, "Sign In Successful!");
                        } catch (Exception e) {
                            Toast.makeText(CitizenLoginActivity.this, "Login parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(int statusCode, String errorMessage) {
                        if (btnLoginSubmit != null) {
                            btnLoginSubmit.setEnabled(true);
                            btnLoginSubmit.setText("Sign In");
                        }
                        if (statusCode == 503) {
                            performOfflineFallbackLogin(username);
                        } else {
                            Toast.makeText(CitizenLoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    private void performOfflineFallbackLogin(String username) {
        try {
            JSONArray persistedUsers = sessionManager.getRegisteredUsers();

            String lower = username.toLowerCase();
            boolean isSystemStaff = lower.contains("admin") ||
                    lower.contains("dispatcher") ||
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

            if (!(isSystemStaff || isRegisteredCitizen || ValenciaGlobalUserStore.accountExists(username))) {
                Toast.makeText(this, "Account not found. Please register first.", Toast.LENGTH_LONG).show();
                return;
            }

            String role = "CITIZEN";
            String name = username;
            String agencyType = "CITIZEN";

            if (lower.contains("admin")) {
                role = "SUPER_ADMIN";
                name = "System Administrator";
                agencyType = "ADMIN";
            } else if (lower.contains("dispatcher")) {
                role = "DISPATCHER";
                name = "Command Dispatcher";
                agencyType = "DISPATCHER";
            } else if (lower.contains("cdrrmo")) {
                role = "RESPONDER";
                name = "CDRRMO Disaster Officer";
                agencyType = "CDRRMO";
            } else if (lower.contains("pnp")) {
                role = "RESPONDER";
                name = "PNP Police Officer";
                agencyType = "PNP";
            } else if (lower.contains("med") || lower.contains("hems")) {
                role = "RESPONDER";
                name = "Medical ER Officer";
                agencyType = "MEDICAL_EMS";
            } else if (isSystemStaff) {
                role = "RESPONDER";
                name = "BFP Fire Officer";
                agencyType = "BFP";
            }

            completeLoginSession("arms_instant_token_" + System.currentTimeMillis(), username, name, role, agencyType, "Offline Sign In Successful!");
        } catch (Exception e) {
            Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
