//
//  Sender.swift
//  Plugin
//
//  Created by Philipp Anné on 15.02.23.
//  Copyright © 2023 ENERTRAG SE. All rights reserved.
//

import Foundation
import Capacitor
import MultipeerConnectivity
import SwiftUI

enum SenderState {
    
    case none
    case browsing
    case waitingForVersion
    case waitingForTransferId
    case waitingForCount
    case transmitting
    
}

struct ResourceDescriptor {
    
    var id: String
    var uri: URL
    
}


class Sender: NSObject {
    
    
    var state: SenderState = .none
    
    var plugin: CAPPlugin
    
    var callbackId: String?
    var transferId: String?
    var resources: [ResourceDescriptor]?
    
    static let NOT_STARTED = -1
    var currentResourceTransferring = NOT_STARTED
    
    var session: MCSession?
    
    var progressModel = TransferProgressModel("")
    var progressView: UIViewController?
    
    var progressObservation: NSKeyValueObservation?
    
    init(_ plugin: CAPPlugin) {
        
        self.plugin = plugin
    }

    func start(_ serviceId: String, transferId: String, resources: [ResourceDescriptor], callbackId: String) {
        
        self.state = .browsing
        
        self.callbackId = callbackId
        self.transferId = transferId
        self.resources = resources
        
        let peerId = MCPeerID(displayName: UIDevice.current.name)
        session = MCSession(peer: peerId)
        
        session!.delegate = self
        
        let view = MCBrowserViewController(serviceType: serviceId, session: session!)
        view.delegate = self
        view.minimumNumberOfPeers = 2
        view.maximumNumberOfPeers = 2
        
        DispatchQueue.main.async {
            
            self.plugin.bridge?.viewController?.present(view, animated: true)
        }
    }
    
    func cleanUp() {
        
        CAPLog.print("🐇  CleanUp is called")
        
        session?.disconnect()
        session = nil
        
        DispatchQueue.main.async {
            
            CAPLog.print("🐇  Dismissing progress view")
            
            self.progressView?.dismiss(animated: true) {
                CAPLog.print("🐇  Dismissing progress view completed")
            }
            self.progressView = nil
        }
    }
    
    func resolveCall() {
        
        CAPLog.print("🐇  Resolving call")
        
        cleanUp()
        
        state = .none
        
        let call = self.plugin.bridge?.savedCall(withID: callbackId!)
        call?.resolve(["success": true])
        
        self.plugin.bridge?.releaseCall(call!)
    }
    
    func failCall(withError error: String) {
        
        CAPLog.print("🐇  Failing call")
        
        cleanUp()
        
        let call = self.plugin.bridge?.savedCall(withID: callbackId!)
        call?.resolve(["success": false, "error": error])
        
        self.plugin.bridge?.releaseCall(call!)
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
            
            self.plugin.bridge?.viewController?.present(self.progressView!, animated: true) {
                CAPLog.print("🐇  Created progress view")
                
                onCompletion()
            }
        }
    }
    
    func updateProgress(title: String? = nil, progress: Double) {
        
        CAPLog.print("🐇  updateProgress \(title ?? "nil") -> \(progress)")
        
        DispatchQueue.main.async {
            
            if let title = title {
                self.progressModel.title = title
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
    
    
    func sendTransferId() {
        
        updateProgress(progress: 0.5)
        sendString("tid.\(transferId!)")
        
        state = .waitingForTransferId
    }
    
    func sendVersion() {

        updateProgress(title: "🤝", progress: 0.25)
        sendString("ver.1")
        
        state = .waitingForVersion
    }
    
    func sendFileCount() {
        
        updateProgress(progress: 0.75)
        sendString("cnt.\(resources!.count)")
        
        state = .waitingForCount
    }
    
    func cancelSend(withError error: String) {
        
        state = .none

        failCall(withError: error)
    }
    
    func sendNextResource() {
        
        CAPLog.print("🐇  sendNextRessource (\(currentResourceTransferring))")
        
        if currentResourceTransferring == Sender.NOT_STARTED {
            currentResourceTransferring = 0
        } else if currentResourceTransferring < resources!.count - 1 {
            currentResourceTransferring += 1
        } else {
            
            
            resolveCall()
            return
        }
        
        updateProgress(title: "📂 \(currentResourceTransferring + 1)/\(resources!.count)", progress: 0.0)
        
        let res = resources![currentResourceTransferring]
        
        let progress = session!.sendResource(at: res.uri, withName: res.id, toPeer: session!.connectedPeers.first!) { error in
            
            self.progressObservation?.invalidate()
            self.progressObservation = nil
            
            if let _ = error {
                self.cancelSend(withError: "transferInterrupted")
                return
            }
            
            self.sendNextResource()
        }
        
        progressObservation = progress!.observe(\.fractionCompleted) { progress, _ in
          
            self.updateProgress(progress: progress.fractionCompleted)
        }
    }
}



extension Sender: MCBrowserViewControllerDelegate {
    
    
    func browserViewControllerDidFinish(_ browserViewController: MCBrowserViewController) {

        CAPLog.print("finished browser")
        
        browserViewController.dismiss(animated: true)

        createProgressView() {
            
            self.sendVersion()
        }
    }
    
    func browserViewControllerWasCancelled(_ browserViewController: MCBrowserViewController) {
        
        CAPLog.print("cancelled browser")
        
        browserViewController.dismiss(animated: true)
        
        failCall(withError: "cancelled")
    }
}

extension Sender: MCSessionDelegate {
    
    func session(_ session: MCSession, peer peerID: MCPeerID, didChange state: MCSessionState) {
        CAPLog.print("🐇  \(peerID.displayName) didChange state \(state)")
        
        if state == .notConnected {
            self.cleanUp()
        }
        
    }
    
    func session(_ session: MCSession, didReceive data: Data, fromPeer peerID: MCPeerID) {
        
        let command = String(decoding: data, as: UTF8.self)
        CAPLog.print("🐇  didReceive from \(peerID.displayName) data = \(command)")

        switch state {
            
        case .waitingForVersion:
                
            if command == "ver.accept" {
        
                sendTransferId()
                
            } else { // == "ver.deny"
                
                cancelSend(withError: "versionMismatch")

            }
        case .waitingForTransferId:
            
            if command == "tid.accept" {
                
                sendFileCount()
                
            } else { // == "tid.deny"
                
                cancelSend(withError: "transferDenied")
            }
            
        case .waitingForCount:
            
            if command == "cnt.accept" {
                
                state = .transmitting
                currentResourceTransferring = Sender.NOT_STARTED
                
                sendNextResource()
                
            } else { // should never happen
                
                cancelSend(withError: "internalError")
            }
            
        default:
            CAPLog.print("invalid state") // received data while not waiting for it
            
            cancelSend(withError: "internalError")
        }
        
        
    }
    
    func session(_ session: MCSession, didReceive stream: InputStream, withName streamName: String, fromPeer peerID: MCPeerID) {
        
    }
    
    func session(_ session: MCSession, didStartReceivingResourceWithName resourceName: String, fromPeer peerID: MCPeerID, with progress: Progress) {
        
    }
    
    func session(_ session: MCSession, didFinishReceivingResourceWithName resourceName: String, fromPeer peerID: MCPeerID, at localURL: URL?, withError error: Error?) {
        
    }
    
    
    
    
    
}
