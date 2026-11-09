package com.example.accidentreportandmonitoringsystem;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;

public class GuestReportActivity extends AppCompatActivity {

    private static final int CAMERA_PHOTO_REQUEST = 3001;
    private static final int CAMERA_VIDEO_REQUEST = 3002;
    private static final int CAMERA_PERMISSION_CODE = 1001;
    private boolean isVideoRequest = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_capture);

        ImageButton ibBack = findViewById(R.id.ibBack);
        MaterialButton btnCapturePhotoEvidence = findViewById(R.id.btnCapturePhotoEvidence);
        MaterialButton btnRecordVideoEvidence = findViewById(R.id.btnRecordVideoEvidence);

        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }

        if (btnCapturePhotoEvidence != null) {
            btnCapturePhotoEvidence.setOnClickListener(v -> checkAndLaunchCamera(false));
        }

        if (btnRecordVideoEvidence != null) {
            btnRecordVideoEvidence.setOnClickListener(v -> checkAndLaunchCamera(true));
        }
    }

    private void checkAndLaunchCamera(boolean isVideo) {
        isVideoRequest = isVideo;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCameraIntent(isVideo);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCameraIntent(isVideoRequest);
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchCameraIntent(boolean isVideo) {
        Intent intent = new Intent(isVideo ? MediaStore.ACTION_VIDEO_CAPTURE : MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(intent, isVideo ? CAMERA_VIDEO_REQUEST : CAMERA_PHOTO_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Bitmap photoBitmap = null;
            String videoUriStr = "";
            boolean hasVideo = (requestCode == CAMERA_VIDEO_REQUEST);

            if (data != null) {
                Uri uri = data.getData();
                if (uri != null && hasVideo) {
                    videoUriStr = uri.toString();
                }

                if (data.getExtras() != null) {
                    Object obj = data.getExtras().get("data");
                    if (obj instanceof Bitmap) {
                        photoBitmap = (Bitmap) obj;
                    }
                }
            }
            proceedToLiveMap(photoBitmap, hasVideo, videoUriStr);
        }
    }

    private void proceedToLiveMap(Bitmap bitmap, boolean hasVideo, String videoUri) {
        Toast.makeText(this, hasVideo ? "🎥 Video captured! Opening live GPS map..." : "📸 Photo captured! Opening live GPS map...", Toast.LENGTH_SHORT).show();

        try {
            String photoBase64 = "";
            if (bitmap != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                photoBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            }

            Intent intent = new Intent(GuestReportActivity.this, IncidentLocationActivity.class);
            intent.putExtra("photo_base64", photoBase64);
            intent.putExtra("has_video", hasVideo);
            intent.putExtra("video_uri", videoUri);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error passing media: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
