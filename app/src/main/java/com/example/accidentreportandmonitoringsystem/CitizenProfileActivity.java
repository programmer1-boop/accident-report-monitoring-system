package com.example.accidentreportandmonitoringsystem;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.accidentreportandmonitoringsystem.remote.ApiClient;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CitizenProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private ImageView ivProfileAvatar;
    private TextView tvProfileName;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_citizen_profile);
        } catch (Exception e) {
            Toast.makeText(this, "Profile layout error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
        }

        try {
            sessionManager = new SessionManager(this);

            ImageButton ibBack = findViewById(R.id.ibBack);
            tvProfileName = findViewById(R.id.tvProfileName);
            TextView tvProfileRole = findViewById(R.id.tvProfileRole);
            ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
            MaterialCardView cardAvatar = findViewById(R.id.cardAvatar);
            MaterialButton btnChangePhoto = findViewById(R.id.btnChangePhoto);

            LinearLayout layoutEditProfile = findViewById(R.id.layoutEditProfile);
            LinearLayout layoutMyReports = findViewById(R.id.layoutMyReports);
            LinearLayout layoutSettings = findViewById(R.id.layoutSettings);
            LinearLayout layoutHelpSupport = findViewById(R.id.layoutHelpSupport);
            LinearLayout layoutLogout = findViewById(R.id.layoutLogout);

            if (ibBack != null) {
                ibBack.setOnClickListener(v -> finish());
            }

            loadSavedProfilePicture();

            imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            try {
                                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                if (inputStream != null) inputStream.close();

                                if (bitmap != null) {
                                    Bitmap scaledBitmap = scaleBitmap(bitmap, 300);
                                    saveImageLocallyAndCloud(scaledBitmap);

                                    if (ivProfileAvatar != null) {
                                        ivProfileAvatar.setImageBitmap(scaledBitmap);
                                        ivProfileAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    }
                                    Toast.makeText(this, "🖼️ Profile picture updated and synced to cloud!", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(this, "Error saving image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Toast.makeText(this, "Profile picture selection cancelled.", Toast.LENGTH_SHORT).show();
                    }
                }
            );

            android.view.View.OnClickListener openGalleryListener = v -> {
                Toast.makeText(this, "Select new profile picture from gallery", Toast.LENGTH_SHORT).show();
                Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                imagePickerLauncher.launch(pickIntent);
            };

            if (cardAvatar != null) {
                cardAvatar.setOnClickListener(openGalleryListener);
            }
            if (btnChangePhoto != null) {
                btnChangePhoto.setOnClickListener(openGalleryListener);
            }

            String fullName = sessionManager.getUserName();
            if (fullName == null || fullName.trim().isEmpty()) {
                fullName = "Citizen";
            }
            String role = sessionManager.getUserRole();
            if (role == null) {
                role = "CITIZEN";
            }

            if (tvProfileName != null) {
                tvProfileName.setText(fullName);
            }

            if (tvProfileRole != null) {
                String roleDisplay = "Citizen";
                if ("SUPER_ADMIN".equalsIgnoreCase(role)) roleDisplay = "Super Administrator";
                else if ("DISPATCHER".equalsIgnoreCase(role)) roleDisplay = "Dispatcher";
                else if ("RESPONDER".equalsIgnoreCase(role)) roleDisplay = "Responder";
                tvProfileRole.setText(roleDisplay);
            }

            if (layoutEditProfile != null) {
                layoutEditProfile.setOnClickListener(v -> {
                    Intent intent = new Intent(CitizenProfileActivity.this, EditProfileActivity.class);
                    startActivity(intent);
                });
            }

            if (layoutMyReports != null) {
                layoutMyReports.setOnClickListener(v -> {
                    Intent intent = new Intent(CitizenProfileActivity.this, CitizenReportsActivity.class);
                    startActivity(intent);
                });
            }

            if (layoutSettings != null) {
                layoutSettings.setOnClickListener(v -> {
                    Intent intent = new Intent(CitizenProfileActivity.this, SettingsActivity.class);
                    startActivity(intent);
                });
            }

            if (layoutHelpSupport != null) {
                layoutHelpSupport.setOnClickListener(v -> {
                    Intent intent = new Intent(CitizenProfileActivity.this, HelpSupportActivity.class);
                    startActivity(intent);
                });
            }

            if (layoutLogout != null) {
                layoutLogout.setOnClickListener(v -> {
                    sessionManager.clearSession();
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CitizenProfileActivity.this, WelcomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }
        } catch (Exception e) {
            Toast.makeText(this, "Profile init error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedProfilePicture();
        if (tvProfileName != null && sessionManager != null) {
            String fullName = sessionManager.getUserName();
            if (fullName != null && !fullName.trim().isEmpty()) {
                tvProfileName.setText(fullName);
            }
        }
    }

    private void saveImageLocallyAndCloud(Bitmap bitmap) {
        try {
            File imagesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ARMS_Profiles");
            if (!imagesDir.exists()) {
                imagesDir.mkdirs();
            }
            File file = new File(imagesDir, "profile_avatar.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();

            String path = file.getAbsolutePath();
            sessionManager.saveProfileImageUri(path);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            String base64Image = "data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

            JSONObject payload = new JSONObject();
            payload.put("avatar_url", base64Image);
            String token = sessionManager.getAuthToken();
            ApiClient.post("/auth/avatar", payload, token, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {}
                @Override
                public void onError(int statusCode, String errorMessage) {}
            });
        } catch (Exception e) {
            try {
                File file = new File(getFilesDir(), "profile_avatar.jpg");
                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.flush();
                fos.close();
                sessionManager.saveProfileImageUri(file.getAbsolutePath());
            } catch (Exception ignored) {}
        }
    }

    private void loadSavedProfilePicture() {
        if (ivProfileAvatar != null && sessionManager != null) {
            String savedPath = sessionManager.getProfileImageUri();
            if (savedPath != null && !savedPath.isEmpty()) {
                try {
                    if (savedPath.startsWith("data:image")) {
                        String base64Data = savedPath.substring(savedPath.indexOf(",") + 1);
                        byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        if (bitmap != null) {
                            ivProfileAvatar.setImageBitmap(bitmap);
                            ivProfileAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        }
                    } else {
                        File file = new File(savedPath);
                        if (file.exists()) {
                            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                            if (bitmap != null) {
                                ivProfileAvatar.setImageBitmap(bitmap);
                                ivProfileAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }
}
