package energy.py.p2pconnect;

import androidx.annotation.NonNull;

public interface IP2pConnect {

    void initialize();

    boolean isAvailable();

    void startAdvertise(String displayName, String serviceId);

    void endAdvertise();

    void startDiscover(String displayName, String serviceId);

    void endDiscover();

    void connect(String peerId, String displayName, @NonNull IP2pOnSuccessListener<String> successCallback, @NonNull IP2pOnFailureListener failureCallback);

    void disconnect(String peerId);
}
