package com.example.app1;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class ProfileActivity extends AppCompatActivity {

    EditText etName, etAge, etAddress, etEmergency1, etEmergency2, etTriggerWord;
    ImageView ivProfilePic;
    Button btnSave;
    ImageButton btnGetLocation;
    FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 1. Views Initialize
        etName = findViewById(R.id.etName);
        etAge = findViewById(R.id.etAge);
        etAddress = findViewById(R.id.etAddress);
        etEmergency1 = findViewById(R.id.etEmergency1);
        etEmergency2 = findViewById(R.id.etEmergency2);
        etTriggerWord = findViewById(R.id.etTriggerWord);  // NEW
        ivProfilePic = findViewById(R.id.ivProfilePic);
        btnSave = findViewById(R.id.btnSaveProfile);
        btnGetLocation = findViewById(R.id.btnGetLocation);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 2. Data Load
        loadData();

        // 3. Click Listeners
        btnGetLocation.setOnClickListener(v -> fetchLocation());

        ivProfilePic.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, 100);
        });

        btnSave.setOnClickListener(v -> saveData());
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        String currentLoc = String.format(java.util.Locale.US, "Lat: %.4f, Long: %.4f", lat, lon);
                        etAddress.setText(currentLoc);
                        Toast.makeText(this, "Live Location Fetched!", Toast.LENGTH_SHORT).show();
                    } else {
                        fusedLocationClient.getLastLocation().addOnSuccessListener(lastLoc -> {
                            if (lastLoc != null) {
                                etAddress.setText("Lat: " + lastLoc.getLatitude() + ", Long: " + lastLoc.getLongitude());
                            } else {
                                Toast.makeText(this, "Please turn on GPS/Location in Settings", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Location Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                try {
                    getContentResolver().takePersistableUriPermission(selectedImage,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    ivProfilePic.setImageURI(selectedImage);
                    SharedPreferences.Editor editor = getSharedPreferences("GuardianPrefs", MODE_PRIVATE).edit();
                    editor.putString("profilePicUri", selectedImage.toString());
                    editor.apply();
                } catch (Exception e) {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void saveData() {
        // Trim and lowercase the trigger word before saving
        String triggerWord = etTriggerWord.getText().toString().trim().toLowerCase();
        if (triggerWord.isEmpty()) {
            triggerWord = "help"; // fallback default
        }

        SharedPreferences.Editor editor = getSharedPreferences("GuardianPrefs", MODE_PRIVATE).edit();
        editor.putString("name", etName.getText().toString());
        editor.putString("age", etAge.getText().toString());
        editor.putString("address", etAddress.getText().toString());
        editor.putString("contact1", etEmergency1.getText().toString());
        editor.putString("contact2", etEmergency2.getText().toString());
        editor.putString("triggerWord", triggerWord);  // NEW
        editor.apply();

        Toast.makeText(this, "Saved! Trigger word: \"" + triggerWord + "\"", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void loadData() {
        SharedPreferences prefs = getSharedPreferences("GuardianPrefs", MODE_PRIVATE);
        etName.setText(prefs.getString("name", ""));
        etAge.setText(prefs.getString("age", ""));
        etAddress.setText(prefs.getString("address", ""));
        etEmergency1.setText(prefs.getString("contact1", ""));
        etEmergency2.setText(prefs.getString("contact2", ""));
        etTriggerWord.setText(prefs.getString("triggerWord", "help"));  // NEW - default "help"

        String imageUriString = prefs.getString("profilePicUri", "");
        if (!imageUriString.isEmpty()) {
            try {
                Uri imageUri = Uri.parse(imageUriString);
                ivProfilePic.setImageURI(imageUri);
            } catch (Exception e) {
                ivProfilePic.setImageResource(R.drawable.profile);
            }
        }
    }
}