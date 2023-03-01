//
//  TransferProgressView.swift
//  Plugin
//
//  Created by Philipp Anné on 16.02.23.
//  Copyright © 2023 ENERTRAG SE All rights reserved.
//

import SwiftUI

struct TransferProgressView: View {
    
    @ObservedObject var viewModel: TransferProgressModel
    
    var body: some View {
        
        ZStack {
         
            Rectangle()
                .foregroundColor(.secondary)
                .background(.ultraThinMaterial)
            
            VStack {
                
                Spacer()
                
                    
                    VStack() {
                        Text(viewModel.title)
                            .font(.title)
                            .padding()
                        ProgressView(value: viewModel.progress)
                            .padding()
                        Text(viewModel.message)
                            .font(.footnote)
                            .padding()
                    }
                    .background(Color(UIColor.systemBackground))
                    .frame(width: 320)
                    .cornerRadius(8.0)
                
                
                Spacer()
            }
         
        }
    }
        
        
}

