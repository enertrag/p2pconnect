// Copyright © 2021 ENERTRAG AG

import Foundation
import UIKit
import Capacitor
import MultipeerConnectivity

var InstanceId = UUID().uuidString

class Log {
    
    static let Info = "🐭  "
    static let Error = "⚡️  "
    static let Fatal = "⛔  "
}

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(P2pConnectPlugin)
public class P2pConnectPlugin: CAPPlugin {

    private var receiver: Receiver!
    private var sender: Sender!
    
    override public func load() {
        CAPLog.print("asdfasdf")
        
        receiver = Receiver(self)
        sender = Sender(self)
    }
    
    func activePresentationController() -> UIViewController? {
        
        guard let base = self.bridge?.viewController else {
            return nil
        }
        
        var top = base
        while let presented = top.presentedViewController {
            top = presented
        }
        
        return top
    }
    
    func present(_ controller: UIViewController,
                 animated: Bool = true,
                 completion: (() -> Void)? = nil) {
        
        guard let presenter = self.activePresentationController() else {
            return
        }
        
        presenter.present(controller, animated: animated, completion: completion)
    }

    deinit {
    }
    
    @objc func startReceive(_ call: CAPPluginCall) {
        
        let serviceId = call.getString("serviceId") ?? "default"
        CAPLog.print("start receive called: \(serviceId)");
        
        receiver.start(serviceId)
        
        call.resolve(["success": true])
        
    }
    
    @objc func stopReceive(_ call: CAPPluginCall) {
        
        CAPLog.print("stop receive called");
        
        receiver.stop()
        
        call.resolve(["success": true])
        
    }
    
    @objc func send(_ call: CAPPluginCall) {
        
        let serviceId = call.getString("serviceId") ?? "default"
        let transferId = call.getString("transferId") ?? "default"
        CAPLog.print("send called: \(serviceId) -> \(transferId)");
        
        var descriptors: [ResourceDescriptor] = []
        
        let resources = call.getArray("resources", JSObject.self)!
        
        for resource in resources {
         
            let id = resource["id"] as! String
            let uri = resource["uri"] as! String
            
            let url = URL(string: uri)!
            
            let descriptor = ResourceDescriptor(id: id, uri: url)
            
            descriptors.append(descriptor)
        }
        
        self.bridge?.saveCall(call)
        
        CAPLog.print("🐇  send: \(descriptors.count) resources")
        
        sender.start(serviceId,
                     transferId: transferId,
                     resources: descriptors,
                     callbackId: call.callbackId)
    }
    
    @objc func acceptTransfer(_ call: CAPPluginCall) {
        
        CAPLog.print("acceptTransfer called")
        
        let transferId = call.getString("transferId") ?? "default"
        let accept = call.getBool("accept") ?? false

        receiver.acceptTransfer(accept, withTransferId: transferId)
        
        call.resolve()
    }
    
    
    
    @objc func isAvailable(_ call: CAPPluginCall) {
        
        CAPLog.print("isAvailable called")
        call.resolve(["available": true])
    }
    
}
