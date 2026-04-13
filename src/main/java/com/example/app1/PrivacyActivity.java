package com.example.app1;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PrivacyActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. EdgeToEdge enable
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_privacy);

        // 2. Padding logic for Status Bar (Ensure ID is privacyWebView in XML)
        View mainView = findViewById(R.id.privacyWebView);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        WebView webView = findViewById(R.id.privacyWebView);

        // 3. Prevent Chrome from opening (Keep it inside the app)
        webView.setWebViewClient(new WebViewClient());

        // 4. WebView Settings for better performance
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // 5. FIXED URL (Your exact Gist Raw URL)
        String myUrl = "https://gist.githubusercontent.com/ankita1090/01fc51e3affd83afe61616fe8257844a/raw/2adcfb51f7ed1d3c666162873e66285be2b2bd50/privacy-policy.md";

        // Load the page
        webView.loadUrl(myUrl.trim());
    }

    // Handle Back Navigation within WebView
    @Override
    public void onBackPressed() {
        WebView webView = findViewById(R.id.privacyWebView);
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}