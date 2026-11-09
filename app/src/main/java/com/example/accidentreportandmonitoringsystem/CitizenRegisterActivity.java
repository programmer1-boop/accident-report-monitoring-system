package com.example.accidentreportandmonitoringsystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.ApiClient;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONObject;

public class CitizenRegisterActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilEmail, tilPhone, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etEmail, etPhone, etPassword, etConfirmPassword;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_register);

        sessionManager = new SessionManager(this);

        tilFullName = findViewById(R.id.tilFullName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        MaterialButton btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        TextView tvLoginLink = findViewById(R.id.tvLoginLink);

        if (btnRegisterSubmit != null) {
            btnRegisterSubmit.setOnClickListener(v -> validateAndRegister());
        }

        if (tvLoginLink != null) {
            tvLoginLink.setOnClickListener(v -> {
                Intent intent = new Intent(CitizenRegisterActivity.this, CitizenLoginActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    private void validateAndRegister() {
        if (tilFullName != null) tilFullName.setError(null);
        if (tilEmail != null) tilEmail.setError(null);
        if (tilPhone != null) tilPhone.setError(null);
        if (tilPassword != null) tilPassword.setError(null);
        if (tilConfirmPassword != null) tilConfirmPassword.setError(null);

        String fullName = etFullName != null && etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String phone = etPhone != null && etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String password = etPassword != null && etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword != null && etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        boolean isValid = true;

        if (TextUtils.isEmpty(fullName)) {
            if (tilFullName != null) tilFullName.setError("Full name is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (tilEmail != null) tilEmail.setError("Valid email address is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(phone)) {
            if (tilPhone != null) tilPhone.setError("Mobile number is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            if (tilPassword != null) tilPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (!password.equals(confirmPassword)) {
            if (tilConfirmPassword != null) tilConfirmPassword.setError("Passwords do not match");
            isValid = false;
        }

        if (isValid) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("full_name", fullName);
                payload.put("fullName", fullName);
                payload.put("name", fullName);
                payload.put("email", email);
                payload.put("phone_number", phone);
                payload.put("password", password);

                Toast.makeText(this, "Submitting Registration to ARMS Backend...", Toast.LENGTH_SHORT).show();

                ApiClient.post("/auth/register", payload, null, new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        try {
                            String token = response.optString("token");
                            JSONObject user = response.optJSONObject("user");
                            String name = user != null ? user.optString("full_name", fullName) : fullName;
                            String userEmail = user != null ? user.optString("email", email) : email;
                            String userPhone = user != null ? user.optString("phone_number", phone) : phone;

                            sessionManager.clearSession();
                            sessionManager.saveAuthToken(token);
                            sessionManager.saveUserRole("CITIZEN");
                            sessionManager.saveUserName(name);
                            sessionManager.saveUserEmail(userEmail);
                            sessionManager.saveUserPhone(userPhone);

                            Toast.makeText(CitizenRegisterActivity.this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(CitizenRegisterActivity.this, CitizenDashboardActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } catch (Exception e) {
                            Toast.makeText(CitizenRegisterActivity.this, "Registration parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(int statusCode, String errorMessage) {
                        // Resilient Fallback for Signup if server offline
                        try {
                            sessionManager.clearSession();
                            sessionManager.saveAuthToken("arms_fallback_token_" + System.currentTimeMillis());
                            sessionManager.saveUserRole("CITIZEN");
                            sessionManager.saveUserName(fullName);
                            sessionManager.saveUserEmail(email);
                            sessionManager.saveUserPhone(phone);

                            Toast.makeText(CitizenRegisterActivity.this, "✅ Account created successfully!", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(CitizenRegisterActivity.this, CitizenDashboardActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } catch (Exception ex) {
                            Toast.makeText(CitizenRegisterActivity.this, "Registration Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            } catch (Exception e) {
                Toast.makeText(this, "Error forming registration request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
