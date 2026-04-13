package com.example.app1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ContactActivity extends AppCompatActivity {

    EditText etName, etEmail, etSubject, etMessage;
    Button btnSend;
    ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Send button
        btnSend.setOnClickListener(v -> {

            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String subject = etSubject.getText().toString().trim();
            String message = etMessage.getText().toString().trim();

            // Validation
            if (TextUtils.isEmpty(name)) {
                etName.setError("Enter your name");
                return;
            }
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Enter your email");
                return;
            }
            if (TextUtils.isEmpty(subject)) {
                etSubject.setError("Enter subject");
                return;
            }
            if (TextUtils.isEmpty(message)) {
                etMessage.setError("Enter your message");
                return;
            }

            // ✅ Opens Gmail/email app with pre-filled details
            String fullMessage = "Name: " + name + "\nEmail: " + email + "\n\n" + message;

            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:careerguardianapp@gmail.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, fullMessage);

            try {
                startActivity(Intent.createChooser(emailIntent, "Send Email"));
            } catch (Exception e) {
                Toast.makeText(ContactActivity.this,
                        "No email app found on this device",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}