package com.example.app1;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. EdgeToEdge enable karein (Modern Android Looks)
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_help);

        // 2. Padding handle karein taaki status bar ke piche text na chhup jaye
        // XML mein top-most layout ki ID 'main' honi chahiye
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // 3. Back Button Logic
        // Humne XML mein button ki ID 'btnBackHelp' rakhi hai
        Button btnBack = findViewById(R.id.btnBackHelp);

        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Current activity ko close karke wapas Settings par jane ke liye
                    finish();
                }
            });
        }
    }

    // Optional: System Back Button handle karne ke liye
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}