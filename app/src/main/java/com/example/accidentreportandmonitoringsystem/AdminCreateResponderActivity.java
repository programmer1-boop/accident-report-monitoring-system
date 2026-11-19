package com.example.accidentreportandmonitoringsystem;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalUserStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONObject;

public class AdminCreateResponderActivity extends AppCompatActivity {

    private TextInputEditText etOfficeEmail, etContactNumber, etUsername, etPassword, etConfirmPassword;
    private Spinner spinnerOfficeName;
    private MaterialButton btnSubmitOfficeAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setBackgroundColor(Color.parseColor("#F8FAFC"));
        setContentView(R.layout.activity_admin_create_responder);

        spinnerOfficeName = findViewById(R.id.spinnerOfficeName);
        etOfficeEmail = findViewById(R.id.etOfficeEmail);
        etContactNumber = findViewById(R.id.etContactNumber);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSubmitOfficeAccount = findViewById(R.id.btnSubmitOfficeAccount);

        setupSpinner();

        if (btnSubmitOfficeAccount != null) {
            btnSubmitOfficeAccount.setOnClickListener(v -> validateAndCreateOfficeAccount());
        }
    }

    private void setupSpinner() {
        String[] offices = {"BFP – Bureau of Fire Protection", "PNP – Philippine National Police", "CDRRMO – City Disaster Risk Reduction and Management Office", "CHO / HEMS – City Health Office"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, offices);
        if (spinnerOfficeName != null) {
            spinnerOfficeName.setAdapter(adapter);
        }
    }

    private void validateAndCreateOfficeAccount() {
        String office = spinnerOfficeName != null ? spinnerOfficeName.getSelectedItem().toString() : "BFP";
        String email = etOfficeEmail != null && etOfficeEmail.getText() != null ? etOfficeEmail.getText().toString().trim() : "";
        String phone = etContactNumber != null && etContactNumber.getText() != null ? etContactNumber.getText().toString().trim() : "";
        String username = etUsername != null && etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword != null && etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPass = etConfirmPassword != null && etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        if (email.isEmpty() || phone.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPass)) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if office already exists
        try {
            JSONArray users = ValenciaGlobalUserStore.getUsers();
            for (int i = 0; i < users.length(); i++) {
                JSONObject u = users.getJSONObject(i);
                if (u.optString("office_name", "").equalsIgnoreCase(office) || u.optString("name", "").equalsIgnoreCase(office)) {
                    new AlertDialog.Builder(this)
                            .setTitle("⚠️ Existing Office Account")
                            .setMessage("This office already has an existing account.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }
            }

            // Create office account
            JSONObject officeAccount = new JSONObject();
            officeAccount.put("name", office);
            officeAccount.put("office_name", office);
            officeAccount.put("email", email);
            officeAccount.put("phone", phone);
            officeAccount.put("username", username);
            officeAccount.put("role", "RESPONDER");
            officeAccount.put("status", "Active");
            officeAccount.put("type", "STAFF");
            ValenciaGlobalUserStore.addUser(officeAccount);

            com.example.accidentreportandmonitoringsystem.remote.SessionManager sm =
                    new com.example.accidentreportandmonitoringsystem.remote.SessionManager(this);
            String token = sm.getAuthToken();
            if (token != null && !token.startsWith("arms_instant_token_") && !token.startsWith("arms_fallback_token_")) {
                JSONObject apiPayload = new JSONObject();
                apiPayload.put("full_name", office);
                apiPayload.put("email", email);
                apiPayload.put("phone_number", phone);
                apiPayload.put("role", "RESPONDER");
                apiPayload.put("password", password);
                com.example.accidentreportandmonitoringsystem.remote.ApiClient.post("/admin/provision-staff", apiPayload, token,
                        new com.example.accidentreportandmonitoringsystem.remote.ApiClient.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {}
                            @Override
                            public void onError(int statusCode, String errorMessage) {}
                        });
            }

            new AlertDialog.Builder(this)
                    .setTitle("✅ Office Account Created")
                    .setMessage("Official account for " + office + " has been successfully provisioned.")
                    .setPositiveButton("Back to Users", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
