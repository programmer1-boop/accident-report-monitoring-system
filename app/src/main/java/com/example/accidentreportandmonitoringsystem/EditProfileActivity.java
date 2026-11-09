package com.example.accidentreportandmonitoringsystem;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class EditProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private TextInputEditText etEditName, etEditEmail, etEditPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        sessionManager = new SessionManager(this);

        ImageButton ibBack = findViewById(R.id.ibBack);
        etEditName = findViewById(R.id.etEditName);
        etEditEmail = findViewById(R.id.etEditEmail);
        etEditPhone = findViewById(R.id.etEditPhone);
        MaterialButton btnSaveProfile = findViewById(R.id.btnSaveProfile);

        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }

        if (etEditName != null) {
            etEditName.setText(sessionManager.getUserName());
        }
        if (etEditEmail != null) {
            etEditEmail.setText(sessionManager.getUserEmail());
        }
        if (etEditPhone != null) {
            etEditPhone.setText(sessionManager.getUserPhone());
        }

        if (btnSaveProfile != null) {
            btnSaveProfile.setOnClickListener(v -> {
                String name = etEditName != null && etEditName.getText() != null ? etEditName.getText().toString().trim() : "";
                String email = etEditEmail != null && etEditEmail.getText() != null ? etEditEmail.getText().toString().trim() : "";
                String phone = etEditPhone != null && etEditPhone.getText() != null ? etEditPhone.getText().toString().trim() : "";

                if (!name.isEmpty()) {
                    sessionManager.saveUserName(name);
                    if (!email.isEmpty()) sessionManager.saveUserEmail(email);
                    if (!phone.isEmpty()) sessionManager.saveUserPhone(phone);

                    Toast.makeText(this, "✅ Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
