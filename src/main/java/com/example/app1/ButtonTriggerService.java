package com.example.app1;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ButtonTriggerService extends Service implements SensorEventListener {
    private static final String PREFS_NAME = "GuardianPrefs";
    private static final String CHANNEL_ID = "guardian_channel";
    private SensorManager sensorManager;
    private PowerManager.WakeLock cpuWakeLock;

    private int tapCount = 0;
    private long lastTapTime = 0;
    private static final float TAP_THRESHOLD = 22.0f;

    private WindowManager windowManager;
    private View bottomTouchView;
    private int locTapCount = 0;
    private long lastLocTapTime = 0;
    private FusedLocationProviderClient fusedLocationClient;

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        startForegroundService();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        // Keeping CPU alive to process sensors/voice
        cpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Guardian:CPUKeepAlive");
        cpuWakeLock.acquire();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        }

        setupFloatingButton();

        if (isGuardianModeActive()) {
            initVoiceTrigger();
        }
    }

    private void initVoiceTrigger() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return;

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                handleVoiceResult(results);
                restartListening();
            }
            @Override
            public void onPartialResults(Bundle partialResults) {
                handleVoiceResult(partialResults);
            }
            @Override
            public void onError(int error) {
                restartListening();
            }
            @Override public void onReadyForSpeech(Bundle b) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float v) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onEvent(int i, Bundle b) {}
        });

        speechRecognizer.startListening(speechIntent);
    }

    private void restartListening() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isGuardianModeActive() && speechRecognizer != null) {
                speechRecognizer.startListening(speechIntent);
            }
        }, 1000);
    }

    private void handleVoiceResult(Bundle bundle) {
        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            String spokenText = matches.get(0).toLowerCase().trim();
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String savedTrigger = prefs.getString("triggerWord", "help").toLowerCase().trim();

            if (spokenText.contains(savedTrigger)) {
                forceWakeScreen();
                startAutoRecording();
                vibrate(200);
            } else if (spokenText.contains("recording off")) {
                stopAutoRecording();
            }
        }
    }

    private void forceWakeScreen() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock screenLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                "Guardian:EmergencyWake"
        );
        screenLock.acquire(10000L); // 10 seconds
    }

    private void startAutoRecording() {
        if (isRecording) return;
        try {
            File file = new File(getExternalFilesDir(null), "Guardian_Record_" + System.currentTimeMillis() + ".mp3");
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(file.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            vibrate(100);
            showToast("Recording Started...");
        } catch (Exception e) {
            Log.e("GuardianError", "Recording failed: " + e.getMessage());
        }
    }

    private void stopAutoRecording() {
        if (isRecording && mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                vibrate(50);
                showToast("Recording Stopped.");
            } catch (Exception e) {}
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isGuardianModeActive()) return;
        float z = event.values[2];
        long now = System.currentTimeMillis();
        float deltaZ = Math.abs(z - 9.8f);

        if (deltaZ > TAP_THRESHOLD) {
            if (now - lastTapTime < 200) return;
            if (now - lastTapTime > 800) tapCount = 0;
            tapCount++;
            lastTapTime = now;
            vibrate(60);
            if (tapCount == 2) {
                tapCount = 0;
                redirectCall();
            }
        }
    }

    private void redirectCall() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String number = prefs.getString("contact1", "");
        if (!number.isEmpty()) {
            // 1. Force Screen On First
            forceWakeScreen();

            // 2. Delay slightly so OS registers screen as 'active'
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:" + number));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                } catch (Exception e) {
                    showToast("Call Failed. Grant Permissions.");
                }
            }, 1000);
        }
    }

    // --- SMS, Floating Button, etc (Same as before) ---
    private void setupFloatingButton() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        View floatingBtn = new View(this);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.parseColor("#CC2196F3"));
        circle.setStroke(4, Color.WHITE);
        floatingBtn.setBackground(circle);

        int layoutType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                120, 120, layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.CENTER;
        try {
            windowManager.addView(floatingBtn, params);
            floatingBtn.setOnTouchListener(new View.OnTouchListener() {
                int initialX, initialY;
                float initialTouchX, initialTouchY;
                boolean isDragging = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x; initialY = params.y;
                            initialTouchX = event.getRawX(); initialTouchY = event.getRawY();
                            isDragging = false; return true;
                        case MotionEvent.ACTION_MOVE:
                            float dx = event.getRawX() - initialTouchX;
                            float dy = event.getRawY() - initialTouchY;
                            if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                                isDragging = true;
                                params.x = initialX + (int) dx;
                                params.y = initialY + (int) dy;
                                windowManager.updateViewLayout(floatingBtn, params);
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                            if (!isDragging) {
                                long now = System.currentTimeMillis();
                                if (now - lastLocTapTime > 5000) locTapCount = 1;
                                else locTapCount++;
                                lastLocTapTime = now;
                                vibrate(50);
                                if (locTapCount == 5) {
                                    locTapCount = 0;
                                    vibrate(500);
                                    fetchLiveLocationAndSendSMS();
                                }
                            }
                            return true;
                    }
                    return false;
                }
            });
            bottomTouchView = floatingBtn;
        } catch (Exception e) {}
    }

    private void fetchLiveLocationAndSendSMS() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String contact = prefs.getString("contact1", "");
        if (contact.isEmpty()) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            // --- Battery Status Logic Start ---
                            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                            Intent batteryStatus = registerReceiver(null, ifilter);
                            int level = -1;
                            if (batteryStatus != null) {
                                level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                            }
                            String batteryInfo = (level != -1) ? level + "%" : "Unknown";
                            // --- Battery Status Logic End ---

                            String mapsLink = "EMERGENCY! Location: http://maps.google.com/maps?q="
                                    + location.getLatitude() + "," + location.getLongitude()
                                    + " | Battery: " + batteryInfo;

                            try {
                                SmsManager smsManager = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ?
                                        getSystemService(SmsManager.class) : SmsManager.getDefault();
                                smsManager.sendTextMessage(contact, null, mapsLink, null, null);
                                showToast("SOS SMS Sent with Battery: " + batteryInfo);
                                vibrate(200);
                            } catch (Exception e) {
                                Log.e("GuardianError", "SMS Failed");
                            }
                        }
                    });
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, "Guardian Active", NotificationManager.IMPORTANCE_LOW);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    private void startForegroundService() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Guardian Protection ON")
                .setContentText("Listening for trigger and shakes...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        startForeground(1, notification);
    }

    private boolean isGuardianModeActive() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean("GuardianMode", false);
    }

    private void vibrate(int duration) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
            } else { v.vibrate(duration); }
        }
    }

    private void showToast(String msg) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show());
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        if (cpuWakeLock != null && cpuWakeLock.isHeld()) cpuWakeLock.release();
        if (windowManager != null && bottomTouchView != null) windowManager.removeView(bottomTouchView);
        if (speechRecognizer != null) speechRecognizer.destroy();
        stopAutoRecording();
        sensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}