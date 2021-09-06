package energy.py.p2pconnect;

import android.Manifest;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

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

    @Override
    public void load() {
        implementation = new P2pConnect(this);
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
            JSObject ret = new JSObject();
            call.resolve(ret);
        }
    }

    @PermissionCallback
    private void completeRequestPermissions(PluginCall call) {
        if (allPermissionsGranted()) {
            implementation.discoverPeers();
        } else {
            call.reject("Location permission was denied");
        }
    }

    @PluginMethod
    public void stopAdvertise(PluginCall call) {

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void startBrowse(PluginCall call) {

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void stopBrowse(PluginCall call) {

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void connect(PluginCall call) {

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void disconnect(PluginCall call) {

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


}
