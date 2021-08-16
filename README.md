# @enertrag/p2pconnect

Capacitor Peer to Peer connectivity plugin

## Install

```bash
npm install @enertrag/p2pconnect
npx cap sync
```

## API

<docgen-index>

* [`isAvailable()`](#isavailable)
* [`startAdvertise(...)`](#startadvertise)
* [`stopAdvertise(...)`](#stopadvertise)
* [`startBrowse(...)`](#startbrowse)
* [`stopBrowse(...)`](#stopbrowse)
* [`connect(...)`](#connect)
* [`disconnect(...)`](#disconnect)
* [`send(...)`](#send)
* [`addListener(...)`](#addlistener)
* [`addListener(...)`](#addlistener)
* [`addListener(...)`](#addlistener)
* [`addListener(...)`](#addlistener)
* [`addListener(...)`](#addlistener)
* [`removeAllListeners()`](#removealllisteners)
* [Interfaces](#interfaces)
* [Enums](#enums)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

asdfasdf asdfasdf

### isAvailable()

```typescript
isAvailable() => any
```

Indicates whether the <a href="#peer">Peer</a> to <a href="#peer">Peer</a> function is available on the device.

**Returns:** <code>any</code>

**Since:** 1.0.0

--------------------


### startAdvertise(...)

```typescript
startAdvertise(options: { displayName?: string; serviceType: string; }) => any
```

Starts advertising the service offered by the local device.

| Param         | Type                                                        | Description                                                                                                                                                                                                                                     |
| ------------- | ----------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`options`** | <code>{ displayName?: string; serviceType: string; }</code> | displayName The name under which the device is to be displayed. If no value is passed, the device name is used. serviceType must be a unique identifier. The maximum length is 15 characters. Valid characters are letters, numbers and dashes. |

**Returns:** <code>any</code>

--------------------


### stopAdvertise(...)

```typescript
stopAdvertise(options: { advertiser: Advertiser; }) => any
```

Stops the advertising.

| Param         | Type                                                               | Description                  |
| ------------- | ------------------------------------------------------------------ | ---------------------------- |
| **`options`** | <code>{ advertiser: <a href="#advertiser">Advertiser</a>; }</code> | the advertiser to be stopped |

**Returns:** <code>any</code>

--------------------


### startBrowse(...)

```typescript
startBrowse(options: BrowseOptions) => any
```

Starts the search for nearby devices that offer the specified service.

| Param         | Type                                                    |
| ------------- | ------------------------------------------------------- |
| **`options`** | <code><a href="#browseoptions">BrowseOptions</a></code> |

**Returns:** <code>any</code>

--------------------


### stopBrowse(...)

```typescript
stopBrowse(options: { browser: Browser; }) => any
```

Stops searching for nearby devices.

After this method call, no more connections to found devices can be established.

| Param         | Type                                                      |
| ------------- | --------------------------------------------------------- |
| **`options`** | <code>{ browser: <a href="#browser">Browser</a>; }</code> |

**Returns:** <code>any</code>

--------------------


### connect(...)

```typescript
connect(options: { browser: Browser; peer: Peer; }) => any
```

Connects to a nearby device.

| Param         | Type                                                                                      |
| ------------- | ----------------------------------------------------------------------------------------- |
| **`options`** | <code>{ browser: <a href="#browser">Browser</a>; peer: <a href="#peer">Peer</a>; }</code> |

**Returns:** <code>any</code>

--------------------


### disconnect(...)

```typescript
disconnect(options: { session: Session; }) => any
```

Disconnects from a nearby device.

| Param         | Type                                                      |
| ------------- | --------------------------------------------------------- |
| **`options`** | <code>{ session: <a href="#session">Session</a>; }</code> |

**Returns:** <code>any</code>

--------------------


### send(...)

```typescript
send(options: { session: Session; message: string; }) => any
```

Sends a message to all connected devices in a session.

| Param         | Type                                                                       |
| ------------- | -------------------------------------------------------------------------- |
| **`options`** | <code>{ session: <a href="#session">Session</a>; message: string; }</code> |

**Returns:** <code>any</code>

--------------------


### addListener(...)

```typescript
addListener(eventName: 'peerFound', listenerFunc: (peer: Peer) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

Indicates that a new device has been found nearby.

| Param              | Type                                                     |
| ------------------ | -------------------------------------------------------- |
| **`eventName`**    | <code>"peerFound"</code>                                 |
| **`listenerFunc`** | <code>(peer: <a href="#peer">Peer</a>) =&gt; void</code> |

**Returns:** <code>any</code>

**Since:** 1.0.0

--------------------


### addListener(...)

```typescript
addListener(eventName: 'peerLost', listenerFunc: (peer: Peer) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

Indicates that a nearby device is no longer available.

| Param              | Type                                                     |
| ------------------ | -------------------------------------------------------- |
| **`eventName`**    | <code>"peerLost"</code>                                  |
| **`listenerFunc`** | <code>(peer: <a href="#peer">Peer</a>) =&gt; void</code> |

**Returns:** <code>any</code>

--------------------


### addListener(...)

```typescript
addListener(eventName: 'connect', listenerFunc: (result: ConnectResult) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

Indicated that the advertised device has been connected.

| Param              | Type                                                                         |
| ------------------ | ---------------------------------------------------------------------------- |
| **`eventName`**    | <code>"connect"</code>                                                       |
| **`listenerFunc`** | <code>(result: <a href="#connectresult">ConnectResult</a>) =&gt; void</code> |

**Returns:** <code>any</code>

--------------------


### addListener(...)

```typescript
addListener(eventName: 'sessionStateChange', listenerFunc: (result: SessionStateResult) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

Indicates that the state of the connection has changed.

| Param              | Type                                                                                   |
| ------------------ | -------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>"sessionStateChange"</code>                                                      |
| **`listenerFunc`** | <code>(result: <a href="#sessionstateresult">SessionStateResult</a>) =&gt; void</code> |

**Returns:** <code>any</code>

--------------------


### addListener(...)

```typescript
addListener(eventName: 'receive', listenerFunc: (result: ReceiveResult) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

Indicates that a message has been received.

| Param              | Type                                                                         |
| ------------------ | ---------------------------------------------------------------------------- |
| **`eventName`**    | <code>"receive"</code>                                                       |
| **`listenerFunc`** | <code>(result: <a href="#receiveresult">ReceiveResult</a>) =&gt; void</code> |

**Returns:** <code>any</code>

--------------------


### removeAllListeners()

```typescript
removeAllListeners() => any
```

Remove all native listeners for this plugin.

**Returns:** <code>any</code>

**Since:** 1.0.0

--------------------


### Interfaces


#### Advertiser

| Prop     | Type                |
| -------- | ------------------- |
| **`id`** | <code>string</code> |


#### BrowseOptions

| Prop                    | Type                 | Description                                                 |
| ----------------------- | -------------------- | ----------------------------------------------------------- |
| **`displayName`**       | <code>string</code>  |                                                             |
| **`serviceType`**       | <code>string</code>  |                                                             |
| **`ignoreLocalDevice`** | <code>boolean</code> | Sets whether to ignore advertisements from your own device. |


#### Browser

| Prop     | Type                |
| -------- | ------------------- |
| **`id`** | <code>string</code> |


#### Peer

| Prop              | Type                |
| ----------------- | ------------------- |
| **`id`**          | <code>string</code> |
| **`displayName`** | <code>string</code> |


#### Session

| Prop     | Type                |
| -------- | ------------------- |
| **`id`** | <code>string</code> |


#### PluginListenerHandle

| Prop         | Type                      |
| ------------ | ------------------------- |
| **`remove`** | <code>() =&gt; any</code> |


#### ConnectResult

| Prop             | Type                                              |
| ---------------- | ------------------------------------------------- |
| **`advertiser`** | <code><a href="#advertiser">Advertiser</a></code> |
| **`session`**    | <code><a href="#session">Session</a></code>       |


#### SessionStateResult

| Prop          | Type                                                  |
| ------------- | ----------------------------------------------------- |
| **`session`** | <code><a href="#session">Session</a></code>           |
| **`state`**   | <code><a href="#sessionstate">SessionState</a></code> |


#### ReceiveResult

| Prop          | Type                                        |
| ------------- | ------------------------------------------- |
| **`session`** | <code><a href="#session">Session</a></code> |
| **`message`** | <code>any</code>                            |


### Enums


#### SessionState

| Members            | Value                       |
| ------------------ | --------------------------- |
| **`NotConnected`** | <code>'notConnected'</code> |
| **`Connecting`**   | <code>'connecting'</code>   |
| **`Connected`**    | <code>'connected'</code>    |

</docgen-api>
