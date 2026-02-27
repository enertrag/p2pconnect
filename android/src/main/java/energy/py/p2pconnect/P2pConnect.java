package energy.py.p2pconnect;

import android.Manifest;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@CapacitorPlugin(
        name = "P2pConnect",
        permissions = {

                @Permission(
                    alias = "location",
                    strings = {
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.NEARBY_WIFI_DEVICES
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
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_ADVERTISE,
                                Manifest.permission.BLUETOOTH_CONNECT
                        }
                )
})
public class P2pConnect extends Plugin {

    public static String TAG = "P2PConnect/Plugin";

    public static final int PROTOCOL_VERSION = 1;

    public static final String ACCEPT_TRANSFER = "acceptTransfer";
    public static final String TRANSFER_COMPLETE = "transferComplete";

    private String _lastDisplayName = null; //Settings.Global.getString(getContext().getContentResolver(), "device_name");

    @Override
    public void load() {
        Log.i(TAG, "load()");
    }

    @PluginMethod
    public void isAvailable(PluginCall call) {
        JSObject ret = new JSObject();

        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(getActivity());
        if(status != ConnectionResult.SUCCESS) {

            if(googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(getActivity(), status, 2404).show();
            }

            ret.put("available", false);
            call.resolve(ret);

            return;
        }

        ret.put("available", true);
        call.resolve(ret);
    }

    @PluginMethod
    public void send(PluginCall call) {

        if(!allPermissionsGranted()) {
            requestAllPermissions(call, "completeSend");

        } else {
            completeSend(call);
        }
    }

    @PluginMethod
    public void startReceive(PluginCall call) {

        if(!allPermissionsGranted()) {
            requestAllPermissions(call, "completeStartReceive");

        } else {
            completeStartReceive(call);
        }
    }

    @PluginMethod
    public void stopReceive(PluginCall call) {

        if(!allPermissionsGranted()) {
            requestAllPermissions(call, "completeStopReceive");

        } else {
            completeStopReceive(call);
        }
    }

    @PluginMethod
    public void acceptTransfer(PluginCall call) {

        String transferId = call.getString("transferId");
        if(transferId == null) {

            Log.e(TAG, "Missing transferId");
            call.reject("missing transferId");
            return;
        }

        boolean accept = call.getBoolean("accept", false);
        boolean result = Receiver.getInstance().acceptTransfer(getContext(), transferId, accept);

        if (result) {
            call.resolve();
        } else {
            call.reject("invalid transferId");
        }
    }

    @PermissionCallback
    private void completeSend(PluginCall call) {

        if(allPermissionsGranted()) {

            String serviceId = call.getString("serviceId");
            if(serviceId == null) {

                Log.e(TAG, "Missing serviceId");
                call.reject("missing serviceId");
                return;
            }

            String transferId = call.getString("transferId");
            if(transferId == null) {

                Log.e(TAG, "Missing transferId");
                call.reject("missing transferId");
                return;
            }

            List<ResourceDescriptor> resourceDescriptors = new ArrayList<ResourceDescriptor>();

            JSArray resources = call.getArray("resources");
            if(resources == null) {

                Log.e(TAG, "Missing resources");
                call.reject("missing resources");
                return;
            }

            try {
                for (int i = 0; i < resources.length(); i++) {

                    JSONObject resource = resources.getJSONObject(i);
                    String id = resource.getString("id");
                    String uri = resource.getString("uri");

                    Log.d(TAG, "ResourceDescriptor[" + i +"] " + id + " = " + uri);

                    resourceDescriptors.add(new ResourceDescriptor(id, uri));
                }
            } catch(JSONException ex) {

                Log.e(TAG, "Failed converting input data", ex);

                call.reject("invalid resource descriptor");
                return;
            }

            Log.d(TAG, "Calling startBrowse with serviceId " + serviceId);
            getBridge().saveCall(call);
            Sender.getInstance().startBrowse(getContext(), serviceId, transferId, resourceDescriptors, () -> getBridge().getSavedCall(call.getCallbackId()));

        } else {

            JSObject result = new JSObject();
            result.put("success", false);
            result.put("error", "permissionDenied");
            call.resolve(result);
        }
    }

    @PermissionCallback
    private void completeStartReceive(PluginCall call) {

        if(allPermissionsGranted()) {

            String serviceId = call.getString("serviceId");
            if(serviceId == null) {

                Log.e(TAG, "Missing serviceId");
                call.reject("missing serviceId");
                return;
            }

            Log.d(TAG, "Calling startAdvertise with serviceId " + serviceId);
            // FIXME create another callback and resolve/reject
            //  call in .addOnSuccessListener/.addOnFailureListener
            Receiver.getInstance().startAdvertise(getContext(), serviceId, transferId -> {

                // accept transfer

                if (transferId == null) return;

                JSObject message = new JSObject();
                message.put("transferId", transferId);

                notifyListeners(ACCEPT_TRANSFER, message);

            }, (transferId, resources) -> {

                // transfer complete

                JSObject message = new JSObject();
                message.put("transferId", transferId);

                JSArray list = new JSArray();

                for(int i = 0; i < resources.size(); i++) {

                    ResourceDescriptor d = resources.get(i);

                    JSObject resource = new JSObject();
                    resource.put("id", d.getId());
                    resource.put("uri", d.getUri());

                    list.put(resource);
                }
                message.put("resources", list);

                notifyListeners(TRANSFER_COMPLETE, message);
            });

            JSObject result = new JSObject();
            result.put("success", true);
            call.resolve(result);

        } else {

            JSObject result = new JSObject();
            result.put("success", false);
            call.resolve(result);
        }
    }

    @PermissionCallback
    private void completeStopReceive(PluginCall call) {

        if(allPermissionsGranted()) {

            Log.d(TAG, "Calling endAdvertise()");
            Receiver.getInstance().endAdvertise(getContext());
            JSObject result = new JSObject();
            result.put("success", true);
            call.resolve(result);

        } else {

            JSObject result = new JSObject();
            result.put("success", false);
            call.resolve(result);
        }
    }

    private boolean allPermissionsGranted() {
        return
            getPermissionState("location") == PermissionState.GRANTED &&
            getPermissionState("wifi") == PermissionState.GRANTED &&
            getPermissionState("bluetooth") == PermissionState.GRANTED;
    }

}
