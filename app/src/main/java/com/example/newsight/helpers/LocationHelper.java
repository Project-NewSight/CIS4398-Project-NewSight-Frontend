package com.example.newsight.helpers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * LocationHelper - Manages GPS location tracking using FusedLocationProviderClient
 * 
 * Usage:
 *   LocationHelper locationHelper = new LocationHelper(context);
 *   locationHelper.setLocationCallback(new LocationHelper.LocationUpdateCallback() {
 *       @Override
 *       public void onLocationUpdate(double lat, double lng, float accuracy) {
 *           // Handle location update
 *       }
 *   });
 *   locationHelper.startLocationUpdates();
 */
public class LocationHelper {

    private static final String TAG = "LocationHelper";
    private static final long UPDATE_INTERVAL_MS = 2000; // 2 seconds
    private static final long FASTEST_INTERVAL_MS = 1000; // 1 second
    
    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private final Handler mainHandler;
    private LocationCallback locationCallback;
    private LocationUpdateCallback callback;
    
    private Location lastKnownLocation;
    private boolean isTracking = false;

    public interface LocationUpdateCallback {
        void onLocationUpdate(double latitude, double longitude, float accuracy);
        void onLocationError(String error);
    }

    public LocationHelper(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setLocationCallback(LocationUpdateCallback callback) {
        this.callback = callback;
    }

    /**
     * Start receiving location updates
     */
    public void startLocationUpdates() {
        if (isTracking) {
            Log.w(TAG, "Already tracking location");
            return;
        }

        if (!checkLocationPermission()) {
            notifyError("Location permission not granted");
            return;
        }

        Log.d(TAG, "📍 Starting location updates...");

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                UPDATE_INTERVAL_MS
        )
                .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
                .setWaitForAccurateLocation(false)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    lastKnownLocation = location;
                    
                    Log.d(TAG, String.format("📍 Location: (%.6f, %.6f) ±%.1fm",
                            location.getLatitude(),
                            location.getLongitude(),
                            location.getAccuracy()));

                    if (callback != null) {
                        mainHandler.post(() -> callback.onLocationUpdate(
                                location.getLatitude(),
                                location.getLongitude(),
                                location.getAccuracy()
                        ));
                    }
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );
            isTracking = true;
            Log.d(TAG, "✅ Location tracking started");
        } catch (SecurityException e) {
            Log.e(TAG, "❌ Location permission error: " + e.getMessage());
            notifyError("Location permission denied");
        }
    }

    /**
     * Stop receiving location updates
     */
    public void stopLocationUpdates() {
        if (!isTracking) {
            return;
        }

        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
        
        isTracking = false;
        Log.d(TAG, "🛑 Location tracking stopped");
    }

    /**
     * Get the last known location (may be null)
     */
    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    /**
     * Request a single location update
     */
    public void requestSingleUpdate(LocationUpdateCallback singleCallback) {
        if (!checkLocationPermission()) {
            if (singleCallback != null) {
                singleCallback.onLocationError("Location permission not granted");
            }
            return;
        }

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            lastKnownLocation = location;
                            if (singleCallback != null) {
                                singleCallback.onLocationUpdate(
                                        location.getLatitude(),
                                        location.getLongitude(),
                                        location.getAccuracy()
                                );
                            }
                        } else {
                            Log.w(TAG, "Last location is null");
                            if (singleCallback != null) {
                                singleCallback.onLocationError("Location not available");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get location: " + e.getMessage());
                        if (singleCallback != null) {
                            singleCallback.onLocationError(e.getMessage());
                        }
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission error: " + e.getMessage());
            if (singleCallback != null) {
                singleCallback.onLocationError("Location permission denied");
            }
        }
    }

    public boolean isTracking() {
        return isTracking;
    }

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void notifyError(String error) {
        if (callback != null) {
            mainHandler.post(() -> callback.onLocationError(error));
        }
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        stopLocationUpdates();
    }
}

