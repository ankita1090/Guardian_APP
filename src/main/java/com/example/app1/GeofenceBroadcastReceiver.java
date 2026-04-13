package com.example.app1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()) return;

        int transitionType = geofencingEvent.getGeofenceTransition();

        // If user ENTERS or EXITS the designated area
        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER ||
                transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {

            sendGuardianPrompt(context);
        }
    }

    private void sendGuardianPrompt(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "guardian_channel")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Security Alert")
                .setContentText("You've entered a new area. Tap to activate Guardian Mode SOS.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(2, builder.build());
    }
}