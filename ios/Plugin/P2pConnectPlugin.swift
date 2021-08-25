// Copyright © 2021 ENERTRAG AG

import Foundation
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

    private var serviceAdvertisers: [Advertiser] = []
    private var serviceBrowsers: [String:Browser] = [:]
    
    private var sessions: [String:MCSession] = [:]
    
    override public func load() {
    }

    deinit {
    }
    
    @objc func isAvailable(_ call: CAPPluginCall) {
        
        CAPLog.print("isAvailable called")
        call.resolve(["available": true])
    }
    
    @objc func startAdvertise(_ call: CAPPluginCall) {
        
        CAPLog.print("startAdvertise called")
        
        let displayName = call.getString("displayName")
        guard let serviceType = call.getString("serviceType") else {
            call.reject("Must provide a service type")
            return
        }
        
        let advertiser = Advertiser(displayName: displayName, serviceType: serviceType)
        advertiser.delegate = self
        
        serviceAdvertisers.append(advertiser)
        
        advertiser.startAdvertisingPeer()
        
        call.resolve(["id": advertiser.id])
    }
    
    @objc func stopAdvertise(_ call: CAPPluginCall) {
        CAPLog.print("stopAdvertise called")
        
        guard let advertiserObj = call.getObject("advertiser") else {
            call.reject("Must provide an advertiser")
            return
        }
        
        guard let key = advertiserObj["id"] as? String else {
            call.reject("Must provide an advertiser ke")
            return
        }
        
        guard let advertiser = serviceAdvertisers.first(where: { $0.id == key }) else  {
            call.reject("Unknown advertiser key")
            return
        }
        
        advertiser.stopAdvertisingPeer()
        
        serviceAdvertisers = serviceAdvertisers.filter { $0 !== advertiser }
        
        call.resolve()
    }
    
    @objc func startBrowse(_ call: CAPPluginCall) {
        CAPLog.print("startBrowse called")
        let displayName = call.getString("displayName")
        
        guard let serviceType = call.getString("serviceType") else {
            call.reject("Must provide a service type")
            return
        }
        
        let ignoreLocalDevice = call.getBool("ignoreLocalDevice") ?? true
        if (ignoreLocalDevice){
            CAPLog.print("ignore local device")
        }
        let browser = Browser(displayName: displayName, serviceType: serviceType, ignoreLocalDevice: ignoreLocalDevice)
        browser.delegate = self

        let uuid = UUID().uuidString
        
        serviceBrowsers[uuid] = browser
        
        browser.startBrowsingForPeers()
        
        call.resolve(["id": uuid])
    }
    
    
    /// Stops the search for nearby devices.
    ///
    /// You can no longer connect to a peer after this method has been executed.
    @objc func stopBrowse(_ call: CAPPluginCall) {
        
        CAPLog.print("stopBrowse called")
        
        guard let browserObj = call.getObject("browser") else {
            call.reject("Must provide a browser")
            return
        }
        
        guard let key = browserObj["id"] as? String else {
            call.reject("Must provide a browser key")
            return
        }
        
        guard let browser = serviceBrowsers[key] else {
            call.reject("Unknown browser key")
            return
        }
        
        browser.stopBrowsingForPeers()
        
        // Connect(ing) to nearby devices does not work after this call:
        serviceBrowsers[key] = nil
        
        call.resolve()
    }
    
    @objc func connect(_ call: CAPPluginCall) {
     
        CAPLog.print("connect called")
        
        guard let browserObj = call.getObject("browser"),
              let key = browserObj["id"] as? String,
              let browser = serviceBrowsers[key] else {
            
            call.reject("Must provide a browser")
            return
        }

        guard let peerObj = call.getObject("peer"),
              let key = peerObj["id"] as? String else {
            
            call.reject("Must provide a peer")
            return
        }

        if !browser.connect(peerId: key) {
            
            call.reject("Cannot connect to peer")
            return
        }
        
        let id = addOrGetSession(session: browser.session!)
        
        call.resolve(["id": id])
    }
    
    @objc func disconnect(_ call: CAPPluginCall) {
     
        CAPLog.print("disconnect called")
        
        guard let sessionObj = call.getObject("session"),
              let key = sessionObj["id"] as? String,
              let session = sessions[key] else {
            
            call.reject("Must provide a valid session")
            return
        }
        
        session.disconnect()
        sessions[key] = nil
        
        call.resolve()
    }
    
    @objc func send(_ call: CAPPluginCall) {
        
        CAPLog.print("send called")
        
        guard let sessionObj = call.getObject("session"),
              let key = sessionObj["id"] as? String,
              let session = sessions[key] else {
            
            call.reject("Must provide a valid session")
            return
        }
        
        guard let message = call.getString("message") else {
            
            call.reject("Must provide a message")
            return
        }
        
        do {
            try session.send(message.data(using: .utf8)!, toPeers: session.connectedPeers, with: .reliable)
        }
        catch {
            call.reject("send failed")
            return
        }
        
        call.resolve()
    }
    
 
    private func addOrGetSession(session: MCSession) -> String {
        
        if let existingSession = sessions.first(where: {$0.value === session}) {
        
            return existingSession.key
        }
        
        let uuid = UUID().uuidString
        sessions[uuid] = session
        
        session.delegate = self
        
        return uuid
    }
    
}

extension P2pConnectPlugin: BrowserDelegate {
    
    func peerStateChanged(key: String, displayName: String, found: Bool) {
        
        CAPLog.print("peerStateChanged \(key) \(displayName) \(found)")
        
        notifyListeners(found ? "peerFound" : "peerLost", data: ["id": key, "displayName": displayName])
    }
    
}

extension P2pConnectPlugin: AdvertiserDelegate {
    
    func advertiserConnected(advertiserId: String, session: MCSession) {
    
        CAPLog.print("advertiserConnected \(advertiserId)")
        
        let sessionId = addOrGetSession(session: session)
        
        notifyListeners("connect", data: ["advertiser": ["id": advertiserId], "session": ["id": sessionId]])
    }
    
}

extension P2pConnectPlugin: MCSessionDelegate {
    
    public func session(_ session: MCSession, peer peerID: MCPeerID, didChange state: MCSessionState) {
        
        // let s = sessions.first(where: {$0.value === session})!
        
        // var jsState: String
        // switch state {
        // case .notConnected:
        //     jsState = "notConnected"
        // case .connecting:
        //    jsState = "connecting"
        //case .connected:
        //    jsState = "connected"
        //@unknown default:
        //    CAPLog.print(Log.Fatal, self.pluginId, "-", "unknown MCSessionState")
        //    jsState = "unknown"
        //}
        
        //notifyListeners("sessionStateChange", data: [
        //    "session": ["id": s.key],
        //    "state": jsState
        //])
        
        
        //CAPLog.print("\(s.key) -> didChangeState \(state.rawValue)")
    }
    
    public func session(_ session: MCSession, didReceive data: Data, fromPeer peerID: MCPeerID) {

        CAPLog.print("didReceive from peer")
        if let item = sessions.first(where: {$0.value === session}) {
            
            let message = String(decoding: data, as: UTF8.self)
            
            notifyListeners("receive", data: [
                "session": ["id": item.key],
                "message": message
            ])
        }
    }
    
    public func session(_ session: MCSession, didReceive stream: InputStream, withName streamName: String, fromPeer peerID: MCPeerID) {
        
        CAPLog.print("didReceive withName")
    }
    
    public func session(_ session: MCSession, didStartReceivingResourceWithName resourceName: String, fromPeer peerID: MCPeerID, with progress: Progress) {
        
        CAPLog.print("didReceive withName & progress")
    }
    
    public func session(_ session: MCSession, didFinishReceivingResourceWithName resourceName: String, fromPeer peerID: MCPeerID, at localURL: URL?, withError error: Error?) {
        
        CAPLog.print("didFinishReceiving")
    }
    
    public func session(_ session: MCSession,
    didReceiveCertificate certificate: [Any]?,
                 fromPeer peerID: MCPeerID,
                 certificateHandler: @escaping (Bool) -> Void) {
        
        certificateHandler(true)
    }
    
}
