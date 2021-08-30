package energy.py.p2pconnect;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "P2pConnect")
public class P2pConnectPlugin extends Plugin {

    private P2pConnect implementation = new P2pConnect();

    @PluginMethod
    public void isAvailable(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("available", implementation.isAvailable());
        call.resolve(ret);
    }

    @PluginMethod
    public void startAdvertise(PluginCall call) {

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void stopAdvertise(PluginCall call) {

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void startBrowse(PluginCall call) {

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void stopBrowse(PluginCall call) {

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void connect(PluginCall call) {

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void disconnect(PluginCall call) {

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void send(PluginCall call) {

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void sendResource(PluginCall call) {

        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void getProgress(PluginCall call) {

        JSObject ret = new JSObject();
        call.resolve(ret);
    }
}
