package com.example.app1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class loginActivity extends AppCompatActivity {

    TextInputEditText emailInput, passwordInput;
    Button loginButton;
    TextView createAccountText;
    TextView signupTab, loginTab;
    TextView forgetPasswordText; // ✅ Step 3 - Added

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Connect XML views
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.button2);
        createAccountText = findViewById(R.id.createAccountText);

        // Connect the tab buttons
        signupTab = findViewById(R.id.textView4);
        loginTab = findViewById(R.id.textView5);

        forgetPasswordText = findViewById(R.id.textView11); // ✅ Step 3 - Connected

        mAuth = FirebaseAuth.getInstance();

        // Signup Tab Click — switch to SignupActivity
        signupTab.setOnClickListener(v -> {
            Intent intent = new Intent(loginActivity.this, SignupActivity.class);
            startActivity(intent);
            finish();
        });

        // Login Tab Click — already on login page, do nothing
        loginTab.setOnClickListener(v -> {
            // Already here
        });

        // Create Account text click
        createAccountText.setOnClickListener(v -> {
            Intent intent = new Intent(loginActivity.this, SignupActivity.class);
            startActivity(intent);
            finish();
        });

        // ✅ Step 3 - Forget Password Click
        forgetPasswordText.setOnClickListener(v -> {
            startActivity(new Intent(loginActivity.this, ForgetPasswordActivity.class));
        });

        // Login Button Logic
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                emailInput.setError("Enter Email");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                passwordInput.setError("Enter Password");
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(loginActivity.this,
                                    "Login Successful", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(loginActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(loginActivity.this,
                                    "User not registered or wrong credentials",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            Intent intent = new Intent(loginActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        }
    }
}