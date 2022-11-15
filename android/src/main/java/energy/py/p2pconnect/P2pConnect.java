package energy.py.p2pconnect;

import android.Manifest;
import android.net.wifi.p2p.WifiP2pDevice;
import android.provider.Settings;
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
import java.util.UUID;

@CapacitorPlugin(
        name = "P2pConnect",
        permissions = {

                @Permission(
                    alias = "location",
                    strings = {
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            // Manifest.permission.NEARBY_WIFI_DEVICES
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
                        alias = "bluetooth",
                        strings = {
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.BLUETOOTH
                        }
                ),
                @Permission(
                        alias = "file",
                        strings = {
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                        }
                )
})
public class P2pConnect extends Plugin {

    public static String TAG = "P2pConnectPlugin";

    private IP2pConnect implementation;

    public static final String PEER_FOUND_EVENT = "peerFound";
    public static final String PEER_LOST_EVENT = "peerLost";
    public static final String CONNECT_EVENT = "connect";
    public static final String SESSION_STATE_CHANGE_EVENT = "sessionStateChange";
    public static final String START_RECEIVE_EVENT = "startReceive";
    public static final String RECEIVE_EVENT = "receive";

    private String _lastDisplayName = Settings.Global.getString(getContext().getContentResolver(), "device_name");;

    @Override
    public void load() {
        Log.i(TAG, "load()");
        implementation = new P2pConnectNearbyImpl(this);



        implementation.initialize();
    }

    @PluginMethod
    public void isAvailable(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("available", implementation.isAvailable());
        call.resolve(ret);
    }

    @PluginMethod
    public void startAdvertise(PluginCall call) {

        if (!allPermissionsGranted()) {
            requestAllPermissions(call, "completeStartAdvertising");
        } else {
            completeStartAdvertising(call);
        }
    }

    @PermissionCallback
    private void completeStartAdvertising(PluginCall call) {
        if (allPermissionsGranted()) {

            String serviceId = call.getString("serviceType");
            String displayName = call.getString("displayName");

            if(displayName == null) {
                displayName = Settings.Global.getString(getContext().getContentResolver(), "device_name");
            }

            implementation.startAdvertise(displayName, serviceId);
            JSObject ret = new JSObject();
            call.resolve(ret);
        } else {
            call.reject("Location permission was denied");
        }
    }

    @PluginMethod
    public void stopAdvertise(PluginCall call) {
        implementation.endAdvertise();
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

            String serviceId = call.getString("serviceType");
            String displayName = call.getString("displayName");

            if(displayName == null) {
                displayName = Settings.Global.getString(getContext().getContentResolver(), "device_name");
            }
            // A little bit "hacky" at this point: We remember the name of the searching device because we need it again when we connect.
            _lastDisplayName = displayName;

            implementation.startDiscover(displayName, serviceId);

            JSObject ret = new JSObject();
            ret.put("id", "abc123");
            call.resolve(ret);
        } else {
            call.reject("Location permission was denied");
        }
    }

    @PluginMethod
    public void stopBrowse(PluginCall call) {

        implementation.endDiscover();

        JSObject ret = new JSObject();
        call.resolve(ret);

        //        call.reject("Error stopping peer discovery. Code " + code);
    }

    @PluginMethod
    public void connect(PluginCall call) {

        JSObject peer = call.getObject("peer");
        String deviceAddress = peer.getString("id");

        Log.d("connect", peer.toString());
        Log.d("connect", deviceAddress);

        implementation.connect(deviceAddress, /* see above */_lastDisplayName,
            (String peerId) -> {
                JSObject ret = new JSObject();
                ret.put("id", peerId);
                call.resolve(ret);
            },
            (Exception e) -> {
                call.reject("Error connecting to peer");
            });
    }

    @PluginMethod
    public void disconnect(PluginCall call) {

        JSObject peer = call.getObject("session");
        String deviceAddress = peer.getString("id");

        implementation.disconnect(deviceAddress);

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

        String url = call.getString("url");
        JSObject peer = call.getObject("peer");
        String groupOwnerHostAddress = peer.getString("id");
        String guid = call.getString("name");

        Log.d("sendResource", url);
        Log.d("sendResource", peer.toString());
        Log.d("groupOwnerHostAddress", groupOwnerHostAddress);
        Log.d("name", guid);

      //  implementation.sendResource(url, guid, groupOwnerHostAddress);

        JSObject ret = new JSObject();
        ret.put("id", guid);
        call.resolve(ret);
    }

    @PluginMethod
    public void getProgress(PluginCall call) {

        String id = call.getString("id");

        boolean finished = false; // implementation.isTransferFinished(id);

        JSObject ret = new JSObject();
        ret.put("isFinished", finished);
        ret.put("isCancelled", false);
        ret.put("fractionCompleted", false);
        call.resolve(ret);
    }

    private boolean allPermissionsGranted() {
        return
            getPermissionState("location") == PermissionState.GRANTED &&
            getPermissionState("wifi") == PermissionState.GRANTED &&
            getPermissionState("bluetooth") == PermissionState.GRANTED &&
            getPermissionState("file") == PermissionState.GRANTED;
    }


    public void notifyPeerFound(Peer peer) {
        if (peer == null) return;

        notifyListeners(PEER_FOUND_EVENT, createBrowserObjectFromDevice(peer));
    }

    public void notifyPeerLost(Peer peer) {
        if (peer == null) return;

        notifyListeners(PEER_LOST_EVENT, createBrowserObjectFromDevice(peer));
    }

    public void notifyStartReceive() {
        notifyListeners(START_RECEIVE_EVENT, createStartReceiveResult());
    }

    public void notifyReceive(String url) {
        notifyListeners(RECEIVE_EVENT, createReceiveEvent(url));
    }

    public void notifyConnect(String groupOwnerAddress) {
        notifyListeners(CONNECT_EVENT, createConnectEvent(groupOwnerAddress));
    }

    private JSObject createConnectEvent(String groupOwnerAddress) {
        JSObject session = new JSObject();
        session.put("id", groupOwnerAddress);

        JSObject advertiser = new JSObject();
        advertiser.put("id", "");

        JSObject ret = new JSObject();
        ret.put("session", session);
        ret.put("advertiser", advertiser);
        return ret;
    }

    private JSObject createReceiveEvent(String url) {
        JSObject session = new JSObject();
        session.put("id", "");

        JSObject ret = new JSObject();
        ret.put("session", session);
        ret.put("message", "");
        ret.put("url", url);
        return ret;
    }

    private JSObject createStartReceiveResult() {
        JSObject session = new JSObject();
        session.put("id", "");

        JSObject ret = new JSObject();
        ret.put("session", session);
        ret.put("name", UUID.randomUUID().toString());
        return ret;
    }

    private JSObject createBrowserObjectFromDevice(Peer device) {
        JSObject ret = new JSObject();
        ret.put("id", device.id);
        ret.put("displayName", device.name);
        return ret;
    }
}
