import type { PluginListenerHandle } from '@capacitor/core';

export enum SessionState {

  NotConnected = 'notConnected',

  Connecting = 'connecting',

  Connected  = 'connected'

}

export interface Advertiser {

  id: string;
}

export interface Browser {

  id: string;
}

export interface Peer {

  id: string;

  displayName?: string;

}

export interface BrowseOptions {

  displayName?: string;

  serviceType: string;

  /**
   * Sets whether to ignore advertisements from your own device.
   */
  ignoreLocalDevice: boolean;
}

export interface Session {

  id: string;
}

export interface ConnectResult {

  advertiser: Advertiser;

  session: Session;

}

export interface SessionStateResult {

  session: Session;

  state: SessionState;
}

export interface ReceiveResult {

  session: Session;

  message: string;

  url: string;
}

export interface StartReceiveResult {

  session: Session;

  name: string;
}

export interface Progress {
  
  isFinished: boolean;

  isCancelled: boolean;

  fractionCompleted: number;

}

/**
 * asdfasdf asdfasdf
 */
export interface P2pConnectPlugin {

  /**
   * Indicates whether the Peer to Peer function is available on the device.
   * 
   * @returns {boolean} asdfasdf
   * @since 1.0.0
   */
  isAvailable(): Promise<{available: boolean}>;

  /**
   * Starts advertising the service offered by the local device.
   * 
   * @param options {{displayName?: string, serviceType: string}} 
   *        displayName The name under which the device is to be displayed. If no value is passed, the device name is used.
   * 
   *        serviceType must be a unique identifier. The maximum length is 15 characters. Valid characters are letters, numbers and dashes.
   */
  startAdvertise(options: { displayName?: string, serviceType: string }): Promise<Advertiser>;

  /**
   * Stops the advertising.
   * 
   * @param options {Advertiser} the advertiser to be stopped
   */
  stopAdvertise(options: { advertiser: Advertiser }): Promise<void>;

  /**
   * Starts the search for nearby devices that offer the specified service.
   * 
   * @param options 
   */
  startBrowse(options: BrowseOptions): Promise<Browser>

  /**
   * Stops searching for nearby devices.
   * 
   * After this method call, no more connections to found devices can be established.
   * 
   * @param options 
   */
  stopBrowse(options: { browser: Browser }): Promise<void>;

  /**
   * Connects to a nearby device.
   * 
   * @param options 
   */
  connect(options: { browser: Browser, peer: Peer }): Promise<Session>;

  /**
   * Disconnects from a nearby device.
   * 
   * @param options 
   */
  disconnect(options: { session: Session }): Promise<void>;

  /**
   * Sends a message to all connected devices in a session.
   */
  send(options: { session: Session, message: string }): Promise<void>;

  /**
   * Sends an (file or HTTP) URL to all connected devices in a session. Returns the id of the progress.
   */
  sendResource(options: { session: Session, peer: Peer, url: string, name: string }): Promise<{id: string}>;

  /**
   * Get the current progress a send or receive action
   * @param options 
   */
  getProgress(options: { id: string }): Promise<Progress>;

  /**
   * Indicates that a new device has been found nearby.
   * 
   * @since 1.0.0
   */
  addListener(
    eventName: 'peerFound',
    listenerFunc: (peer: Peer) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Indicates that a nearby device is no longer available.
   * 
   */
  addListener(
    eventName: 'peerLost',
    listenerFunc: (peer: Peer) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Indicated that the advertised device has been connected.
   */
  addListener(
    eventName: 'connect',
    listenerFunc: (result: ConnectResult) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Indicates that the state of the connection has changed.
   * 
   */
  addListener(
    eventName: 'sessionStateChange',
    listenerFunc: (result: SessionStateResult) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Indicates that receiving data has been started.
   */
   addListener(
    eventName: 'startReceive',
    listenerFunc: (result: StartReceiveResult) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Indicates that a message or url has been received.
   */
  addListener(
    eventName: 'receive',
    listenerFunc: (result: ReceiveResult) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Remove all native listeners for this plugin.
   *
   * @since 1.0.0
   */
  removeAllListeners(): Promise<void>;
}
