package energy.py.p2pconnect;

import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.*;

public class P2pConnectNearbyImpl implements IP2pConnect {

    private static final Strategy STRATEGY = Strategy.P2P_POINT_TO_POINT;
    private static String TAG = "P2pConnect.Nearby";

    P2pConnect _plugin;

    static class ReceiveBytesPayloadListener extends PayloadCallback {

        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            // This always gets the full data of the payload. Is null if it's not a BYTES payload.
            if (payload.getType() == Payload.Type.BYTES) {
                byte[] receivedBytes = payload.asBytes();
            }
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
            // Bytes payloads are sent as a single chunk, so you'll receive a SUCCESS update immediately
            // after the call to onPayloadReceived().
        }
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {

                    Log.i(TAG, "accepting connection initiated from '" + endpointId + "' " + connectionInfo.getEndpointName());

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
                            // We're connected! Can now start sending and receiving data.
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            Log.i(TAG, "connection result = ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED");
                            // The connection was rejected by one or both sides.
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.
                            Log.i(TAG, "connection result = ConnectionsStatusCodes.STATUS_ERROR");
                            break;
                        default:
                            Log.w(TAG, "connection result = unknown");
                            // Unknown status code
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {

                    Log.i(TAG, "disconnected from endpoint '" + endpointId + "'");
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
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
                                successCallback.onSuccess(endpointId);
                                break;
                            case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                                Log.i(TAG, "connection result = ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED");
                                // The connection was rejected by one or both sides.
                                failureCallback.onFailure(null);
                                break;
                            case ConnectionsStatusCodes.STATUS_ERROR:
                                // The connection broke before it was able to be accepted.
                                Log.i(TAG, "connection result = ConnectionsStatusCodes.STATUS_ERROR");
                                failureCallback.onFailure(null);
                                break;
                            default:
                                Log.w(TAG, "connection result = unknown");
                                failureCallback.onFailure(null);
                                // Unknown status code
                        }
                    }

                    @Override
                    public void onDisconnected(String endpointId) {

                        Log.i(TAG, "disconnected from endpoint '" + endpointId + "'");
                        _plugin.notifyPeerLost(new Peer(endpointId, null));
                        // We've been disconnected from this endpoint. No more data can be
                        // sent or received.
                    }
                };

        // An endpoint was found. We request a connection to it.
        Nearby.getConnectionsClient(_plugin.getContext())
            .requestConnection(displayName, peerId, callback)
            .addOnSuccessListener(
            (Void unused) -> {
                // We successfully requested a connection. Now both sides
                // must accept before the connection is established.
                Log.i(TAG, "connection request successful - waiting for connection handshake...");
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
}
