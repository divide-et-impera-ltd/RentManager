package com.example.rentmanager.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.rentmanager.R;
import com.example.rentmanager.databinding.ActivityMainBinding;
import com.example.rentmanager.fragments.HomeFragment;
import com.example.rentmanager.fragments.ResidencesFragment;
import com.example.rentmanager.fragments.SettingsFragment;
import com.example.rentmanager.models.Residence;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView bottomNavigationView = binding.bottomNavigation;

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            switch (item.getItemId()) {
                case R.id.action_home:
                    selectedFragment = HomeFragment.newInstance();
                    break;
                case R.id.action_residence:
                    selectedFragment = ResidencesFragment.newInstance(getApplication());
                    break;
                case R.id.action_settings:
                    selectedFragment = SettingsFragment.newInstance();
                    break;
            }
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.frame_layout, selectedFragment);
            transaction.commit();
            return true;
        });

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_layout, HomeFragment.newInstance());
        transaction.commit();
    }

    private void navigateToAddAddressActivity() {
        Intent addAddressActivityIntent = new Intent(this, AddAddressActivity.class);
        startActivity(addAddressActivityIntent);
    }
}
