package com.example.accidentreportandmonitoringsystem;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class CameraCaptureActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 1001;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setBackgroundColor(android.graphics.Color.TRANSPARENT);

        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(this, "📸 Media captured! Opening live GPS map...", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CameraCaptureActivity.this, IncidentLocationActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Capture cancelled.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        );

        checkAndLaunchNativeCamera();
    }

    private void checkAndLaunchNativeCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchNativeCameraApp();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, CAMERA_PERMISSION_CODE);
        }
    }

    private void launchNativeCameraApp() {
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(cameraIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to launch native camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchNativeCameraApp();
            } else {
                Toast.makeText(this, "Camera and audio permissions are required to capture evidence.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
