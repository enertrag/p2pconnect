# @enertrag/p2pconnect

This Capacitor plugin enables file exchange between two devices. They do not need to be connected to the Internet and do not need to be on the same network.

The plugin uses peer-to-peer technologies of the respective platforms. Devices with iOS use MultipeerConnectivity, devices with Android use Google Nearby.

The plugin does not allow data exchange between devices of different systems (iOS vs. Android).

## Install

```bash
npm install @enertrag/p2pconnect
npx cap sync
```

## Sequence

The following flowchart describes the communication between sender and receiver.

```mermaid
sequenceDiagram
    actor Sender
    participant Plugin/Sender
    participant Plugin/Receiver
    actor Receiver
    Sender->>Plugin/Sender: send
    activate Sender

    activate Plugin/Sender
    Receiver->>Plugin/Receiver: start receive

    activate Receiver
    activate Plugin/Receiver
    Plugin/Sender-->>Plugin/Sender: browse
    Plugin/Receiver-->>Plugin/Receiver: advertise
    Plugin/Receiver-->>Receiver: ok
    deactivate Receiver

    Plugin/Sender-->>Plugin/Receiver: connect
    deactivate Plugin/Sender

    Plugin/Receiver-->>Plugin/Sender: accept
    activate Plugin/Sender

    Plugin/Sender->>Plugin/Receiver: check version
    deactivate Plugin/Sender
    Plugin/Receiver->>Plugin/Sender: accept version
    activate Plugin/Sender
    deactivate Plugin/Receiver

    Plugin/Sender->>Plugin/Receiver: check transfer id
    deactivate Plugin/Sender
    activate Plugin/Receiver

    Plugin/Receiver-->>Receiver: notify transfer id
    deactivate Plugin/Receiver
    activate Receiver
    Receiver->>Plugin/Receiver: accept transfer
    deactivate Receiver
    activate Plugin/Receiver


    Plugin/Receiver->>Plugin/Sender: accept transfer
    activate Plugin/Sender

    Plugin/Sender-->>Plugin/Receiver: transfer resource count
    Plugin/Sender-->>Plugin/Receiver: transfer ids (android only)
    deactivate Plugin/Receiver

    Plugin/Sender->>Plugin/Receiver: send resources
    activate Plugin/Receiver
    Plugin/Sender->>Plugin/Sender: send
    Plugin/Sender-->>Sender: done (iOS)
    deactivate Plugin/Sender

    Plugin/Receiver->>Receiver: transfer complete
    activate Receiver
    Plugin/Receiver-->>Plugin/Sender: ok (android)
    activate Plugin/Sender
    deactivate Plugin/Receiver
    Plugin/Sender-->>Sender: done (android)
    deactivate Plugin/Sender

    deactivate Sender
    Receiver-->>Receiver: move files
    deactivate Receiver
```

## API

<docgen-index>

- [`isAvailable()`](#isavailable)
- [`removeAllListeners()`](#removealllisteners)
- [`addListener(...)`](#addlistener)
- [`addListener(...)`](#addlistener)
- [`send(...)`](#send)
- [`startReceive(...)`](#startreceive)
- [`stopReceive()`](#stopreceive)
- [`acceptTransfer(...)`](#accepttransfer)
- [Interfaces](#interfaces)
- [Enums](#enums)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

asdfasdf asdfasdf

### isAvailable()

```typescript
isAvailable() => any
```

Indicates whether the Peer to Peer function is available on the device.

**Returns:** <code>any</code>

**Since:** 1.0.0

---

### removeAllListeners()

```typescript
removeAllListeners() => any
```

Remove all native listeners for this plugin.

**Returns:** <code>any</code>

**Since:** 1.0.0

---

### addListener(...)

```typescript
addListener(eventName: 'acceptTransfer', listenerFunc: (request: AcceptTransferRequest) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

The notification is triggered on the recipient's side when a new transfer
is received that needs to be confirmed.

| Param              | Type                                                                                          |
| ------------------ | --------------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>"acceptTransfer"</code>                                                                 |
| **`listenerFunc`** | <code>(request: <a href="#accepttransferrequest">AcceptTransferRequest</a>) =&gt; void</code> |

**Returns:** <code>any</code>

---

### addListener(...)

```typescript
addListener(eventName: 'transferComplete', listenerFunc: (result: TransferResult) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

Notification is triggered on the recipient's side
when a transfer is complete.

| Param              | Type                                                                           |
| ------------------ | ------------------------------------------------------------------------------ |
| **`eventName`**    | <code>"transferComplete"</code>                                                |
| **`listenerFunc`** | <code>(result: <a href="#transferresult">TransferResult</a>) =&gt; void</code> |

**Returns:** <code>any</code>

---

### send(...)

```typescript
send(options: SendOptions) => any
```

Starts a transfer on the sender's side.

| Param         | Type                                                |
| ------------- | --------------------------------------------------- |
| **`options`** | <code><a href="#sendoptions">SendOptions</a></code> |

**Returns:** <code>any</code>

---

### startReceive(...)

```typescript
startReceive(options: ReceiveOptions) => any
```

Activates the reception of a transfer on the recipient's side.

| Param         | Type                                                      |
| ------------- | --------------------------------------------------------- |
| **`options`** | <code><a href="#receiveoptions">ReceiveOptions</a></code> |

**Returns:** <code>any</code>

---

### stopReceive()

```typescript
stopReceive() => any
```

Cancels the reception on the receiver's side.

**Returns:** <code>any</code>

---

### acceptTransfer(...)

```typescript
acceptTransfer(options: AcceptTransferOptions) => any
```

Must be called in response to the 'acceptTransfer'
notification and determines on the recipient's side whether the
transfer should be accepted or rejected.

| Param         | Type                                                                    |
| ------------- | ----------------------------------------------------------------------- |
| **`options`** | <code><a href="#accepttransferoptions">AcceptTransferOptions</a></code> |

**Returns:** <code>any</code>

---

### Interfaces

#### AcceptTransferRequest

Describes a request to the recipient to confirm
or decline acceptance of the transfer.

| Prop             | Type                | Description                                             |
| ---------------- | ------------------- | ------------------------------------------------------- |
| **`transferId`** | <code>string</code> | The ID of the transfer whose status is to be confirmed. |

#### PluginListenerHandle

| Prop         | Type                      |
| ------------ | ------------------------- |
| **`remove`** | <code>() =&gt; any</code> |

#### TransferResult

The result of a transmission process for the receiver.

| Prop             | Type                | Description                            |
| ---------------- | ------------------- | -------------------------------------- |
| **`transferId`** | <code>string</code> | The ID for the transfer process.       |
| **`resources`**  | <code>{}</code>     | The list of the transferred resources. |

#### ResourceDescriptor

Describes a resource to be transferred.

| Prop      | Type                | Description                                                                                                                                                                          |
| --------- | ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **`id`**  | <code>string</code> | An identifier for the resource. This will be the same for sender and receiver.                                                                                                       |
| **`uri`** | <code>string</code> | The resource URI. This must be an absolute URI. It will include a schema, depending of the target system. The path (especially the last part) will vary between sender and receiver. |

#### SendOptions

| Prop             | Type                | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| ---------------- | ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`serviceId`**  | <code>string</code> | The identifier for the P2P process. Only devices that use the same identifier can be found. To remain compatible with iOS devices, the identifier must meet the following criteria: &lt;ul&gt; &lt;li&gt;Must be 1–15 characters long&lt;/li&gt; &lt;li&gt;Can contain only ASCII lowercase letters, numbers, and hyphens&lt;/li&gt; &lt;li&gt;Must contain at least one ASCII letter&lt;/li&gt; &lt;li&gt;Must not begin or end with a hyphen&lt;/li&gt; &lt;li&gt;Must not contain hyphens adjacent to other hyphens.&lt;/li&gt; &lt;/ul&gt; |
| **`transferId`** | <code>string</code> | The ID for the transfer process.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| **`resources`**  | <code>{}</code>     | The list of the resources to be transferred.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |

#### ReceiveOptions

Defines the parameters for receiving a transfer.

| Prop            | Type                | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| --------------- | ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`serviceId`** | <code>string</code> | The identifier for the P2P process. Only devices that use the same identifier can be found. To remain compatible with iOS devices, the identifier must meet the following criteria: &lt;ul&gt; &lt;li&gt;Must be 1–15 characters long&lt;/li&gt; &lt;li&gt;Can contain only ASCII lowercase letters, numbers, and hyphens&lt;/li&gt; &lt;li&gt;Must contain at least one ASCII letter&lt;/li&gt; &lt;li&gt;Must not begin or end with a hyphen&lt;/li&gt; &lt;li&gt;Must not contain hyphens adjacent to other hyphens.&lt;/li&gt; &lt;/ul&gt; |

#### AcceptTransferOptions

Accepts or rejects a transfer.

| Prop             | Type                 | Description                                                                                   |
| ---------------- | -------------------- | --------------------------------------------------------------------------------------------- |
| **`transferId`** | <code>string</code>  | The ID for the transfer process.                                                              |
| **`accept`**     | <code>boolean</code> | &lt;code&gt;true&lt;code&gt; to accept the transfer, &lt;code&gt;fale&lt;/code&gt; otherwise. |

### Enums

#### SendError

| Members                   | Value                              | Description                                                |
| ------------------------- | ---------------------------------- | ---------------------------------------------------------- |
| **`transferInterrupted`** | <code>'transferInterrupted'</code> | The transfer was interrupted by either sender or receiver. |
| **`versionMismatch`**     | <code>'versionMismatch'</code>     | The plugin version differs between sender and receiver.    |
| **`transferDenied`**      | <code>'transferDenied'</code>      | The recipient has refused to receive the transfer.         |
| **`cancelled`**           | <code>'cancelled'</code>           | The sending process was cancelled.                         |
| **`permissionDenied`**    | <code>'permissionDenied'</code>    | The user has not granted the requested permissions.        |
| **`internalError`**       | <code>'internalError'</code>       | An internal error occured. Something went terribly wrong.  |

</docgen-api>
