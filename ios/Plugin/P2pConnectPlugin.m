#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

// Define the plugin using the CAP_PLUGIN Macro, and
// each method the plugin supports using the CAP_PLUGIN_METHOD macro.
CAP_PLUGIN(P2pConnectPlugin, "P2pConnect",
           CAP_PLUGIN_METHOD(isAvailable, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(startAdvertise, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(stopAdvertise, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(startBrowse, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(stopBrowse, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(connect, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(disconnect, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(send, CAPPluginReturnPromise);
)
