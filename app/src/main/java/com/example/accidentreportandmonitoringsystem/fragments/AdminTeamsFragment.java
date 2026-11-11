package com.example.accidentreportandmonitoringsystem.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.accidentreportandmonitoringsystem.R;
import org.json.JSONArray;
import org.json.JSONObject;

public class AdminTeamsFragment extends Fragment {

    private LinearLayout containerAdminTeamsList;
    private EditText etSearchTeams;
    private TextView tvTotalTeamsCount, tvTotalMembersCount;
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_teams, container, false);

        containerAdminTeamsList = view.findViewById(R.id.containerAdminTeamsList);
        etSearchTeams = view.findViewById(R.id.etSearchTeams);
        tvTotalTeamsCount = view.findViewById(R.id.tvTotalTeamsCount);
        tvTotalMembersCount = view.findViewById(R.id.tvTotalMembersCount);

        setupSearch();
        loadTeams();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTeams();
    }

    private void setupSearch() {
        if (etSearchTeams != null) {
            etSearchTeams.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().trim().toLowerCase();
                    loadTeams();
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void loadTeams() {
        if (containerAdminTeamsList == null) return;
        containerAdminTeamsList.removeAllViews();

        try {
            JSONArray teams = new JSONArray();
            teams.put(new JSONObject().put("acronym", "CDRRMO").put("name", "City Disaster Risk Reduction and Management Office").put("members", 12).put("status", "Active"));
            teams.put(new JSONObject().put("acronym", "BFP").put("name", "Bureau of Fire Protection").put("members", 10).put("status", "Active"));
            teams.put(new JSONObject().put("acronym", "PNP").put("name", "Philippine National Police").put("members", 14).put("status", "Active"));
            teams.put(new JSONObject().put("acronym", "HEMS").put("name", "Health Emergency Medical Services / Valencia City Health Office").put("members", 6).put("status", "Active"));

            int totalMembers = 0;
            int displayedTeams = 0;

            for (int i = 0; i < teams.length(); i++) {
                JSONObject t = teams.getJSONObject(i);
                String acronym = t.optString("acronym", "");
                String name = t.optString("name", "");
                int members = t.optInt("members", 0);
                String status = t.optString("status", "Active");
                totalMembers += members;

                boolean matches = searchQuery.isEmpty() ||
                        acronym.toLowerCase().contains(searchQuery) ||
                        name.toLowerCase().contains(searchQuery);

                if (matches) {
                    displayedTeams++;
                    View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_admin_team, containerAdminTeamsList, false);
                    TextView tvAcronym = cardView.findViewById(R.id.tvTeamAcronym);
                    TextView tvFullName = cardView.findViewById(R.id.tvTeamFullName);
                    TextView tvMembers = cardView.findViewById(R.id.tvTeamMembersCount);
                    TextView tvStatus = cardView.findViewById(R.id.tvTeamStatus);

                    if (tvAcronym != null) tvAcronym.setText(acronym);
                    if (tvFullName != null) tvFullName.setText(name);
                    if (tvMembers != null) tvMembers.setText(members + " Active Members");
                    if (tvStatus != null) tvStatus.setText(status);

                    cardView.setOnClickListener(v -> {
                        Toast.makeText(requireContext(), "🛡️ Team Details: " + acronym + " (" + name + ")", Toast.LENGTH_SHORT).show();
                    });

                    containerAdminTeamsList.addView(cardView);
                }
            }

            if (tvTotalTeamsCount != null) tvTotalTeamsCount.setText(String.valueOf(teams.length()));
            if (tvTotalMembersCount != null) tvTotalMembersCount.setText(String.valueOf(totalMembers));

        } catch (Exception ignored) {}
    }
}
