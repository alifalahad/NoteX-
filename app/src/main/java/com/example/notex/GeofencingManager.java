package com.example.notex;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.notex.models.Reminder;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

/**
 * GeofencingManager - Handles location-based reminders using Google Play Services Geofencing
 */
public class GeofencingManager {

    private static final String TAG = "GeofencingManager";
    private GeofencingClient geofencingClient;
    private Context context;

    public GeofencingManager(Context context) {
        this.context = context;
        this.geofencingClient = LocationServices.getGeofencingClient(context);
    }

    /**
     * Add a geofence for a location-based reminder
     */
    public void addGeofence(Reminder reminder) {
        // Check if location permissions are granted
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            return;
        }

        // Create geofence
        Geofence geofence = new Geofence.Builder()
                .setRequestId(reminder.getId())
                .setCircularRegion(
                        reminder.getLatitude(),
                        reminder.getLongitude(),
                        reminder.getRadiusMeters()
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();

        // Create geofencing request
        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        // Create pending intent for geofence transitions
        PendingIntent geofencePendingIntent = getGeofencePendingIntent(reminder);

        // Add geofence
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Geofence added successfully for: " + reminder.getTitle());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add geofence: " + e.getMessage());
                });
    }

    /**
     * Remove a geofence for a reminder
     */
    public void removeGeofence(String reminderId) {
        geofencingClient.removeGeofences(java.util.Collections.singletonList(reminderId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Geofence removed successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove geofence: " + e.getMessage());
                });
    }

    /**
     * Get PendingIntent for geofence transitions
     */
    private PendingIntent getGeofencePendingIntent(Reminder reminder) {
        Intent intent = new Intent(context, GeofenceReceiver.class);
        intent.putExtra("reminder_id", reminder.getId());
        intent.putExtra("reminder_title", reminder.getTitle());
        intent.putExtra("reminder_description", reminder.getDescription());
        intent.putExtra("reminder_type", reminder.getType());

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }

        return PendingIntent.getBroadcast(
                context,
                reminder.getId().hashCode(),
                intent,
                flags
        );
    }
}
