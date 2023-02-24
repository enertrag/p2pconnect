package energy.py.p2pconnect;

import java.util.ArrayList;
import java.util.Objects;

public class Peer {

    private String _id;
    private String _name;

    public Peer(String id, String name) {
        _id = id;
        _name = name;
    }

    public String getName() {
        return _name;
    }
    public String getId() { return _id; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer peer = (Peer) o;
        return Objects.equals(_id, peer._id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id);
    }
}
