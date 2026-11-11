package com.example.accidentreportandmonitoringsystem;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.example.accidentreportandmonitoringsystem.fragments.AdminActivityFragment;
import com.example.accidentreportandmonitoringsystem.fragments.AdminHomeFragment;
import com.example.accidentreportandmonitoringsystem.fragments.AdminProfileFragment;
import com.example.accidentreportandmonitoringsystem.fragments.AdminTeamsFragment;
import com.example.accidentreportandmonitoringsystem.fragments.AdminUsersFragment;
import com.example.accidentreportandmonitoringsystem.remote.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminDashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private final Fragment homeFragment = new AdminHomeFragment();
    private final Fragment usersFragment = new AdminUsersFragment();
    private final Fragment teamsFragment = new AdminTeamsFragment();
    private final Fragment activityFragment = new AdminActivityFragment();
    private final Fragment profileFragment = new AdminProfileFragment();
    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment activeFragment = homeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.parseColor("#F8FAFC")));
        setContentView(R.layout.activity_admin_dashboard);

        sessionManager = new SessionManager(this);

        BottomNavigationView bottomNavigation = findViewById(R.id.adminBottomNavigation);

        if (savedInstanceState == null) {
            try {
                fm.beginTransaction().add(R.id.adminFragmentContainer, profileFragment, "5").hide(profileFragment).commit();
                fm.beginTransaction().add(R.id.adminFragmentContainer, activityFragment, "4").hide(activityFragment).commit();
                fm.beginTransaction().add(R.id.adminFragmentContainer, teamsFragment, "3").hide(teamsFragment).commit();
                fm.beginTransaction().add(R.id.adminFragmentContainer, usersFragment, "2").hide(usersFragment).commit();
                fm.beginTransaction().add(R.id.adminFragmentContainer, homeFragment, "1").commit();
                activeFragment = homeFragment;
            } catch (Exception e) {
                Toast.makeText(this, "Error loading admin dashboard: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_admin_home);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_admin_home) {
                    switchFragment(homeFragment);
                    return true;
                } else if (id == R.id.nav_admin_users) {
                    switchFragment(usersFragment);
                    return true;
                } else if (id == R.id.nav_admin_teams) {
                    switchFragment(teamsFragment);
                    return true;
                } else if (id == R.id.nav_admin_activity) {
                    switchFragment(activityFragment);
                    return true;
                } else if (id == R.id.nav_admin_profile) {
                    switchFragment(profileFragment);
                    return true;
                }
                return false;
            });
        }
    }

    private void switchFragment(Fragment target) {
        if (activeFragment != target && target != null) {
            try {
                fm.beginTransaction().hide(activeFragment).show(target).commit();
                activeFragment = target;
            } catch (Exception e) {
                Toast.makeText(this, "Navigation error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
