package com.armongate.mobilepasssdk.delegate;

public interface MobilePassDelegate {
    void onPassCancelled();
    void onPassCompleted();
    void needCameraPermission();
    void needLocationPermission();
}
