package energy.py.p2pconnect;

import static android.content.ContentValues.TAG;
import static android.os.Looper.getMainLooper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

public class P2pConnect {

    P2pConnectPlugin plugin;
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    WiFiDirectBroadcastReceiver receiver;
    IntentFilter intentFilter;

    public P2pConnect(P2pConnectPlugin p2pConnectPlugin) {
        plugin = p2pConnectPlugin;

        if (!initP2p()) {
            plugin.getActivity().finish();
        }

        receiver = new WiFiDirectBroadcastReceiver(manager, channel, plugin);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    private boolean initP2p() {
        // Device capability definition check
        if (!plugin.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Log.e(TAG, "Wi-Fi Direct is not supported by this device.");
            return false;
        }

        // Hardware capability check
        WifiManager wifiManager = (WifiManager) plugin.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            Log.e(TAG, "Cannot get Wi-Fi system service.");
            return false;
        }

        if (!wifiManager.isP2pSupported()) {
            Log.e(TAG, "Wi-Fi Direct is not supported by the hardware or Wi-Fi is off.");
            return false;
        }

        manager = (WifiP2pManager) plugin.getContext().getSystemService(Context.WIFI_P2P_SERVICE);
        if (manager == null) {
            Log.e(TAG, "Cannot get Wi-Fi Direct system service.");
            return false;
        }

        channel = manager.initialize(plugin.getContext(), getMainLooper(), null);
        if (channel == null) {
            Log.e(TAG, "Cannot initialize Wi-Fi Direct.");
            return false;
        }

        return true;
    }

    public void start() {
        Log.e(TAG, "start()");
        plugin.getContext().registerReceiver(receiver, intentFilter);
    }

    public void end() {
        Log.e(TAG, "end()");
        plugin.getContext().unregisterReceiver(receiver);
    }

    @SuppressLint("MissingPermission")
    public void discoverPeers(ActionListenerCallback callback) {
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onFailure(int reasonCode) {
                callback.onFailure(reasonCode);
            }
        });
    }

    public void stopPeerDiscovery(ActionListenerCallback callback) {
        manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onFailure(int reasonCode) {
                callback.onFailure(reasonCode);
            }
        });
    }

    @SuppressLint("MissingPermission")
    public void connect(String deviceAddress, ActionListenerCallback connectCallback) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                connectCallback.onSuccess();
            }

            @Override
            public void onFailure(int reason) {
                connectCallback.onFailure(reason);
            }
        });
    }

    public void disconnect(ActionListenerCallback callback) {
        manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onFailure(int reasonCode) {
                callback.onFailure(reasonCode);
            }
        });
    }

    public boolean isAvailable() {
        return receiver.isAvailable();
    }


}
