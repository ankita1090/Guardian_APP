package com.example.app1;

import android.Manifest;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1;
    private static final int REQUEST_ID_ROLE_MANAGER = 101;
    private static final int OVERLAY_PERMISSION_REQ_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Sabse pehle Consent Check hoga
        checkUserConsent();
    }

    private void checkUserConsent() {
        SharedPreferences sharedPreferences = getSharedPreferences("GuardianPrefs", MODE_PRIVATE);
        boolean isAgreed = sharedPreferences.getBoolean("isAgreed", false);

        if (!isAgreed) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Safety & Privacy Consent");

            // --- UI for Checkbox inside Dialog ---
            final LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 40, 50, 10);

            final TextView message = new TextView(this);
            message.setText("Guardian requires sensitive permissions to provide emergency services: \n\n" +
                    "• Location: To share your spot during SOS.\n" +
                    "• Microphone: To detect 'HELP' voice command.\n" +
                    "• SMS: To alert your contacts.\n\n" +
                    "Please accept to continue.");
            message.setTextColor(android.graphics.Color.BLACK);
            layout.addView(message);

            final CheckBox checkBox = new CheckBox(this);
            checkBox.setText("I agree to the Privacy Policy and allow background safety services.");
            checkBox.setPadding(0, 20, 0, 20);
            layout.addView(checkBox);

            builder.setView(layout);
            builder.setCancelable(false);

            builder.setPositiveButton("Agree", null); // Set to null for custom validation

            builder.setNegativeButton("View Policy", (dialog, which) -> {
                startActivity(new Intent(this, PrivacyActivity.class));
            });

            final AlertDialog dialog = builder.create();
            dialog.show();

            // Checkbox validation logic
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (checkBox.isChecked()) {
                    sharedPreferences.edit().putBoolean("isAgreed", true).apply();
                    dialog.dismiss();
                    startPermissionFlow(); // Agree karne par hi aage badhega
                } else {
                    Toast.makeText(this, "Please check the box to continue", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            startPermissionFlow();
        }
    }

    private void startPermissionFlow() {
        checkOverlayPermission();
        checkAllRuntimePermissions();
        requestDefaultDialerRole();
    }

    // --- AAPKA PURANA PERMISSION LOGIC ---

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
                Toast.makeText(this, "Please allow 'Display over other apps' for SOS feature", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkAllRuntimePermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CALL_PHONE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        // Voice trigger ke liye mic permission bhi add kar di hai
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    private void requestDefaultDialerRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager) getSystemService(Context.ROLE_SERVICE);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                    Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER);
                    startActivityForResult(intent, REQUEST_ID_ROLE_MANAGER);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Toast.makeText(this, "All Permissions Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions Denied! Some features may not work.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ID_ROLE_MANAGER) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "App set as default dialer", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Overlay Permission Enabled", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}