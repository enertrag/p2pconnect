//
//  Receiver.swift
//  Plugin
//
//  Created by Philipp Anné on 15.02.23.
//  Copyright © 2023 ENERTRAG SE. All rights reserved.
//

import Foundation
import Capacitor
import MultipeerConnectivity
import SwiftUI

enum ReceiverState {
    
    case none
    case advertising
    case waitingForTransferId
    case waitingForAccept
    case waitingForCount
    case receiving
    
}



class Receiver: NSObject {
    
    static let NOTHING_RECEIVED = -1
    
    var state: ReceiverState = .none
    
    var plugin: CAPPlugin
    
    var advertiser: MCNearbyServiceAdvertiser?
    var session: MCSession?
    
    var progressModel = TransferProgressModel("")
    var progressView: UIViewController?
    
    var numberOfResourcesToReceive = 0
    var currentResourceNumber = NOTHING_RECEIVED
    var currentTransferId = ""
    
    var progressObservation: NSKeyValueObservation?
    
    var resources: [ResourceDescriptor] = []

    
    init(_ plugin: CAPPlugin) {
        
        self.plugin = plugin
    }
    
    func start(_ serviceId: String) {
        
        CAPLog.print("🐇  Starting advertiser")
        
        let peerId = MCPeerID(displayName: UIDevice.current.name)

        session = MCSession(peer: peerId)
        session!.delegate = self
        
        advertiser = MCNearbyServiceAdvertiser(peer: peerId,
                                                discoveryInfo: nil,
                                                serviceType: serviceId)
        advertiser!.delegate = self
        advertiser!.startAdvertisingPeer()
        
        state = .advertising
        
        numberOfResourcesToReceive = 0
        currentTransferId = ""
        currentResourceNumber = Receiver.NOTHING_RECEIVED
        
        resources = []
    }
    
    func stop() {
        
        CAPLog.print("🐇  Stopping advertiser")
        
        state = .none
        
        dismissAdvertiser() // should/could be cleanUp?
    }
    
    func cleanUp(dismissSession: Bool = false) {
        
        CAPLog.print("🐇  cleaning up")

        state = .none
        
        dismissAdvertiser()
        
        if dismissSession {
            session?.disconnect()
            session = nil
        }
        
        DispatchQueue.main.async {
            
            self.progressView?.dismiss(animated: true) {
                CAPLog.print("🐇  dismissed progress view")
            }
            self.progressView = nil
        }
    }
    
    func createProgressView(withCompletionHandler onCompletion: @escaping () -> Void) {
        
        progressModel.progress = 0.0
        progressModel.title = "🔗"
        
        DispatchQueue.main.async {
            
            CAPLog.print("🐇  Creating progress view")
            
            self.progressView = UIHostingController(rootView: TransferProgressView(viewModel: self.progressModel))
            
            self.progressView!.modalPresentationStyle = UIModalPresentationStyle.overCurrentContext
            self.progressView!.modalTransitionStyle = UIModalTransitionStyle.crossDissolve //crossDissolve
            self.progressView!.view?.backgroundColor = .clear
            
            if let plugin = self.plugin as? P2pConnectPlugin {
                plugin.present(self.progressView!, animated: true) {
                    CAPLog.print("🐇  Created progress view")
                    
                    onCompletion()
                }
            } else {
                self.plugin.bridge?.viewController?.present(self.progressView!, animated: true) {
                    CAPLog.print("🐇  Created progress view")
                    
                    onCompletion()
                }
            }
        }
    }
    
    func updateProgress(title: String? = nil, progress: Double, message: String? = nil) {
        
        CAPLog.print("🐇  updateProgress \(title ?? "nil") -> \(progress)")
        
        DispatchQueue.main.async {
            
            if let title = title {
                self.progressModel.title = title
            }
            if let message = message {
                self.progressModel.message = message
            }
            
            self.progressModel.progress = progress
        }
    }
    
    func sendString(_ value: String) {
        
        let data: Data? = value.data(using: .utf8)
        
        CAPLog.print("🐇  sendString = \(value)")
        
        do {
            try session!.send(data!, toPeers: session!.connectedPeers, with: .reliable)
        } catch {
            CAPLog.print("🐙  session send failed")
        }
    }
    
    
    
    func acceptTransferId() {
        
        state = .waitingForCount
        
        updateProgress(progress:  1.0)
        sendString("tid.accept")
    }

    func denyTransferIdAndCleanUp() {
        
        state = .none
        
        updateProgress(progress:  1.0)
        sendString("tid.deny")
        
        cleanUp()
    }

    func acceptVersion() {

        state = .waitingForTransferId
        
        updateProgress(progress: 0.75)
        sendString("ver.accept")
    }
    
    func denyVersionAndCleanUp() {
        
        updateProgress(progress:  1.0)
        sendString("ver.deny")
        
        cleanUp()
    }
    
    func acceptCount() {

        state = .receiving

        currentResourceNumber = Receiver.NOTHING_RECEIVED
        
        updateProgress(title: "📂 0/\(numberOfResourcesToReceive)", progress:  0.0, message: "Transferring..." /* String(localized: "message_send") */)
        sendString("cnt.accept")
    }
    
    func dismissAdvertiser() {
        
        guard advertiser != nil else {
            return
        }
        
        CAPLog.print("🐇  dismissing advertiser")
        
        advertiser?.stopAdvertisingPeer()
        advertiser = nil
    }
    
    @objc func cancelAcceptTransfer(_ transferId: String) {
        
        CAPLog.print("🐉  cancel transfer for \(transferId) (not yet implemented)")
        
        CAPLog.print("🐙  you MUST listen for acceptTransfer events and MUST call acceptTransfer within 30 seconds")
    }
    
    func acceptTransfer(_ accept: Bool, withTransferId transferId: String) {
        
        CAPLog.print("🐇  (\(accept)) accept transfer for transferId \(transferId)")

        guard state == .waitingForAccept else {
            
            CAPLog.print("🖐  not waiting for acceptance - ignoring call")
            return
        }
        
        DispatchQueue.main.async {

            CAPLog.print("🐇  cancel perfom request")
            NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(self.cancelAcceptTransfer), object: self.currentTransferId)
        }
        
        if transferId == currentTransferId && accept {
            
            acceptTransferId()
            
        } else {
            
            denyVersionAndCleanUp()
        }
    }
    
    func notifyCompletion() {
        
        CAPLog.print("🐇  notify completion")
        
        let serializableList = self.resources.map { r -> JSObject in
            
            var result = JSObject()
            result["id"] = r.id
            result["uri"] = r.uri.absoluteString
            
            return result
        }
        
        self.plugin.notifyListeners("transferComplete", data: [
            "transferId": self.currentTransferId,
            "resources": serializableList
        ])
    }
    
}

extension Receiver: MCNearbyServiceAdvertiserDelegate {
    
    func advertiser(_ advertiser: MCNearbyServiceAdvertiser,
                    didReceiveInvitationFromPeer peerID: MCPeerID,
                    withContext context: Data?,
                    invitationHandler: @escaping (Bool, MCSession?) -> Void) {
        
        CAPLog.print("🐇  accepting invitation from \(peerID.displayName)")
        invitationHandler(true, session)
    }
    
    func advertiser(_ advertiser: MCNearbyServiceAdvertiser,
                    didNotStartAdvertisingPeer error: Error) {
        
        CAPLog.print("🐇  advertiser failed to start: \(error)")
    }
}

extension Receiver: MCSessionDelegate {
    
    func session(_ session: MCSession, peer peerID: MCPeerID, didChange state: MCSessionState) {
        CAPLog.print("🐇  \(peerID.displayName) didChange state \(state)")

        if state == .notConnected {
        
            CAPLog.print("🐇  disconnecting local session")
            self.session?.disconnect()
            self.session = nil
        }
    }
    
    func session(_ session: MCSession, didReceive data: Data, fromPeer peerID: MCPeerID) {
        
        let input = String(decoding: data, as: UTF8.self)
        
        CAPLog.print("🐇  didReceive data from \(peerID.displayName): \(input)")
        
        dismissAdvertiser()
        
        let command = input.prefix(4)
        
        switch (state, command) {
            
        case (.advertising, "ver."):
            
            let version = input.dropFirst(4)
        
            guard version == "1" else {
                denyVersionAndCleanUp()
                return
            }
                
            createProgressView() {
                self.updateProgress(title: "🤝", progress: 0.0, message: "Synchronizing..." /* String(localized: "message_sync") */)
                self.acceptVersion()
            }
            
        case (.waitingForTransferId, "tid."):
                
            let transferId = input.dropFirst(4)
            
            CAPLog.print("🐇  notify listeners about transferId \(transferId)")
            
            state = .waitingForAccept
            currentTransferId = String(transferId)
            
            DispatchQueue.main.async {
                self.perform(#selector(self.cancelAcceptTransfer), with: self.currentTransferId, afterDelay: 30)
            }
                
            self.plugin.notifyListeners("acceptTransfer", data: [
                "transferId": String(transferId)
            ])
            
        case (.waitingForCount, "cnt."):
         
            let count = Int(input.dropFirst(4)) ?? 0
            
            CAPLog.print("🐇  awaiting \(count) resources")
            
            numberOfResourcesToReceive = count
            acceptCount()
            
        default:
            
            CAPLog.print("🐙  invalid state/command")
            cleanUp(dismissSession: true)
        }
        
    }
    
    func session(_ session: MCSession, didReceive stream: InputStream, withName streamName: String, fromPeer peerID: MCPeerID) {
    }
    
    func session(_ session: MCSession, didStartReceivingResourceWithName resourceName: String, fromPeer peerID: MCPeerID, with progress: Progress) {

        currentResourceNumber += 1
        
        self.updateProgress(title: "📂 \(currentResourceNumber + 1)/\(numberOfResourcesToReceive)", progress: 0.0)
        
        guard currentResourceNumber < numberOfResourcesToReceive else {
            
            CAPLog.print("🐙  too many resources sent")
            cleanUp(dismissSession: true)
            return
        }
        
        progressObservation = progress.observe(\.fractionCompleted) { progress, _ in
          
            self.updateProgress(progress: progress.fractionCompleted)
        }

        
    }
    
    func session(_ session: MCSession, didFinishReceivingResourceWithName resourceName: String, fromPeer peerID: MCPeerID, at localURL: URL?, withError error: Error?) {
        
        progressObservation?.invalidate()
        progressObservation = nil
        
        if let error = error {
            
            CAPLog.print("🐙  transmission error: \(error.localizedDescription)")
            cleanUp(dismissSession: true)
            return
        }
        
        var newURL = FileManager().temporaryDirectory
        newURL.appendPathComponent(resourceName)
        
        CAPLog.print("🐇  received resource \(localURL!)")
        CAPLog.print("🐇  moving resource to \(newURL)")
        
        do {
            if FileManager.default.fileExists(atPath: newURL.path) {
                try FileManager.default.removeItem(atPath: newURL.path)
            }
            try FileManager.default.moveItem(atPath: localURL!.path, toPath: newURL.path)
                
            CAPLog.print("🐇  received resource moved to \(newURL)")
            
            resources.append(ResourceDescriptor(id: resourceName, uri: newURL))

        } catch {

            CAPLog.print("🐙  io error: \(error.localizedDescription)")
            cleanUp(dismissSession: true)
            return
        }
        
        if currentResourceNumber >= numberOfResourcesToReceive - 1 {
            
            CAPLog.print("🐇  transmission complete")
            cleanUp()
            
            notifyCompletion()
            
            return
        }
    }
    
}
