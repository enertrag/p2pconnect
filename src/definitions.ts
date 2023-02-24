import type { PluginListenerHandle } from '@capacitor/core';

/**
 * Describes a resource to be transferred. 
 */
export interface ResourceDescriptor {

  /** 
   * An identifier for the resource. 
   * This will be the same for sender and receiver.
   */
  id: string;
  /** 
   * The resource URI.
   * This must be an absolute URI. It will include a schema, depending of 
   * the target system. 
   * The path (especially the last part) will vary between sender and receiver.
   */
  uri: string;
}

/**
 * The result of a transmission process for the receiver.
 */
export interface TransferResult {

  /** The ID for the transfer process. */
  transferId: string;
  /** The list of the transferred resources. */
  resources: ResourceDescriptor[]

}

/** Defines the parameters of the sender to transfer files. */
export interface SendOptions {

  /** 
   * The identifier for the P2P process.
   * Only devices that use the same identifier can be found.  
   * To remain compatible with iOS devices, the identifier must meet the following criteria:
   * <ul>
   * <li>Must be 1–15 characters long</li>
   * <li>Can contain only ASCII lowercase letters, numbers, and hyphens</li>
   * <li>Must contain at least one ASCII letter</li>
   * <li>Must not begin or end with a hyphen</li>
   * <li>Must not contain hyphens adjacent to other hyphens.</li>
   * </ul>
   */
  serviceId: string;
  /** The ID for the transfer process. */
  transferId: string;
  /** The list of the resources to be transferred. */
  resources: ResourceDescriptor[];
}

/** Accepts or rejects a transfer. */
export interface AcceptTransferOptions {

  /** The ID for the transfer process. */
  transferId: string
  /** <code>true<code> to accept the transfer, <code>fale</code> otherwise. */
  accept: boolean
}

/** Defines the parameters for receiving a transfer. */
export interface ReceiveOptions {
  /** 
    * The identifier for the P2P process.
    * Only devices that use the same identifier can be found.  
    * To remain compatible with iOS devices, the identifier must meet the following criteria:
    * <ul>
    * <li>Must be 1–15 characters long</li>
    * <li>Can contain only ASCII lowercase letters, numbers, and hyphens</li>
    * <li>Must contain at least one ASCII letter</li>
    * <li>Must not begin or end with a hyphen</li>
    * <li>Must not contain hyphens adjacent to other hyphens.</li>
    * </ul>
    */
  serviceId: string;
}

/**
 * Describes the possible error sources of the send operation.
 */
export enum SendError {

  /** The transfer was interrupted by either sender or receiver. */
  transferInterrupted = 'transferInterrupted',
  /** The plugin version differs between sender and receiver. */
  versionMismatch = 'versionMismatch',
  /** The recipient has refused to receive the transfer. */
  transferDenied = 'transferDenied',
  /** The sending process was cancelled. */
  cancelled = 'cancelled',
  /** The user has not granted the requested permissions. */
  permissionDenied = 'permissionDenied',
  /** An internal error occured. Something went terribly wrong. */
  internalError = 'internalError'
}

/**
 * Describes a request to the recipient to confirm 
 * or decline acceptance of the transfer. 
 */
export interface AcceptTransferRequest {

  /** The ID of the transfer whose status is to be confirmed. */
  transferId: string;
}

/**
 * The methods of the peer-to-peer interface are described below.
 */
export interface P2pConnectPlugin {

  /**
   * Indicates whether the Peer to Peer function is available on the device.
   * 
   * @returns {boolean} asdfasdf
   * @since 1.0.0
   */
  isAvailable(): Promise<{ available: boolean }>;


  /**
   * Remove all native listeners for this plugin.
   *
   * @since 1.0.0
   */
  removeAllListeners(): Promise<void>;

  /**
   * The notification is triggered on the recipient's side when a new transfer 
   * is received that needs to be confirmed.
   */
  addListener(
    eventName: 'acceptTransfer',
    listenerFunc: (request: AcceptTransferRequest) => void
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Notification is triggered on the recipient's side 
   * when a transfer is complete.
   */
  addListener(
    eventName: 'transferComplete',
    listenerFunc: (result: TransferResult) => void
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /** Starts a transfer on the sender's side. */
  send(options: SendOptions): Promise<{ success: boolean, error: SendError | null }>;

  /** Activates the reception of a transfer on the recipient's side. */
  startReceive(options: ReceiveOptions): Promise<{ success: boolean }>;
  /** Cancels the reception on the receiver's side. */
  stopReceive(): Promise<{ success: boolean }>;

  /**
   * Must be called in response to the 'acceptTransfer' 
   * notification and determines on the recipient's side whether the 
   * transfer should be accepted or rejected.
   */
  acceptTransfer(options: AcceptTransferOptions): Promise<void>;
}
