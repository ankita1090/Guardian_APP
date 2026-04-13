package com.example.app1;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

// --- OSM Imports ---
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    ImageView profileIcon, menuIcon;
    ImageView navHome, navContact, navSetting;
    Switch guardianSwitch;
    DrawerLayout drawerLayout;

    // --- Map & Location Variables ---
    private MapView map = null;
    private MyLocationNewOverlay locationOverlay;

    private static final int REQ_CODE_PERMISSIONS = 101;
    private static final int REQ_CODE_BACKGROUND_LOCATION = 102;
    private static final int REQ_CODE_OVERLAY = 103;

    private static final String PREFS_NAME = "GuardianPrefs";

    String[] appPermissions = {
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Status bar and nav bar fix
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#FFEB3B"));
            getWindow().setNavigationBarColor(android.graphics.Color.WHITE);
        }

        // ✅ OSM Configuration (SharedPreferences se pehle initialize karna zaroori hai)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));

        setContentView(R.layout.activity_home);

        // --- OSM Map Setup with Live Location ---
        map = findViewById(R.id.map);
        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setBuiltInZoomControls(false);
            map.setMultiTouchControls(true);

            // Default location as Bhopal
            GeoPoint startPoint = new GeoPoint(23.2599, 77.4126);
            map.getController().setZoom(17.0);
            map.getController().setCenter(startPoint);

            // ✅ LIVE LOCATION OVERLAY: Isse blue dot dikhega aur map follow karega
            locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
            locationOverlay.enableMyLocation();
            locationOverlay.enableFollowLocation(); // Map automatically moves with you
            map.getOverlays().add(locationOverlay);
        }

        // --- Drawer & Top Nav Setup ---
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        menuIcon = findViewById(R.id.imageView5);
        profileIcon = findViewById(R.id.profileIcon);

        menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        profileIcon.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        // --- Bottom Navigation Setup ---
        navHome = findViewById(R.id.imageView9);
        navContact = findViewById(R.id.imageView10);
        navSetting = findViewById(R.id.imageView11);

        navHome.setOnClickListener(v -> Toast.makeText(this, "You are already on Home", Toast.LENGTH_SHORT).show());
        navContact.setOnClickListener(v -> startActivity(new Intent(this, ContactActivity.class)));
        navSetting.setOnClickListener(v -> startActivity(new Intent(this, SettingActivity.class)));

        // --- Drawer Navigation Listener ---
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
            else if (id == R.id.nav_settings) startActivity(new Intent(this, SettingActivity.class));
            else if (id == R.id.nav_permission) startActivity(new Intent(this, PermissionActivity.class));
            else if (id == R.id.nav_help) startActivity(new Intent(this, HelpActivity.class));
            else if (id == R.id.nav_contact) startActivity(new Intent(this, ContactActivity.class));
            else if (id == R.id.nav_recordings) startActivity(new Intent(this, RecordingsActivity.class));

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // --- Guardian Mode Switch Setup ---
        guardianSwitch = findViewById(R.id.switch5);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isModeOn = prefs.getBoolean("GuardianMode", false);
        guardianSwitch.setChecked(isModeOn);

        guardianSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (checkAndRequestPermissions()) {
                    if (checkOverlayPermission()) {
                        requestIgnoreBatteryOptimization();
                        if (checkBackgroundLocationPermission()) {
                            activateGuardian();
                        }
                    } else {
                        guardianSwitch.setChecked(false);
                    }
                } else {
                    guardianSwitch.setChecked(false);
                }
            } else {
                deactivateGuardian();
            }
        });

        // --- Back Press Logic ---
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    finish();
                }
            }
        });
    }

    // ✅ Lifecycle management for OSM (Live location ke liye zaroori hai)
    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
        if (locationOverlay != null) locationOverlay.enableMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
        if (locationOverlay != null) locationOverlay.disableMyLocation();
    }

    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Enable Overlay Permission", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQ_CODE_OVERLAY);
                return false;
            }
        }
        return true;
    }

    private void activateGuardian() {
        saveGuardianState(true);
        startGuardianService();
        Toast.makeText(this, "Guardian Mode: ACTIVE", Toast.LENGTH_SHORT).show();
    }

    private void deactivateGuardian() {
        saveGuardianState(false);
        stopGuardianService();
        Toast.makeText(this, "Guardian Mode: OFF", Toast.LENGTH_SHORT).show();
    }

    private void saveGuardianState(boolean state) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean("GuardianMode", state).apply();
    }

    private void startGuardianService() {
        Intent serviceIntent = new Intent(this, ButtonTriggerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent);
        else startService(serviceIntent);
    }

    private void stopGuardianService() {
        stopService(new Intent(this, ButtonTriggerService.class));
    }

    private void requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    private boolean checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQ_CODE_BACKGROUND_LOCATION);
                return false;
            }
        }
        return true;
    }

    private boolean checkAndRequestPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String perm : appPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perm);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), REQ_CODE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CODE_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) { if (result != PackageManager.PERMISSION_GRANTED) allGranted = false; }
            if (allGranted && checkOverlayPermission() && checkBackgroundLocationPermission()) activateGuardian();
            else guardianSwitch.setChecked(false);
        }
    }
}