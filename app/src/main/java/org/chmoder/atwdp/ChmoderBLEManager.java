package org.chmoder.atwdp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.chmoder.atwdp.models.ChmoderProperties;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by tcross on 6/5/2017.
 */

public class ChmoderBLEManager<T extends Activity> {
    private final static String TAG = ChmoderBLEManager.class.getSimpleName();
    private static ChmoderBLEManager mInstance;
    private T activity;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private HashSet<BluetoothDevice> availableDevices = new HashSet<BluetoothDevice>();

    private BluetoothGattServer mBluetoothGattServer;
    private BroadcastReceiver broadcastReceiver;
    private Boolean isAdvertising = false;

    private static final String UUID_WIFI_PROVISIONING_SERVICE = "e6217646-4738-11e7-a919-92ebcb67fe33";
    private static final String UUID_WIFI_PROVISIONING_SERVICE_GET_SSID = "dd22a957-dd9c-4a3d-9bbf-acd5b464168a";
    private static final String UUID_WIFI_PROVISIONING_SERVICE_SETUP_WIFI = "97869662-523f-11e7-b114-b2f933d5fe66";

    private ChmoderBLEManager(T activity) {
        this.activity = activity;
        this.close();

        final BluetoothManager bluetoothManager = getBluetoothManager();
        this.mBluetoothAdapter = bluetoothManager.getAdapter();
        this.mBluetoothAdapter.setName("Android Thing");
    }

    public static synchronized <T extends Activity> ChmoderBLEManager getInstance(T activity) {
        if (mInstance == null) {
            mInstance = new ChmoderBLEManager(activity);
        }
        return mInstance;
    }

    public void toggleBluetooth() {
        if(mBluetoothAdapter.isEnabled()) {
            // close();
            // mBluetoothAdapter.disable();
        } else {
            registerReceiver();
            Log.d(TAG, "Enabling Bluetooth");
            mBluetoothAdapter.enable();
        }
    }

    private BroadcastReceiver getBroadcastReceiver() {
        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final String action = intent.getAction();

                    if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        switch (state) {
                            case BluetoothAdapter.STATE_OFF:
                                Log.d(TAG, "Bluetooth is off");
                                toggleBluetooth();
                                break;
                            case BluetoothAdapter.STATE_TURNING_OFF:
                                Log.d(TAG, "Bluetooth is turning off");
                                break;
                            case BluetoothAdapter.STATE_ON:
                                Log.d(TAG, "Start Advertising");
                                startAdvertising();
                                break;
                            case BluetoothAdapter.STATE_TURNING_ON:
                                Log.d(TAG, "Bluetooth is turning on");
                                break;
                            default:
                                Log.d(TAG, String.valueOf(state));
                                break;
                        }
                    }
                }
            };
        }
        return broadcastReceiver;
    }

    private BluetoothManager getBluetoothManager() {
        if(mBluetoothManager == null) {
            mBluetoothManager =  (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        }
        return mBluetoothManager;
    }

    private List<ScanFilter> getScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
        ScanFilter.Builder scanFilterBuilder = new ScanFilter.Builder();
        ScanFilter scanFilter = scanFilterBuilder
                .setServiceUuid(
                        ParcelUuid.fromString(
                                this.lookupUUID("wifiProvisioningService", null)
                        )
                )
                .build();
        scanFilters.add(scanFilter);
        return scanFilters;
    }

    private ScanSettings getScanSettings() {
        ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
        ScanSettings scanSettings = scanSettingsBuilder.build();
        return scanSettings;
    }

    private ScanCallback getScanCallback() {
        ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                BluetoothDevice device = result.getDevice();
                availableDevices.add(device);
                String deviceName = device.getName().equals(null) ? "Unavailable":device.getName();
                Log.d(TAG, deviceName);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };
        return scanCallback;
    }

    private String lookupUUID(String serviceName, String characteristicName) {
        if(serviceName.equals("wifiProvisioningService")) {
            if(characteristicName == null) {
                return UUID_WIFI_PROVISIONING_SERVICE;
            } else if(characteristicName.equals("getSSID")) {
                return UUID_WIFI_PROVISIONING_SERVICE_GET_SSID;
            } else if(characteristicName.equals("setupWIFI")) {
                return UUID_WIFI_PROVISIONING_SERVICE_SETUP_WIFI;
            } else{
                Log.d(TAG, "unknown characteristic name");
            }
        }
        return null;
    }

    private AdvertiseSettings getAdvertiseSettings() {
        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();
        return advertiseSettings;
    }

    private AdvertiseData getAdvertiseData() {
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .addServiceUuid(
                        ParcelUuid.fromString(
                                this.lookupUUID("wifiProvisioningService", null)
                        )
                )
                .build();
        return advertiseData;
    }

    private AdvertiseCallback getAdvertiseCallback() {
        AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.d(TAG, "Advertising Started");
                startServer();
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Log.e(TAG, "Error start advertising error code " + String.valueOf(errorCode));
            }
        };
        return advertiseCallback;
    }

    public void startAdvertising() {
        // Some advertising settings. We don't set an advertising timeout
        // since our device is always connected to AC power.
        if(isAdvertising) {
            return;
        }
        isAdvertising = true;
        AdvertiseSettings advertiseSettings = getAdvertiseSettings();
        AdvertiseData advertiseData = getAdvertiseData();
        AdvertiseCallback advertiseCallback = getAdvertiseCallback();

        // Starts advertising.
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mBluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
//        Log.d(TAG, "advertising started");
    }

    public void stopAdvertising() {
        AdvertiseCallback advertiseCallback = getAdvertiseCallback();
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mBluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
    }

    private BluetoothGattServerCallback getGattServerCallback() {
        BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                super.onConnectionStateChange(device, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Connected to GATT device.");

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from GATT device.");
                }
            }

            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                super.onServiceAdded(status, service);
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                String characteristicUuid = characteristic.getUuid().toString();
                Log.d(TAG, "characteristic read request with UUID " + characteristicUuid);

                if(characteristicUuid.equals(lookupUUID("wifiProvisioningService", "getSSID"))) {
                    mBluetoothGattServer.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            (ChmoderWIFIManager.getSSID(activity) + " " + ChmoderWIFIManager.getIPAddress(activity)).getBytes()
                    );
                } else if (characteristicUuid.equals(lookupUUID("wifiProvisioningService", "setupWIFI"))) {
                    // mWIFIManager.setupWIFI();
                }

            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                String command = byteToString(value, value.length);
                Log.d(TAG, "characteristic write request: " + command);

                Map<String, String> commandObj = fromJSON(command);
                String ssidName = commandObj.get("ssidName");
                String ssidPassword = commandObj.get("ssidPassword");
                String uid = commandObj.get("uid");

                ChmoderProperties.setProperties(null, uid, ChmoderProperties.DEVICE_UNPROVISIONED_STATUS);
                ChmoderWIFIManager.getInstance(activity).setupWIFI(ssidName, ssidPassword);

                // the rest of the work happens in the wifi manager
                mBluetoothGattServer.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        "wifi set up".getBytes()
                );
            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            }

            @Override
            public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
                super.onExecuteWrite(device, requestId, execute);
            }

            @Override
            public void onNotificationSent(BluetoothDevice device, int status) {
                super.onNotificationSent(device, status);
            }

            @Override
            public void onMtuChanged(BluetoothDevice device, int mtu) {
                super.onMtuChanged(device, mtu);
            }
        };

        return bluetoothGattServerCallback;
    }

    private String byteToString(byte[] bytes, int numberOfBytes) {
        return new String(bytes, 0, numberOfBytes, Charset.forName("UTF-8"));
    }

    private BluetoothGattService getBluetoothGattService(UUID uuid) {
        BluetoothGattService bluetoothGattService = new BluetoothGattService(uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        return bluetoothGattService;
    }

    private BluetoothGattService getBluetoothGattService(String uuid) {
        return this.getBluetoothGattService(UUID.fromString(uuid));
    }

    private BluetoothGattCharacteristic getBluetoothGattCharacteristic(UUID uuid, int properties, int permissions) {
        BluetoothGattCharacteristic bluetoothGattCharacteristic = new BluetoothGattCharacteristic(
                uuid,
                properties,
                permissions
        );
        return bluetoothGattCharacteristic;
    }

    private BluetoothGattCharacteristic getBluetoothGattCharacteristic(String uuid, int properties, int permissions) {
        return this.getBluetoothGattCharacteristic(
                UUID.fromString(uuid),
                properties,
                permissions
        );
    }

    private BluetoothGattCharacteristic getBluetoothGattCharacteristicRead(String uuid) {
        return this.getBluetoothGattCharacteristic(
                uuid,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
        );
    }

    private BluetoothGattCharacteristic getBluetoothGattCharacteristicWrite(String uuid) {
        return this.getBluetoothGattCharacteristic(
                uuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE
        );
    }

    private BluetoothGattService createBluetoothGattService() {
        BluetoothGattService wifiProvisioningService = this.getBluetoothGattService(
                this.lookupUUID("wifiProvisioningService", null)
        );
        BluetoothGattCharacteristic bluetoothGattCharacteristicGetSSID = this.getBluetoothGattCharacteristicRead(
                this.lookupUUID("wifiProvisioningService", "getSSID")
        );
        BluetoothGattCharacteristic bluetoothGattCharacteristicSetupWIFI = this.getBluetoothGattCharacteristicWrite(
                this.lookupUUID("wifiProvisioningService", "setupWIFI")
        );

        wifiProvisioningService.addCharacteristic(bluetoothGattCharacteristicGetSSID);
        wifiProvisioningService.addCharacteristic(bluetoothGattCharacteristicSetupWIFI);
        return wifiProvisioningService;
    }

    private void startServer() {
        final BluetoothManager bluetoothManager = getBluetoothManager();
        BluetoothGattService wifiProvisioningService = createBluetoothGattService();

        this.mBluetoothGattServer = bluetoothManager.openGattServer(activity, getGattServerCallback());
        this.mBluetoothGattServer.addService(wifiProvisioningService);
    }

    private void stopServer() {
        if (this.mBluetoothGattServer == null) {
            return;
        }

        mBluetoothGattServer.close();
    }

    private void registerReceiver() {
        Log.d("TAG", "Registering BLE Receiver!");
        activity.registerReceiver(getBroadcastReceiver(), new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    private void unregisterReceiver() {
        try {
            activity.unregisterReceiver(getBroadcastReceiver());
        } catch(IllegalArgumentException e) {
            // i don't care if there is no receiver
        } finally {
            this.broadcastReceiver = null;
        }
    }

    public void close() {
        stopServer();
        unregisterReceiver();
    }

    private Map<String, String> fromJSON(String jsonString) {
        Type typeOfHashMap = new TypeToken<HashMap<String, String>>() { }.getType();
        Gson gson = new Gson();
        return gson.fromJson(jsonString, typeOfHashMap);
    }
}
