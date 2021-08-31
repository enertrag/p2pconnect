import Foundation
import MultipeerConnectivity
import Capacitor

protocol BrowserDelegate: AnyObject {
    
    func peerStateChanged(key: String, displayName: String, found: Bool)
    
}

class Browser: NSObject {
    
    private var serviceBrowser: MCNearbyServiceBrowser
    
    private var detectedPeers: [String:MCPeerID] = [:]
    private var ignoreLocalDevice: Bool
    
    var session: MCSession?
    
    weak var delegate: BrowserDelegate?
    
    private var localPeer: MCPeerID

    init(displayName name: String?, serviceType: String, ignoreLocalDevice localIgnore: Bool) {
        
        let displayName = name ?? UIDevice.current.name
        
        ignoreLocalDevice = localIgnore
        
        localPeer = MCPeerID(displayName: displayName)
        
        serviceBrowser = MCNearbyServiceBrowser(peer: localPeer, serviceType: serviceType)
    }

    func startBrowsingForPeers() {
        
        detectedPeers = [:]
        serviceBrowser.delegate = self
        
        serviceBrowser.startBrowsingForPeers()
    }

    func stopBrowsingForPeers() {
        
        serviceBrowser.stopBrowsingForPeers()
        detectedPeers.removeAll()
    }
    
    func connect(peerId: String) -> Bool {
        
        guard let peer = detectedPeers[peerId] else {
            
            CAPLog.print("peer Id not found \(peerId)")
            return false
        }
        
        if session == nil {
            session = MCSession(peer: localPeer, securityIdentity: nil, encryptionPreference: MCEncryptionPreference.none)
        }
        
        serviceBrowser.invitePeer(peer, to: session!, withContext: nil, timeout: 0)
        
        return true
    }
}

extension Browser: MCNearbyServiceBrowserDelegate {
    
    func browser(_ browser: MCNearbyServiceBrowser, foundPeer peerID: MCPeerID, withDiscoveryInfo info: [String : String]?) {
        CAPLog.print("FoundPeer \(peerID)")
        
        if ignoreLocalDevice, let info = info?["instance"] {
            
            if info == InstanceId {
                return
            }
            
        }
        
        let key = UUID().uuidString
        
        detectedPeers[key] = peerID
        
        delegate?.peerStateChanged(key: key, displayName: peerID.displayName, found: true)
    }
    
    func browser(_ browser: MCNearbyServiceBrowser, lostPeer peerID: MCPeerID) {
        
        if let item = detectedPeers.first(where: { $0.value == peerID }) {

            detectedPeers.removeValue(forKey: item.key)
            
            delegate?.peerStateChanged(key: item.key, displayName: item.value.displayName, found: false)
        }
    }
    
}

