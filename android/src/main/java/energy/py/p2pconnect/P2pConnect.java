package energy.py.p2pconnect;

import static android.os.Looper.getMainLooper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;

public class P2pConnect {

    P2pConnectPlugin plugin;
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    WiFiDirectBroadcastReceiver receiver;
    IntentFilter intentFilter;

    public P2pConnect(P2pConnectPlugin p2pConnectPlugin) {
        plugin = p2pConnectPlugin;
        manager = (WifiP2pManager) plugin.getContext().getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(plugin.getContext(), getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, plugin);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    public void start() {
        plugin.getContext().registerReceiver(receiver, intentFilter);
    }

    public void end() {
        plugin.getContext().unregisterReceiver(receiver);
    }

    @SuppressLint("MissingPermission")
    public void discoverPeers() {
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reasonCode) {

            }
        });
    }

    public boolean isAvailable() {
        return receiver.isAvailable();
    }
}
