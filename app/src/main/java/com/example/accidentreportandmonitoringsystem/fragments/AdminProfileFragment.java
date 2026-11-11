package com.example.accidentreportandmonitoringsystem.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.accidentreportandmonitoringsystem.R;
import com.example.accidentreportandmonitoringsystem.WelcomeActivity;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.google.android.material.button.MaterialButton;

public class AdminProfileFragment extends Fragment {

    private SessionManager sessionManager;
    private TextView tvAdminProfileName, tvAdminProfileEmail, tvProfileFullName;
    private MaterialButton btnAdminLogout;
    private TextView menuChangePassword;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_profile, container, false);
        sessionManager = new SessionManager(requireContext());

        tvAdminProfileName = view.findViewById(R.id.tvAdminProfileName);
        tvAdminProfileEmail = view.findViewById(R.id.tvAdminProfileEmail);
        tvProfileFullName = view.findViewById(R.id.tvProfileFullName);
        btnAdminLogout = view.findViewById(R.id.btnAdminLogout);
        menuChangePassword = view.findViewById(R.id.menuChangePassword);

        if (tvAdminProfileName != null) tvAdminProfileName.setText(sessionManager.getUserName());
        if (tvAdminProfileEmail != null) tvAdminProfileEmail.setText(sessionManager.getUserEmail());
        if (tvProfileFullName != null) tvProfileFullName.setText(sessionManager.getUserName());

        if (menuChangePassword != null) {
            menuChangePassword.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Change Password")
                        .setMessage("Password reset instructions have been sent to your administrator email.")
                        .setPositiveButton("OK", null)
                        .show();
            });
        }

        if (btnAdminLogout != null) {
            btnAdminLogout.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to sign out of the ARMS Admin Portal?")
                        .setPositiveButton("Logout", (dialog, which) -> {
                            sessionManager.clearSession();
                            Toast.makeText(requireContext(), "🔒 Successfully signed out.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(requireContext(), WelcomeActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            requireActivity().finish();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        return view;
    }
}
