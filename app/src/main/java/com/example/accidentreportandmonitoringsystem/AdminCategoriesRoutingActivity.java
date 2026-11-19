package com.example.accidentreportandmonitoringsystem;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class AdminCategoriesRoutingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_categories_routing);

        ImageButton ibBack = findViewById(R.id.ibBack);
        if (ibBack != null) ibBack.setOnClickListener(v -> finish());

        MaterialButton btnSave = findViewById(R.id.btnSaveRoutingRules);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                Toast.makeText(this, "Routing configuration saved.", Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }
}
