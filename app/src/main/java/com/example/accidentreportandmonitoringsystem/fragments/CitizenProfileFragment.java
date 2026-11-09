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
import androidx.fragment.app.Fragment;
import com.example.accidentreportandmonitoringsystem.R;
import com.example.accidentreportandmonitoringsystem.WelcomeActivity;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.google.android.material.button.MaterialButton;

public class CitizenProfileFragment extends Fragment {

    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_citizen_profile, container, false);

        sessionManager = new SessionManager(requireContext());

        TextView tvProfileName = view.findViewById(R.id.tvProfileName);
        TextView tvProfileRole = view.findViewById(R.id.tvProfileRole);
        MaterialButton btnSignOut = view.findViewById(R.id.btnSignOut);

        if (tvProfileName != null) {
            tvProfileName.setText(sessionManager.getUserName());
        }

        if (tvProfileRole != null) {
            tvProfileRole.setText(sessionManager.getUserRole());
        }

        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> {
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
}
