import Foundation
import MultipeerConnectivity
import Capacitor

protocol AdvertiserDelegate: AnyObject {

    func advertiserConnected(advertiserId: String, session: MCSession)

}

protocol SessionProtocol: AnyObject {
}

class Advertiser: NSObject {
    
    private var serviceAdvertiser: MCNearbyServiceAdvertiser
    
    weak var delegate: AdvertiserDelegate?
    
    private var localPeer: MCPeerID
    private var session: MCSession?
    
    var id: String
    
    init(displayName name: String?, serviceType: String) {

        id = UUID().uuidString
        
        let displayName = name ?? UIDevice.current.name
        
        localPeer = MCPeerID(displayName: displayName)
        
        serviceAdvertiser = MCNearbyServiceAdvertiser(peer: localPeer, discoveryInfo: ["instance": InstanceId], serviceType: serviceType)
    }
    
    func startAdvertisingPeer() {
        
        serviceAdvertiser.delegate = self
        
        serviceAdvertiser.startAdvertisingPeer()
    }
    
    func stopAdvertisingPeer() {
        serviceAdvertiser.stopAdvertisingPeer()
    }
    
}

extension Advertiser: MCNearbyServiceAdvertiserDelegate {
    
    func advertiser(_ advertiser: MCNearbyServiceAdvertiser, didReceiveInvitationFromPeer peerID: MCPeerID, withContext context: Data?, invitationHandler: @escaping (Bool, MCSession?) -> Void) {
        
        session = MCSession(peer: localPeer, securityIdentity: nil, encryptionPreference: MCEncryptionPreference.none)
        
        invitationHandler(true, session)
        
        serviceAdvertiser.stopAdvertisingPeer()
        
        delegate?.advertiserConnected(advertiserId: self.id, session: session!)
    }
}
