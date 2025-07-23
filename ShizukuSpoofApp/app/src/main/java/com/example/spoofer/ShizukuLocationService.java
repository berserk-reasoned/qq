package com.example.spoofer;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.RemoteException;

import java.lang.reflect.Method;

public class ShizukuLocationService extends IShizukuLocationService.Stub {

    private LocationManager locationManager;
    private Context context;

    public ShizukuLocationService() {
        try {
            // Get system context using reflection
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Method currentActivityThread = activityThread.getMethod("currentActivityThread");
            Object activityThreadInstance = currentActivityThread.invoke(null);
            Method getSystemContext = activityThread.getMethod("getSystemContext");
            context = (Context) getSystemContext.invoke(activityThreadInstance);
            
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setMockLocation(double latitude, double longitude) throws RemoteException {
        try {
            String provider = LocationManager.GPS_PROVIDER;
            
            // Remove existing test provider if it exists
            try {
                locationManager.removeTestProvider(provider);
            } catch (Exception ignored) {}

            // Add test provider with system permissions
            locationManager.addTestProvider(provider, false, false, false, false,
                    true, true, true, 0, 5);
            locationManager.setTestProviderEnabled(provider, true);

            // Create and set mock location
            Location mockLocation = new Location(provider);
            mockLocation.setLatitude(latitude);
            mockLocation.setLongitude(longitude);
            mockLocation.setAccuracy(1.0f);
            mockLocation.setTime(System.currentTimeMillis());
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                mockLocation.setElapsedRealtimeNanos(android.os.SystemClock.elapsedRealtimeNanos());
            }

            locationManager.setTestProviderLocation(provider, mockLocation);
            
        } catch (Exception e) {
            throw new RemoteException("Failed to set mock location: " + e.getMessage());
        }
    }

    @Override
    public void stopMockLocation() throws RemoteException {
        try {
            if (locationManager != null) {
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            }
        } catch (Exception e) {
            throw new RemoteException("Failed to stop mock location: " + e.getMessage());
        }
    }

    @Override
    public boolean isLocationSpoofing() throws RemoteException {
        try {
            return locationManager.getProvider(LocationManager.GPS_PROVIDER) != null &&
                   locationManager.getProvider(LocationManager.GPS_PROVIDER).getName().equals(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            return false;
        }
    }
}
