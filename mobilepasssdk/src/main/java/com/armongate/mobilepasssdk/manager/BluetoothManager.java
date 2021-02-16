package com.armongate.mobilepasssdk.manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Base64;

import com.armongate.mobilepasssdk.constant.DataTypes;
import com.armongate.mobilepasssdk.constant.PacketHeaders;
import com.armongate.mobilepasssdk.constant.UUIDs;
import com.armongate.mobilepasssdk.delegate.BluetoothManagerDelegate;
import com.armongate.mobilepasssdk.model.BLEDataContent;
import com.armongate.mobilepasssdk.model.BLEScanConfiguration;
import com.armongate.mobilepasssdk.model.DeviceCapability;
import com.armongate.mobilepasssdk.model.DeviceConnection;
import com.armongate.mobilepasssdk.model.DeviceConnectionStatus;
import com.armongate.mobilepasssdk.model.DeviceInRange;
import com.armongate.mobilepasssdk.model.DeviceSignalInfo;
import com.armongate.mobilepasssdk.model.DeviceWriteItem;
import com.armongate.mobilepasssdk.model.StorageDataDevice;
import com.armongate.mobilepasssdk.model.StorageDataUserDetails;
import com.armongate.mobilepasssdk.util.ArrayUtil;
import com.armongate.mobilepasssdk.util.ConverterUtil;
import com.armongate.mobilepasssdk.util.DataParserUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

public class BluetoothManager {

    // Singleton

    private static BluetoothManager instance = null;
    private BluetoothManager() {
        List<String> connectionIssuedDevices = Arrays.asList("HUAWEI", "XIAOMI", "XİAOMI");

        String manufacturer = Build.MANUFACTURER;
        // TODO LogManager.getInstance().debug("Device manufacturer: " + manufacturer.toUpperCase());
    }

    public static BluetoothManager getInstance() {
        if (instance == null) {
            instance = new BluetoothManager();
        }

        return instance;
    }

    // Fields

    public BluetoothManagerDelegate delegate;

    private Context activeContext               = null;
    private DeviceCapability bluetoothState              = null;

    private List<UUID>                      filterServiceUUIDs          = new ArrayList<>();
    private List<UUID>                      filterCharacteristicUUIDs   = new ArrayList<>();

    private Map<String, BluetoothDevice>    currentDevicesInRange       = new HashMap<>();
    private Map<String, DeviceConnection>   currentConnectedDevices     = new HashMap<>();
    private BLEScanConfiguration currentConfiguration        = null;
    private BluetoothAdapter currentBluetoothAdapter     = null;
    private BluetoothLeScanner currentBluetoothScanner     = null;

    private boolean isConnectionActive = false;
    private boolean isConnectedBefore  = false;
    private Map<String, Long>               lastConnectionTime          = new HashMap<>();

    private boolean isScanningActive            = false;
    private boolean isReceiverRegistered        = false;
    
    private final Queue<DeviceWriteItem> mWriteQueue     = new LinkedList<>();
    private boolean                         mWriteActive    = false;

    // Public Functions

    public void setContext(Context context) {
        this.activeContext = context;

        if (this.currentBluetoothAdapter == null) {
            readyBluetoothAdapter();
        }
    }

    public DeviceCapability getCurrentState() {
        if (this.bluetoothState == null) {
            this.bluetoothState = new DeviceCapability(false, false, false);
        }

        return this.bluetoothState;
    }

    public void registerStateReceiver() {
        if (this.currentBluetoothScanner != null && this.activeContext != null && !this.isReceiverRegistered) {
            this.registerReceiver();
        }
    }

    public void unregisterStateReceiver() {
        if (this.currentBluetoothScanner != null && this.activeContext != null && this.isReceiverRegistered) {
            this.activeContext.unregisterReceiver(mBLEStateReceiver);

            // TODO LogManager.getInstance().info("Bluetooth state listener is unregistered");
            this.isReceiverRegistered = false;
        }
    }

    public void startScan(BLEScanConfiguration configuration) {
        // TODO LogManager.getInstance().info("Bluetooth scanner is starting...");

        // Check current Bluetooth Adapter instance
        if (currentBluetoothAdapter == null) {
            readyBluetoothAdapter();
        }

        // Check current Bluetooth Scanner instance
        if (currentBluetoothScanner == null) {
            currentBluetoothScanner = currentBluetoothAdapter.getBluetoothLeScanner();
            // TODO LogManager.getInstance().info("Bluetooth scanner is initialized");
        }

        // TODO LogManager.getInstance().warn("Flush pending scan results for Bluetooth scanner");
        currentBluetoothScanner.flushPendingScanResults(mPeripheralScanCallback);

        // Ready for new scanning
        clearFieldsForNewScan(configuration);

        // Prepare service and characteristic UUID lists for new scan
        List<ScanFilter>    scanFilters     = prepareUUIDLists();
        ScanSettings scanSettings    = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        // TODO LogManager.getInstance().info("Bluetooth scanner is ready for " + configuration.typeDescription());

        // Check active scanning status
        if (isScanningActive) {
            // TODO LogManager.getInstance().warn("Bluetooth scanner is already scanning, new starter is ignored!");
            return;
        }

        isScanningActive = true;

        // TODO LogManager.getInstance().info("Starting new scan process...");
        currentBluetoothScanner.startScan(scanFilters, scanSettings, mPeripheralScanCallback);
    }

    public void stopScan(boolean disconnect) {
        // TODO LogManager.getInstance().info("Bluetooth scanner is stopping...");

        if(currentBluetoothAdapter == null || currentBluetoothScanner == null) {
            // TODO LogManager.getInstance().warn("Stop Bluetooth scanner request is ignored, scanner is not initialized yet");
            return;
        }

        isScanningActive = false;

        try {
            currentBluetoothScanner.stopScan(mPeripheralScanCallback);
            currentBluetoothScanner.flushPendingScanResults(mPeripheralScanCallback);
        } catch (Exception ex) {
            // TODO LogManager.getInstance().error("Stop scan failed, error: " + ex.getLocalizedMessage());
        }

        // TODO LogManager.getInstance().info("Bluetooth scanner is not active now");

        if (disconnect) {
            disconnect();
        }
    }

    public void connectToDevice(String deviceIdentifier) {
        if (!currentDevicesInRange.containsKey(deviceIdentifier)) {
            // TODO LogManager.getInstance().warn("Selected device not found in list to connect, device identifier: " + deviceIdentifier);
            onConnectionStateChanged(deviceIdentifier, DeviceConnectionStatus.ConnectionState.NOT_FOUND);
            return;
        }

        if(isConnectionActive ||isConnectedBefore) {
            // TODO LogManager.getInstance().info("Another connection is active now, ignore new")
            // TODO onConnectionStateChanged!
        }

        isConnectionActive = true;
        isConnectedBefore = true;

        // TODO LogManager.getInstance().info("Connect to device requested for identifier: " + deviceIdentifier);
        onConnectionStateChanged(deviceIdentifier, DeviceConnectionStatus.ConnectionState.CONNECTING);

        // TODO LogManager.getInstance().info("Connecting to device...");
        BluetoothDevice device = currentDevicesInRange.get(deviceIdentifier);

        if (device != null) {
            BluetoothGatt connection = device.connectGatt(activeContext, false, mBluetoothGattCallback);
            currentConnectedDevices.put(deviceIdentifier, new DeviceConnection(device, connection));
        } else {
            // TODO LogManager.getInstance().warn("Connection cancelled due to empty device instance that received from range list");
        }
    }

    public void disconnectFromDevice() {
        disconnect();
    }

    // Private Functions

    private void readyBluetoothAdapter() {
        // TODO LogManager.getInstance().info("Initialize Bluetooth adapter");
        android.bluetooth.BluetoothManager manager = (android.bluetooth.BluetoothManager) this.activeContext.getSystemService(Context.BLUETOOTH_SERVICE);

        if (manager != null) {
            this.currentBluetoothAdapter = manager.getAdapter();
            // TODO LogManager.getInstance().info("Bluetooth adapter is ready for scanner");

            this.registerReceiver();
        } else {
            // TODO LogManager.getInstance().error("Bluetooth adapter can not be accessed!");
        }
    }

    private void registerReceiver() {
        if (this.currentBluetoothAdapter != null && this.activeContext != null) {
            this.updateBLECapability(this.currentBluetoothAdapter.getState());

            IntentFilter bleStateFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            this.activeContext.registerReceiver(mBLEStateReceiver, bleStateFilter);

            // TODO LogManager.getInstance().info("Bluetooth state listener is registered");
            this.isReceiverRegistered = true;
        }
    }

    private void updateBLECapability(int state) {
        // TODO LogManager.getInstance().info("Bluetooth state is changed, new state: " + (state == BluetoothAdapter.STATE_ON ? "Enabled" : "Disabled"));
        this.bluetoothState = new DeviceCapability(this.currentBluetoothAdapter != null, state == BluetoothAdapter.STATE_ON, false);

        if (this.delegate != null) {
            this.delegate.onBLEStateChanged(this.bluetoothState);
        }
    }

    private void clearFieldsForNewScan(BLEScanConfiguration configuration) {
        // Set fields for new scanning
        currentConfiguration    = configuration;

        // Clear devices that found before
        currentDevicesInRange.clear();

        // Clear devices that stay in connected devices list
        currentConnectedDevices.clear();

        // Clear connection limit flags
        isConnectedBefore = false;
        isConnectionActive = false;
    }

    private List<ScanFilter> prepareUUIDLists() {
        if (currentConfiguration == null) {
            return new ArrayList<>();
        }

        filterServiceUUIDs.clear();
        filterCharacteristicUUIDs.clear();

        for (String uuid :
                currentConfiguration.uuidFilter) {
            // TODO LogManager.getInstance().info("Filter services with uuid: " + uuid);
            filterServiceUUIDs.add(UUID.fromString(uuid));
            filterCharacteristicUUIDs.add(UUID.fromString(UUIDs.CHARACTERISTIC));
        }


        List<ScanFilter> result = new ArrayList<>();

        /*
        for (UUID uuid : filterServiceUUIDs) {
            final ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(uuid)).build();
            result.add(scanFilter);
        }
         */

        return result;
    }

    private void disconnect() {
        boolean isDisconnectCalled = false;

        if (currentConnectedDevices == null || currentConnectedDevices.size() == 0) {
            // TODO LogManager.getInstance().info("There is no connected device now, ignore disconnect process!");
        } else {
            for (DeviceConnection connectedDevice :
                    currentConnectedDevices.values()) {
                if (connectedDevice.connection == null) {
                    // TODO LogManager.getInstance().info("Peripheral exists but not connected, ignore disconnect");
                    break;
                }

                // TODO LogManager.getInstance().info("Disconnecting from peripheral");
                isDisconnectCalled = true;
                connectedDevice.connection.disconnect();
            }
        }

        if (!isDisconnectCalled) {
            onDisconnectedCompleted();
        }
    }

    private void onDisconnectedCompleted() {
        isConnectionActive = false;
    }

    private void onConnectionStateChanged(String identifier, DeviceConnectionStatus.ConnectionState connectionState) {
        onConnectionStateChanged(identifier, connectionState, null);
    }

    private void onConnectionStateChanged(String identifier, DeviceConnectionStatus.ConnectionState connectionState, Integer failReason) {
        if (delegate != null) {
            if (identifier != null) {
                // TODO LogManager.getInstance().info("Connection state changed for " + identifier + " > " + connectionState.toString());
                delegate.onConnectionStateChanged(new DeviceConnectionStatus(identifier, connectionState, failReason));
            } else {
                // TODO LogManager.getInstance().warn("Connection state changed event could not be sent, identifier is empty!");
            }
        }

        if (connectionState == DeviceConnectionStatus.ConnectionState.FAILED) {
            disconnect();
        } else if (connectionState == DeviceConnectionStatus.ConnectionState.DISCONNECTED) {
            if (identifier != null) {
                lastConnectionTime.put(identifier, System.currentTimeMillis());

                if (currentConnectedDevices.containsKey(identifier)) {
                    // TODO LogManager.getInstance().info("Device (" + identifier + ") is removed from connected devices' list!");
                    DeviceConnection connectedDevice = currentConnectedDevices.get(identifier);

                    // Close connection before remove
                    if (connectedDevice != null && connectedDevice.connection != null) {
                        connectedDevice.connection.close();
                        connectedDevice.connection = null;
                    }

                    currentConnectedDevices.remove(identifier);
                }

                if (currentDevicesInRange.containsKey(identifier)) {
                    // TODO LogManager.getInstance().info("Device (" + identifier + ") is removed from list of devices in range");
                    currentDevicesInRange.remove(identifier);
                }
            } else {
                // TODO LogManager.getInstance().warn("Identifier is empty, so related operations cannot be done for disconnect!");
            }

            onDisconnectedCompleted();
        }
    }

    private void changeSubscription(boolean enable, String identifier, BluetoothGattCharacteristic forCharacteristic) {
        // TODO LogManager.getInstance().info("Change subscription state for " + forCharacteristic.getUuid().toString() + " with value: " + enable);
        // TODO LogManager.getInstance().info("Descriptor count for characteristic: " + forCharacteristic.getDescriptors().size());

        BluetoothGattDescriptor descriptor = null;

        for (BluetoothGattDescriptor descriptorIterator: forCharacteristic.getDescriptors()) {
            descriptor = descriptorIterator;
        }

        if (descriptor != null) {
            // Check notify property
            if ((forCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            } else {
                // TODO LogManager.getInstance().warn("Characteristic does not have NOTIFY property!");
            }

            // TODO LogManager.getInstance().info("Adding write descriptor request for " + forCharacteristic.getUuid().toString());
            DeviceConnection connectedDevice = currentConnectedDevices.get(identifier);

            if (connectedDevice != null && connectedDevice.connection != null) {
                writeToDevice(enable, forCharacteristic, descriptor, connectedDevice.connection );
            } else {
                onWriteForDescriptorFailed("Write descriptor is ignored, current connection is null!", null);
            }
        } else {
            onWriteForDescriptorFailed("Change subscription failed, descriptor not found for characteristic", null);
        }
    }

    private void writeToDevice(byte[] data, BluetoothGattCharacteristic characteristic, BluetoothGatt connection) {
        this.writeToDevice(new DeviceWriteItem(data, characteristic, connection));
    }

    private void writeToDevice(boolean notifyEnable, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor, BluetoothGatt connection) {
        this.writeToDevice(new DeviceWriteItem(notifyEnable, characteristic, descriptor, connection));
    }

    private void writeToDevice(DeviceWriteItem item) {
        if (item.connection != null) {
            // TODO LogManager.getInstance().info("Write to device > " + (item.useDescriptor ? item.notifyEnable : ConverterUtil.bytesToHexString(item.message)));

            this.mWriteQueue.add(item);
            checkWriteQueue();
        }
    }

    private void onWriteCompleted() {
        this.mWriteActive = false;
        checkWriteQueue();
    }

    private void checkWriteQueue() {
        if (this.mWriteQueue.size() > 0 && !this.mWriteActive) {
            // Get next item from queue to send
            DeviceWriteItem currentItem = this.mWriteQueue.remove();

            // Set active flag true to prevent sending another data packet before complete current one
            this.mWriteActive = true;

            if (currentItem.useDescriptor) {
                // TODO LogManager.getInstance().info("Writing data to device for descriptor");

                if (currentItem.connection.setCharacteristicNotification(currentItem.characteristic, currentItem.notifyEnable)) {
                    try {
                        if (currentItem.connection.writeDescriptor(currentItem.descriptor)) {
                            // TODO LogManager.getInstance().info("Set notify " + currentItem.notifyEnable + " is completed | Characteristic UUID: " + currentItem.characteristic.getUuid().toString());
                        } else {
                            onWriteForDescriptorFailed("Failed to set notification subscription! - WriteDescriptor return false", currentItem.connection);
                        }
                    } catch (Exception ex) {
                        onWriteForDescriptorFailed("Failed to set notification subscription with error: " + ex.getLocalizedMessage(), currentItem.connection);
                    }

                } else {
                    onWriteForDescriptorFailed("Failed to set notification subscription! - SetCharacteristicNotification return false", currentItem.connection);
                }
            } else {
                // TODO LogManager.getInstance().info("Writing data to device for characteristic");

                currentItem.characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                currentItem.characteristic.setValue(currentItem.message);

                currentItem.connection.writeCharacteristic(currentItem.characteristic);
            }
        }
    }

    private void onWriteForDescriptorFailed(String logMessage, BluetoothGatt connection) {
        // TODO LogManager.getInstance().error(logMessage);

        if (connection!= null && connection.getDevice() != null) {
            onConnectionStateChanged(connection.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
        } else {
            onConnectionStateChanged(null, DeviceConnectionStatus.ConnectionState.FAILED);
        }

        onWriteCompleted();
    }

    private void processChallengeResult(String deviceIdentifier, BLEDataContent result) {
        if (result.result == DataTypes.RESULT.Succeed) {
            DeviceConnection device = currentConnectedDevices.get(deviceIdentifier);

            onConnectionStateChanged(deviceIdentifier, DeviceConnectionStatus.ConnectionState.CONNECTED);

            // TODO Vibrate > NotificationManager.getInstance().vibrate();

            // TODO LogManager.getInstance().info("Disconnect from device after successful process of passing!");
            disconnect();
        } else {
            onConnectionStateChanged(deviceIdentifier, DeviceConnectionStatus.ConnectionState.FAILED, result.data.containsKey("reason") ? (int)result.data.get("reason") : null);
        }
    }

    private byte[] generateChallengeResponseDataWithDirection(int challengeType, String deviceId, byte[] challenge, byte[] iv) throws Exception {
        StorageDataDevice       foundDevice = StorageManager.getInstance().findDevice(activeContext, deviceId);
        StorageDataUserDetails  foundUser   = StorageManager.getInstance().findUser(activeContext, foundDevice.userId);

        byte[] resultData = new byte[] {
                PacketHeaders.PROTOCOLV2.GROUP.AUTH,
                PacketHeaders.PROTOCOLV2.AUTH.DIRECTION_CHALLENGE,
                PacketHeaders.PLATFORM_ANDROID
        };

        resultData = ArrayUtil.concat(resultData, ConverterUtil.hexStringToBytes(foundUser.userId.replace("-", "")));
        resultData = ArrayUtil.concat(resultData, ConverterUtil.hexStringToBytes(currentConfiguration.dataAccessPointId.replace("-", "")));
        resultData = ArrayUtil.add(resultData, (byte)currentConfiguration.dataDirection);

        byte[] encryptedResponse = CryptoManager.getInstance().encryptBytesWithIV(foundUser.privateKey, foundDevice.devicePublicKey, challenge, iv);
        resultData = ArrayUtil.concat(resultData, encryptedResponse);

        return resultData;
    }

    private void changeCommunicationState(String identifier, boolean start, int rssiValue) {
        if (currentConnectedDevices.containsKey(identifier)) {
            DeviceConnection connectedDevice = currentConnectedDevices.get(identifier);

            if (connectedDevice != null) {
                String communicationState = start ? "Starting" : "Ending";
                String subscriptionState = start ? "Subscribe" : "Unsubscribe";

                // TODO LogManager.getInstance().info(communicationState + " communication with " + identifier + " at RSSI: " + rssiValue);

                for (BluetoothGattCharacteristic characteristic :
                        connectedDevice.characteristics.values()) {
                    // TODO LogManager.getInstance().info(subscriptionState + " for characteristic: " + characteristic.getUuid().toString());
                    changeSubscription(start, identifier, characteristic);
                }
            } else {
                // TODO LogManager.getInstance().warn("Device identifier could not be found in connected devices list");
                onConnectionStateChanged(identifier, DeviceConnectionStatus.ConnectionState.FAILED);
            }
        }
    }


    // Broadcast Receivers

    private final BroadcastReceiver mBLEStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                updateBLECapability(state);
            }
        }
    };

    // Callbacks

    private ScanCallback mPeripheralScanCallback = new ScanCallback() {
        @Override
        public void onScanFailed(int errorCode) {
            // TODO LogManager.getInstance().warn("Scan failed! Error Code: " + errorCode);
            super.onScanFailed(errorCode);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (isScanningActive) {
                if (!isConnectionActive) {
                    if (filterServiceUUIDs == null || filterServiceUUIDs.size() == 0) {
                        // TODO LogManager.getInstance().info("There is no service UUID filter, ignore scan result!");
                        super.onScanResult(callbackType, result);
                        return;
                    }

                    ScanRecord scanRecord = result.getScanRecord();

                    if (scanRecord == null) {
                        super.onScanResult(callbackType, result);
                        return;
                    }

                    String      armonDeviceName     = scanRecord.getDeviceName();
                    boolean     hasValidServiceUUID = false;

                    List<ParcelUuid> recordServiceUUIDs = scanRecord.getServiceUuids();

                    if (recordServiceUUIDs != null && recordServiceUUIDs.size() > 0) {
                        for (ParcelUuid uuid : recordServiceUUIDs) {
                            if (uuid.getUuid() != null && filterServiceUUIDs.contains(uuid.getUuid())) {
                                hasValidServiceUUID = true;
                            }
                        }
                    } else {
                        super.onScanResult(callbackType, result);
                        return;
                    }

                    if (!hasValidServiceUUID) {
                        // TODO LogManager.getInstance().debug("Peripheral found with unknown service UUID, ignore scan result! | " + armonDeviceName);
                        super.onScanResult(callbackType, result);
                        return;
                    }

                    // TODO LogManager.getInstance().debug("Discovered peripheral (" + armonDeviceName + ") at " + result.getRssi());

                    if (currentConfiguration == null) {
                        // TODO LogManager.getInstance().debug("Connection configuration is not defined yet!");
                    } else {
                        if (delegate != null) {
                            delegate.onRSSIValueChanged(new DeviceSignalInfo(armonDeviceName, result.getRssi()));
                        }

                        if (result.getRssi() < 0 && result.getRssi() > -95) {
                            // Device in range for specified RSSI limit

                            // Check device added to list before
                            if (!currentDevicesInRange.containsKey(result.getDevice().getAddress())) {
                                if (lastConnectionTime.containsKey(result.getDevice().getAddress())) {
                                    Long lastConnection = lastConnectionTime.get(result.getDevice().getAddress());
                                    long currentTime = System.currentTimeMillis();

                                    if (lastConnection != null) {
                                        double elapsedTime = (currentTime - lastConnection) / 1000.0;

                                        if (elapsedTime < 5) {
                                            // TODO LogManager.getInstance().debug("Device is ignored because of reconnection timeout, elapsed time: " + elapsedTime);
                                            super.onScanResult(callbackType, result);
                                            return;
                                        }
                                    }
                                }

                                currentDevicesInRange.put(result.getDevice().getAddress(), result.getDevice());
                                // TODO LogManager.getInstance().info("Peripheral is added to device list, current device count: " + currentDevicesInRange.size());

                                if (delegate != null) {
                                    delegate.onDeviceAdded(new DeviceInRange(result.getDevice().getAddress(), result.getDevice().getName()));
                                }
                            }

                        } else if (result.getRssi() != 127) {
                            if (currentDevicesInRange.containsKey(result.getDevice().getAddress())) {
                                currentDevicesInRange.remove(result.getDevice().getAddress());

                                // TODO LogManager.getInstance().info("Peripheral is removed from device list, current device count: " + currentDevicesInRange.size());

                                if (delegate != null) {
                                    delegate.onDeviceRemoved(result.getDevice().getAddress());
                                }
                            }
                        }
                    }
                }
            } else {
                // TODO LogManager.getInstance().debug("Scanning active flag is false now, scan result ignored");
            }

            super.onScanResult(callbackType, result);
        }
    };

    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String state = "";

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    state = "Connected";
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    state = "Connecting";
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    state = "Disconnecting";
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    state = "Disconnected";
                    break;
                default:
                    state = "Unknown";
            }

            // TODO LogManager.getInstance().info("Connection state changed, new state: " + state + ", status: " + (status == BluetoothGatt.GATT_SUCCESS ? "Succeed" : "Failed"));

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // TODO LogManager.getInstance().info("Connected to " + gatt.getDevice().getName());

                // Change MTU size for large packets
                gatt.requestMtu(185);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // TODO LogManager.getInstance().info("Disconnected from " + gatt.getDevice().getName());

                mWriteActive = false;
                mWriteQueue.clear();

                onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.DISCONNECTED);
            }

            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);

            if(status == BluetoothGatt.GATT_SUCCESS) {
                // TODO LogManager.getInstance().info("MTU size changed successfully: " + mtu);
                // TODO LogManager.getInstance().info("Starting discover services...");

                gatt.discoverServices();
            }
            else {
                // TODO LogManager.getInstance().error("Change MTU size failed!");
                onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            // TODO LogManager.getInstance().info("Service discovered with status: " + status);

            if (gatt.getServices() == null || gatt.getServices().isEmpty()) {
                // TODO LogManager.getInstance().info("List of peripheral services is empty for " + gatt.getDevice().getAddress());
                onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
                return;
            }

            for (BluetoothGattService service: gatt.getServices()) {
                if (filterServiceUUIDs.contains(service.getUuid())) {
                    // TODO LogManager.getInstance().debug("Service found for peripheral (" + gatt.getDevice().getAddress() + "), UUID: " + service.getUuid().toString());

                    if (currentConnectedDevices.containsKey(gatt.getDevice().getAddress())) {
                        DeviceConnection device = currentConnectedDevices.get(gatt.getDevice().getAddress());

                        if (device != null) {
                            for (BluetoothGattCharacteristic characteristicIterator : service.getCharacteristics()) {
                                if (filterCharacteristicUUIDs.contains(characteristicIterator.getUuid())) {
                                    // TODO LogManager.getInstance().debug("Characteristic found for peripheral (" + gatt.getDevice().getAddress() + "), UUID: " + characteristicIterator.getUuid().toString());

                                    // TODO LogManager.getInstance().info("Store characteristic with UUID: " + characteristicIterator.getUuid().toString());
                                    device.characteristics.put(characteristicIterator.getUuid().toString(), characteristicIterator);
                                }
                            }

                            if (currentConfiguration != null) {
                                // TODO LogManager.getInstance().info("Auto subscribe for modes other than passing with close phone");
                                changeCommunicationState(gatt.getDevice().getAddress(), true, 0);
                            }
                        } else {
                            // TODO LogManager.getInstance().warn("Device cannot be found in connected devices list, stop process!");
                            onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
                        }
                    } else {
                        // TODO LogManager.getInstance().warn("Device cannot be found in connected devices list, stop process!");
                        onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // TODO LogManager.getInstance().info("Write to device for characteristic completed successfully!");
            } else {
                // TODO LogManager.getInstance().error("Write to device for characteristic failed!");
            }

            onWriteCompleted();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            byte[] dataValue = descriptor.getValue();
            if(dataValue == null) {
                // TODO LogManager.getInstance().warn("Descriptor value is empty on this go-round");
                onWriteCompleted();
                return;
            }

            // 0x01 - Subscribe
            // 0x00 - Unsubscribe

            if (dataValue[0] == 0x01) {
                // TODO LogManager.getInstance().info("Subscribed to characteristic: " + descriptor.getCharacteristic().getUuid().toString());
            } else if (dataValue[0] == 0x00) {
                // TODO LogManager.getInstance().info("Unsubscribed from characteristic: " + descriptor.getCharacteristic().getUuid().toString());
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // TODO LogManager.getInstance().info("Write to device for descriptor completed successfully!");
            } else {
                // TODO LogManager.getInstance().error("Write to device for descriptor failed!");
            }

            onWriteCompleted();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            try {
                byte[] receivedData = characteristic.getValue();

                if (receivedData == null || receivedData.length == 0) {
                    // TODO LogManager.getInstance().warn("Value that received from characteristic is empty, disconnect now");
                    onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
                    return;
                } else {
                    // TODO LogManager.getInstance().info("New value received from characteristic");
                    // TODO LogManager.getInstance().debug("Received Data: " + ConverterUtil.bytesToHexString(receivedData));
                }

                if (!currentConnectedDevices.containsKey(gatt.getDevice().getAddress())) {
                    // TODO LogManager.getInstance().warn("Value received but peripheral is not in connected devices' list, ignore data!");
                    return;
                }


                BLEDataContent result = DataParserUtil.getInstance().parse(receivedData);

                if (result != null) {

                    if (result.type == DataTypes.TYPE.AuthChallengeForPublicKey) {
                        try {
                            String deviceId = result.data.containsKey("deviceId") ? result.data.get("deviceId").toString() : "";
                            DeviceConnection connectedDevice = currentConnectedDevices.get(gatt.getDevice().getAddress());

                            if (connectedDevice != null) {
                                connectedDevice.deviceId = deviceId;

                                // TODO LogManager.getInstance().debug("Auth challenge received, device id: " + deviceId);

                                byte[] resultData = generateChallengeResponseDataWithDirection(
                                        result.type,
                                        deviceId,
                                        result.data.containsKey("challenge") ? ConverterUtil.hexStringToBytes(result.data.get("challenge").toString()) : new byte[0],
                                        result.data.containsKey("iv") ? ConverterUtil.hexStringToBytes(result.data.get("iv").toString()) : new byte[0]
                                );

                                writeToDevice(resultData, characteristic, connectedDevice.connection);
                            } else {
                                // TODO LogManager.getInstance().error("Generate challenge response failed because of empty stored device definition");
                                onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
                            }
                        } catch (Exception ex) {
                            // TODO LogManager.getInstance().error("Generate challenge response failed with error: " + ex.getLocalizedMessage());
                            onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
                        }
                    } else if (result.type == DataTypes.TYPE.AuthChallengeResult) {
                        processChallengeResult(gatt.getDevice().getAddress(), result);
                    } else {
                        // TODO LogManager.getInstance().warn("Unknown data type received from device, disconnect now!");
                        onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
                    }

                } else {
                    // TODO LogManager.getInstance().warn("Unknown data received from device, disconnect now!");
                    onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
                }
            } catch (Exception ex) {
                // TODO LogManager.getInstance().error("Handle data that received from device failed!");
                onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
            }
        }

    };
}
