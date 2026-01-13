package com.example.notex.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * LocationPermissionHelper - Helper class to request location permissions
 */
public class LocationPermissionHelper {

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    public static final int BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1002;

    /**
     * Check if location permissions are granted
     */
    public static boolean hasLocationPermissions(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if background location permission is granted (Android 10+)
     */
    public static boolean hasBackgroundLocationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Not required for older versions
    }

    /**
     * Request location permissions
     */
    public static void requestLocationPermissions(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    /**
     * Request background location permission (Android 10+)
     */
    public static void requestBackgroundLocationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }
}
