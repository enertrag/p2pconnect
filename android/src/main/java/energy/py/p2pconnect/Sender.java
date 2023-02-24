package energy.py.p2pconnect;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.List;

import androidx.annotation.NonNull;

public class Sender {

    private static final String TAG = "P2PConnect/Sender";
    private static final Strategy STRATEGY = Strategy.P2P_POINT_TO_POINT;

    private final static Sender _instance = new Sender();

    public interface CallResolver {
        PluginCall getCall();
    }

    public interface DiscoverCallback {
        void onPeerFound(Peer peer);
        void onPeerLost(Peer peer);
    }

    private SenderState _state;

    private String _currentServiceId;
    private String _currentTransferId;
    private List<ResourceDescriptor> _currentResources;
    private int _currentResourceIndex;
    private long _currentFilePayloadId;

    private String _currentPeerId;
    private CallResolver _callResolver;

    private Sender() {
    }

    public static Sender getInstance() {
        return _instance;
    }


    class PayloadHandler extends PayloadCallback implements ProgressCallback {

        private final Context _context;
        private final WeakReference<ProgressCallback> _progressCallback;

        PayloadHandler(Context context, ProgressCallback progressCallback) {

            _context = context;
            _progressCallback = new WeakReference<>(progressCallback);
        }

        @Override public void updateProgress(String title, int progress) {

            ProgressCallback callback = _progressCallback.get();
            if (callback != null) {
                callback.updateProgress(title, progress);
            }
        }

        private void failCall(String endpoint, String error) {

            Log.w(TAG, "Call failed: " + error);
            _state = SenderState.NONE;

            updateProgress(null, -1);

            Nearby.getConnectionsClient(_context).disconnectFromEndpoint(endpoint);

            JSObject result = new JSObject();
            result.put("success", false);
            result.put("error", error);
            _callResolver.getCall().resolve(result);
        }

        @Override
        public void onPayloadReceived(String endpointId, @NonNull Payload payload) {
            // This always gets the full data of the payload. Is null if it's not a BYTES payload.
            if (payload.getType() == Payload.Type.BYTES) {

                Log.i(TAG, "Payload received: BYTES");
                byte[] receivedBytes = payload.asBytes();

                String message = new String(receivedBytes, StandardCharsets.UTF_8);
                Log.d(TAG, "Received message '" + message + "'");

                switch(_state) {

                    case WAITING_FOR_VERSION:

                        if(message.equals("ver.accept")) {

                            _state = SenderState.WAITING_FOR_TRANSFER_ID;
                            updateProgress(null, 50);
                            sendMessage(_context, endpointId, "tid." + _currentTransferId);

                        } else { // ver.deny

                            failCall(endpointId, "versionMismatch");
                        }

                        break;

                    case WAITING_FOR_TRANSFER_ID:

                        if(message.equals("tid.accept")) {

                            _state = SenderState.WAITING_FOR_COUNT;
                            updateProgress(null, 75);
                            sendMessage(_context, endpointId, "cnt." + _currentResources.size());

                        } else { // tid.deny

                            failCall(endpointId, "transferDenied");
                        }

                        break;

                    case WAITING_FOR_COUNT:

                        if(message.equals("cnt.accept")) {

                            updateProgress(null, 90);

                            for (int i = 0; i < _currentResources.size(); i++) {
                                String m = "id." + i + "." + _currentResources.get(i).getId();
                                sendMessage(_context, endpointId, m);
                            }

                            _state = SenderState.WAITING_FOR_ID;
                            sendMessage(_context, endpointId, "id.done");

                        } else { // cnt.deny (?) not really possible atm

                                failCall(endpointId, "internalError");
                        }

                        break;

                    case WAITING_FOR_ID:

                        if (message.equals("id.accept")) {

                            _state = SenderState.TRANSFERRING_RESOURCES;

                            updateProgress(null, 100);
                            try {
                                sendResources(_context, endpointId, this);
                            } catch(IOException ex) {
                                Log.e(TAG, "Failed sending next resource", ex);
                                failCall(endpointId, "internalError");
                                return;
                            }

                        } else { // cnt.deny (?) not really possible atm

                            failCall(endpointId, "internalError");
                        }

                        break;

                    default:

                        // Invalid internal state
                        failCall(endpointId, "internalError");

                        break;
                }

            } else if (payload.getType() == Payload.Type.FILE) {

                Log.e(TAG, "Received file payload");
                failCall(endpointId, "internalError");
            }
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, @NonNull PayloadTransferUpdate update) {

            if (update.getPayloadId() != _currentFilePayloadId) {
                // we are not interested in other payload updates here
                return;
            }

            switch(update.getStatus()) {

                case PayloadTransferUpdate.Status.SUCCESS:

                    Log.i(TAG, "Transfer complete for payload " + _currentFilePayloadId);

                    _currentFilePayloadId = 0;
                    updateProgress(null, 100);
                    try {
                        sendNextResource(_context, endpointId, this);
                    } catch(IOException ex) {
                        Log.e(TAG, "Failed sending next resource", ex);
                        failCall(endpointId, "internalError");
                        return;
                    }

                    break;

                case PayloadTransferUpdate.Status.IN_PROGRESS:

                    int progress = (int)(100.0 * update.getBytesTransferred() / (double)update.getTotalBytes());
                    Log.d(TAG, "PayloadTransferUpdate: progress = " + progress);

                    updateProgress(null, progress);

                    break;

                default:

                    Log.e(TAG, "Invalid PayloadUpdateStatus (" + update.getStatus() + ")");
                    failCall(endpointId, "internalError");
                    break;
            }
        }
    }

    private void sendNextResource(Context context, String endpoint, ProgressCallback callback) throws FileNotFoundException {

        _currentResourceIndex++;
        Log.i(TAG, "Preparing for next resource (" + _currentResourceIndex + ")");

        if(_currentResourceIndex >= _currentResources.size()) {

            Log.i(TAG, "Nothing left to do for index " + _currentResourceIndex);
            // Finish
            _state = SenderState.NONE;

            Log.i(TAG, "Transfer completed");
            callback.updateProgress(null, -1);

            Nearby.getConnectionsClient(context).disconnectFromEndpoint(endpoint);

            JSObject result = new JSObject();
            result.put("success", true);
            _callResolver.getCall().resolve(result);

            return;
        }

        // 📂 x/y
        callback.updateProgress("\uD83D\uDCC2 " + (_currentResourceIndex + 1) + "/" + _currentResources.size(), 0);

        ResourceDescriptor resource = _currentResources.get(_currentResourceIndex);

        Uri u = Uri.parse(resource.getUri());
        Log.i(TAG, "path = " + u.getPath());

        ContentResolver contentResolver = context.getContentResolver();
        ParcelFileDescriptor fileDescriptor = contentResolver.openFileDescriptor(u, "r");

        Payload filePayload = Payload.fromFile(fileDescriptor);

        _currentFilePayloadId = filePayload.getId();
        Log.i(TAG, "Starting resource transfer with payload " + _currentFilePayloadId);

        Nearby.getConnectionsClient(context).sendPayload(endpoint, filePayload);
    }

    private void sendResources(Context context, String endpoint, ProgressCallback callback) throws FileNotFoundException {

        _state = SenderState.TRANSFERRING_RESOURCES;
        _currentResourceIndex = -1;
        sendNextResource(context, endpoint, callback);
    }

    private boolean checkState(String method, SenderState expectedState) {

        if (_state == expectedState) return true;

        Log.w(TAG, "Invalid state when calling '" + method + "' (expected '"
                + expectedState + "' found '" + _state + "')");

        return false;
    }

    public void startBrowse(Context context, String serviceId, String transferId, List<ResourceDescriptor> resources, CallResolver callResolver) {

        checkState("startBrowse", SenderState.NONE);

        _currentServiceId = serviceId;
        _currentTransferId = transferId;
        _currentResources = resources;

        _callResolver = callResolver;

        _state = SenderState.START_BROWSING;

        Intent intent = new Intent(context, PeerBrowserActivity.class);
        context.startActivity(intent);
    }

    void cancelBrowse(Context context) {

        Log.i(TAG, "Cancel browsing");

        if (checkState("cancelBrowse", SenderState.BROWSING)) {
            Nearby.getConnectionsClient(context).stopDiscovery();
        }

        _state = SenderState.NONE;

        JSObject result = new JSObject();
        result.put("success", false);
        result.put("error", "cancelled");

        _callResolver.getCall().resolve(result);
    }

    public void startDiscover(Context context, DiscoverCallback uiCallback) {

        Log.i(TAG, "Starting peer discovery");

        checkState("startDiscover", SenderState.START_BROWSING);
        _state = SenderState.BROWSING;

        EndpointDiscoveryCallback endpointDiscoveryCallback =
                new EndpointDiscoveryCallback() {

                    @Override
                    public void onEndpointFound(String endpointId, @NonNull DiscoveredEndpointInfo info) {

                        Log.i(TAG, "found endpoint '" + endpointId + "' " + info.getServiceId() + "@" + info.getEndpointName());
                        uiCallback.onPeerFound(new Peer(endpointId, info.getEndpointName()));
                    }

                    @Override
                    public void onEndpointLost(String endpointId) {
                        // A previously discovered endpoint has gone away.
                        Log.i(TAG, "endpoint '" + endpointId + "' lost");

                        Peer lostPeer = new Peer(endpointId, "");
                        uiCallback.onPeerLost(lostPeer);
                    }
                };

        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();

        Nearby.getConnectionsClient(context)
                .startDiscovery(_currentServiceId,  endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            Log.i(TAG, "discovery started successfully");
                            // We're discovering!
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // We're unable to start discovering.
                            Log.e(TAG, "Nearby.getConnectionsClient().startDiscovery() failed", e);
                        });
    }

    void connect(Context context, String peerId) {

        Log.i(TAG, "Connect to peer " + peerId);

        if (checkState("connect", SenderState.BROWSING)) {
            Nearby.getConnectionsClient(context).stopDiscovery();
        }

        _currentPeerId = peerId;

        _state = SenderState.START_CONNECTING;

        Intent intent = new Intent(context, SendActivity.class);
        context.startActivity(intent);
    }

    private void sendMessage(Context context, String peerId, String message) {

        Log.i(TAG, "Sending message to endpoint '" + peerId + "'");
        Log.d(TAG, "Message content = '" + message + "'");

        Payload payload = Payload.fromBytes(message.getBytes(StandardCharsets.UTF_8));
        Nearby.getConnectionsClient(context).sendPayload(peerId, payload);
    }


    public void startTransfer(Context context, ProgressCallback progressCallback) {

        checkState("startTransfer", SenderState.START_CONNECTING);

        final ConnectionLifecycleCallback callback =
                new ConnectionLifecycleCallback() {
                    @Override
                    public void onConnectionInitiated(String endpointId, @NonNull ConnectionInfo connectionInfo) {

                        Log.i(TAG, "accepting connection initiated from '" + endpointId + "' " + connectionInfo.getEndpointName());

                        // Automatically accept the connection on both sides.
                        Nearby.getConnectionsClient(context).acceptConnection(endpointId, new PayloadHandler(context, progressCallback));
                    }

                    @Override
                    public void onConnectionResult(String endpointId, @NonNull ConnectionResolution result) {

                        Log.d(TAG, "Connection result changed");

                        switch (result.getStatus().getStatusCode()) {
                            case ConnectionsStatusCodes.STATUS_OK:
                                Log.i(TAG, "Connection result = ConnectionsStatusCodes.STATUS_OK, endoint '" + endpointId + "'");

                                _state = SenderState.WAITING_FOR_VERSION; // 🤝 🔗 📂
                                progressCallback.updateProgress("\uD83E\uDD1D", 25);
                                sendMessage(context, endpointId, "ver." + P2pConnect.PROTOCOL_VERSION);

                                break;
                            case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                                Log.i(TAG, "connection result = ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED");
                                // The connection was rejected by one or both sides.
                                // TODO _plugin.notifySessionStateChanged(SessionState.NotConnected, endpointId);
                                break;
                            case ConnectionsStatusCodes.STATUS_ERROR:
                                // The connection broke before it was able to be accepted.
                                Log.i(TAG, "connection result = ConnectionsStatusCodes.STATUS_ERROR");
                                // TODO _plugin.notifySessionStateChanged(SessionState.NotConnected, endpointId);
                                break;
                            default:
                                Log.w(TAG, "Connection result = unknown");
                                // TODO _plugin.notifySessionStateChanged(SessionState.NotConnected, endpointId);
                                // Unknown status code
                        }
                    }

                    @Override
                    public void onDisconnected(String endpointId) {

                        Log.i(TAG, "disconnected from endpoint '" + endpointId + "'");

                        // We've been disconnected from this endpoint. No more data can be
                        // sent or received.
                        // TODO _plugin.notifySessionStateChanged(SessionState.NotConnected, endpointId);
                        // FIXME check for state and resolve call
                    }
                };

        String deviceName = Settings.Global.DEVICE_NAME;
        Log.i(TAG, "connecting to peer '" + _currentPeerId + "' as " + deviceName);
        _state = SenderState.CONNECTING;

        // An endpoint was found. We request a connection to it.
        Nearby.getConnectionsClient(context)
                .requestConnection(deviceName, _currentPeerId, callback)
                .addOnSuccessListener(
                        (Void unused) -> {
                            // We successfully requested a connection. Now both sides
                            // must accept before the connection is established.
                            Log.i(TAG, "connection request successful - waiting for connection handshake...");
                       // TODO     successCallback.onSuccess(peerId);
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // Nearby Connections failed to request the connection.
                            Log.e(TAG, "Nearby.getConnectionsClient().requestConnection() failed", e);
                          // TODO  failureCallback.onFailure(e);
                            // FIXME muss resolve call
                        });
    }

}
