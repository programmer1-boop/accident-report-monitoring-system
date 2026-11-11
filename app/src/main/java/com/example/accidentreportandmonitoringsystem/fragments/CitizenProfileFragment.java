package com.example.accidentreportandmonitoringsystem.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.accidentreportandmonitoringsystem.EditProfileActivity;
import com.example.accidentreportandmonitoringsystem.HelpSupportActivity;
import com.example.accidentreportandmonitoringsystem.R;
import com.example.accidentreportandmonitoringsystem.SettingsActivity;
import com.example.accidentreportandmonitoringsystem.WelcomeActivity;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class CitizenProfileFragment extends Fragment {

    private SessionManager sessionManager;
    private ImageView ivProfileAvatar;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private long lastClickTime = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_citizen_profile, container, false);

        sessionManager = new SessionManager(requireContext());

        TextView tvProfileName = view.findViewById(R.id.tvProfileName);
        TextView tvProfileRole = view.findViewById(R.id.tvProfileRole);
        MaterialButton btnSignOut = view.findViewById(R.id.btnSignOut);
        ImageButton btnProfileSettings = view.findViewById(R.id.btnProfileSettings);
        FrameLayout frameAvatarContainer = view.findViewById(R.id.frameAvatarContainer);
        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar);

        if (tvProfileName != null) {
            tvProfileName.setText(sessionManager.getUserName());
        }

        if (tvProfileRole != null) {
            tvProfileRole.setText(sessionManager.getUserRole());
        }

        loadSavedProfilePicture();

        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            if (inputStream != null) inputStream.close();

                            if (bitmap != null) {
                                Bitmap scaledBitmap = scaleBitmap(bitmap, 300);
                                saveImageLocally(scaledBitmap);

                                if (ivProfileAvatar != null) {
                                    ivProfileAvatar.post(() -> {
                                        ivProfileAvatar.setImageBitmap(scaledBitmap);
                                        ivProfileAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                        ivProfileAvatar.requestLayout();
                                    });
                                }
                                Toast.makeText(requireContext(), "🖼️ Profile picture updated successfully!", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "Error saving image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        );

        if (frameAvatarContainer != null) {
            frameAvatarContainer.setOnClickListener(v -> {
                if (System.currentTimeMillis() - lastClickTime < 800) return;
                lastClickTime = System.currentTimeMillis();
                Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                imagePickerLauncher.launch(pickIntent);
            });
        }

        if (btnProfileSettings != null) {
            btnProfileSettings.setOnClickListener(v -> {
                if (System.currentTimeMillis() - lastClickTime < 800) return;
                lastClickTime = System.currentTimeMillis();
                Intent intent = new Intent(requireContext(), SettingsActivity.class);
                startActivity(intent);
            });
        }

        setupClickListeners(view);

        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> {
                if (System.currentTimeMillis() - lastClickTime < 800) return;
                lastClickTime = System.currentTimeMillis();
                sessionManager.clearSession();
                Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(requireContext(), WelcomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSavedProfilePicture();
    }

    private void loadSavedProfilePicture() {
        try {
            String savedAvatar = sessionManager.getProfileImageUri();
            if (savedAvatar != null && !savedAvatar.trim().isEmpty() && ivProfileAvatar != null) {
                byte[] decodedBytes = Base64.decode(savedAvatar, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                if (decodedBitmap != null) {
                    ivProfileAvatar.post(() -> {
                        ivProfileAvatar.setImageBitmap(decodedBitmap);
                        ivProfileAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        ivProfileAvatar.requestLayout();
                    });
                }
            }
        } catch (Exception ignored) {}
    }

    private void saveImageLocally(Bitmap bitmap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            String encoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            sessionManager.saveProfileImageUri(encoded);
        } catch (Exception ignored) {}
    }

    private Bitmap scaleBitmap(Bitmap bm, int maxSize) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        if (width <= maxSize && height <= maxSize) return bm;
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(bm, width, height, true);
    }

    private void setupClickListeners(View view) {
        setupMenuRow(view, R.id.menuPersonalDetails);
        setupMenuRow(view, R.id.menuAddress);
        setupMenuRow(view, R.id.menuContact);
        setupMenuRow(view, R.id.menuEmergencyContact);
        setupMenuRow(view, R.id.menuMedicalInfo);
        setupMenuRow(view, R.id.menuSecurity);
        setupMenuRow(view, R.id.menuNotificationsSetting);

        View menuPrivacy = view.findViewById(R.id.menuPrivacy);
        if (menuPrivacy != null) {
            menuPrivacy.setOnClickListener(v -> {
                if (System.currentTimeMillis() - lastClickTime < 800) return;
                lastClickTime = System.currentTimeMillis();
                Toast.makeText(requireContext(), "Privacy Policy: All data encrypted & secure.", Toast.LENGTH_SHORT).show();
            });
        }

        View menuHelp = view.findViewById(R.id.menuHelp);
        if (menuHelp != null) {
            menuHelp.setOnClickListener(v -> {
                if (System.currentTimeMillis() - lastClickTime < 800) return;
                lastClickTime = System.currentTimeMillis();
                Intent intent = new Intent(requireContext(), HelpSupportActivity.class);
                startActivity(intent);
            });
        }

        View menuAbout = view.findViewById(R.id.menuAbout);
        if (menuAbout != null) {
            menuAbout.setOnClickListener(v -> {
                if (System.currentTimeMillis() - lastClickTime < 800) return;
                lastClickTime = System.currentTimeMillis();
                Toast.makeText(requireContext(), "ARMS (Accident Report & Monitoring System) v1.0.0 - Valencia City", Toast.LENGTH_LONG).show();
            });
        }
    }

    private void setupMenuRow(View view, int id) {
        View row = view.findViewById(id);
        if (row != null) {
            row.setOnClickListener(v -> {
                if (System.currentTimeMillis() - lastClickTime < 800) return;
                lastClickTime = System.currentTimeMillis();
                Intent intent = new Intent(requireContext(), EditProfileActivity.class);
                startActivity(intent);
            });
        }
    }
}
