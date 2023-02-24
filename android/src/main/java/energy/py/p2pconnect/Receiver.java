package energy.py.p2pconnect;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;

public class Receiver implements ProgressCallback {

    private static final String TAG = "P2PConnect/Receiver";
    private static final Strategy STRATEGY = Strategy.P2P_POINT_TO_POINT;

    private static Receiver _instance = new Receiver();

    private String _currentTransferId;
    /** A connected endpoint to receive the acceptTransfer answer. Only valid during Transfer-Id check */
    private String _currentTransferEndpoint;

    private NotifyAcceptTransferCallback _notifyCallback;
    private TransferCompleteCallback _transferCompleteCallback;

    private ReceiverState _state = ReceiverState.NONE;
    private int _numberOfResourcesToReceive = 0;
    private Payload _currentPayload;
    private int _currentReceivingResource;
    private String[] _currentIds;
    private List<ResourceDescriptor> _receivedResources;

    public interface NotifyAcceptTransferCallback {

        void notify(String transferId);
    }

    public interface TransferCompleteCallback {

        void notify(String transferId, List<ResourceDescriptor> resources);
    }

    private WeakReference<ProgressCallback> _progressCallback;

    @Override
    public void updateProgress(String title, int progress) {

        ProgressCallback callback = _progressCallback.get();
        if(callback != null) {
            callback.updateProgress(title, progress);
        }

    }


    private Receiver() {
    }

    public static Receiver getInstance() {
        return _instance;
    }



    private void sendMessage(Context context, String peerId, String message) {

        Log.i(TAG, "Sending message to endpoint '" + peerId + "'");
        Log.d(TAG, "Message content = '" + message + "'");

        Payload payload = Payload.fromBytes(message.getBytes(StandardCharsets.UTF_8));
        Nearby.getConnectionsClient(context).sendPayload(peerId, payload);
    }

    public boolean acceptTransfer(Context context, String transferId, boolean accept) {

        if (_state != ReceiverState.WAITING_FOR_TRANSFER_ACCEPT) {
            Log.e(TAG, "Invalid state (expected: 'WAITING_FOR_TRANSFER_ACCEPT' found '" + _state + "')");
            return false;
        }

        if (!transferId.equals(_currentTransferId)) {
            Log.e(TAG, "Invalid transferId to accept (expected: '" + _currentTransferId + "' found '" + transferId + "')");
            return false;
        }

        String message = "tid." + (accept ? "accept" : "deny");

        updateProgress(null, 66);

        _state = ReceiverState.WAITING_FOR_COUNT;
        sendMessage(context, _currentTransferEndpoint, message);

        return true;
    }

    class ReceiveBytesPayloadListener extends PayloadCallback {

        private Context _context;


        public ReceiveBytesPayloadListener(Context context) {
            super();

            _context = context;

            Log.d(TAG, "ReceiveBytesPayloadListener() initialized");
        }

        @Override protected void finalize() throws Throwable {
            super.finalize();

            Log.d(TAG, "ReceiveBytesPayloadListener() finalized");
        }

        void invalidState(String message) {
            Log.w(TAG, "Invalid state '" + _state + "' for message '" + message + "'");
        }

        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            // This always gets the full data of the payload. Is null if it's not a BYTES payload.
            if (payload.getType() == Payload.Type.BYTES) {

                Log.d(TAG, "Payload received: BYTES");
                byte[] receivedBytes = payload.asBytes();

                String message = new String(receivedBytes, StandardCharsets.UTF_8);

                Log.d(TAG, "Payload message = " + message);

                switch(_state) {

                    case WAITING_FOR_VERSION:

                        if (message.startsWith("ver.")) {

                            int version = Integer.parseInt(message.substring("ver.".length()));
                            String versionAnswer = (version == P2pConnect.PROTOCOL_VERSION) ?
                                    "ver.accept" : "ver.deny";
                            Log.i(TAG, "Answering version message (" + message + ") with '"
                                    + versionAnswer + "'");

                            updateProgress(null, 25);
                            _state = ReceiverState.WAITING_FOR_TRANSFER_ID;
                            sendMessage(_context, endpointId, versionAnswer);
                        } else {
                            invalidState(message);
                        }

                        break;

                    case WAITING_FOR_TRANSFER_ID:

                        if(message.startsWith("tid.")) {

                            _currentTransferId = message.substring("tid.".length());
                            _currentTransferEndpoint = endpointId;
                            Log.i(TAG, "Notifying client about transferId '"
                                    + _currentTransferId + "'");

                            updateProgress(null, 50);
                            _state = ReceiverState.WAITING_FOR_TRANSFER_ACCEPT;
                            _notifyCallback.notify(_currentTransferId);

                        } else {
                            invalidState(message);
                        }

                        break;

                    case WAITING_FOR_COUNT:

                        if (message.startsWith("cnt.")) {

                            _currentReceivingResource = -1; // nothing received yet
                            _numberOfResourcesToReceive = Integer.parseInt(message.substring("cnt.".length()));

                            _currentIds = new String[_numberOfResourcesToReceive];
                            _receivedResources = new ArrayList<>();

                            Log.i(TAG, "Expecting " + _numberOfResourcesToReceive + " resource(s)");

                            updateProgress(null, 90);
                            _state = ReceiverState.WAITING_FOR_ID;
                            sendMessage(_context, endpointId, "cnt.accept");

                        } else {
                            invalidState(message);
                        }

                        break;

                    case WAITING_FOR_ID:

                        if(message.equals("id.done")) {

                            updateProgress(null, 100);
                            _state = ReceiverState.RECEIVING;
                            sendMessage(_context, endpointId, "id.accept");

                        } else if (message.startsWith("id.")) {

                            String indexString = message.substring("id.".length());
                            int i = indexString.indexOf('.');

                            String id = indexString.substring(i + 1);
                            indexString = indexString.substring(0, i);
                            int index = Integer.parseInt(indexString);

                            Log.i(TAG, "Received id " + index + " '" + id +"'");
                            _currentIds[index] = id;

                        } else {
                            invalidState(message);
                        }

                        break;

                    default:

                        invalidState(message);
                        break;

                }

            } else if (payload.getType() == Payload.Type.FILE) {

                Log.i(TAG, "Payload received: FILE " + payload.getId());

                if (_currentPayload != null) {
                    Log.w(TAG, "Invalid state: _currentPayload is not null");
                }

                _currentReceivingResource++;
                if(_currentReceivingResource >= _numberOfResourcesToReceive) {
                    Log.w(TAG, "Invalid number of resources (expected: "
                            + _numberOfResourcesToReceive
                            + " found: " + _currentReceivingResource + ")");
                }

                updateProgress("\uD83D\uDCC2 " + (_currentReceivingResource + 1) +  "/"
                        + _numberOfResourcesToReceive, 0);

                _currentPayload = payload;
            }
        }

        private String moveReceivedFileToCacheDir(Uri uri) {

            try {

                String filename = UUID.randomUUID().toString();

                InputStream in = _context.getContentResolver().openInputStream(uri);
                File file = new File(_context.getCacheDir(), filename);

                OutputStream out = new FileOutputStream(file);

                try {
                    byte[] buffer = new byte[1024];
                    int read;
                    while((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                }
                finally {
                    in.close();
                    out.close();
                }

                Log.i(TAG, "Moved file from " + uri.getPath() + " to " + file.getAbsolutePath());

                return "file://" + file.getAbsolutePath();

            } catch (IOException ex) {

                Log.e(TAG, "moveReceivedFileToCacheDir(): failed", ex);
                return null;

            } finally {

                _context.getContentResolver().delete(uri, null, null);
            }

        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {

            // Bytes payloads are sent as a single chunk, so you'll receive a SUCCESS update immediately
            // after the call to onPayloadReceived().

            if (_currentPayload != null && _currentPayload.getId() == update.getPayloadId()) {

                switch(update.getStatus()) {
                    case PayloadTransferUpdate.Status.IN_PROGRESS:

                        int progress = (int)(100.0 * update.getBytesTransferred()
                                / (double)update.getTotalBytes());

                        Log.d(TAG, "OnPayloadTransferUpdate: progress = " + progress);
                        updateProgress(null, progress);

                        break;

                    case PayloadTransferUpdate.Status.SUCCESS:

                        Uri uri = _currentPayload.asFile().asUri();
                        Log.d(TAG, "OnPayloadTransferUpdate: Success for URI '" + uri + "'");

                        if(uri != null) {
                            String targetUri = moveReceivedFileToCacheDir(uri);
                            Log.d(TAG, "OnPayloadTransferUpdate: moved file to = '" + targetUri + "'");

                            _receivedResources.add(new ResourceDescriptor(
                                    _currentIds[_currentReceivingResource],
                                    targetUri
                                ));

                        }
                        updateProgress("", 100);

                        if (_currentReceivingResource >= _numberOfResourcesToReceive - 1) {
                            finishCall(_context, endpointId);
                        }

                        break;

                    case PayloadTransferUpdate.Status.FAILURE:
                    case PayloadTransferUpdate.Status.CANCELED:
                        // TODO disconnect
                        break;
                }
            }
        }
    }

    void finishCall(Context context, String endpoint) {

        Log.i(TAG, "Transfer finished");

        _state = ReceiverState.NONE;
        updateProgress(null, -1);

        // Nearby.getConnectionsClient(context).disconnectFromEndpoint(endpoint);

        _transferCompleteCallback.notify(_currentTransferId, _receivedResources);

    }

    void acceptEndpoint(Context context, String endpointId, ProgressCallback progressCallback) {

        _progressCallback = new WeakReference<>(progressCallback);

        Log.i(TAG, "Stop advertising");
        Nearby.getConnectionsClient(context).stopAdvertising();
        Log.i(TAG, "Accept connection to " + endpointId);
        Nearby.getConnectionsClient(context).acceptConnection(endpointId, new Receiver.ReceiveBytesPayloadListener(context));
    }

    public void startAdvertise(Context context, String serviceId,
                               NotifyAcceptTransferCallback notifyCallback,
                               TransferCompleteCallback transferCompleteCallback) {

        Log.i(TAG, "Starting advertising");

        String displayName = Settings.Global.getString(context.getContentResolver(), Settings.Global.DEVICE_NAME);

        _notifyCallback = notifyCallback;
        _transferCompleteCallback = transferCompleteCallback;

        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
        _state = ReceiverState.WAITING_FOR_CONNECT;

        Nearby.getConnectionsClient(context)
                .startAdvertising(
                        displayName, serviceId, new ConnectionLifecycleCallback() {
                                @Override
                                public void onConnectionInitiated (String endpointId, ConnectionInfo
                                connectionInfo){

                                    Log.i(TAG, "Connection initiated from '" + endpointId + "' " + connectionInfo.getEndpointName());

                                    _state = ReceiverState.CONNECTING;

                                    Intent intent = new Intent(context, ReceiveActivity.class);
                                    intent.putExtra("endpointId", endpointId);
                                    context.startActivity(intent);
                                }

                                @Override
                                public void onConnectionResult (String
                                endpointId, ConnectionResolution result){

                                    Log.i(TAG, "connection result changed");

                                    switch (result.getStatus().getStatusCode()) {
                                        case ConnectionsStatusCodes.STATUS_OK:
                                            Log.i(TAG, "connection result = ConnectionsStatusCodes.STATUS_OK from endpoint '" + endpointId + "'");

                                            // 🤝 🔗 📂

                                            updateProgress("\uD83E\uDD1D", 0 );
                                            _state = ReceiverState.WAITING_FOR_VERSION;

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
                                            Log.w(TAG, "connection result = unknown");
                                          // TODO  _plugin.notifySessionStateChanged(SessionState.NotConnected, endpointId);
                                            // Unknown status code
                                    }
                                }

                                @Override
                                public void onDisconnected (String endpointId){

                                    // We've been disconnected from this endpoint. No more data can be
                                    // sent or received.
                                    Log.i(TAG, "disconnected from endpoint '" + endpointId + "'");

                                    // We also must call disconnect otherwise the PayloadCallback will not be removed
                                    Nearby.getConnectionsClient(context).disconnectFromEndpoint(endpointId);

                                    // TODO _plugin.notifySessionStateChanged(SessionState.NotConnected, endpointId);

                                }

                        }, advertisingOptions)
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

    public void endAdvertise(Context context) {

        Log.i(TAG, "Stop advertising while in state '" + _state + "'");

        Nearby.getConnectionsClient(context).stopAdvertising();
        _state = ReceiverState.NONE;
    }

}
