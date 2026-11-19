package com.example.accidentreportandmonitoringsystem;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;

public class ForgotPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        TextInputEditText etForgotEmail = findViewById(R.id.etForgotEmail);
        MaterialButton btnResetPasswordSubmit = findViewById(R.id.btnResetPasswordSubmit);

        if (btnResetPasswordSubmit != null) {
            btnResetPasswordSubmit.setOnClickListener(v -> {
                String email = etForgotEmail != null && etForgotEmail.getText() != null
                        ? etForgotEmail.getText().toString().trim() : "";
                if (email.isEmpty()) {
                    Toast.makeText(this, "Please enter your email or username.", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnResetPasswordSubmit.setEnabled(false);
                btnResetPasswordSubmit.setText("Sending...");

                try {
                    JSONObject payload = new JSONObject();
                    payload.put("email", email);
                    ApiClient.post("/auth/forgot-password", payload, null, new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            btnResetPasswordSubmit.setEnabled(true);
                            btnResetPasswordSubmit.setText("Send Reset Code");
                            Toast.makeText(ForgotPasswordActivity.this,
                                    response.optString("message", "Reset request submitted."),
                                    Toast.LENGTH_LONG).show();
                            finish();
                        }

                        @Override
                        public void onError(int statusCode, String errorMessage) {
                            btnResetPasswordSubmit.setEnabled(true);
                            btnResetPasswordSubmit.setText("Send Reset Code");
                            Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    btnResetPasswordSubmit.setEnabled(true);
                    btnResetPasswordSubmit.setText("Send Reset Code");
                }
            });
        }
    }
}
