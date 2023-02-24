package energy.py.p2pconnect;

public class ResourceDescriptor {

    private String _id;
    private String _uri;

    public ResourceDescriptor(String id, String uri) {
        _id = id;
        _uri = uri;
    }

    public String getUri() {
        return _uri;
    }

    public String getId() {
        return _id;
    }
}
