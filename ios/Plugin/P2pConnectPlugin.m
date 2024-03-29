#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

// Define the plugin using the CAP_PLUGIN Macro, and
// each method the plugin supports using the CAP_PLUGIN_METHOD macro.
CAP_PLUGIN(P2pConnectPlugin, "P2pConnect",
           CAP_PLUGIN_METHOD(isAvailable, CAPPluginReturnPromise);
           
           CAP_PLUGIN_METHOD(startReceive, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(stopReceive, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(acceptTransfer, CAPPluginReturnPromise);
           
           CAP_PLUGIN_METHOD(send, CAPPluginReturnPromise);

)
