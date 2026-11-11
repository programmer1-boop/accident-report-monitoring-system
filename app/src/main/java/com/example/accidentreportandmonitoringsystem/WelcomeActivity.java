package com.example.accidentreportandmonitoringsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getWindow().getDecorView().setBackgroundColor(android.graphics.Color.parseColor("#F4F6F9"));
            setContentView(R.layout.activity_welcome);

            MaterialButton btnContinueGuest = findViewById(R.id.btnContinueGuest);
            MaterialButton btnSignIn = findViewById(R.id.btnSignIn);
            TextView tvSignUpLink = findViewById(R.id.tvSignUpLink);

            if (btnContinueGuest != null) {
                btnContinueGuest.setOnClickListener(v -> {
                    Intent intent = new Intent(WelcomeActivity.this, GuestReportActivity.class);
                    startActivity(intent);
                });
            }

            if (btnSignIn != null) {
                btnSignIn.setOnClickListener(v -> {
                    Intent intent = new Intent(WelcomeActivity.this, CitizenLoginActivity.class);
                    startActivity(intent);
                });
            }

            if (tvSignUpLink != null) {
                tvSignUpLink.setOnClickListener(v -> {
                    Intent intent = new Intent(WelcomeActivity.this, CitizenRegisterActivity.class);
                    startActivity(intent);
                });
            }
        } catch (Throwable t) {
            Toast.makeText(this, "Welcome Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            t.printStackTrace();
        }
    }
}
