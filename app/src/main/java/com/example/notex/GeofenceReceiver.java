package com.example.notex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

/**
 * GeofenceReceiver - Handles geofence transition events for location-based reminders
 */
public class GeofenceReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        
        if (geofencingEvent == null) {
            Log.e(TAG, "Geofencing event is null");
            return;
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: " + geofencingEvent.getErrorCode());
            return;
        }

        // Get the transition type
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Handle enter transition (user entered the geofence area)
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Get reminder details from intent
            String reminderId = intent.getStringExtra("reminder_id");
            String title = intent.getStringExtra("reminder_title");
            String description = intent.getStringExtra("reminder_description");
            String type = intent.getStringExtra("reminder_type");

            Log.d(TAG, "User entered geofence area for reminder: " + title);

            // Trigger the reminder notification
            Intent reminderIntent = new Intent(context, ReminderReceiver.class);
            reminderIntent.putExtra("reminder_id", reminderId);
            reminderIntent.putExtra("reminder_title", title);
            reminderIntent.putExtra("reminder_description", description);
            reminderIntent.putExtra("reminder_type", type);
            
            // Send broadcast to trigger notification
            context.sendBroadcast(reminderIntent);
        }
    }
}
