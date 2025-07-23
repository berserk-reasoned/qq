package com.example.spoofer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity implements Shizuku.OnRequestPermissionResultListener {

    private static final int SHIZUKU_PERMISSION_REQUEST_CODE = 1000;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    
    private LocationManager locationManager;
    private EditText latInput, lonInput;
    private Button startSpoofBtn, stopSpoofBtn;
    private TextView statusText;
    private boolean isSpoofing = false;
    
    private IShizukuLocationService shizukuService;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            shizukuService = IShizukuLocationService.Stub.asInterface(binder);
            updateStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            shizukuService = null;
            updateStatus();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize HiddenApiBypass for Android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            HiddenApiBypass.addHiddenApiExemptions("");
        }

        initViews();
        setupListeners();
        checkEnvironment();
        
        // Add Shizuku permission listener
        Shizuku.addRequestPermissionResultListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeRequestPermissionResultListener(this);
        if (shizukuService != null) {
            Shizuku.unbindUserService(serviceConnection, true);
        }
    }

    private void initViews() {
        latInput = findViewById(R.id.editLatitude);
        lonInput = findViewById(R.id.editLongitude);
        startSpoofBtn = findViewById(R.id.buttonSpoof);
        stopSpoofBtn = findViewById(R.id.buttonStopSpoof);
        statusText = findViewById(R.id.statusText);
        
        // Set default coordinates (New York City as example)
        latInput.setText("40.7128");
        lonInput.setText("-74.0060");
    }

    private void setupListeners() {
        startSpoofBtn.setOnClickListener(v -> startLocationSpoof());
        stopSpoofBtn.setOnClickListener(v -> stopLocationSpoof());
    }

    private void startLocationSpoof() {
        if (!checkAllPermissions()) {
            return;
        }

        if (isSpoofing) {
            Toast.makeText(this, "Location spoofing is already active", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String latStr = latInput.getText().toString().trim();
            String lonStr = lonInput.getText().toString().trim();
            
            if (latStr.isEmpty() || lonStr.isEmpty()) {
                Toast.makeText(this, "Please enter valid coordinates", Toast.LENGTH_SHORT).show();
                return;
            }

            double latitude = Double.parseDouble(latStr);
            double longitude = Double.parseDouble(lonStr);
            
            // Validate coordinates
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                Toast.makeText(this, "Invalid coordinates range", Toast.LENGTH_SHORT).show();
                return;
            }

            if (shizukuService != null) {
                // Use Shizuku service for system-level spoofing
                shizukuService.setMockLocation(latitude, longitude);
                isSpoofing = true;
                Toast.makeText(this, "Location spoofing started via Shizuku", Toast.LENGTH_SHORT).show();
            } else {
                // Fallback to regular mock location
                startRegularMockLocation(latitude, longitude);
            }
            
            updateStatus();
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid coordinate format", Toast.LENGTH_SHORT).show();
        } catch (RemoteException e) {
            Toast.makeText(this, "Shizuku service error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startRegularMockLocation(double latitude, double longitude) {
        try {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            String provider = LocationManager.GPS_PROVIDER;

            // Remove existing test provider if it exists
            try {
                locationManager.removeTestProvider(provider);
            } catch (Exception ignored) {}

            // Add test provider
            locationManager.addTestProvider(provider, false, false, false, false,
                    true, true, true, 0, 5);
            locationManager.setTestProviderEnabled(provider, true);

            // Create mock location
            Location mockLocation = new Location(provider);
            mockLocation.setLatitude(latitude);
            mockLocation.setLongitude(longitude);
            mockLocation.setAccuracy(1.0f);
            mockLocation.setTime(System.currentTimeMillis());
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                mockLocation.setElapsedRealtimeNanos(android.os.SystemClock.elapsedRealtimeNanos());
            }

            locationManager.setTestProviderLocation(provider, mockLocation);
            isSpoofing = true;
            Toast.makeText(this, "Mock location set successfully", Toast.LENGTH_SHORT).show();
            
        } catch (SecurityException e) {
            Toast.makeText(this, "Mock location permission denied. Enable in Developer Options.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Mock location failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationSpoof() {
        try {
            if (shizukuService != null) {
                shizukuService.stopMockLocation();
            }
            
            if (locationManager != null) {
                try {
                    locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
                } catch (Exception ignored) {}
            }
            
            isSpoofing = false;
            Toast.makeText(this, "Location spoofing stopped", Toast.LENGTH_SHORT).show();
            updateStatus();
            
        } catch (RemoteException e) {
            Toast.makeText(this, "Error stopping Shizuku service: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error stopping spoof: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkAllPermissions() {
        // Check Shizuku permission
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Shizuku is not running", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                Toast.makeText(this, "Shizuku permission required for system-level location spoofing", Toast.LENGTH_LONG).show();
            }
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE);
            return false;
        }

        // Check location permissions
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 
                LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }

        // Bind Shizuku service if not already bound
        if (shizukuService == null) {
            bindShizukuService();
        }

        return true;
    }

    private void bindShizukuService() {
        try {
            Shizuku.bindUserService(new Shizuku.UserServiceArgs(new ComponentName(this, ShizukuLocationService.class))
                    .processNameSuffix("shizuku"), serviceConnection);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to bind Shizuku service: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkEnvironment() {
        updateStatus();
    }

    private void updateStatus() {
        StringBuilder status = new StringBuilder();

        // Check Shizuku
        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                status.append("✅ Shizuku Permission Granted\n");
            } else {
                status.append("⚠️ Shizuku Permission Needed\n");
            }
        } else {
            status.append("❌ Shizuku Not Running\n");
        }

        // Check location permissions
        if (hasLocationPermission()) {
            status.append("✅ Location Permission Granted\n");
        } else {
            status.append("❌ Location Permission Missing\n");
        }

        // Check service connection
        if (shizukuService != null) {
            status.append("✅ Shizuku Service Connected\n");
        } else {
            status.append("⚠️ Shizuku Service Disconnected\n");
        }

        // Check spoofing status
        if (isSpoofing) {
            status.append("🔄 Location Spoofing Active\n");
        } else {
            status.append("⏸️ Location Spoofing Inactive\n");
        }

        status.append("\nAndroid API: ").append(android.os.Build.VERSION.SDK_INT);

        statusText.setText(status.toString());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            updateStatus();
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, int grantResult) {
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            updateStatus();
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                bindShizukuService();
            }
        }
    }
}
