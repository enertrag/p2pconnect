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
    
    private var progress: [String:Progress] = [:]
    
    private var receiver: Receiver!
    private var sender: Sender!
    
    override public func load() {
        CAPLog.print("asdfasdf")
        
        receiver = Receiver(self)
        sender = Sender(self)
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
        serviceBrowsers.removeValue(forKey: key)
        
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
        
        CAPLog.print("Connect new SessionID: \(id)")
        
        call.resolve(["id": id])
    }
    
    private func addOrGetSession(session: MCSession) -> String {
        
        if let existingSession = sessions.first(where: {$0.value === session}) {
            CAPLog.print("addOrGetSession:> existingSession:\(existingSession.key)")
            return existingSession.key
        }
        
        let uuid = UUID().uuidString
        sessions[uuid] = session
        
        session.delegate = self
        
        return uuid
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
        sessions.removeValue(forKey: key)
        
        call.resolve()
    }
    
    @objc func send2(_ call: CAPPluginCall) {
        
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
 
    @objc func sendResource(_ call: CAPPluginCall) {
        
        CAPLog.print("send binary called")
        CAPLog.print("send binary called => SESSIONS: \(sessions)")
        
        
        guard let sessionObj = call.getObject("session"),
              let sessionId = sessionObj["id"] as? String,
              let session = sessions[sessionId] else {
            
            call.reject("Must provide a valid session")
            return
        }
        
        CAPLog.print("sendResource found sessionID: \(sessionId)")
        
        guard let url = call.getString("url") else {
            
            call.reject("Must provide an url")
            return
        }
        
        guard let name = call.getString("name") else {
            
            call.reject("Must provide a name")
            return
        }
        
        guard let peerObj = call.getObject("peer"),
            let displayName = peerObj["displayName"] as? String else {
            
            call.reject("Must provide a peer")
            return
        }
        
        guard let urlToSend = URL(string: url) else{
            call.reject("Must provide a valid url")
            return
        }
        
        CAPLog.print("sendResource:> urlToSend:\(urlToSend) name:\(name) peerDisplayName:\(displayName) session: \(session)")
        
        guard let peerId = session.connectedPeers.first(where: {$0.displayName == displayName}) else{
            call.reject("Peer not connected")
            
            return
        }
    
        CAPLog.print("sendResource:> peerId:\(peerId)")
        
        let progressRequest = session.sendResource(at: urlToSend, withName: name, toPeer: peerId, withCompletionHandler: nil)
        
        let uuid = UUID().uuidString
        progress[uuid] = progressRequest
        
        call.resolve(["id": uuid])
    }
 
    @objc func getProgress(_ call: CAPPluginCall) {
        
        CAPLog.print("getProgress called")
        
        guard let id = call.getString("id") else{
            call.reject("Must provide an id for the progress")
            return
        }
        
        guard let p = progress[id] else {
            
            call.reject("Progress unkown")
            return
        }
    
        call.resolve(["isFinished": p.isFinished, "isCancelled": p.isCancelled, "fractionCompleted": p.fractionCompleted])
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
        
        guard let s = sessions.first(where: {$0.value === session}) else {
            return
        }
           
        var jsState: String
        switch state {
        case .notConnected:
            jsState = "notConnected"
        case .connecting:
           jsState = "connecting"
       case .connected:
           jsState = "connected"
       @unknown default:
           CAPLog.print(Log.Fatal, self.pluginId, "-", "unknown MCSessionState")
           jsState = "unknown"
       }
       
       notifyListeners("sessionStateChange", data: [
           "session": ["id": s.key],
           "state": jsState
       ])
       
       CAPLog.print("\(s.key) -> didChangeState \(state.rawValue)")
        
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
        
        if let item = sessions.first(where: {$0.value === session}) {
            
            notifyListeners("startReceive", data: [
                "session": ["id": item.key],
                "name": resourceName
            ])
        }
        
        CAPLog.print("didReceive withName & progress")
    }
    
    public func session(_ session: MCSession, didFinishReceivingResourceWithName resourceName: String, fromPeer peerID: MCPeerID, at localURL: URL?, withError error: Error?) {
        
        if let item = sessions.first(where: {$0.value === session}) {
            
            let url = localURL?.absoluteString
            
            notifyListeners("receive", data: [
                "session": ["id": item.key],
                "url": url
            ])
        }
       
        CAPLog.print("didFinishReceiving")
    }
    
    public func session(_ session: MCSession,
    didReceiveCertificate certificate: [Any]?,
                 fromPeer peerID: MCPeerID,
                 certificateHandler: @escaping (Bool) -> Void) {
        
        certificateHandler(true)
    }
    
}
