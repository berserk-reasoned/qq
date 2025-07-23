// IShizukuLocationService.aidl
package com.example.spoofer;

interface IShizukuLocationService {
    void setMockLocation(double latitude, double longitude);
    void stopMockLocation();
    boolean isLocationSpoofing();
}
