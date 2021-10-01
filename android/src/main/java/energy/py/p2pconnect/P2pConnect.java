package energy.py.p2pconnect;

import static android.os.Looper.getMainLooper;

import static energy.py.p2pconnect.WiFiDirectBroadcastReceiver.copyFile;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class P2pConnect {

    public static String TAG = "P2pConnect";

    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_enertragP2PConnect";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";

    P2pConnectPlugin plugin;
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    WiFiDirectBroadcastReceiver receiver;
    IntentFilter intentFilter;

    private Intent receiverIntent;
    private WifiP2pDnsSdServiceRequest serviceRequest;
    private WifiP2pDnsSdServiceInfo service;

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

    public void registerReceiver() {
        Log.e(TAG, "registerReceiver()");
        if (receiverIntent == null)
            receiverIntent = plugin.getContext().registerReceiver(receiver, intentFilter);
    }
    public void unregisterReceiver() {
        Log.e(TAG, "unregisterReceiver()");
        plugin.getContext().unregisterReceiver(receiver);
        receiverIntent = null;
    }

    @SuppressLint("MissingPermission")
    public void startAdvertise() {
        Log.e(TAG, "startAdvertise()");
        registerReceiver();
        startRegistrationAndDiscovery();
    }

    public void endAdvertise() {
        Log.e(TAG, "endAdvertise()");
        unregisterReceiver();
        endRegistrationAndDiscovery();
    }

    @SuppressLint("MissingPermission")
    public void discoverPeers(ActionListenerCallback callback) {
        registerReceiver();
        startRegistrationAndDiscovery();
    }

    public void stopPeerDiscovery(ActionListenerCallback callback) {
        Log.e(TAG, "stopPeerDiscovery()");
        unregisterReceiver();
        endRegistrationAndDiscovery();
    }

    @SuppressLint("MissingPermission")
    public void connect(String deviceAddress, ActionListenerCallback connectCallback) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 0;

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

    private final ArrayList<String> finishedTransfers = new ArrayList<>();

    public void sendResource(String url, String guid, String hostAddress) {

        Socket socket = new Socket();

        try {
            Log.d("sendResource", "Opening client socket - ");
            socket.bind(null);

            socket.connect((new InetSocketAddress(hostAddress, 8988)), 10000);

            Log.d("sendResource", "Client socket - " + socket.isConnected());
            OutputStream stream = socket.getOutputStream();
            ContentResolver cr = plugin.getContext().getContentResolver();
            InputStream is = null;
            try {
                is = cr.openInputStream(Uri.parse(url));
            } catch (FileNotFoundException e) {
                Log.d("sendResource", e.toString());
            }
            copyFile(is, stream);
            Log.d("sendResource", "Client: Data written");
            finishedTransfers.add(guid);
            Log.d("sendResource", "Added finished transfer: "  + guid);
        } catch (IOException e) {
            Log.e("sendResource", e.getMessage());
        } finally {
            if (socket != null) {
                if (socket.isConnected()) {
                    try {
                        Log.d("sendResource", "Closing socket");
                        socket.close();
                    } catch (IOException e) {
                        // Give up
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    /**
     * Registers a local service and then initiates a service discovery
     */
    @SuppressLint("MissingPermission")
    public void startRegistrationAndDiscovery() {
        registerLocalService();
        discoverService();
    }

    public void endRegistrationAndDiscovery() {
        manager.removeLocalService(channel, service, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Removed Local Service");
            }

            @Override
            public void onFailure(int i) {
                Log.d(TAG, "Failed to remove a service");
            }
        });

        manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Stopped peer discovery");
            }

            @Override
            public void onFailure(int i) {
                Log.d(TAG, "Failed stopping peer discovery");
            }
        });

        manager.removeServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Removed service request");
            }

            @Override
            public void onFailure(int i) {
                Log.d(TAG, "Failed removing service request");
            }
        });
    }

    @SuppressLint("MissingPermission")
    public void registerLocalService() {
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");

        service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);

        manager.addLocalService(channel, service, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Added Local Service");
            }

            @Override
            public void onFailure(int error) {
                Log.d(TAG, "Failed to add a service");
            }
        });


    }

    @SuppressLint("MissingPermission")
    private void discoverService() {

        /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */

        manager.setDnsSdResponseListeners(channel,
                new WifiP2pManager.DnsSdServiceResponseListener() {

                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                                                        String registrationType, WifiP2pDevice srcDevice) {

                        // A service has been discovered. Is this our app?

                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {

                            ArrayList<WifiP2pDevice> wifiP2pDevices = new ArrayList<>();
                            wifiP2pDevices.add(srcDevice);
                            plugin.notifyPeersFound(wifiP2pDevices);
                        }

                    }
                }, new WifiP2pManager.DnsSdTxtRecordListener() {

                    /**
                     * A new TXT record is available. Pick up the advertised
                     * buddy name.
                     */
                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomainName, Map<String, String> record,
                            WifiP2pDevice device) {
                        Log.d(TAG,
                                device.deviceName + " is "
                                        + record.get(TXTRECORD_PROP_AVAILABLE));
                    }
                });

        // After attaching listeners, create a service request and initiate
        // discovery.
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        manager.addServiceRequest(channel, serviceRequest,
                new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Added service discovery request");
                    }

                    @Override
                    public void onFailure(int arg0) {
                        Log.d(TAG, "Failed adding service discovery request");
                    }
                });
        manager.discoverServices(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Service discovery initiated");
            }

            @Override
            public void onFailure(int arg0) {
                Log.d(TAG, "Service discovery failed");
            }
        });
    }

    public boolean isTransferFinished(String id) {
        Log.d("isTransferFinished", "Looking for finished transfer: " + id);
        return finishedTransfers.contains(id);
    }
}
