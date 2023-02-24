package energy.py.p2pconnect;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PeerAdapter extends RecyclerView.Adapter<PeerAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView nameTextView;
        public Button connectButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            nameTextView = (TextView) itemView.findViewById(R.id.contact_name);
            connectButton = (Button) itemView.findViewById(R.id.message_button);
        }
    }

    private List<Peer> _peers;
    private OnPeerClickedListener _listener;

    public PeerAdapter(List<Peer> peers, OnPeerClickedListener listener) {

        _peers = peers;
        _listener = listener;
    }

    public interface OnPeerClickedListener {
        void onClick(Peer peer);
    }


    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public PeerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.item_peer, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(contactView);


        return viewHolder;
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(PeerAdapter.ViewHolder holder, int position) {
        // Get the data model based on position
        Peer peer = _peers.get(position);

        // Set item views based on your views and data model
        TextView textView = holder.nameTextView;
        textView.setText(peer.getName());

        Button button = holder.connectButton;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("PeerAdapter.Click", "" + peer.getName());
                _listener.onClick(peer);
            }
        });
        //button.setOnClickListener();
        /*Button button = holder.messageButton;
        button.setText(contact.isOnline() ? "Message" : "Offline");
        button.setEnabled(contact.isOnline());*/
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return _peers.size();
    }

}
