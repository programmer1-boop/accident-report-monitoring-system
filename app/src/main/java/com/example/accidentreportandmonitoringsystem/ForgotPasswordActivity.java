package com.example.accidentreportandmonitoringsystem;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class ForgotPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        MaterialButton btnResetPasswordSubmit = findViewById(R.id.btnResetPasswordSubmit);
        if (btnResetPasswordSubmit != null) {
            btnResetPasswordSubmit.setOnClickListener(v -> {
                Toast.makeText(this, "Reset Code Requested (Awaiting Phase 4 Auth API)", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
