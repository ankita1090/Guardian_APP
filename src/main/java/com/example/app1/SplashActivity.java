package com.example.app1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkUserLogin();
        }, 2000); // 2 sec splash
    }

    private void checkUserLogin() {

        SharedPreferences pref =
                getSharedPreferences("LOGIN", MODE_PRIVATE);

        boolean isLoggedIn =
                pref.getBoolean("isLoggedIn", false);

        Intent intent;

        if (isLoggedIn) {
            intent = new Intent(
                    SplashActivity.this,
                    HomeActivity.class);
        } else {
            intent = new Intent(
                    SplashActivity.this,
                    loginActivity.class);
        }

        startActivity(intent);
        finish();
    }
}