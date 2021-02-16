package com.armongate.mobilepasssdk.delegate;

import com.armongate.mobilepasssdk.model.DeviceCapability;
import com.armongate.mobilepasssdk.model.DeviceConnectionStatus;
import com.armongate.mobilepasssdk.model.DeviceInRange;
import com.armongate.mobilepasssdk.model.DeviceSignalInfo;

public interface BluetoothManagerDelegate {
    void onDeviceAdded(DeviceInRange device);
    void onDeviceRemoved(String deviceId);
    void onConnectionStateChanged(DeviceConnectionStatus state);
    void onBLEStateChanged(DeviceCapability state);
    void onRSSIValueChanged(DeviceSignalInfo info);
}
