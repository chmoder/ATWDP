package org.chmoder.atwdp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.chmoder.atwdp.models.ChmoderProperties;

import java.util.Calendar;
import java.util.List;

/**
 * Created by tcross on 5/28/2017.
 */

public class ChmoderWIFIManager<T extends Activity> {
    private static final String TAG = "WIFIManager";
    private static ChmoderWIFIManager mInstance;
    private WifiManager mainWifi;
    private BroadcastReceiver mDeviceDiscoveryReceiver = null;
//    private ChmoderFirebaseManager mChmoderFirebaseManager;
    private ChmoderBLEManager mChmoderBLEManager;
    private T activity;
//    private Boolean toggledBluetooth = false;
    private Boolean setAuthListener = false;
//    private Boolean isProvisioningDevice = false;

    private ChmoderWIFIManager(T activity) {
        this.activity = activity;
//        this.mChmoderFirebaseManager = ChmoderFirebaseManager.getInstance(this.activity);
        this.mChmoderBLEManager = ChmoderBLEManager.getInstance(this.activity);
        mainWifi = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        registerBroadcastReceivers();

        if(!mainWifi.isWifiEnabled()) {
            mainWifi.setWifiEnabled(true);
        }
    }

    public static synchronized <T extends Activity> ChmoderWIFIManager getInstance(T activity) {
        if (mInstance == null) {
            mInstance = new ChmoderWIFIManager(activity);
        }
        return mInstance;
    }

    public static Boolean isConnectingOrConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo == null) {
            Log.d(TAG, "net info is null");
        } else {
            Log.d(TAG, String.valueOf(networkInfo.getType()));
        }

        return (networkInfo != null) && ((networkInfo.getState() == NetworkInfo.State.CONNECTED) || (networkInfo.getState() == NetworkInfo.State.CONNECTING));
    }

    private void registerBroadcastReceivers() {
        if (mDeviceDiscoveryReceiver == null) {
            mDeviceDiscoveryReceiver = new BroadcastReceiver() {
                public void onReceive(final Context context, final Intent intent) {
                    Log.d(TAG, "in broadcast receiver");
//                    ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//                    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                    int extraWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);


                    switch (extraWifiState) {
                        case WifiManager.WIFI_STATE_DISABLED:
                            mainWifi.setWifiEnabled(true);
                            break;
                        default:
                            break;
                    }

                    String action = intent.getAction();
                    if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                        NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                        boolean connected = info.isConnected();
                        Log.d(TAG, "is connecting or connected");
                        //if (connected && ChmoderProperties.getProperties() != null) {
                        if (connected) {
                            if (ChmoderProperties.getProperties().getProvisionedStatus().equals(ChmoderProperties.DEVICE_UNPROVISIONED_STATUS)) {
                                // if is not provisioned
                                Log.d(TAG, "is connected");
//                                toggledBluetooth = false;
                                ChmoderProperties.setProperties(null, null, ChmoderProperties.DEVICE_PROVISIONING_STATUS);
//                                mChmoderFirebaseManager.provisionDevice();
                            } else if (ChmoderProperties.getProperties().getProvisionedStatus().equals(ChmoderProperties.DEVICE_PROVISIONED_STATUS)) {
                                // if is provisioned
                                Log.d(TAG, "should set auth listener");
                                if (setAuthListener.equals(false)) {
                                    Log.d(TAG, "set auth listener");
                                    setAuthListener = true;
//                                    toggledBluetooth = false;
//                                    mChmoderFirebaseManager.setAuthListener();
                                }
                            }
                        } else {
                            // internet is not connected
                            Log.d(TAG, "internet is off");
//                            if (networkInfo == null) {
//                                if (toggledBluetooth.equals(false)) {

                                    Log.d(TAG, "START bluetooth from wifi manager");
                                    // TODO: start advert or toggle?
                                    mChmoderBLEManager.toggleBluetooth();
                                    mChmoderBLEManager.startAdvertising();
//                                    toggledBluetooth = true;
                                    setAuthListener = false;
//                                }
//                            }
                        }
                    }
                }
            };

            // connectivity manager connectivity change
            IntentFilter filter = new IntentFilter();
//            filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
//            filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
            this.activity.registerReceiver(mDeviceDiscoveryReceiver, filter);

//            if (toggledBluetooth.equals(false)) {
//                Log.d(TAG, "START bluetooth from wifi manager");
//                mChmoderBLEManager.toggleBluetooth();
//                toggledBluetooth = true;
//                setAuthListener = false;
//            }
        }
    }

    public Boolean isConnected() {
        WifiInfo connectionInfo = mainWifi.getConnectionInfo();
        if (mainWifi.isWifiEnabled() && !connectionInfo.getSSID().contains("unknown ssid") && connectionInfo.getLinkSpeed() > 0)
            return true;
        else {
            return false;
        }
    }

    public String getSSID() {
        WifiInfo currentWifi = mainWifi.getConnectionInfo();
        return currentWifi.getSSID().replaceAll("\"", "");
    }

    public String getIPAddress() {
        WifiInfo currentWifi = mainWifi.getConnectionInfo();
        int ip = currentWifi.getIpAddress();
        String result = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));

        return result;
    }

    public static String getSSID(Context activity) {
        WifiManager wifiSystemService = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        WifiInfo currentWifi = wifiSystemService.getConnectionInfo();
        return currentWifi.getSSID().replaceAll("\"", "");
    }

    public static String getIPAddress(Context activity) {
        WifiManager wifiSystemService = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        WifiInfo currentWifi = wifiSystemService.getConnectionInfo();
        int ip = currentWifi.getIpAddress();
        String result = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));

        return result;
    }

    public void setupWIFI(String ssidName, String ssidPassword) {
        this.clearNetworks();
        registerBroadcastReceivers();
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssidName);
        wifiConfig.preSharedKey = String.format("\"%s\"", ssidPassword);

        Log.d(TAG, ssidName);
        Log.d(TAG, ssidPassword);
        //remember id
        int netId = mainWifi.addNetwork(wifiConfig);
        if (netId > -1) {
            mainWifi.disconnect();
            mainWifi.enableNetwork(netId, true);
            mainWifi.reconnect();

            Log.d(TAG, "before connected");
            int startTime = Calendar.getInstance().get(Calendar.SECOND);
            while(Calendar.getInstance().get(Calendar.SECOND) - startTime > 10 || !isConnected()) {}
            Log.d(TAG, "after connected");
        }
    }

    public void clearNetworks() {
        Log.d(TAG, "Clearing WIFI");
        if (isConnected()) {
            mainWifi.disconnect();
            Log.d(TAG, "disconnecting wifi");
        }

        List<WifiConfiguration> configuredNetworks = mainWifi.getConfiguredNetworks();

        for (WifiConfiguration configuredNetwork : configuredNetworks) {
            mainWifi.removeNetwork(configuredNetwork.networkId);
        }
    }

    public void close() {
        if (mDeviceDiscoveryReceiver != null) {
            try {
                activity.unregisterReceiver(mDeviceDiscoveryReceiver);
            } catch(IllegalArgumentException e) {
                Log.w(TAG, "Receiver not registered: org.chmoder.androidthings.device_manager.ChmoderWIFIManager");
            }

        }
        this.mDeviceDiscoveryReceiver = null;
    }
}
