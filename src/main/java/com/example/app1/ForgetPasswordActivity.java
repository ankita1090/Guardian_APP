package com.example.app1;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;  // ✅ Step 4 - Added import
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgetPasswordActivity extends AppCompatActivity {

    TextInputEditText emailInput;
    Button resetButton;
    TextView backToLogin; // ✅ Step 4 - Added
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        emailInput = findViewById(R.id.resetEmailInput);
        resetButton = findViewById(R.id.resetButton);
        backToLogin = findViewById(R.id.backToLogin); // ✅ Step 4 - Connected
        mAuth = FirebaseAuth.getInstance();

        // ✅ Step 4 - Back to Login click
        backToLogin.setOnClickListener(v -> finish());

        resetButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                emailInput.setError("Enter your email");
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ForgetPasswordActivity.this,
                                    "Reset link sent! Check your email.",
                                    Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(ForgetPasswordActivity.this,
                                    "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}