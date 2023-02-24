package energy.py.p2pconnect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import energy.py.p2pconnect.R;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.getcapacitor.Plugin;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.ArrayList;

public class PeerBrowserActivity extends AppCompatActivity {

    private static String TAG = "P2pConnect/PeerBrowserActivity";
    private ArrayList<Peer> _peers;
    private PeerAdapter _peerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer_browser);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Sender.getInstance().startDiscover(this, new Sender.DiscoverCallback() {
            @Override
            public void onPeerFound(Peer peer) {
                _peers.add(peer);
                _peerAdapter.notifyItemInserted(_peers.size() - 1);
            }

            @Override
            public void onPeerLost(Peer peer) {

                int index = _peers.indexOf(peer);
                if(index >= 0) {
                    _peers.remove(index);
                    _peerAdapter.notifyItemRemoved(index);
                }

            }
        });

        RecyclerView rvPeers = (RecyclerView) findViewById(R.id.rvPeers);
        _peers = new ArrayList<Peer>();

        _peerAdapter = new PeerAdapter(_peers, peer -> {

            Log.i(TAG, "clicked " + peer.getId());

            Log.d(TAG, "Start transfer");
            Sender.getInstance().connect(PeerBrowserActivity.this, peer.getId());
            Log.d(TAG, "Removing activity");
            finish(); // remove this activity
        });

        rvPeers.setAdapter(_peerAdapter);
        rvPeers.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {

            Sender.getInstance().cancelBrowse(this);
            finish();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}