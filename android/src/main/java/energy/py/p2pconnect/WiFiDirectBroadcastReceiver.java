package energy.py.p2pconnect;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver implements WifiP2pManager.ConnectionInfoListener {

    public static String TAG = "WiFiDirectBroadcastReceiver";

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    public P2pConnect activity;
    private ArrayList<WifiP2pDevice> peers = new ArrayList<>();

    private boolean wifiP2pState;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       P2pConnect activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
                wifiP2pState = true;
            } else {
                // Wi-Fi P2P is not enabled
                wifiP2pState = false;
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                // We are connected with the other device, request connection
                // info to find group owner IP
                manager.requestConnectionInfo(channel, this);
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
        }
    }

    public boolean isAvailable() {
        return wifiP2pState;
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        Log.d("isGroupOwner", String.valueOf(wifiP2pInfo.isGroupOwner));
        Log.d("groupFormed", String.valueOf(wifiP2pInfo.groupFormed));
        Log.d("groupOwnerHostAddress", wifiP2pInfo.groupOwnerAddress.getHostAddress());

        String text = null;

        if (wifiP2pInfo.groupFormed) {
            if (wifiP2pInfo.isGroupOwner) {
                new FileServerAsyncTask(activity.getActivity(), text, activity)
                        .execute();
            }
            else {
                activity.notifyConnect(wifiP2pInfo.groupOwnerAddress.getHostAddress());
            }
        }
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private String statusText;
        private P2pConnect activity;

        /**
         * @param context
         * @param statusText
         * @param activity
         */
        public FileServerAsyncTask(Context context, String statusText, P2pConnect activity) {
            this.context = context;
            this.statusText = statusText;
            this.activity = activity;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(TAG, "Server: connection done");

                activity.notifyStartReceive();

                final File f = new File(context.getExternalFilesDir("received"),
                        "wifip2pshared-" + System.currentTimeMillis()
                                + ".data");

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();

                Log.d(TAG, "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();

                activity.notifyReceive(f.toURI().toString());

                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            statusText = "Opening a server socket";
        }
    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            return false;
        }
        return true;
    }
}
