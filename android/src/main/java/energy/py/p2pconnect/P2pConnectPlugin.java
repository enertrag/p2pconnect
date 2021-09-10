package energy.py.p2pconnect;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.util.ArrayList;
import java.util.Collection;

@CapacitorPlugin(
        name = "P2pConnect",
        permissions = {
                @Permission(
                    alias = "location",
                    strings = {
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    }
                ),
                @Permission(
                        alias = "wifi",
                        strings = {
                                Manifest.permission.ACCESS_WIFI_STATE,
                                Manifest.permission.CHANGE_WIFI_STATE
                        }
                ),
                @Permission(
                        alias = "network",
                        strings = {
                                Manifest.permission.CHANGE_NETWORK_STATE,
                                Manifest.permission.INTERNET,
                                Manifest.permission.ACCESS_NETWORK_STATE
                        }
                )
})
public class P2pConnectPlugin extends Plugin {

    private P2pConnect implementation;

    public static final String PEER_FOUND_EVENT = "peerFound";
    public static final String PEER_LOST_EVENT = "peerLost";
    public static final String CONNECT_EVENT = "connect";
    public static final String SESSION_STATE_CHANGE_EVENT = "sessionStateChange";
    public static final String START_RECEIVE_EVENT = "startReceive";
    public static final String RECEIVE_EVENT = "receive";

    @Override
    public void load() {
        Log.e(TAG, "load()");
        implementation = new P2pConnect(this);
        implementation.start();
    }

    @PluginMethod
    public void isAvailable(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("available", implementation.isAvailable());
        call.resolve(ret);
        notifyListeners("",null);
        // bridge.triggerJSEvent("", "", "");
    }

    @PluginMethod
    public void startAdvertise(PluginCall call) {
        if (!allPermissionsGranted()) {
            requestAllPermissions(call, "completeStartAdvertising");
        } else {
            implementation.start();
            JSObject ret = new JSObject();
            call.resolve(ret);
        }
    }

    @PermissionCallback
    private void completeStartAdvertising(PluginCall call) {
        if (allPermissionsGranted()) {
            implementation.start();
        } else {
            call.reject("Location permission was denied");
        }
    }

    @PluginMethod
    public void stopAdvertise(PluginCall call) {
        implementation.end();
        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void startBrowse(PluginCall call) {

        if (!allPermissionsGranted()) {
            requestAllPermissions(call, "completeStartBrowsing");
        } else {
            completeStartBrowsing(call);
        }
    }

    @PermissionCallback
    private void completeStartBrowsing(PluginCall call) {
        if (allPermissionsGranted()) {
            implementation.discoverPeers(new ActionListenerCallback() {
                @Override
                public void onSuccess() {
                    JSObject ret = new JSObject();
                    ret.put("id", "abc123");
                    call.resolve(ret);
                }

                @Override
                public void onFailure(int code) {
                    call.reject("Error discovering peers. Code " + code);
                }
            });
        } else {
            call.reject("Location permission was denied");
        }
    }

    @PluginMethod
    public void stopBrowse(PluginCall call) {
        implementation.stopPeerDiscovery(new ActionListenerCallback() {
            @Override
            public void onSuccess() {
                JSObject ret = new JSObject();
                call.resolve(ret);
            }

            @Override
            public void onFailure(int code) {
                call.reject("Error stopping peer discovery. Code " + code);
            }
        });

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void connect(PluginCall call) {

        JSObject peer = call.getObject("peer");
        String deviceAddress = peer.getString("id");

        implementation.connect(deviceAddress, new ActionListenerCallback() {
            @Override
            public void onSuccess() {
                JSObject ret = new JSObject();
                ret.put("id", "abc123");
                call.resolve(ret);
            }

            @Override
            public void onFailure(int code) {
                call.reject("Error connecting to peer. Code " + code);
            }
        });

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void disconnect(PluginCall call) {
        implementation.disconnect(new ActionListenerCallback() {
            @Override
            public void onSuccess() {
                JSObject ret = new JSObject();
                call.resolve(ret);
            }

            @Override
            public void onFailure(int code) {
                call.reject("Error disconnecting. Code " + code);
            }
        });

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void send(PluginCall call) {

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void sendResource(PluginCall call) {

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void getProgress(PluginCall call) {

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    private boolean allPermissionsGranted() {
        return
            getPermissionState("location") == PermissionState.GRANTED &&
            getPermissionState("wifi") == PermissionState.GRANTED &&
            getPermissionState("network") == PermissionState.GRANTED;
    }


    public void notifyPeersFound(Collection<WifiP2pDevice> deviceList) {
        if (deviceList == null) return;

        for (WifiP2pDevice device: deviceList) {
            notifyListeners(PEER_FOUND_EVENT, createBrowserObjectFromDevice(device));
        }
    }

    public void notifyPeersLost(ArrayList<WifiP2pDevice> peers) {
        if (peers == null) return;

        for (WifiP2pDevice device: peers) {
            notifyListeners(PEER_LOST_EVENT, createBrowserObjectFromDevice(device));
        }
    }

    private JSObject createBrowserObjectFromDevice(WifiP2pDevice device) {
        JSObject ret = new JSObject();
        ret.put("id", device.deviceAddress);
        ret.put("displayName", device.deviceName);
        return ret;
    }
}
