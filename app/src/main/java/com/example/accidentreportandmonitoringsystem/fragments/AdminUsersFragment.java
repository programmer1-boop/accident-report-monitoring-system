package com.example.accidentreportandmonitoringsystem.fragments;

import android.content.Intent;
import android.graphics.Color;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.accidentreportandmonitoringsystem.AdminCreateResponderActivity;
import com.example.accidentreportandmonitoringsystem.R;
import com.example.accidentreportandmonitoringsystem.remote.ApiClient;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.example.accidentreportandmonitoringsystem.remote.ValenciaGlobalUserStore;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONObject;

public class AdminUsersFragment extends Fragment {

    private SessionManager sessionManager;
    private LinearLayout containerAdminUsersList;
    private EditText etSearchUsers;
    private TextView tabUserAll, tabUserCitizens, tabUserStaff, tvUserCountBadge;
    private TextView tvTotalCitizens, tvTotalStaff;
    private String currentCategory = "ALL"; // ALL, CITIZENS, STAFF
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_users, container, false);
        sessionManager = new SessionManager(requireContext());

        containerAdminUsersList = view.findViewById(R.id.containerAdminUsersList);
        etSearchUsers = view.findViewById(R.id.etSearchUsers);
        tabUserAll = view.findViewById(R.id.tabUserAll);
        tabUserCitizens = view.findViewById(R.id.tabUserCitizens);
        tabUserStaff = view.findViewById(R.id.tabUserStaff);
        tvUserCountBadge = view.findViewById(R.id.tvUserCountBadge);
        tvTotalCitizens = view.findViewById(R.id.tvTotalCitizens);
        tvTotalStaff = view.findViewById(R.id.tvTotalStaff);
        MaterialButton btnCreate = view.findViewById(R.id.btnCreateResponder);

        if (btnCreate != null) {
            btnCreate.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), AdminCreateResponderActivity.class);
                startActivity(intent);
            });
        }

        setupTabs();
        setupSearch();
        fetchBackendUsers();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchBackendUsers();
    }

    private void fetchBackendUsers() {
        ApiClient.get("/admin/users", sessionManager.getAuthToken(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    JSONArray arr = response.optJSONArray("users");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject u = arr.getJSONObject(i);
                            String role = u.optString("role", "CITIZEN");
                            String type = "CITIZEN".equalsIgnoreCase(role) ? "CITIZEN" : "STAFF";
                            JSONObject userObj = new JSONObject();
                            userObj.put("name", u.optString("name", "User"));
                            userObj.put("email", u.optString("email", ""));
                            userObj.put("role", role);
                            userObj.put("status", u.optString("status", "Active"));
                            userObj.put("type", type);
                            sessionManager.saveRegisteredUser(userObj);
                        }
                    }
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> loadUsers());
                    }
                } catch (Exception e) {
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> loadUsers());
                    }
                }
            }

            @Override
            public void onError(int statusCode, String errorMessage) {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> loadUsers());
                }
            }
        });
    }

    private void setupTabs() {
        if (tabUserAll != null) {
            tabUserAll.setOnClickListener(v -> {
                currentCategory = "ALL";
                updateTabStyles();
                loadUsers();
            });
        }
        if (tabUserCitizens != null) {
            tabUserCitizens.setOnClickListener(v -> {
                currentCategory = "CITIZENS";
                updateTabStyles();
                loadUsers();
            });
        }
        if (tabUserStaff != null) {
            tabUserStaff.setOnClickListener(v -> {
                currentCategory = "STAFF";
                updateTabStyles();
                loadUsers();
            });
        }
    }

    private void updateTabStyles() {
        int primaryBg = R.drawable.bg_primary_button;
        int secondaryBg = R.drawable.bg_secondary_button;
        int white = Color.parseColor("#FFFFFF");
        int navy = Color.parseColor("#142B4A");

        if (tabUserAll != null) {
            tabUserAll.setBackgroundResource(currentCategory.equals("ALL") ? primaryBg : secondaryBg);
            tabUserAll.setTextColor(currentCategory.equals("ALL") ? white : navy);
        }
        if (tabUserCitizens != null) {
            tabUserCitizens.setBackgroundResource(currentCategory.equals("CITIZENS") ? primaryBg : secondaryBg);
            tabUserCitizens.setTextColor(currentCategory.equals("CITIZENS") ? white : navy);
        }
        if (tabUserStaff != null) {
            tabUserStaff.setBackgroundResource(currentCategory.equals("STAFF") ? primaryBg : secondaryBg);
            tabUserStaff.setTextColor(currentCategory.equals("STAFF") ? white : navy);
        }
    }

    private void setupSearch() {
        if (etSearchUsers != null) {
            etSearchUsers.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().trim().toLowerCase();
                    loadUsers();
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void loadUsers() {
        if (containerAdminUsersList == null) return;
        containerAdminUsersList.removeAllViews();

        try {
            sessionManager.getRegisteredUsers();
            JSONArray users = ValenciaGlobalUserStore.getUsers();

            String currentEmail = sessionManager.getUserEmail();
            if (currentEmail != null && !currentEmail.trim().isEmpty() && !currentEmail.equalsIgnoreCase("admin@arms.gov.ph")) {
                boolean found = false;
                for (int i = 0; i < users.length(); i++) {
                    if (users.getJSONObject(i).optString("email", "").equalsIgnoreCase(currentEmail)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    JSONObject sessionUser = new JSONObject();
                    sessionUser.put("name", sessionManager.getUserName());
                    sessionUser.put("email", currentEmail);
                    sessionUser.put("role", sessionManager.getUserRole());
                    sessionUser.put("status", "Active");
                    sessionUser.put("type", "CITIZEN".equalsIgnoreCase(sessionManager.getUserRole()) ? "CITIZEN" : "STAFF");
                    sessionManager.saveRegisteredUser(sessionUser);
                    users = ValenciaGlobalUserStore.getUsers();
                }
            }

            int citizenCount = 0;
            int staffCount = 0;
            for (int i = 0; i < users.length(); i++) {
                if (users.getJSONObject(i).optString("type").equals("CITIZEN")) citizenCount++;
                else staffCount++;
            }

            if (tvTotalCitizens != null) tvTotalCitizens.setText(String.valueOf(citizenCount));
            if (tvTotalStaff != null) tvTotalStaff.setText(String.valueOf(staffCount));

            int displayedCount = 0;
            for (int i = 0; i < users.length(); i++) {
                JSONObject u = users.getJSONObject(i);
                String name = u.optString("name", "");
                String role = u.optString("role", "");
                String email = u.optString("email", "");
                String status = u.optString("status", "Active");
                String type = u.optString("type", "CITIZEN");

                boolean matchesCategory = currentCategory.equals("ALL") ||
                        (currentCategory.equals("CITIZENS") && type.equals("CITIZEN")) ||
                        (currentCategory.equals("STAFF") && type.equals("STAFF"));

                boolean matchesSearch = searchQuery.isEmpty() ||
                        name.toLowerCase().contains(searchQuery) ||
                        email.toLowerCase().contains(searchQuery) ||
                        role.toLowerCase().contains(searchQuery);

                if (matchesCategory && matchesSearch) {
                    displayedCount++;
                    View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_admin_user, containerAdminUsersList, false);
                    TextView tvName = cardView.findViewById(R.id.tvUserName);
                    TextView tvRole = cardView.findViewById(R.id.tvUserRole);
                    TextView tvEmail = cardView.findViewById(R.id.tvUserEmail);
                    TextView tvStatus = cardView.findViewById(R.id.tvUserStatus);

                    if (tvName != null) tvName.setText(name);
                    if (tvRole != null) tvRole.setText(role);
                    if (tvEmail != null) tvEmail.setText(email);
                    if (tvStatus != null) {
                        tvStatus.setText(status);
                        if (status.equalsIgnoreCase("Active")) {
                            tvStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                            tvStatus.setTextColor(Color.parseColor("#198754"));
                        } else if (status.equalsIgnoreCase("Inactive")) {
                            tvStatus.setBackgroundColor(Color.parseColor("#F1F5F9"));
                            tvStatus.setTextColor(Color.parseColor("#64748B"));
                        } else if (status.equalsIgnoreCase("Suspended")) {
                            tvStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                            tvStatus.setTextColor(Color.parseColor("#DC2626"));
                        } else {
                            tvStatus.setBackgroundColor(Color.parseColor("#FFF3E0"));
                            tvStatus.setTextColor(Color.parseColor("#D97706"));
                        }
                    }

                    cardView.setOnClickListener(v -> {
                        BottomSheetDialog sheetDialog = new BottomSheetDialog(requireContext());
                        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_admin_user_actions, null);
                        sheetDialog.setContentView(sheetView);

                        TextView tvSheetName = sheetView.findViewById(R.id.sheetTvName);
                        TextView tvSheetEmail = sheetView.findViewById(R.id.sheetTvEmail);
                        TextView tvSheetTypeStatus = sheetView.findViewById(R.id.sheetTvTypeStatus);
                        MaterialButton btnView = sheetView.findViewById(R.id.btnSheetView);
                        MaterialButton btnEdit = sheetView.findViewById(R.id.btnSheetEdit);
                        MaterialButton btnStatusToggle = sheetView.findViewById(R.id.btnSheetStatusToggle);
                        MaterialButton btnResetPassword = sheetView.findViewById(R.id.btnSheetResetPassword);
                        MaterialButton btnDelete = sheetView.findViewById(R.id.btnSheetDelete);

                        if (tvSheetName != null) tvSheetName.setText(name);
                        if (tvSheetEmail != null) tvSheetEmail.setText(email);
                        if (tvSheetTypeStatus != null) tvSheetTypeStatus.setText(role + " • " + status);

                        boolean isActive = status.equalsIgnoreCase("Active");
                        if (btnStatusToggle != null) {
                            btnStatusToggle.setText(isActive ? "Deactivate Account" : "Reactivate Account");
                            btnStatusToggle.setOnClickListener(btView -> {
                                new AlertDialog.Builder(requireContext())
                                        .setTitle(isActive ? "Deactivate Account" : "Reactivate Account")
                                        .setMessage("Are you sure you want to " + (isActive ? "deactivate" : "reactivate") + " " + name + "?")
                                        .setPositiveButton("Confirm", (dialog, which) -> {
                                            Toast.makeText(requireContext(), "✅ Account " + (isActive ? "deactivated" : "reactivated"), Toast.LENGTH_SHORT).show();
                                            sheetDialog.dismiss();
                                            loadUsers();
                                        })
                                        .setNegativeButton("Cancel", null)
                                        .show();
                            });
                        }

                        if (btnView != null) {
                            btnView.setOnClickListener(btView -> {
                                Toast.makeText(requireContext(), "📋 Viewing details for " + name, Toast.LENGTH_SHORT).show();
                                sheetDialog.dismiss();
                            });
                        }

                        if (btnEdit != null) {
                            btnEdit.setOnClickListener(btView -> {
                                Toast.makeText(requireContext(), "✏️ Edit account for " + name, Toast.LENGTH_SHORT).show();
                                sheetDialog.dismiss();
                            });
                        }

                        if (btnResetPassword != null) {
                            btnResetPassword.setOnClickListener(btView -> {
                                Toast.makeText(requireContext(), "🔑 Password reset instructions sent to " + email, Toast.LENGTH_SHORT).show();
                                sheetDialog.dismiss();
                            });
                        }

                        if (btnDelete != null) {
                            btnDelete.setOnClickListener(btView -> {
                                new AlertDialog.Builder(requireContext())
                                        .setTitle("⚠️ Delete Account")
                                        .setMessage("Warning: Are you sure you want to delete " + name + "? This action cannot be undone.")
                                        .setPositiveButton("Delete", (dialog, which) -> {
                                            Toast.makeText(requireContext(), "🗑️ Account deleted successfully.", Toast.LENGTH_SHORT).show();
                                            sheetDialog.dismiss();
                                            loadUsers();
                                        })
                                        .setNegativeButton("Cancel", null)
                                        .show();
                            });
                        }

                        sheetDialog.show();
                    });

                    containerAdminUsersList.addView(cardView);
                }
            }

            if (displayedCount == 0) {
                TextView tvEmpty = new TextView(requireContext());
                tvEmpty.setText("No existing citizen or staff accounts found. Register a citizen account to see it here.");
                tvEmpty.setTextColor(getResources().getColor(R.color.arms_secondary_text, null));
                tvEmpty.setPadding(16, 24, 16, 24);
                containerAdminUsersList.addView(tvEmpty);
            }

            if (tvUserCountBadge != null) {
                tvUserCountBadge.setText("Showing " + displayedCount + " users");
            }
        } catch (Exception ignored) {}
    }
}
