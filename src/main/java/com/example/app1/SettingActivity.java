package com.example.app1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.content.SharedPreferences;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth; // ✅ Added

public class SettingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setting);

        // 1. Handle Window Insets (Status bar/Navigation bar padding)
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // 2. Initialize UI Elements
        RelativeLayout btnProfile = findViewById(R.id.btnProfile);
        RelativeLayout btnEmergency = findViewById(R.id.btnEmergencyContacts);
        RelativeLayout btnHelp = findViewById(R.id.btnHelpSettings);
        RelativeLayout btnPrivacy = findViewById(R.id.btnPrivacySettings);
        Button btnLogout = findViewById(R.id.btnLogout);

        // 3. Set OnClick Listeners

        // Open Profile
        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(SettingActivity.this, ProfileActivity.class));
        });

        // Open Emergency Contacts Setup
        btnEmergency.setOnClickListener(v -> {
            startActivity(new Intent(SettingActivity.this, ProfileActivity.class));
        });

        // Open Help & FAQ Page
        btnHelp.setOnClickListener(v -> {
            startActivity(new Intent(SettingActivity.this, HelpActivity.class));
        });

        // Open Privacy Policy (WebView Page)
        btnPrivacy.setOnClickListener(v -> {
            startActivity(new Intent(SettingActivity.this, PrivacyActivity.class));
        });

        // Handle Logout ✅ Fixed
        btnLogout.setOnClickListener(v -> {

            // ✅ Fix 1 - Sign out Firebase session
            FirebaseAuth.getInstance().signOut();

            // ✅ Fix 2 - Clear correct SharedPreferences
            SharedPreferences pref = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.clear();
            editor.apply();

            // ✅ Fix 3 - Go directly to loginActivity, not Splash
            Intent intent = new Intent(SettingActivity.this, loginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}