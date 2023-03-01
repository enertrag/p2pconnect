//
//  TransferProgressModel.swift
//  Plugin
//
//  Created by Philipp Anné on 16.02.23.
//  Copyright © 2023 Max Lynch. All rights reserved.
//

import Foundation

class TransferProgressModel: ObservableObject {
    
    @Published var progress: Double
    @Published var title: String
    @Published var message: String
    
    init(_ title: String) {
        self.title = title
        self.progress = 0.0
        self.message = "Connecting..." // String(localized: "message_connect")
    }
}
