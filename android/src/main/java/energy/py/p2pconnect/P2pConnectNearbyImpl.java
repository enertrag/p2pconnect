package energy.py.p2pconnect;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import androidx.collection.SimpleArrayMap;

public class P2pConnectNearbyImpl implements IP2pConnect {

    private static final Strategy STRATEGY = Strategy.P2P_POINT_TO_POINT;
    private static String TAG = "P2pConnect.Nearby";

    private SimpleArrayMap<Long, Payload> _activePayloads = new SimpleArrayMap<>();

    P2pConnect _plugin;

    class ReceiveBytesPayloadListener extends PayloadCallback {

        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            // This always gets the full data of the payload. Is null if it's not a BYTES payload.
            if (payload.getType() == Payload.Type.BYTES) {

                Log.d(TAG, "payload received: BYTES");
                byte[] receivedBytes = payload.asBytes();

                String message = new String(receivedBytes, StandardCharsets.UTF_8);
                _plugin.notifyMessage(endpointId, message);

            } else if(payload.getType() == Payload.Type.FILE) {

                Log.d(TAG, "payload received: FILE");
                // This does not work for parallel transfers!
                // The user must ensure that files are transferred sequentially.

                _activePayloads.put(payload.getId(), payload);
            }
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {

            // Bytes payloads are sent as a single chunk, so you'll receive a SUCCESS update immediately
            // after the call to onPayloadReceived().

            if(_activePayloads.containsKey(update.getPayloadId())) {

                Log.i(TAG, "onPayloadTransferUpdate(): received status '" + update.getStatus() + "' from endpoint '" + endpointId + "'");

                switch(update.getStatus()) {

                    case PayloadTransferUpdate.Status.SUCCESS:

                        Payload payload = _activePayloads.get(update.getPayloadId());
                        Uri uri = payload.asFile().asUri();
                        String uriString = uri != null ? uri.getPath() : null;

                        _plugin.notifyFileProgress(TransferState.Success, 100, uriString);
                        break;
                    case PayloadTransferUpdate.Status.IN_PROGRESS:

                        int progress = (int)(100.0 * update.getBytesTransferred() / (double)update.getTotalBytes());
                        Log.d(TAG, "onPayloadTransferUpdate(): progress = " + progress);

                        _plugin.notifyFileProgress(TransferState.InProgress, progress, null);
                        break;
                    case PayloadTransferUpdate.Status.FAILURE:
                        _activePayloads.remove(update.getPayloadId());
                        _plugin.notifyFileProgress(TransferState.Failure, 0, null);
                        break;
                    case PayloadTransferUpdate.Status.CANCELED:
                        _activePayloads.remove(update.getPayloadId());
                        _plugin.notifyFileProgress(TransferState.Canceled, 0, null);
                        break;

                }

            }

        }
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {

                    Log.i(TAG, "accepting connection initiated from '" + endpointId + "' " + connectionInfo.getEndpointName());

                    _plugin.notifyConnect(endpointId);
                    _plugin.notifySessionStateChanged(SessionState.Connecting, endpointId);
                    // Automatically accept the connection on both sides.
                    Nearby.getConnectionsClient(_plugin.getContext()).acceptConnection(endpointId, new ReceiveBytesPayloadListener());
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {

                    Log.i(TAG, "connection result changed");

                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            Log.i(TAG, "connection result = ConnectionsStatusCodes.STATUS_OK from endpoint '" + endpointId + "'");
                            _plugin.notifyConnect(endpointId);
                            _plugin.notifySessionStateChanged(SessionState.Connected, endpointId);
                            // We're connected! Can now start sending and receiving data.
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            Log.i(TAG, "connection result = ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED");
                            // The connection was rejected by one or both sides.
                            _plugin.notifySessionStateChanged(SessionState.NotConnected, endpointId);
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.
                            Log.i(TAG, "connection result = ConnectionsStatusCodes.STATUS_ERROR");
                            _plugin.notifySessionStateChanged(SessionState.NotConnected, endpointId);
                            break;
                        default:
                            Log.w(TAG, "connection result = unknown");
                            _plugin.notifySessionStateChanged(SessionState.NotConnected, endpointId);
                            // Unknown status code
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {

                    Log.i(TAG, "disconnected from endpoint '" + endpointId + "'");
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                    _plugin.notifySessionStateChanged(SessionState.NotConnected, endpointId);

                }
            };

    public P2pConnectNearbyImpl(P2pConnect plugin) {
        _plugin = plugin;
    }

    @Override public void initialize() {
    }

    @Override public boolean isAvailable() {
        return true;
    }

    @Override public void startAdvertise(String displayName, String serviceId) {

        Log.i(TAG, "starting advertising");

        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
        Nearby.getConnectionsClient(_plugin.getContext())
                .startAdvertising(
                        displayName, serviceId, connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            Log.i(TAG, "advertising started successfully");
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // We were unable to start advertising.
                            Log.e(TAG, "Nearby.getConnectionsClient().startAdvertising() failed", e);
                        });
    }

    @Override public void endAdvertise() {

        Log.i(TAG, "stopping advertising");

        Nearby.getConnectionsClient(_plugin.getContext()).stopAdvertising();
    }

    @Override public void startDiscover(String displayName, String serviceId) {

        Log.i(TAG, "starting peer discovery");

        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();

        Nearby.getConnectionsClient(_plugin.getContext())
                .startDiscovery(serviceId,  new EndpointDiscoveryCallback() {
                    @Override
                    public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {

                        Log.i(TAG, "found endpoint '" + endpointId + "' " + info.getServiceId() + "@" + info.getEndpointName());

                        _plugin.notifyPeerFound(new Peer(endpointId, info.getEndpointName()));
                    }

                    @Override
                    public void onEndpointLost(String endpointId) {
                        // A previously discovered endpoint has gone away.
                        Log.i(TAG, "endpoint '" + endpointId + "' lost");

                        _plugin.notifyPeerLost(new Peer(endpointId, null));
                    }
                }, discoveryOptions)
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

    @Override public void endDiscover() {

        Log.i(TAG, "stopping peer discovery");

        Nearby.getConnectionsClient(_plugin.getContext())
                .stopDiscovery();
    }

    @Override public void connect(String peerId, String displayName, IP2pOnSuccessListener successCallback, IP2pOnFailureListener failureCallback) {

        final ConnectionLifecycleCallback callback =
                new ConnectionLifecycleCallback() {
                    @Override
                    public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {

                        Log.i(TAG, "accepting connection initiated from '" + endpointId + "' " + connectionInfo.getEndpointName());

                        _plugin.notifySessionStateChanged(SessionState.Connecting, endpointId);
                        // Automatically accept the connection on both sides.
                        Nearby.getConnectionsClient(_plugin.getContext()).acceptConnection(endpointId, new ReceiveBytesPayloadListener());
                    }

                    @Override
                    public void onConnectionResult(String endpointId, ConnectionResolution result) {

                        Log.i(TAG, "connection result changed");

                        switch (result.getStatus().getStatusCode()) {
                            case ConnectionsStatusCodes.STATUS_OK:
                                Log.i(TAG, "connection result = ConnectionsStatusCodes.STATUS_OK, endoint '" + endpointId + "'");
                                // We're connected! Can now start sending and receiving data.
                                _plugin.notifySessionStateChanged(SessionState.Connected, endpointId);
                                break;
                            case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                                Log.i(TAG, "connection result = ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED");
                                // The connection was rejected by one or both sides.
                                _plugin.notifySessionStateChanged(SessionState.NotConnected, endpointId);
                                break;
                            case ConnectionsStatusCodes.STATUS_ERROR:
                                // The connection broke before it was able to be accepted.
                                Log.i(TAG, "connection result = ConnectionsStatusCodes.STATUS_ERROR");
                                _plugin.notifySessionStateChanged(SessionState.NotConnected, endpointId);
                                break;
                            default:
                                Log.w(TAG, "connection result = unknown");
                                _plugin.notifySessionStateChanged(SessionState.NotConnected, endpointId);
                                // Unknown status code
                        }
                    }

                    @Override
                    public void onDisconnected(String endpointId) {

                        Log.i(TAG, "disconnected from endpoint '" + endpointId + "'");
                        // We've been disconnected from this endpoint. No more data can be
                        // sent or received.
                        _plugin.notifySessionStateChanged(SessionState.NotConnected, endpointId);
                    }
                };

        Log.i(TAG, "connecting to peer '" + peerId + "' as " + displayName);

        // An endpoint was found. We request a connection to it.
        Nearby.getConnectionsClient(_plugin.getContext())
            .requestConnection(displayName, peerId, callback)
            .addOnSuccessListener(
            (Void unused) -> {
                // We successfully requested a connection. Now both sides
                // must accept before the connection is established.
                Log.i(TAG, "connection request successful - waiting for connection handshake...");
                successCallback.onSuccess(peerId);
            })
            .addOnFailureListener(
                (Exception e) -> {
                    // Nearby Connections failed to request the connection.
                    Log.e(TAG, "Nearby.getConnectionsClient().requestConnection() failed", e);
                    failureCallback.onFailure(e);
                });
    }

    @Override public void disconnect(String peerId) {

        Log.i(TAG, "disconnecting from endpoint '" + peerId + "'");

        Nearby.getConnectionsClient(_plugin.getContext()).disconnectFromEndpoint(peerId);
    }

    @Override public void sendMessage(String peerId, String message) {

        Log.i(TAG, "sending message to endpoint '" + peerId + "'");

        Payload payload = Payload.fromBytes(message.getBytes(StandardCharsets.UTF_8));
        Nearby.getConnectionsClient(_plugin.getContext()).sendPayload(peerId, payload);
    }

    @Override public void sendFile(String peerId, String url) throws FileNotFoundException {

        Log.i(TAG, "sending file to endpoint '" + peerId + "'");

        Uri u = Uri.parse(url);
        Log.i(TAG, "path = " + u.getPath());

        ContentResolver contentResolver = _plugin.getContext().getContentResolver();
        ParcelFileDescriptor fileDescriptor = contentResolver.openFileDescriptor(u, "r");

        Payload filePayload = Payload.fromFile(fileDescriptor);

        // Not sure when to add the payload to the list of active payloads
        // because sendPayload may fail afterwards?
        _activePayloads.put(filePayload.getId(), filePayload);
        Nearby.getConnectionsClient(_plugin.getContext()).sendPayload(peerId, filePayload);

    }
}
