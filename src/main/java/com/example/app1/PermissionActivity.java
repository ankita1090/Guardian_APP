package com.example.app1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button; // ✅ Added
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionActivity extends AppCompatActivity {

    Switch callSwitch, locationSwitch, contactSwitch, smsSwitch, audioSwitch,
            notificationSwitch, backgroundSwitch;
    Button btnBattery; // ✅ Added

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        // Initialization
        callSwitch = findViewById(R.id.switch2);
        locationSwitch = findViewById(R.id.switch4);
        contactSwitch = findViewById(R.id.switch7);
        smsSwitch = findViewById(R.id.switch8);
        audioSwitch = findViewById(R.id.switch9);
        notificationSwitch = findViewById(R.id.switch10);
        backgroundSwitch = findViewById(R.id.switch11);
        btnBattery = findViewById(R.id.btnBattery); // ✅ Added

        findViewById(R.id.imageView8).setOnClickListener(v -> finish());

        // Status Sync
        updateSwitchStates();

        // 1. Normal Permissions
        callSwitch.setOnClickListener(v -> handleManualClick(callSwitch, Manifest.permission.CALL_PHONE, 1));
        locationSwitch.setOnClickListener(v -> handleManualClick(locationSwitch, Manifest.permission.ACCESS_FINE_LOCATION, 2));
        contactSwitch.setOnClickListener(v -> handleManualClick(contactSwitch, Manifest.permission.READ_CONTACTS, 3));
        smsSwitch.setOnClickListener(v -> handleManualClick(smsSwitch, Manifest.permission.SEND_SMS, 4));
        audioSwitch.setOnClickListener(v -> handleManualClick(audioSwitch, Manifest.permission.RECORD_AUDIO, 5));

        // 2. Notification Permission (Android 13+)
        notificationSwitch.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                handleManualClick(notificationSwitch, Manifest.permission.POST_NOTIFICATIONS, 7);
            } else {
                Toast.makeText(this, "Allowed by default on this version", Toast.LENGTH_SHORT).show();
            }
        });

        // 3. Background Location
        backgroundSwitch.setOnClickListener(v -> {
            if (backgroundSwitch.isChecked()) {
                if (isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 6);
                    } else {
                        Toast.makeText(this, "Not required for this Android version", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    backgroundSwitch.setChecked(false);
                    Toast.makeText(this, "Pehle 'Allow Location Access' ON karein!", Toast.LENGTH_LONG).show();
                }
            } else {
                openSettings("Manual Revoke: Settings > Permissions > Location > Allow all the time (OFF)");
            }
        });

        // ✅ 4. Battery Optimization Button
        btnBattery.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
    }

    private void handleManualClick(Switch s, String perm, int code) {
        if (s.isChecked()) {
            if (!isGranted(perm)) {
                ActivityCompat.requestPermissions(this, new String[]{perm}, code);
            }
        } else {
            openSettings("Please turn off " + perm.split("\\.")[2] + " manually in Settings");
        }
    }

    private void openSettings(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    private void updateSwitchStates() {
        callSwitch.setChecked(isGranted(Manifest.permission.CALL_PHONE));
        locationSwitch.setChecked(isGranted(Manifest.permission.ACCESS_FINE_LOCATION));
        contactSwitch.setChecked(isGranted(Manifest.permission.READ_CONTACTS));
        smsSwitch.setChecked(isGranted(Manifest.permission.SEND_SMS));
        audioSwitch.setChecked(isGranted(Manifest.permission.RECORD_AUDIO));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationSwitch.setChecked(isGranted(Manifest.permission.POST_NOTIFICATIONS));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundSwitch.setChecked(isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION));
        }
    }

    private boolean isGranted(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSwitchStates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        updateSwitchStates();
    }
}